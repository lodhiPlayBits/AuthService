package com.lodhi.auth.dtos.response;

import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RoleResponseDTO {
    
    private UUID id;
    private String roleName;
    private String description;
    
    @Builder.Default
    private Set<PermissionResponseDTO> permissions = new HashSet<>();
}
