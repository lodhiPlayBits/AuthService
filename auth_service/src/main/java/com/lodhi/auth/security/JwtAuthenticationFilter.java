package com.lodhi.auth.security;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh"
    );

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Jws<Claims> parsed = jwtService.parseToken(token);
            Claims claims = parsed.getPayload();

            // Check token type
            if (!jwtService.isAccessToken(claims)) {
                log.debug("Token is not an access token");
                filterChain.doFilter(request, response);
                return;
            }

            // Check expiration explicitly
            if (claims.getExpiration().before(new Date())) {
                log.debug("Access token has expired");
                filterChain.doFilter(request, response);
                return;
            }

            String userId = claims.getSubject();
            String jti = claims.getId();
            Object rolesClaim = claims.get("roles");
            Object permissionsClaim = claims.get("permissions");

            if (!(rolesClaim instanceof List<?> rawRoles)
                    || rawRoles.stream().anyMatch(role -> !(role instanceof String))) {
                throw new JwtException("Invalid roles claim");
            }

            List<String> roles = rawRoles.stream()
                    .map(String.class::cast)
                    .toList();

            // Extract permissions from JWT
            Set<String> permissions = Set.of();
            if (permissionsClaim instanceof List<?> rawPermissions) {
                permissions = rawPermissions.stream()
                        .filter(perm -> perm instanceof String)
                        .map(String.class::cast)
                        .collect(java.util.stream.Collectors.toSet());
            }

            // Create JwtPrincipal with user information
            JwtPrincipal principal = JwtPrincipal.builder()
                    .userId(Long.parseLong(userId))
                    .roles(roles)
                    .permissions(permissions)
                    .jti(jti)
                    .build();

            // Add user context to MDC for structured logging
            com.lodhi.auth.utils.LoggingUtils.setUserContext(principal.getUserId(), userId);

            // Add role authorities (prefixed with ROLE_)
            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(java.util.stream.Collectors.toList());

            // Add permission authorities (as-is, e.g., "user:read", "admin:create")
            List<GrantedAuthority> permissionAuthorities = permissions.stream()
                    .map(perm -> (GrantedAuthority) new SimpleGrantedAuthority(perm))
                    .collect(java.util.stream.Collectors.toList());

            authorities.addAll(permissionAuthorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,  // Now using JwtPrincipal instead of String userId
                            null,
                            authorities
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected access token: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in JWT authentication filter", e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_ENDPOINTS.contains(request.getRequestURI());
    }
}