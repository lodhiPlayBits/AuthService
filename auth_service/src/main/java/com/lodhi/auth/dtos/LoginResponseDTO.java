package com.lodhi.auth.dtos;


import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LoginResponseDTO {

    private CreateUserResponseDTO user;
    private String accessToken;
    // refreshToken is NOT included - it's only in HttpOnly cookie for security
}
