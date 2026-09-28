package com.lodhi.auth.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.lodhi.auth.audit.AuditService;
import com.lodhi.auth.constants.SystemRoles;
import com.lodhi.auth.dtos.LoginResponseDTO;
import com.lodhi.auth.dtos.request.CompleteProfileRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.exceptions.authentication.OAuth2AuthenticationException;
import com.lodhi.auth.exceptions.validation.ValidationException;
import com.lodhi.auth.model.RefreshToken;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.security.CookieService;
import com.lodhi.auth.security.JwtService;
import com.lodhi.auth.security.TokenHashService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google OAuth2 service handling both Sign-Up (new user) and Sign-In (existing user).
 * 
 * Sign-Up is two-step:
 *   Step 1: authenticateWithGoogle() — verifies Google ID token, creates partial user
 *   Step 2: completeProfile() — user submits phone, gender, optional username
 * 
 * Sign-In: authenticateWithGoogle() — verifies token, issues JWT for existing user
 * 
 * Reuses existing JWT infrastructure (JwtService, CookieService, RefreshTokenService)
 * so OAuth users get identical token management as local users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuth2Service {

    private final GoogleIdTokenVerifier verifier;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;
    private final TokenHashService tokenHashService;
    private final AuditService auditService;
    private final ModelMapper modelMapper;

    // ─── Google Sign-In / Sign-Up ─────────────────────────────

    /**
     * Authenticate a user with a Google ID Token.
     * 
     * If the user doesn't exist yet, creates a partial account (profileComplete=false)
     * and the frontend should redirect to the "complete profile" form.
     * 
     * If the user already exists, signs them in and returns tokens.
     *
     * @param idTokenString Google ID Token from the frontend
     * @param response HTTP response (for refresh token cookie)
     * @param request HTTP request (for audit logging)
     * @return LoginResponseDTO with access token, user info, and profileComplete flag
     */
    @Transactional
    public LoginResponseDTO authenticateWithGoogle(
            String idTokenString,
            HttpServletResponse response,
            HttpServletRequest request) {

        // 1. Verify Google ID Token (signature, issuer, audience, expiry)
        GoogleIdToken idToken = verifyGoogleToken(idTokenString);
        GoogleIdToken.Payload payload = idToken.getPayload();

        String googleId       = payload.getSubject();           // Unique Google user ID
        String email          = payload.getEmail();
        boolean emailVerified = payload.getEmailVerified();
        String name           = (String) payload.get("name");
        String picture        = (String) payload.get("picture");

        if (!emailVerified) {
            throw new OAuth2AuthenticationException("Google email is not verified");
        }

        // 2. Find existing Google user OR create a new partial user
        boolean isNewUser = false;
        User user = userRepository.findByProviderAndProviderId(Provider.GOOGLE, googleId)
                .orElse(null);

        if (user == null) {
            // Check if email is already registered with LOCAL provider
            Optional<User> localUser = userRepository.findByEmail(email);
            if (localUser.isPresent() && localUser.get().getProvider() == Provider.LOCAL) {
                throw new OAuth2AuthenticationException(
                    "An account with this email already exists. "
                    + "Please sign in with your password, then link your Google account from settings."
                );
            }

            // Create new partial user (profileComplete = false)
            user = createPartialGoogleUser(googleId, email, name, picture);
            isNewUser = true;
        }

        // 3. Update profile picture if changed (for returning users)
        if (!isNewUser && picture != null && !picture.equals(user.getImage())) {
            user.setImage(picture);
            userRepository.save(user);
        }

        // 4. Check account is enabled
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        // 5. Issue tokens (reusing existing JWT infrastructure)
        LoginResponseDTO loginResponse = issueTokens(user, response);

        // 6. Audit
        if (isNewUser) {
            log.info("New Google OAuth user registered: email={}", email);
        }
        auditService.logLoginSuccess(user.getId(), user.getEmail(), request);

        return loginResponse;
    }

    // ─── Complete Profile (Step 2 of Sign-Up) ─────────────────

    /**
     * Complete the profile for a Google OAuth user.
     * Called after authenticateWithGoogle() when profileComplete is false.
     *
     * @param userId authenticated user's ID (from JWT principal)
     * @param dto phone number, gender, optional username/name
     * @return updated user response
     */
    @Transactional
    public CreateUserResponseDTO completeProfile(
            Long userId,
            CompleteProfileRequestDTO dto) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        // Guard: only incomplete profiles can be completed
        if (user.isProfileComplete()) {
            throw new ValidationException("Profile is already complete");
        }

        // Guard: only OAuth users use this flow
        if (user.getProvider() == Provider.LOCAL) {
            throw new ValidationException("This endpoint is only for OAuth users");
        }

        // Check uniqueness of phone number
        if (dto.getPhoneNumber() != null
                && userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new ValidationException(
                "Phone number " + dto.getPhoneNumber() + " is already in use");
        }

        // Check uniqueness of username (if they want to change from email-based default)
        if (dto.getUsername() != null
                && !dto.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(dto.getUsername())) {
            throw new ValidationException(
                "Username " + dto.getUsername() + " is already taken");
        }

        // Update fields
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setGender(dto.getGender());

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            user.setUsername(dto.getUsername().trim().toLowerCase());
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName().trim());
        }

        // Mark profile as complete
        user.setProfileComplete(true);

        User saved = userRepository.save(user);
        log.info("Google OAuth user completed profile: userId={}", userId);

        return modelMapper.map(saved, CreateUserResponseDTO.class);
    }

    // ─── Private Helpers ──────────────────────────────────────

    private GoogleIdToken verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new OAuth2AuthenticationException("Invalid Google ID token");
            }
            return idToken;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Google token verification failed", e);
            throw new OAuth2AuthenticationException("Google token verification failed");
        }
    }

    private User createPartialGoogleUser(
            String googleId, String email, String name, String picture) {

        Role userRole = roleService.getRoleByName(SystemRoles.USER);

        User user = User.builder()
                .email(email)
                .name(name != null ? name : email.split("@")[0])
                .username(email)                // Default username = email; user can change later
                .image(picture)
                .provider(Provider.GOOGLE)
                .providerId(googleId)
                .password(null)                 // No local password for OAuth users
                .phoneNumber(null)              // Filled in step 2
                .gender(null)                   // Filled in step 2
                .profileComplete(false)         // Triggers "complete profile" on frontend
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(user);
    }

    private LoginResponseDTO issueTokens(User user, HttpServletResponse response) {
        String jti      = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();
        String jtiHash  = tokenHashService.hashJti(jti);

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

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        cookieService.attachRefreshCookie(response, refreshToken, jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .user(modelMapper.map(user, CreateUserResponseDTO.class))
                .profileComplete(user.isProfileComplete())
                .build();
    }
}
