package com.lodhi.auth.dtos;

import com.lodhi.auth.enums.Gender;

import lombok.Data;

/**
 * DTO for updating non-sensitive user profile information.
 * Security-sensitive fields (password, email, phoneNumber) are NOT allowed here.
 * Use dedicated endpoints for those operations.
 */
@Data
public class UpdateUserRequestDTO {
    private String username;
    private String name;
    private Gender gender;
    private String image;
    private String email;
    private String phoneNumber;
}
