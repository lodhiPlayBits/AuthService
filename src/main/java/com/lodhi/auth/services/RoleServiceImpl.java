package com.lodhi.auth.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lodhi.auth.audit.AuditEventType;
import com.lodhi.auth.audit.AuditService;
import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.exceptions.validation.ValidationException;
import com.lodhi.auth.model.Permission;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.respositories.PermissionRepository;
import com.lodhi.auth.respositories.RoleRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    @Override
    public Role getRoleByName(String roleName){
        return roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role", roleName));
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
        String roleName = requestDTO.getRoleName().toUpperCase().trim();
        
        // Validate role name format
        if (!roleName.matches("^[A-Z_]+$")) {
            throw new ValidationException("Role name must contain only uppercase letters and underscores");
        }
        
        // Check if role already exists
        if (roleRepository.existsByName(roleName)) {
            throw new ValidationException("Role already exists: " + roleName);
        }
        
        Role role = Role.builder()
                .name(roleName)
                .description(requestDTO.getDescription())
                .systemRole(false) // User-created roles are never system roles
                .permissions(new HashSet<>())
                .build();
        
        Role savedRole = roleRepository.save(role);
        
        // Audit log
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.ROLE_CREATED, true,
            "Role created: " + roleName);
        
        return mapToRoleResponseDTO(savedRole);
    }

    @Override
    @Transactional
    public void deleteRole(UUID roleId) {
        Role role = getRoleById(roleId);
        
        // Prevent deletion of system roles
        if (role.isSystemRole()) {
            throw new ValidationException("Cannot delete system role: " + role.getName());
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.ROLE_DELETED, true,
            "Role deleted: " + role.getName());
        
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
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.ROLE_PERMISSIONS_ASSIGNED, true,
            "Assigned " + permissions.size() + " permissions to role: " + role.getName());
        
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
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.ROLE_PERMISSIONS_REVOKED, true,
            "Revoked " + permissionsToRevoke.size() + " permissions from role: " + role.getName());
        
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
                .roleName(role.getName())
                .description(role.getDescription())
                .permissions(permissionDTOs)
                .build();
    }
}
