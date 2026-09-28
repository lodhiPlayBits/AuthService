package com.lodhi.auth.dtos;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class LoginRequestDTO {
    
    @NotBlank(message = "Email or Phone Number or Username  is required")
    private String identifier;
    
    @NotBlank(message = "Password is required")
    private String password;
}
