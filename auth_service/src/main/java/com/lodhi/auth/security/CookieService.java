package com.lodhi.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

@Service
@Getter
public class CookieService {

    private final String refreshTokenCookieName;
    private final boolean cookieHttpOnly;
    private final boolean cookieSecure;
    private final String cookieDomain;
    private final String cookieSameSite;
    private final String cookiePath;

    public CookieService(
            @Value("${security.jwt.refresh-cookie-name}")
            String refreshTokenCookieName,

            @Value("${security.jwt.cookie-http-only}")
            boolean cookieHttpOnly,

            @Value("${security.jwt.cookie-secure}")
            boolean cookieSecure,

            @Value("${security.jwt.cookie-domain}")
            String cookieDomain,

            @Value("${security.jwt.cookie-same-site}")
            String cookieSameSite,
            
            @Value("${security.jwt.cookie-path:/api/v1/auth/refresh}")
            String cookiePath
    ) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
        this.cookiePath = cookiePath;
    }

    public void attachRefreshCookie(
            HttpServletResponse response,
            String value,
            long maxAge
    ) {

        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(
                                refreshTokenCookieName,
                                value
                        )
                        .httpOnly(cookieHttpOnly)
                        .secure(cookieSecure)
                        .path(cookiePath)  // Restrict to refresh endpoint only
                        .maxAge(maxAge)
                        .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie cookie = builder.build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public void clearRefreshCookie(
            HttpServletResponse response
    ) {

        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(
                                refreshTokenCookieName,
                                ""
                        )
                        .httpOnly(cookieHttpOnly)
                        .secure(cookieSecure)
                        .path(cookiePath)  // Must match the path used when setting
                        .maxAge(0)
                        .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie cookie = builder.build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    public void addNoStoreHeaders(
            HttpServletResponse response
    ) {

        response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                "no-store"
        );

        response.setHeader(
                HttpHeaders.PRAGMA,
                "no-cache"
        );

        // Security headers
        response.setHeader(
                "X-Content-Type-Options",
                "nosniff"
        );

        response.setHeader(
                "X-Frame-Options",
                "DENY"
        );

        response.setHeader(
                "X-XSS-Protection",
                "1; mode=block"
        );

        response.setHeader(
                "Strict-Transport-Security",
                "max-age=31536000; includeSubDomains"
        );

        response.setHeader(
                "Content-Security-Policy",
                "default-src 'self'"
        );
    }
}