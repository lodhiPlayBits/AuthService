package com.lodhi.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lodhi.auth.model.Permission;
import com.lodhi.auth.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter

public class JwtService {

    private final SecretKey secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-ttl-second}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-ttl-second}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience
    ) {
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
        this.audience = audience;

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {

        Instant now = Instant.now();
        String jti= UUID.randomUUID().toString();

        // Extract roles
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .toList();

        // Extract all permissions from all roles (flatten and deduplicate)
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(issuer)
                .audience().add(audience).and()  // Specify intended audience
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusSeconds(accessTtlSeconds)
                ))
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("type", "access")
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user, String jti) {

        Instant now = Instant.now();

        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(issuer)
                .audience().add(audience).and()  // Specify intended audience
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses and validates a JWT token with comprehensive validation:
     * - Signature verification
     * - Issuer validation
     * - Audience validation
     * - Expiration validation
     * - Claims structural validation
     * 
     * @param token the JWT token string
     * @return parsed and validated JWT claims
     * @throws JwtException if any validation fails
     */
    public Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)      // Validate issuer matches expected value
                .requireAudience(audience)  // Validate audience matches expected value
                .build()
                .parseSignedClaims(token);
        // Note: Expiration is validated automatically by parseSignedClaims()
        // Token type validation happens in isAccessToken() / isRefreshToken()
    }
    
    /**
     * Validates that a token is an access token with proper structure.
     * 
     * @param claims the JWT claims
     * @return true if token is a valid access token
     * @throws JwtException if claims are invalid
     */
    public boolean isAccessToken(Claims claims) {
        String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
            return false;
        }
        
        // Validate required claims exist
        if (claims.getSubject() == null || claims.getId() == null) {
            throw new io.jsonwebtoken.JwtException("Access token missing required claims (sub or jti)");
        }
        
        // Validate roles and permissions claims exist and are lists
        Object rolesClaim = claims.get("roles");
        Object permissionsClaim = claims.get("permissions");
        
        if (!(rolesClaim instanceof List<?>)) {
            throw new io.jsonwebtoken.JwtException("Access token 'roles' claim must be a list");
        }
        
        if (!(permissionsClaim instanceof List<?>) && !(permissionsClaim instanceof Set<?>)) {
            throw new io.jsonwebtoken.JwtException("Access token 'permissions' claim must be a list or set");
        }
        
        return true;
    }

    /**
     * Validates that a token is a refresh token with proper structure.
     * 
     * @param claims the JWT claims
     * @return true if token is a valid refresh token
     * @throws JwtException if claims are invalid
     */
    public boolean isRefreshToken(Claims claims) {
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            return false;
        }
        
        // Validate required claims exist
        if (claims.getSubject() == null || claims.getId() == null) {
            throw new io.jsonwebtoken.JwtException("Refresh token missing required claims (sub or jti)");
        }
        
        return true;
    }

    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

}