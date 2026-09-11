package com.lodhi.auth.security;

import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-ttl-second}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-ttl-second}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer
    ) {
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {

        Instant now = Instant.now();

        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles()
                .stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        return Jwts.builder()
                .id(user.getId().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusSeconds(accessTtlSeconds)
                ))
                .claim("email", user.getEmail())
                .claim("roles", roles)
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
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusSeconds(refreshTtlSeconds)
                ))
                .claim("email", user.getEmail())
                .claim("type", "refresh")
                .signWith(secretKey)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    public boolean isAccessToken(String token) {

        Claims claims = parseToken(token).getPayload();

        return "access".equals(
                claims.get("type", String.class)
        );
    }

    public boolean isRefreshToken(String token) {

        Claims claims = parseToken(token).getPayload();

        return "refresh".equals(
                claims.get("type", String.class)
        );
    }

    public Long getUserId(String token) {

        Claims claims = parseToken(token).getPayload();

        return Long.parseLong(
                claims.getSubject()
        );
    }

    public String getJti(String token) {

        Claims claims = parseToken(token).getPayload();

        return claims.getId();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public String getIssuer() {
        return issuer;
    }
}