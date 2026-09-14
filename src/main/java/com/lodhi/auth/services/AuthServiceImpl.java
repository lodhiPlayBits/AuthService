package com.lodhi.auth.services;

import java.time.Instant;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lodhi.auth.audit.AuditService;
import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.LoginResponseDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.model.RefreshToken;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.security.CookieService;
import com.lodhi.auth.security.JwtService;

import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.lodhi.auth.respositories.UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final ModelMapper mapper;
    private final AuditService auditService;
    private final com.lodhi.auth.security.TokenHashService tokenHashService;
    private final RefreshTokenFamilyService refreshTokenFamilyService;

    @Override
    public CreateUserResponseDTO registeruser(CreateUserRequestDTO createUserRequestDTO) {
        return userService.createUser(createUserRequestDTO);
    }

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO, HttpServletResponse response, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDTO.getIdentifier(),
                            loginRequestDTO.getPassword()
                    )
            );
            // If we reach this line, Spring Security has ALREADY verified:
            // - user exists, account enabled, account not locked, password matches.
            // Any failure above throws AuthenticationException, caught by your
            // existing GlobalExceptionHandler.handleAuthenticationException().

            User user = (User) authentication.getPrincipal();
            
            // Generate unique identifiers
            String jti = UUID.randomUUID().toString();
            String familyId = UUID.randomUUID().toString();
            
            // Hash JTI for secure storage
            String jtiHash = tokenHashService.hashJti(jti);
            
            // Save refresh token to database for validation and revocation
            RefreshToken refreshTokenEntity = RefreshToken.builder()
                    .jti(jti)
                    .jtiHash(jtiHash)
                    .familyId(familyId)
                    .user(user)
                    .createdAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                    .revoked(false)
                    .build();
            
            refreshTokenRepository.save(refreshTokenEntity);
            
            // Generate JWT tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user, jti);

            // Set refresh token as HttpOnly cookie
            cookieService.attachRefreshCookie(response, refreshToken, jwtService.getRefreshTtlSeconds());
            cookieService.addNoStoreHeaders(response);

            // Log successful login
            auditService.logLoginSuccess(user.getId(), user.getEmail(), request);

            // Build response (refresh token NOT included in body, only in cookie)
            return LoginResponseDTO.builder()
                    .accessToken(accessToken)
                    .user(mapper.map(user, CreateUserResponseDTO.class))
                    .build();
                    
        } catch (BadCredentialsException | DisabledException | LockedException e) {
            // Log failed login attempt
            auditService.logLoginFailure(loginRequestDTO.getIdentifier(), e.getMessage(), request);
            throw e;
        }
    }

    @Override
    @Transactional
    public void logout(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        try {
            // Parse refresh token to get user info and family ID
            Jws<io.jsonwebtoken.Claims> jws = jwtService.parseToken(refreshToken);
            io.jsonwebtoken.Claims claims = jws.getPayload();
            
            String jti = claims.getId();
            Long userId = Long.parseLong(claims.getSubject());
            
            // Hash JTI for lookup
            String jtiHash = tokenHashService.hashJti(jti);
            
            // Find the refresh token
            RefreshToken storedToken = refreshTokenRepository.findByJtiHash(jtiHash)
                    .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
            
            // Revoke the entire token family (logs out current device)
            String familyId = storedToken.getFamilyId();
            refreshTokenFamilyService.revokeFamily(familyId);
            
            // Clear the refresh token cookie
            cookieService.clearRefreshCookie(response);
            
            // Log successful logout
            User user = storedToken.getUser();
            auditService.logLogout(userId, user.getEmail(), request);
            
        } catch (Exception e) {
            // Even if token is invalid, still clear the cookie
            cookieService.clearRefreshCookie(response);
            throw new BadCredentialsException("Logout failed", e);
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(Long userId, HttpServletRequest request, HttpServletResponse response) {
        // Revoke all refresh tokens for this user by setting revoked = true
        int revokedCount = refreshTokenRepository.revokeAllForUser(userId);
        
        // Clear the refresh token cookie
        cookieService.clearRefreshCookie(response);
        
        // Get user info for audit (fetch by ID with roles for audit context)
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        
        // Log logout from all devices
        auditService.logLogout(userId, user.getEmail(), request);
        
        // Log for monitoring
        org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class)
            .info("User logged out from all devices: userId={}, tokensRevoked={}", userId, revokedCount);
    }
}