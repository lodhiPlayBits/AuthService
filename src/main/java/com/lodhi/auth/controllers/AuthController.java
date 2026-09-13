package com.lodhi.auth.controllers;

import com.lodhi.auth.dtos.*;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.model.User;
import com.lodhi.auth.services.AuthService;
import com.lodhi.auth.services.RefreshTokenService;

import com.lodhi.auth.services.UserService;
import com.lodhi.auth.utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AuthUtils authUtils;


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        return ok(authService.login(loginRequestDTO, response, request));
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<CreateUserResponseDTO> registerUser(@Valid @RequestBody CreateUserRequestDTO createUserRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registeruser(createUserRequestDTO));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken =
                authUtils.readRefreshTokenFromRequest(refreshTokenRequest, request)
                        .orElseThrow(() -> new BadCredentialsException("Refresh token missing"));

        return ok(refreshTokenService.rotate(refreshToken, response));
    }


}