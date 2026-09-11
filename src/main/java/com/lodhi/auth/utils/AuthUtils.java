package com.lodhi.auth.utils;

import com.lodhi.auth.dtos.RefreshTokenRequest;
import com.lodhi.auth.security.CookieService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Component
public class AuthUtils {

    private final CookieService cookieService;
    public Optional<String> readRefreshTokenFromRequest(
            RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request
    ) {
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookieService.getRefreshTokenCookieName().equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst();

            if (fromCookie.isPresent()) {
                return fromCookie;
            }
        }

        if (refreshTokenRequest != null
                && refreshTokenRequest.refreshToken() != null
                && !refreshTokenRequest.refreshToken().isBlank()) {
            return Optional.of(refreshTokenRequest.refreshToken());
        }

        String refreshHeader = request.getHeader("X-Refresh-Token");
        if (refreshHeader != null && !refreshHeader.isBlank()) {
            return Optional.of(refreshHeader.trim());
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String candidateToken = authHeader.substring(7).trim();
            if (!candidateToken.isEmpty()) {
                return Optional.of(candidateToken);
            }
        }

        return Optional.empty();
    }
}
