package com.lodhi.auth.utils;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.lodhi.auth.security.CookieService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class AuthUtils {

    private final CookieService cookieService;
    
    /**
     * Reads refresh token from HTTP-only cookie only.
     * @param request the HTTP request
     * @return Optional containing the refresh token if present in cookie
     */
    public Optional<String> readRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieService.getRefreshTokenCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
