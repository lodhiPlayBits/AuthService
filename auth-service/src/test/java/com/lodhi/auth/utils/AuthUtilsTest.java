package com.lodhi.auth.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lodhi.auth.security.CookieService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthUtilsTest {

    @Mock
    private CookieService cookieService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthUtils authUtils;

    @BeforeEach
    void setUp() {
        // Only lenient when we actually need it for the test
    }

    @Test
    void testReadRefreshTokenFromCookie_NoCookies() {
        when(request.getCookies()).thenReturn(null);
        Optional<String> result = authUtils.readRefreshTokenFromCookie(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void testReadRefreshTokenFromCookie_EmptyCookies() {
        when(request.getCookies()).thenReturn(new Cookie[0]);
        Optional<String> result = authUtils.readRefreshTokenFromCookie(request);
        assertTrue(result.isEmpty());
    }

    @Test
    void testReadRefreshTokenFromCookie_CookiePresent() {
        when(cookieService.getRefreshTokenCookieName()).thenReturn("refreshToken");
        
        Cookie c1 = new Cookie("otherCookie", "value1");
        Cookie c2 = new Cookie("refreshToken", "my-refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[]{c1, c2});
        
        Optional<String> result = authUtils.readRefreshTokenFromCookie(request);
        
        assertTrue(result.isPresent());
        assertEquals("my-refresh-token", result.get());
    }

    @Test
    void testReadRefreshTokenFromCookie_CookiePresentButEmpty() {
        when(cookieService.getRefreshTokenCookieName()).thenReturn("refreshToken");
        
        Cookie c1 = new Cookie("refreshToken", "");
        when(request.getCookies()).thenReturn(new Cookie[]{c1});
        
        Optional<String> result = authUtils.readRefreshTokenFromCookie(request);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testReadRefreshTokenFromCookie_CookiePresentButNull() {
        when(cookieService.getRefreshTokenCookieName()).thenReturn("refreshToken");
        
        Cookie c1 = new Cookie("refreshToken", null);
        when(request.getCookies()).thenReturn(new Cookie[]{c1});
        
        Optional<String> result = authUtils.readRefreshTokenFromCookie(request);
        
        assertTrue(result.isEmpty());
    }
}
