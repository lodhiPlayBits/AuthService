package com.lodhi.auth.services;

import com.lodhi.auth.dtos.TokenResponse;
import com.lodhi.auth.dtos.UserResponseDTO;
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
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
@Service
@AllArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;   // add this
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final ModelMapper mapper;
    private final RefreshTokenFamilyService refreshTokenFamilyService;
    @Transactional
    public TokenResponse rotate(String refreshToken, HttpServletResponse response) {

        Jws<Claims> jws = jwtService.parseToken(refreshToken);

        Claims claims = jws.getPayload();

        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String jti = jwtService.getJti(refreshToken);
        Long userId = jwtService.getUserId(refreshToken);

        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!storedRefreshToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String newJti = UUID.randomUUID().toString();
        String familyId = storedRefreshToken.getFamilyId();

        int updated = refreshTokenRepository.revokeIfActive(jti, newJti, Instant.now());

        if (updated == 0) {
            // Reuse detected — this token was already consumed or expired.
            // Assume compromise: kill every token in this family, including
            // whatever the legitimate client is currently holding.
            refreshTokenFamilyService.revokeFamily(familyId);
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        // Fetch fresh — the entityManager.clear() in revokeIfActive() detached
        // any proxy we held before this point, so don't reuse storedRefreshToken.getUser().
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        RefreshToken newEntity = RefreshToken.builder()
                .jti(newJti)
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
                mapper.map(user, UserResponseDTO.class)
        );
    }
}