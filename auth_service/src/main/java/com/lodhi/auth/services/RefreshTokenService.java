package com.lodhi.auth.services;

import java.time.Instant;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.lodhi.auth.dtos.TokenResponse;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.model.RefreshToken;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.security.CookieService;
import com.lodhi.auth.security.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;   // add this
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final ModelMapper mapper;
    private final RefreshTokenFamilyService refreshTokenFamilyService;
    private final com.lodhi.auth.security.TokenHashService tokenHashService;
    @Transactional
    public TokenResponse rotate(String refreshToken, HttpServletResponse response) {

        Jws<Claims> jws = jwtService.parseToken(refreshToken);

        Claims claims = jws.getPayload();

        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String jti = claims.getId();
        Long userId = Long.parseLong(claims.getSubject());

        // Hash the JTI for secure lookup
        String jtiHash = tokenHashService.hashJti(jti);

        RefreshToken storedRefreshToken = refreshTokenRepository.findByJtiHash(jtiHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!storedRefreshToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        // Explicit expiration check (defense in depth)
        if (storedRefreshToken.getExpiresAt().isBefore(Instant.now())) {
            // Token has expired - revoke family for security
            log.warn("Expired refresh token detected for user: {}", userId);
            refreshTokenFamilyService.revokeFamily(storedRefreshToken.getFamilyId());
            throw new BadCredentialsException("Refresh token has expired");
        }

        String newJti = UUID.randomUUID().toString();
        String familyId = storedRefreshToken.getFamilyId();

        // Hash the new JTI
        String newJtiHash = tokenHashService.hashJti(newJti);

        int updated = refreshTokenRepository.revokeIfActive(jtiHash, newJti, Instant.now());

        if (updated == 0) {
            // Reuse detected — this token was already consumed or expired.
            // Assume compromise: kill every token in this family, including
            // whatever the legitimate client is currently holding.
            log.warn("Token reuse detected for user: {}, revoking family: {}", userId, familyId);
            refreshTokenFamilyService.revokeFamily(familyId);
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        // Fetch fresh with roles and permissions — the entityManager.clear() in revokeIfActive() detached
        // any proxy we held before this point, so don't reuse storedRefreshToken.getUser().
        // Use fetch join to load roles and permissions for token generation
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Check account status: prevent disabled or locked accounts from refreshing tokens
        if (!user.isEnabled()) {
            // Revoke entire token family for disabled account
            log.warn("Disabled account attempted token refresh: userId={}", userId);
            refreshTokenFamilyService.revokeFamily(familyId);
            throw new BadCredentialsException("Account is disabled");
        }

        if (!user.isAccountNonLocked()) {
            // Revoke entire token family for locked account
            log.warn("Locked account attempted token refresh: userId={}", userId);
            refreshTokenFamilyService.revokeFamily(familyId);
            throw new BadCredentialsException("Account is locked");
        }

        // Additional check: ensure credentials are still valid
        if (!user.isCredentialsNonExpired()) {
            // Revoke entire token family for expired credentials
            log.warn("Expired credentials detected during token refresh: userId={}", userId);
            refreshTokenFamilyService.revokeFamily(familyId);
            throw new BadCredentialsException("Credentials have expired");
        }

        RefreshToken newEntity = RefreshToken.builder()
                .jti(newJti)
                .jtiHash(newJtiHash)
                .familyId(familyId)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(newEntity);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newJti);

        cookieService.attachRefreshCookie(response, newRefreshToken, jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        return TokenResponse.of(
                newAccessToken,
                jwtService.getAccessTtlSeconds(),
                "Bearer",
                mapper.map(user, CreateUserResponseDTO.class)
        );
    }
}