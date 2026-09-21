package com.lodhi.auth.services;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    @Cacheable(value={"roles"}, key="#roleName")
    public Role getRoleByName(String roleName){
        // Use fetch join to load permissions with role
        return roleRepository.findByNameWithPermissions(roleName)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role", roleName));
    }

    @Override
    @Cacheable(value={"roles"}, key="#roleId")
    public Role getRoleById(UUID roleId) {
        // Use fetch join to load permissions with role
        return roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value={"roles:all"})
    public Page<RoleResponseDTO> getAllRoles(Pageable pageable) {
        // Enforce maximum page size to prevent unbounded queries
        int maxPageSize = 100;
        if (pageable.getPageSize() > maxPageSize) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                maxPageSize, 
                pageable.getSort()
            );
        }
        
        Page<Role> rolePage = roleRepository.findAll(pageable);
        return rolePage.map(this::mapToRoleResponseDTO);
    }

    @Override
    @Transactional
    @Caching(evict={@CacheEvict(value={"roles"}, allEntries=true), @CacheEvict(value={"roles:all"}, allEntries=true)})
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
    @Caching(evict={@CacheEvict(value={"roles"}, key="#roleId"), @CacheEvict(value={"roles:all"}, allEntries=true), @CacheEvict(value={"user-permissions"}, allEntries=true)})
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
    @Caching(evict={@CacheEvict(value={"roles"}, key="#roleId"), @CacheEvict(value={"user-permissions"}, allEntries=true)})
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
    @Caching(evict={@CacheEvict(value={"roles"}, key="#roleId"), @CacheEvict(value={"user-permissions"}, allEntries=true)})
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
