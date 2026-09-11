package com.lodhi.auth.dtos;

public record TokenResponse(
        String accessToken,
        long expiresIn,
        String tokenType,
        UserResponseDTO user
) {

    public static TokenResponse of(
            String accessToken,
            long expiresIn,
            String tokenType,
            UserResponseDTO user
    ) {
        return new TokenResponse(
                accessToken,
                expiresIn,
                tokenType,
                user
        );
    }
}