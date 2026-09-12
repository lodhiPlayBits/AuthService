package com.lodhi.auth.security;

import com.lodhi.auth.respositories.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            Jws<Claims> parsed = jwtService.parseToken(token);
            Claims claims = parsed.getPayload();
            try {
                if (!jwtService.isAccessToken(claims)) {
                    // Not an access token (e.g. a refresh token used here by mistake,
                    // or wrong type claim). Don't authenticate. Not an error — continue
                    // unauthenticated and let Spring Security's normal 401/403 handling
                    // take over for protected endpoints.
                    filterChain.doFilter(request, response);
                    return;
                }

                Long userId = Long.parseLong(claims.getSubject()); // was claims.getId() — jti, not sub

                userRepository.findById(userId).ifPresent(user -> {
                    List<GrantedAuthority> authorityList =
                            user.getRoles() == null
                                    ? List.of()
                                    : user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                                    .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorityList);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });

            } catch (JwtException | IllegalArgumentException e) {
                // Expected: garbage/expired/tampered/malformed tokens. Attackers send
                // these deliberately and constantly — do NOT log stack traces here or
                // you've built yourself a self-inflicted logging DoS. One line, no trace,
                // and just don't authenticate. Spring Security handles the 401/403 from here.
                log.debug("Rejected access token: {}", e.getMessage());

            } catch (Exception e) {
                // Unexpected: a real bug (DB failure, NPE, programming error) — this is
                // NOT the same thing as "bad token" and must not look like one in your
                // logs. Full stack trace, ERROR level, so it's actually visible and
                // distinguishable from routine token rejection.
                log.error("Unexpected error in JWT authentication filter", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_ENDPOINTS.contains(request.getRequestURI());
    }
}