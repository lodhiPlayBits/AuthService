package com.lodhi.auth.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.LoginResponseDTO;
import com.lodhi.auth.dtos.TokenResponse;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.security.JwtPrincipal;
import com.lodhi.auth.services.AuthService;
import com.lodhi.auth.services.RefreshTokenService;
import com.lodhi.auth.utils.AuthUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_ShouldReturnOk() {
        LoginRequestDTO req = new LoginRequestDTO();
        LoginResponseDTO res = new LoginResponseDTO();
        when(authService.login(req, response, request)).thenReturn(res);
        
        ResponseEntity<LoginResponseDTO> entity = authController.login(req, response, request);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(res, entity.getBody());
    }

    @Test
    void registerUser_ShouldReturnCreated() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        CreateUserResponseDTO res = new CreateUserResponseDTO();
        when(authService.registeruser(req)).thenReturn(res);
        
        ResponseEntity<CreateUserResponseDTO> entity = authController.registerUser(req);
        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
        assertEquals(res, entity.getBody());
    }

    @Test
    void refreshToken_ShouldReturnOk() {
        when(authUtils.readRefreshTokenFromCookie(request)).thenReturn(Optional.of("token"));
        TokenResponse res = new TokenResponse("new_access", 3600, "Bearer", new CreateUserResponseDTO());
        when(refreshTokenService.rotate("token", response)).thenReturn(res);
        
        ResponseEntity<TokenResponse> entity = authController.refreshToken(request, response);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(res, entity.getBody());
    }

    @Test
    void refreshToken_MissingCookie_ShouldThrowException() {
        when(authUtils.readRefreshTokenFromCookie(request)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(BadCredentialsException.class, () -> {
            authController.refreshToken(request, response);
        });
    }

    @Test
    void logout_ShouldReturnNoContent() {
        when(authUtils.readRefreshTokenFromCookie(request)).thenReturn(Optional.of("token"));
        
        ResponseEntity<Void> entity = authController.logout(request, response);
        verify(authService).logout("token", request, response);
        assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
    }

    @Test
    void logout_MissingCookie_ShouldThrowException() {
        when(authUtils.readRefreshTokenFromCookie(request)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(BadCredentialsException.class, () -> {
            authController.logout(request, response);
        });
    }

    @Test
    void logoutAllDevices_ShouldReturnNoContent() {
        JwtPrincipal principal = JwtPrincipal.builder().userId(1L).build();
        when(authentication.getPrincipal()).thenReturn(principal);
        
        ResponseEntity<Void> entity = authController.logoutAllDevices(authentication, request, response);
        
        verify(authService).logoutAllDevices(1L, request, response);
        assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
    }
}
