package com.lodhi.auth.dtos;


import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LoginResponseDTO {

    private UserResponseDTO user;
    private String accessToken;
    // refreshToken is NOT included - it's only in HttpOnly cookie for security
}
