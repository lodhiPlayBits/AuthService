package com.lodhi.auth.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.lodhi.auth.audit.AuditService;
import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.LoginResponseDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.model.RefreshToken;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.security.CookieService;
import com.lodhi.auth.security.JwtService;
import com.lodhi.auth.security.TokenHashService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserService userService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private CookieService cookieService;
    @Mock private ModelMapper mapper;
    @Mock private AuditService auditService;
    @Mock private TokenHashService tokenHashService;
    @Mock private RefreshTokenFamilyService refreshTokenFamilyService;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private Jws<Claims> jws;
    @Mock private Claims claims;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .build();
    }

    @Test
    void testRegisterUser_Success() {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        CreateUserResponseDTO res = new CreateUserResponseDTO();
        when(userService.createUser(req)).thenReturn(res);

        CreateUserResponseDTO result = authService.registeruser(req);
        assertEquals(res, result);
        verify(userService).createUser(req);
    }

    @Test
    void testLogin_Success() {
        LoginRequestDTO req = new LoginRequestDTO("test@test.com", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(tokenHashService.hashJti(anyString())).thenReturn("hashedJti");
        when(jwtService.getRefreshTtlSeconds()).thenReturn(3600L);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(eq(user), anyString())).thenReturn("refreshToken");
        
        CreateUserResponseDTO userResponse = new CreateUserResponseDTO();
        when(mapper.map(user, CreateUserResponseDTO.class)).thenReturn(userResponse);

        LoginResponseDTO res = authService.login(req, response, request);

        assertNotNull(res);
        assertEquals("accessToken", res.getAccessToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(cookieService).attachRefreshCookie(response, "refreshToken", 3600);
        verify(auditService).logLoginSuccess(user.getId(), user.getEmail(), request);
    }

    @Test
    void testLogin_Failure_BadCredentials() {
        LoginRequestDTO req = new LoginRequestDTO("test@test.com", "wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad creds"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req, response, request));
        verify(auditService).logLoginFailure(eq("test@test.com"), anyString(), eq(request));
    }

    @Test
    void testLogout_Success() {
        String refreshTokenStr = "validRefreshToken";
        String jti = UUID.randomUUID().toString();
        String jtiHash = "hashedJti";
        
        when(jwtService.parseToken(refreshTokenStr)).thenReturn(jws);
        when(jws.getPayload()).thenReturn(claims);
        when(claims.getId()).thenReturn(jti);
        when(claims.getSubject()).thenReturn("1");
        
        when(tokenHashService.hashJti(jti)).thenReturn(jtiHash);
        
        RefreshToken storedToken = RefreshToken.builder()
                .jti(jti)
                .familyId("familyId")
                .user(user)
                .build();
        when(refreshTokenRepository.findByJtiHash(jtiHash)).thenReturn(Optional.of(storedToken));
        
        authService.logout(refreshTokenStr, request, response);
        
        verify(refreshTokenFamilyService).revokeFamily("familyId");
        verify(cookieService).clearRefreshCookie(response);
        verify(auditService).logLogout(1L, "test@test.com", request);
    }

    @Test
    void testLogoutAllDevices_Success() {
        when(refreshTokenRepository.revokeAllForUser(1L)).thenReturn(5);
        when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(user));
        
        authService.logoutAllDevices(1L, request, response);
        
        verify(refreshTokenRepository).revokeAllForUser(1L);
        verify(cookieService).clearRefreshCookie(response);
        verify(auditService).logLogout(1L, "test@test.com", request);
    }
}
