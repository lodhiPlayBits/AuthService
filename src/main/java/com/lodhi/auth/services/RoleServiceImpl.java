package com.lodhi.auth.services;

import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;
import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.exceptions.validation.ValidationException;
import com.lodhi.auth.model.Permission;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.respositories.PermissionRepository;
import com.lodhi.auth.respositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public Role getRole(RoleType roleType){
        return roleRepository.findByRoleType(roleType)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role", roleType.name()));
    }

    @Override
    public Role getRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }

    @Override
    public List<RoleResponseDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToRoleResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleResponseDTO createRole(CreateRoleRequestDTO requestDTO) {
        // Check if role already exists
        RoleType roleType;
        try {
            roleType = RoleType.valueOf(requestDTO.getRoleName().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role name. Must be one of: USER, ADMIN, or custom role");
        }
        
        if (roleRepository.existsByRoleType(roleType)) {
            throw new ValidationException("Role already exists: " + requestDTO.getRoleName());
        }
        
        Role role = Role.builder()
                .roleType(roleType)
                .description(requestDTO.getDescription())
                .permissions(new HashSet<>())
                .build();
        
        Role savedRole = roleRepository.save(role);
        return mapToRoleResponseDTO(savedRole);
    }

    @Override
    @Transactional
    public void deleteRole(UUID roleId) {
        Role role = getRoleById(roleId);
        
        // Prevent deletion of system roles
        if (role.getRoleType() == RoleType.USER || role.getRoleType() == RoleType.ADMIN) {
            throw new ValidationException("Cannot delete system roles: USER and ADMIN");
        }
        
        roleRepository.delete(role);
    }

    @Override
    @Transactional
    public RoleResponseDTO assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds) {
        Role role = getRoleById(roleId);
        
        Set<Permission> permissions = permissionIds.stream()
                .map(permissionId -> permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId)))
                .collect(Collectors.toSet());
        
        role.getPermissions().addAll(permissions);
        Role updatedRole = roleRepository.save(role);
        
        return mapToRoleResponseDTO(updatedRole);
    }

    @Override
    @Transactional
    public RoleResponseDTO revokePermissionsFromRole(UUID roleId, Set<UUID> permissionIds) {
        Role role = getRoleById(roleId);
        
        Set<Permission> permissionsToRevoke = permissionIds.stream()
                .map(permissionId -> permissionRepository.findById(permissionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId)))
                .collect(Collectors.toSet());
        
        role.getPermissions().removeAll(permissionsToRevoke);
        Role updatedRole = roleRepository.save(role);
        
        return mapToRoleResponseDTO(updatedRole);
    }
    
    private RoleResponseDTO mapToRoleResponseDTO(Role role) {
        Set<PermissionResponseDTO> permissionDTOs = role.getPermissions().stream()
                .map(permission -> PermissionResponseDTO.builder()
                        .id(permission.getId())
                        .name(permission.getName())
                        .description(permission.getDescription())
                        .build())
                .collect(Collectors.toSet());
        
        return RoleResponseDTO.builder()
                .id(role.getId())
                .roleName(role.getRoleType().name())
                .description(role.getDescription())
                .permissions(permissionDTOs)
                .build();
    }
}
