package com.lodhi.auth.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieServiceTest {

    private CookieService cookieServiceWithDomain;
    private CookieService cookieServiceWithoutDomain;
    private CookieService cookieServiceNullDomain;
    
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        
        cookieServiceWithDomain = new CookieService(
                "refreshToken",
                true,
                true,
                "example.com",
                "Strict",
                "/api/v1/auth/refresh"
        );
        
        cookieServiceWithoutDomain = new CookieService(
                "refreshToken",
                true,
                true,
                "", // Blank domain
                "Strict",
                "/api/v1/auth/refresh"
        );
        
        cookieServiceNullDomain = new CookieService(
                "refreshToken",
                true,
                true,
                null,
                "Strict",
                "/api/v1/auth/refresh"
        );
    }

    @Test
    void attachRefreshCookie_WithDomain_ShouldSetDomain() {
        cookieServiceWithDomain.attachRefreshCookie(response, "token_value", 3600);
        
        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken=token_value"));
        assertTrue(cookieHeader.contains("Domain=example.com"));
        assertTrue(cookieHeader.contains("Max-Age=3600"));
        assertTrue(cookieHeader.contains("Secure"));
        assertTrue(cookieHeader.contains("HttpOnly"));
        assertTrue(cookieHeader.contains("SameSite=Strict"));
        assertTrue(cookieHeader.contains("Path=/api/v1/auth/refresh"));
    }

    @Test
    void attachRefreshCookie_WithoutDomain_ShouldNotSetDomain() {
        cookieServiceWithoutDomain.attachRefreshCookie(response, "token_value", 3600);
        
        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken=token_value"));
        assertFalse(cookieHeader.contains("Domain="));
    }

    @Test
    void clearRefreshCookie_WithDomain_ShouldSetZeroMaxAge() {
        cookieServiceWithDomain.clearRefreshCookie(response);
        
        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken="));
        assertTrue(cookieHeader.contains("Domain=example.com"));
        assertTrue(cookieHeader.contains("Max-Age=0"));
    }

    @Test
    void clearRefreshCookie_WithoutDomain_ShouldNotSetDomain() {
        cookieServiceWithoutDomain.clearRefreshCookie(response);
        
        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken="));
        assertFalse(cookieHeader.contains("Domain="));
        assertTrue(cookieHeader.contains("Max-Age=0"));
    }
    
    @Test
    void attachRefreshCookie_WithNullDomain_ShouldNotSetDomain() {
        cookieServiceNullDomain.attachRefreshCookie(response, "token_value", 3600);
        
        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertFalse(cookieHeader.contains("Domain="));
    }
    
    @Test
    void clearRefreshCookie_WithNullDomain_ShouldNotSetDomain() {
        cookieServiceNullDomain.clearRefreshCookie(response);
        
        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertFalse(cookieHeader.contains("Domain="));
    }

    @Test
    void addNoStoreHeaders_ShouldSetSecurityHeaders() {
        cookieServiceWithDomain.addNoStoreHeaders(response);
        
        assertEquals("no-store", response.getHeader(HttpHeaders.CACHE_CONTROL));
        assertEquals("no-cache", response.getHeader(HttpHeaders.PRAGMA));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("1; mode=block", response.getHeader("X-XSS-Protection"));
        assertEquals("max-age=31536000; includeSubDomains", response.getHeader("Strict-Transport-Security"));
        assertEquals("default-src 'self'", response.getHeader("Content-Security-Policy"));
    }
}
