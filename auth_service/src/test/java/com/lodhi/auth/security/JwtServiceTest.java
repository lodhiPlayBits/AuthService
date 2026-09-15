package com.lodhi.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private Claims claims;

    @BeforeEach
    void setUp() {
        // Need 32 bytes minimum for HMAC SHA
        String secret = "this-is-a-very-long-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256";
        jwtService = new JwtService(secret, 3600, 86400, "issuer", "audience");
    }

    @Test
    void isAccessToken_ReturnsFalse_WhenTypeIsNotAccess() {
        when(claims.get("type", String.class)).thenReturn("refresh");
        assertFalse(jwtService.isAccessToken(claims));
    }

    @Test
    void isAccessToken_ThrowsException_WhenSubjectIsNull() {
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.getSubject()).thenReturn(null);
        
        JwtException ex = assertThrows(JwtException.class, () -> jwtService.isAccessToken(claims));
        assertEquals("Access token missing required claims (sub or jti)", ex.getMessage());
    }

    @Test
    void isAccessToken_ThrowsException_WhenJtiIsNull() {
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn(null);
        
        JwtException ex = assertThrows(JwtException.class, () -> jwtService.isAccessToken(claims));
        assertEquals("Access token missing required claims (sub or jti)", ex.getMessage());
    }

    @Test
    void isAccessToken_ThrowsException_WhenRolesNotList() {
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti");
        when(claims.get("roles")).thenReturn("Not a list");
        
        JwtException ex = assertThrows(JwtException.class, () -> jwtService.isAccessToken(claims));
        assertEquals("Access token 'roles' claim must be a list", ex.getMessage());
    }

    @Test
    void isAccessToken_ThrowsException_WhenPermissionsNotListOrSet() {
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti");
        when(claims.get("roles")).thenReturn(List.of("ADMIN"));
        when(claims.get("permissions")).thenReturn("Not a list or set");
        
        JwtException ex = assertThrows(JwtException.class, () -> jwtService.isAccessToken(claims));
        assertEquals("Access token 'permissions' claim must be a list or set", ex.getMessage());
    }

    @Test
    void isAccessToken_ReturnsTrue_WhenValid() {
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti");
        when(claims.get("roles")).thenReturn(List.of("ADMIN"));
        when(claims.get("permissions")).thenReturn(Set.of("user:read"));
        
        assertTrue(jwtService.isAccessToken(claims));
    }

    @Test
    void isRefreshToken_ReturnsFalse_WhenTypeIsNotRefresh() {
        when(claims.get("type", String.class)).thenReturn("access");
        assertFalse(jwtService.isRefreshToken(claims));
    }

    @Test
    void isRefreshToken_ThrowsException_WhenSubjectIsNull() {
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn(null);
        
        JwtException ex = assertThrows(JwtException.class, () -> jwtService.isRefreshToken(claims));
        assertEquals("Refresh token missing required claims (sub or jti)", ex.getMessage());
    }

    @Test
    void isRefreshToken_ThrowsException_WhenJtiIsNull() {
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn(null);
        
        JwtException ex = assertThrows(JwtException.class, () -> jwtService.isRefreshToken(claims));
        assertEquals("Refresh token missing required claims (sub or jti)", ex.getMessage());
    }

    @Test
    void isRefreshToken_ReturnsTrue_WhenValid() {
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti");
        
        assertTrue(jwtService.isRefreshToken(claims));
    }
}
