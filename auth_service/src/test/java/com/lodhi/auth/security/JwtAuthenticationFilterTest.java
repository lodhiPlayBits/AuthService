package com.lodhi.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jws<Claims> jws;

    @Mock
    private Claims claims;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_NoHeader_ShouldContinue() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidHeaderPrefix_ShouldContinue() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidToken_ShouldCatchExceptionAndContinue() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer invalid-token");
        when(jwtService.parseToken("invalid-token")).thenThrow(new JwtException("Invalid token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_NotAccessToken_ShouldContinue() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.parseToken("token")).thenReturn(jws);
        when(jws.getPayload()).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ExpiredToken_ShouldContinue() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.parseToken("token")).thenReturn(jws);
        when(jws.getPayload()).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 10000)); // Past date

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_InvalidRolesClaim_ShouldCatchException() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.parseToken("token")).thenReturn(jws);
        when(jws.getPayload()).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti");
        when(claims.get("roles")).thenReturn("Not A List"); // Invalid type

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ValidToken_NoPermissions_ShouldAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.parseToken("token")).thenReturn(jws);
        when(jws.getPayload()).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti");
        when(claims.get("roles")).thenReturn(List.of("USER"));
        when(claims.get("permissions")).thenReturn(null); // Missing permissions

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        JwtPrincipal principal = (JwtPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(1L, principal.getUserId());
        assertTrue(principal.getPermissions().isEmpty());
    }
    
    @Test
    void doFilterInternal_ValidToken_WithPermissions_ShouldAuthenticate() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.parseToken("token")).thenReturn(jws);
        when(jws.getPayload()).thenReturn(claims);
        when(jwtService.isAccessToken(claims)).thenReturn(true);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getSubject()).thenReturn("1");
        when(claims.getId()).thenReturn("jti");
        when(claims.get("roles")).thenReturn(List.of("ADMIN"));
        when(claims.get("permissions")).thenReturn(List.of("user:read")); // Has permissions

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        JwtPrincipal principal = (JwtPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(1L, principal.getUserId());
        assertTrue(principal.getPermissions().contains("user:read"));
    }

    @Test
    void shouldNotFilter_ReturnsTrueForPublicEndpoints() {
        request.setRequestURI("/api/v1/auth/login");
        assertTrue(jwtAuthenticationFilter.shouldNotFilter(request));
        
        request.setRequestURI("/api/v1/auth/register");
        assertTrue(jwtAuthenticationFilter.shouldNotFilter(request));
        
        request.setRequestURI("/api/v1/auth/refresh");
        assertTrue(jwtAuthenticationFilter.shouldNotFilter(request));
    }
    
    @Test
    void shouldNotFilter_ReturnsFalseForProtectedEndpoints() {
        request.setRequestURI("/api/v1/users/1");
        assertFalse(jwtAuthenticationFilter.shouldNotFilter(request));
    }
}
