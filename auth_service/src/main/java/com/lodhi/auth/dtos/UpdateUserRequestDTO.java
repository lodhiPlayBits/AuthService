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
    private String name;
    private Gender gender;
    private String image;
    
    // Security note: password, email, phoneNumber explicitly excluded
    // - Password changes: Use dedicated password change endpoint
    // - Email changes: Requires verification flow
    // - Phone changes: May require verification flow
}
