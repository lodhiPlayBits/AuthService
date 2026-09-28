package com.lodhi.auth.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GoogleOAuth2RequestDTO {

    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
