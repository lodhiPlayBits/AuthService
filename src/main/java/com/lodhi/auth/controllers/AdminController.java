package com.lodhi.auth.controllers;

import com.lodhi.auth.dtos.request.*;
import com.lodhi.auth.dtos.response.*;
import com.lodhi.auth.services.PermissionService;
import com.lodhi.auth.services.RoleService;
import com.lodhi.auth.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin:role:manage')")
public class AdminController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserService userService;

    // ==================== ROLE MANAGEMENT ====================
    
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleResponseDTO> createRole(@Valid @RequestBody CreateRoleRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(requestDTO));
    }

    @DeleteMapping("/roles/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<RoleResponseDTO> assignPermissionsToRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignPermissionsRequestDTO requestDTO) {
        return ResponseEntity.ok(roleService.assignPermissionsToRole(roleId, requestDTO.getPermissionIds()));
    }

    @DeleteMapping("/roles/{roleId}/permissions")
    public ResponseEntity<RoleResponseDTO> revokePermissionsFromRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignPermissionsRequestDTO requestDTO) {
        return ResponseEntity.ok(roleService.revokePermissionsFromRole(roleId, requestDTO.getPermissionIds()));
    }

    // ==================== PERMISSION MANAGEMENT ====================

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<List<PermissionResponseDTO>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @PostMapping("/permissions")
    public ResponseEntity<PermissionResponseDTO> createPermission(@Valid @RequestBody CreatePermissionRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.createPermission(requestDTO));
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== USER ROLE MANAGEMENT ====================

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<CreateUserResponseDTO> assignRolesToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AssignRolesRequestDTO requestDTO) {
        return ResponseEntity.ok(userService.assignRolesToUser(userId, requestDTO.getRoleIds()));
    }
}
