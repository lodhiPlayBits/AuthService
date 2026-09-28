package com.lodhi.auth.dtos.request;

import com.lodhi.auth.enums.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for completing a user profile after Google OAuth sign-up.
 * Phone number and gender are required; username and name are optional overrides.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CompleteProfileRequestDTO {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Size(min = 2, max = 100, message = "Username must be between 2 and 100 characters")
    private String username;  // Optional — defaults to email if not provided

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;      // Optional — already set from Google profile
}
