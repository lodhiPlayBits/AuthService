package com.lodhi.auth.services;

import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.TokenResponse;
import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;
import com.lodhi.auth.model.RefreshToken;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.security.CookieService;
import com.lodhi.auth.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final ModelMapper mapper;

    @Override
    public UserResponseDTO registeruser(UserRequestDTO userRequestDTO) {
        return userService.createUser(userRequestDTO);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequestDTO loginRequestDTO, HttpServletResponse response) {

        Authentication authentication = authenticate(loginRequestDTO);

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        return issueTokens(user, response);
    }

    private TokenResponse issueTokens(User user, HttpServletResponse response) {

        String jti = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .jti(jti)
                .familyId(familyId)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, jti);

        cookieService.attachRefreshCookie(response, refreshToken, jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        return TokenResponse.of(
                accessToken,
                jwtService.getAccessTtlSeconds(),
                "Bearer",
                mapper.map(user, UserResponseDTO.class)
        );
    }

    private Authentication authenticate(LoginRequestDTO loginRequestDTO) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDTO.getEmail(),
                            loginRequestDTO.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}