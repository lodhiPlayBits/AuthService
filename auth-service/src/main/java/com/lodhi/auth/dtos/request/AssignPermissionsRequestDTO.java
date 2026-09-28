package com.lodhi.auth.dtos.request;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
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
public class AssignPermissionsRequestDTO {
    
    @NotEmpty(message = "At least one permission ID is required")
    private Set<UUID> permissionIds;
}
