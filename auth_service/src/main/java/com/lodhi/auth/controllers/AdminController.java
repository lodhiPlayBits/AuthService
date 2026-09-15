package com.lodhi.auth.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lodhi.auth.dtos.request.AssignPermissionsRequestDTO;
import com.lodhi.auth.dtos.request.AssignRolesRequestDTO;
import com.lodhi.auth.dtos.request.CreatePermissionRequestDTO;
import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.dtos.response.PagedResponse;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;
import com.lodhi.auth.security.RequiresPermission;
import com.lodhi.auth.services.PermissionService;
import com.lodhi.auth.services.RoleService;
import com.lodhi.auth.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserService userService;

    // ==================== ROLE MANAGEMENT ====================
    
    @GetMapping("/roles")
    @RequiresPermission("roles:read")
    public ResponseEntity<PagedResponse<RoleResponseDTO>> getAllRoles(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "name") String sortBy) {
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sortBy));
        
        org.springframework.data.domain.Page<RoleResponseDTO> rolesPage = roleService.getAllRoles(pageable);
        return ResponseEntity.ok(PagedResponse.of(rolesPage));
    }

    @PostMapping("/roles")
    @RequiresPermission("roles:create")
    public ResponseEntity<RoleResponseDTO> createRole(@Valid @RequestBody CreateRoleRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(requestDTO));
    }

    @DeleteMapping("/roles/{roleId}")
    @RequiresPermission("roles:delete")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/roles/{roleId}/permissions")
    @RequiresPermission("roles:update")
    public ResponseEntity<RoleResponseDTO> assignPermissionsToRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignPermissionsRequestDTO requestDTO) {
        return ResponseEntity.ok(roleService.assignPermissionsToRole(roleId, requestDTO.getPermissionIds()));
    }

    @DeleteMapping("/roles/{roleId}/permissions")
    @RequiresPermission("roles:update")
    public ResponseEntity<RoleResponseDTO> revokePermissionsFromRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignPermissionsRequestDTO requestDTO) {
        return ResponseEntity.ok(roleService.revokePermissionsFromRole(roleId, requestDTO.getPermissionIds()));
    }

    // ==================== PERMISSION MANAGEMENT ====================

    @GetMapping("/permissions")
    @RequiresPermission("permissions:read")
    public ResponseEntity<PagedResponse<PermissionResponseDTO>> getAllPermissions(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "name") String sortBy) {
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sortBy));
        
        org.springframework.data.domain.Page<PermissionResponseDTO> permissionsPage = 
            permissionService.getAllPermissions(pageable);
        return ResponseEntity.ok(PagedResponse.of(permissionsPage));
    }

    @PostMapping("/permissions")
    @RequiresPermission("permissions:create")
    public ResponseEntity<PermissionResponseDTO> createPermission(@Valid @RequestBody CreatePermissionRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.createPermission(requestDTO));
    }

    @DeleteMapping("/permissions/{permissionId}")
    @RequiresPermission("permissions:delete")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== USER ROLE MANAGEMENT ====================
    
    @GetMapping("/users")
    @RequiresPermission("users:read")
    public ResponseEntity<PagedResponse<CreateUserResponseDTO>> getAllUsers(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "id") String sortBy) {
        
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(sortBy));
        
        org.springframework.data.domain.Page<CreateUserResponseDTO> usersPage = userService.getAllUsers(pageable);
        return ResponseEntity.ok(PagedResponse.of(usersPage));
    }

    @PutMapping("/users/{userId}/roles")
    @RequiresPermission("users:assign-roles")
    public ResponseEntity<CreateUserResponseDTO> assignRolesToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRolesRequestDTO requestDTO) {
        return ResponseEntity.ok(userService.assignRolesToUser(userId, requestDTO.getRoleIds()));
    }
}
