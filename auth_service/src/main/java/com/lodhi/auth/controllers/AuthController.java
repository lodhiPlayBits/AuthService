package com.lodhi.auth.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.springframework.http.ResponseEntity.ok;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

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

    /**
     * Refreshes the access token using the refresh token from HTTP-only cookie.
     * 
     * <p><b>CSRF Protection Required:</b> This endpoint requires a valid CSRF token because
     * it uses cookie-based authentication (browser automatically attaches the refresh token cookie).
     * 
     * <p><b>Client Contract:</b>
     * <ul>
     *   <li>Cookie: refreshToken=&lt;token&gt; (automatically sent by browser)</li>
     *   <li>Cookie: XSRF-TOKEN=&lt;csrf-token&gt; (automatically sent by browser)</li>
     *   <li>X-XSRF-TOKEN: &lt;csrf-token&gt; (must be manually added by JavaScript)</li>
     * </ul>
     * 
     * <p>See CSRF_PROTECTION_GUIDE.md for detailed implementation guide.
     * 
     * @return TokenResponse containing new access token
     * @throws BadCredentialsException if refresh token is missing or invalid
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = authUtils.readRefreshTokenFromCookie(request)
                .orElseThrow(() -> new BadCredentialsException("Refresh token missing from cookie"));

        return ok(refreshTokenService.rotate(refreshToken, response));
    }

    /**
     * Logout from current device/session.
     * Revokes the token family associated with the refresh token.
     * 
     * @return 204 No Content on successful logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = authUtils.readRefreshTokenFromCookie(request)
                .orElseThrow(() -> new BadCredentialsException("Refresh token missing from cookie"));

        authService.logout(refreshToken, request, response);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Logout from all devices.
     * Revokes all refresh tokens for the authenticated user.
     * Requires valid access token (Bearer authentication).
     * 
     * @param authentication Current authenticated user
     * @return 204 No Content on successful logout
     */
    @PostMapping("/logout/all")
    public ResponseEntity<Void> logoutAllDevices(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Get user ID from JWT principal
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Long userId = principal.getUserId();

        authService.logoutAllDevices(userId, request, response);
        
        return ResponseEntity.noContent().build();
    }


}