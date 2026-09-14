package com.lodhi.auth.services;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;
import com.lodhi.auth.model.Role;

public interface RoleService {

    Role getRoleByName(String roleName);
    
    Role getRoleById(UUID roleId);
    
    /**
     * Get all roles with pagination support.
     * Maximum page size is enforced to prevent unbounded queries.
     * 
     * @param pageable pagination parameters (page number, size, sort)
     * @return paginated list of roles
     */
    Page<RoleResponseDTO> getAllRoles(Pageable pageable);
    
    RoleResponseDTO createRole(CreateRoleRequestDTO requestDTO);
    
    void deleteRole(UUID roleId);
    
    RoleResponseDTO assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds);
    
    RoleResponseDTO revokePermissionsFromRole(UUID roleId, Set<UUID> permissionIds);
}
