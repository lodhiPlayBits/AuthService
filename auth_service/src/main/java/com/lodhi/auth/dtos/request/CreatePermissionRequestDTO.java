package com.lodhi.auth.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CreatePermissionRequestDTO {
    
    @NotBlank(message = "Permission name is required")
    @Pattern(regexp = "^[a-z]+:[a-z]+$", message = "Permission name must be in format: resource:action (e.g., user:read)")
    @Size(max = 100, message = "Permission name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}
