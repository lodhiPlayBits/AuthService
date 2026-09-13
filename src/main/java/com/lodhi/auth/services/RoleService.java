package com.lodhi.auth.services;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;
import com.lodhi.auth.model.Role;

public interface RoleService {

    Role getRoleByName(String roleName);
    
    Role getRoleById(UUID roleId);
    
    List<RoleResponseDTO> getAllRoles();
    
    RoleResponseDTO createRole(CreateRoleRequestDTO requestDTO);
    
    void deleteRole(UUID roleId);
    
    RoleResponseDTO assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds);
    
    RoleResponseDTO revokePermissionsFromRole(UUID roleId, Set<UUID> permissionIds);
}
