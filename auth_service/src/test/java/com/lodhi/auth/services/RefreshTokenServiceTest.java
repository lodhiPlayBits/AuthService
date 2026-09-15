package com.lodhi.auth.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;

import com.lodhi.auth.dtos.TokenResponse;
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
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private CookieService cookieService;

    @Mock
    private ModelMapper mapper;

    @Mock
    private RefreshTokenFamilyService refreshTokenFamilyService;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private RefreshToken refreshTokenEntity;
    private Jws<Claims> jws;
    private Claims claims;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEnabled(true);
        // Note: isAccountNonLocked and isCredentialsNonExpired default to true via UserDetails methods

        refreshTokenEntity = RefreshToken.builder()
                .id(java.util.UUID.randomUUID())
                .jti("old-jti")
                .jtiHash("old-jti-hash")
                .familyId("family-id")
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        claims = mock(Claims.class);
        lenient().when(claims.getId()).thenReturn("old-jti");
        lenient().when(claims.getSubject()).thenReturn("1");

        // Use a simple stub implementation for Jws since it's an interface
        jws = mock(Jws.class);
        lenient().when(jws.getPayload()).thenReturn(claims);
    }

    @Test
    void testRotate_Success() {
        String oldTokenStr = "valid-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        lenient().when(tokenHashService.hashJti(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        lenient().when(refreshTokenRepository.findByJtiHash("old-jti-hash")).thenReturn(Optional.of(refreshTokenEntity));
        
        lenient().when(refreshTokenRepository.revokeIfActive(eq("old-jti-hash"), any(String.class), any(Instant.class))).thenReturn(1);
        
        lenient().when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(user));
        
        lenient().when(jwtService.getRefreshTtlSeconds()).thenReturn(3600L);
        lenient().when(jwtService.getAccessTtlSeconds()).thenReturn(900L);
        lenient().when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        lenient().when(jwtService.generateRefreshToken(eq(user), any(String.class))).thenReturn("new-refresh-token");
        
        lenient().when(mapper.map(user, CreateUserResponseDTO.class)).thenReturn(new CreateUserResponseDTO());

        TokenResponse result = refreshTokenService.rotate(oldTokenStr, response);

        assertNotNull(result);
        assertEquals("new-access-token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(cookieService).attachRefreshCookie(response, "new-refresh-token", 3600L);
        verify(cookieService).addNoStoreHeaders(response);
    }

    @Test
    void testRotate_InvalidTokenPayload() {
        String oldTokenStr = "invalid-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(oldTokenStr, response));
    }

    @Test
    void testRotate_TokenReuseDetected() {
        String oldTokenStr = "reused-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        lenient().when(tokenHashService.hashJti(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        lenient().when(refreshTokenRepository.findByJtiHash("old-jti-hash")).thenReturn(Optional.of(refreshTokenEntity));
        
        // Return 0 indicating it was not active (already revoked or consumed)
        lenient().when(refreshTokenRepository.revokeIfActive(eq("old-jti-hash"), any(String.class), any(Instant.class))).thenReturn(0);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(oldTokenStr, response));
        assertEquals("Refresh token has been revoked", ex.getMessage());
        
        // Ensure the entire family gets revoked
        verify(refreshTokenFamilyService).revokeFamily("family-id");
    }

    @Test
    void testRotate_UserMismatch() {
        String oldTokenStr = "valid-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        lenient().when(tokenHashService.hashJti(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        
        // Return a refresh token that belongs to a different user
        User otherUser = new User();
        otherUser.setId(99L);
        RefreshToken otherUserToken = RefreshToken.builder()
                .jtiHash("old-jti-hash")
                .user(otherUser)
                .build();

        lenient().when(refreshTokenRepository.findByJtiHash("old-jti-hash")).thenReturn(Optional.of(otherUserToken));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(oldTokenStr, response));
        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void testRotate_TokenExpired() {
        String oldTokenStr = "valid-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        lenient().when(tokenHashService.hashJti(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        
        // Token expired in the past
        refreshTokenEntity.setExpiresAt(Instant.now().minusSeconds(10));
        lenient().when(refreshTokenRepository.findByJtiHash("old-jti-hash")).thenReturn(Optional.of(refreshTokenEntity));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(oldTokenStr, response));
        assertEquals("Refresh token has expired", ex.getMessage());
        
        verify(refreshTokenFamilyService).revokeFamily("family-id");
    }

    @Test
    void testRotate_UserDisabled() {
        String oldTokenStr = "valid-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        lenient().when(tokenHashService.hashJti(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        lenient().when(refreshTokenRepository.findByJtiHash("old-jti-hash")).thenReturn(Optional.of(refreshTokenEntity));
        lenient().when(refreshTokenRepository.revokeIfActive(eq("old-jti-hash"), any(String.class), any(Instant.class))).thenReturn(1);
        
        user.setEnabled(false);
        lenient().when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(user));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(oldTokenStr, response));
        assertEquals("Account is disabled", ex.getMessage());
        verify(refreshTokenFamilyService).revokeFamily("family-id");
    }

    @Test
    void testRotate_UserLocked() {
        String oldTokenStr = "valid-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        lenient().when(tokenHashService.hashJti(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        lenient().when(refreshTokenRepository.findByJtiHash("old-jti-hash")).thenReturn(Optional.of(refreshTokenEntity));
        lenient().when(refreshTokenRepository.revokeIfActive(eq("old-jti-hash"), any(String.class), any(Instant.class))).thenReturn(1);
        
        User lockedUser = mock(User.class);
        lenient().when(lockedUser.getId()).thenReturn(1L);
        lenient().when(lockedUser.isEnabled()).thenReturn(true);
        lenient().when(lockedUser.isAccountNonLocked()).thenReturn(false);
        lenient().when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(lockedUser));

        // Let the first user pass check
        refreshTokenEntity.setUser(lockedUser);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(oldTokenStr, response));
        assertEquals("Account is locked", ex.getMessage());
        verify(refreshTokenFamilyService).revokeFamily("family-id");
    }

    @Test
    void testRotate_CredentialsExpired() {
        String oldTokenStr = "valid-refresh-token";

        when(jwtService.parseToken(oldTokenStr)).thenReturn(jws);
        when(jwtService.isRefreshToken(claims)).thenReturn(true);
        lenient().when(tokenHashService.hashJti(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0) + "-hash");
        lenient().when(refreshTokenRepository.findByJtiHash("old-jti-hash")).thenReturn(Optional.of(refreshTokenEntity));
        lenient().when(refreshTokenRepository.revokeIfActive(eq("old-jti-hash"), any(String.class), any(Instant.class))).thenReturn(1);
        
        User expiredUser = mock(User.class);
        lenient().when(expiredUser.getId()).thenReturn(1L);
        lenient().when(expiredUser.isEnabled()).thenReturn(true);
        lenient().when(expiredUser.isAccountNonLocked()).thenReturn(true);
        lenient().when(expiredUser.isCredentialsNonExpired()).thenReturn(false);
        lenient().when(userRepository.findByIdWithRolesAndPermissions(1L)).thenReturn(Optional.of(expiredUser));

        refreshTokenEntity.setUser(expiredUser);

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate(oldTokenStr, response));
        assertEquals("Credentials have expired", ex.getMessage());
        verify(refreshTokenFamilyService).revokeFamily("family-id");
    }
}
