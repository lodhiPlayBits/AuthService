package com.lodhi.auth.controllers;

import com.lodhi.auth.dtos.*;
import com.lodhi.auth.model.RefreshToken;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.security.CookieService;
import com.lodhi.auth.security.JwtService;
import com.lodhi.auth.services.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;

import org.modelmapper.ModelMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final ModelMapper mapper;


    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletResponse response
    ) {

        Authentication authentication =
                authenticate(loginRequestDTO);

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid username or password"
                        )
                );

        if (!user.isEnabled()) {
            throw new DisabledException(
                    "User is disabled"
            );
        }


        // Create JTI for refresh token
        String jti = UUID.randomUUID().toString();


        // Store refresh token in DB
        RefreshToken refreshTokenEntity =
                RefreshToken.builder()
                        .jti(jti)
                        .user(user)
                        .createdAt(Instant.now())
                        .expiresAt(
                                Instant.now().plusSeconds(
                                        jwtService.getRefreshTtlSeconds()
                                )
                        )
                        .revoked(false)
                        .build();

        refreshTokenRepository.save(refreshTokenEntity);


        // Generate access token
        String accessToken =
                jwtService.generateAccessToken(user);


        // Generate refresh token
        String refreshToken =
                jwtService.generateRefreshToken(
                        user,
                        jti
                );


        // Put refresh token in HttpOnly cookie
        cookieService.attachRefreshCookie(
                response,
                refreshToken,
                jwtService.getRefreshTtlSeconds()
        );

        cookieService.addNoStoreHeaders(response);


        TokenResponse tokenResponse =
                TokenResponse.of(
                        accessToken,
                        jwtService.getAccessTtlSeconds(),
                        "Bearer",
                        mapper.map(
                                user,
                                UserResponseDTO.class
                        )
                );

        return ResponseEntity.ok(tokenResponse);
    }


    // =========================================================
    // AUTHENTICATION
    // =========================================================

    private Authentication authenticate(
            LoginRequestDTO loginRequestDTO
    ) {

        try {

            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDTO.getEmail(),
                            loginRequestDTO.getPassword()
                    )
            );

        } catch (Exception e) {

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }
    }


    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(
            @RequestBody UserRequestDTO userRequestDTO
    ) {

        return ok(
                authService.registeruser(userRequestDTO)
        );
    }


    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false)
            RefreshTokenRequest refreshTokenRequest,

            HttpServletRequest request,

            HttpServletResponse response
    ) {

        // -----------------------------------------------------
        // 1. Get refresh token
        // -----------------------------------------------------

        String refreshToken =
                readRefreshTokenFromRequest(
                        refreshTokenRequest,
                        request
                )
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Refresh token missing"
                                )
                        );


        // -----------------------------------------------------
        // 2. Validate JWT and ensure it is a refresh token
        // -----------------------------------------------------

        try {

            if (!jwtService.isRefreshToken(refreshToken)) {

                throw new BadCredentialsException(
                        "Invalid refresh token"
                );
            }

        } catch (Exception e) {

            throw new BadCredentialsException(
                    "Invalid refresh token"
            );
        }


        // -----------------------------------------------------
        // 3. Extract JTI and user ID
        // -----------------------------------------------------

        String jti;

        Long userId;

        try {

            jti = jwtService.getJti(refreshToken);

            userId = jwtService.getUserId(refreshToken);

        } catch (Exception e) {

            throw new BadCredentialsException(
                    "Invalid refresh token"
            );
        }


        // -----------------------------------------------------
        // 4. Find refresh token in DB
        // -----------------------------------------------------

        RefreshToken storedRefreshToken =
                refreshTokenRepository
                        .findByJti(jti)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid refresh token"
                                )
                        );


        // -----------------------------------------------------
        // 5. Check revoked
        // -----------------------------------------------------

        if (storedRefreshToken.isRevoked()) {

            throw new BadCredentialsException(
                    "Refresh token has been revoked"
            );
        }


        // -----------------------------------------------------
        // 6. Check expiration
        // -----------------------------------------------------

        if (storedRefreshToken
                .getExpiresAt()
                .isBefore(Instant.now())) {

            throw new BadCredentialsException(
                    "Refresh token has expired"
            );
        }


        // -----------------------------------------------------
        // 7. Check user ID
        // -----------------------------------------------------

        if (!storedRefreshToken
                .getUser()
                .getId()
                .equals(userId)) {

            throw new BadCredentialsException(
                    "Invalid refresh token"
            );
        }


        User user = storedRefreshToken.getUser();


        // -----------------------------------------------------
        // 8. Revoke old refresh token
        // -----------------------------------------------------

        storedRefreshToken.setRevoked(true);


        // -----------------------------------------------------
        // 9. Create new JTI
        // -----------------------------------------------------

        String newJti =
                UUID.randomUUID().toString();


        // Track replacement
        storedRefreshToken.setReplaceToken(newJti);

        refreshTokenRepository.save(
                storedRefreshToken
        );


        // -----------------------------------------------------
        // 10. Create new refresh token DB record
        // -----------------------------------------------------

        RefreshToken newRefreshTokenEntity =
                RefreshToken.builder()
                        .jti(newJti)
                        .user(user)
                        .createdAt(Instant.now())
                        .expiresAt(
                                Instant.now().plusSeconds(
                                        jwtService.getRefreshTtlSeconds()
                                )
                        )
                        .revoked(false)
                        .build();

        refreshTokenRepository.save(
                newRefreshTokenEntity
        );


        // -----------------------------------------------------
        // 11. Generate new access token
        // -----------------------------------------------------

        String newAccessToken =
                jwtService.generateAccessToken(user);


        // -----------------------------------------------------
        // 12. Generate new refresh JWT
        // -----------------------------------------------------

        String newRefreshToken =
                jwtService.generateRefreshToken(
                        user,
                        newJti
                );


        // -----------------------------------------------------
        // 13. Replace refresh cookie
        // -----------------------------------------------------

        cookieService.attachRefreshCookie(
                response,
                newRefreshToken,
                jwtService.getRefreshTtlSeconds()
        );

        cookieService.addNoStoreHeaders(response);


        // -----------------------------------------------------
        // 14. Return new access token
        // -----------------------------------------------------

        TokenResponse tokenResponse =
                TokenResponse.of(
                        newAccessToken,
                        jwtService.getAccessTtlSeconds(),
                        "Bearer",
                        mapper.map(
                                user,
                                UserResponseDTO.class
                        )
                );

        return ResponseEntity.ok(
                tokenResponse
        );
    }


    // =========================================================
    // READ REFRESH TOKEN
    // =========================================================

    private Optional<String> readRefreshTokenFromRequest(
            RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request
    ) {

        // -----------------------------------------------------
        // 1. Cookie
        // -----------------------------------------------------

        if (request.getCookies() != null) {

            Optional<String> fromCookie =
                    Arrays.stream(
                                    request.getCookies()
                            )
                            .filter(cookie ->
                                    cookieService
                                            .getRefreshTokenCookieName()
                                            .equals(cookie.getName())
                            )
                            .map(Cookie::getValue)
                            .filter(value ->
                                    value != null &&
                                            !value.isBlank()
                            )
                            .findFirst();

            if (fromCookie.isPresent()) {
                return fromCookie;
            }
        }


        // -----------------------------------------------------
        // 2. Request body
        // -----------------------------------------------------

        if (refreshTokenRequest != null
                && refreshTokenRequest.refreshToken() != null
                && !refreshTokenRequest.refreshToken().isBlank()) {

            return Optional.of(
                    refreshTokenRequest.refreshToken()
            );
        }


        // -----------------------------------------------------
        // 3. X-Refresh-Token header
        // -----------------------------------------------------

        String refreshHeader =
                request.getHeader("X-Refresh-Token");

        if (refreshHeader != null
                && !refreshHeader.isBlank()) {

            return Optional.of(
                    refreshHeader.trim()
            );
        }


        // -----------------------------------------------------
        // 4. Authorization: Bearer
        // -----------------------------------------------------

        String authHeader =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );

        if (authHeader != null
                && authHeader.regionMatches(
                true,
                0,
                "Bearer ",
                0,
                7
        )) {

            String candidateToken =
                    authHeader.substring(7).trim();

            if (!candidateToken.isEmpty()) {

                return Optional.of(
                        candidateToken
                );
            }
        }


        return Optional.empty();
    }
}