package com.lodhi.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lodhi.auth.dtos.LoginResponseDTO;
import com.lodhi.auth.dtos.request.CompleteProfileRequestDTO;
import com.lodhi.auth.dtos.request.GoogleOAuth2RequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.security.JwtPrincipal;
import com.lodhi.auth.services.GoogleOAuth2Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final GoogleOAuth2Service googleOAuth2Service;

    /**
     * Google Sign-In / Sign-Up.
     *
     * - New user → creates partial account (profileComplete=false), returns tokens
     * - Existing user → signs in, returns tokens (profileComplete=true)
     *
     * Frontend checks {@code profileComplete} and redirects to profile form if false.
     */
    @PostMapping("/google")
    public ResponseEntity<LoginResponseDTO> googleAuth(
            @Valid @RequestBody GoogleOAuth2RequestDTO dto,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
            googleOAuth2Service.authenticateWithGoogle(dto.getIdToken(), response, request)
        );
    }

    /**
     * Complete profile after Google Sign-Up (Step 2).
     *
     * Requires valid access token (user is already authenticated from Step 1).
     * Accepts phone number, gender, and optional username/name updates.
     */
    @PatchMapping("/complete-profile")
    public ResponseEntity<CreateUserResponseDTO> completeProfile(
            @Valid @RequestBody CompleteProfileRequestDTO dto,
            Authentication authentication
    ) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.getUserId();

        return ResponseEntity.ok(
            googleOAuth2Service.completeProfile(userId, dto)
        );
    }
}
