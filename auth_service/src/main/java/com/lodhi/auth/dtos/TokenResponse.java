package com.lodhi.auth.dtos;

import com.lodhi.auth.dtos.response.CreateUserResponseDTO;

public record TokenResponse(
        String accessToken,
        long expiresIn,
        String tokenType,
        CreateUserResponseDTO user
) {

    public static TokenResponse of(
            String accessToken,
            long expiresIn,
            String tokenType,
            CreateUserResponseDTO user
    ) {
        return new TokenResponse(
                accessToken,
                expiresIn,
                tokenType,
                user
        );
    }
}