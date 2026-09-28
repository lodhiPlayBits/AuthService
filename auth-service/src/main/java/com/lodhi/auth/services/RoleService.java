package com.lodhi.auth.services;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lodhi.auth.dtos.cache.RoleCache;
import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;

public interface RoleService {

    /**
     * Returns the role as a Redis-safe RoleCache DTO (not a JPA entity).
     * Use getRoleEntityByName() if you need to attach the role to a new User.
     */
    RoleCache getRoleByName(String roleName);

    /**
     * Returns the role as a Redis-safe RoleCache DTO (not a JPA entity).
     * Use getRoleEntityById() if you need to attach the role to a new User.
     */
    RoleCache getRoleById(UUID roleId);

    /**
     * Get all roles with pagination support.
     * Maximum page size is enforced to prevent unbounded queries.
     */
    Page<RoleResponseDTO> getAllRoles(Pageable pageable);

    RoleResponseDTO createRole(CreateRoleRequestDTO requestDTO);

    void deleteRole(UUID roleId);

    RoleResponseDTO assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds);

    RoleResponseDTO revokePermissionsFromRole(UUID roleId, Set<UUID> permissionIds);
}
