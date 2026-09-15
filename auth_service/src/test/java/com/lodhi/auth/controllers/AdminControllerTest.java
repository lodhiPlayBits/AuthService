package com.lodhi.auth.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.lodhi.auth.dtos.request.AssignPermissionsRequestDTO;
import com.lodhi.auth.dtos.request.AssignRolesRequestDTO;
import com.lodhi.auth.dtos.request.CreatePermissionRequestDTO;
import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.dtos.response.PagedResponse;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;
import com.lodhi.auth.services.PermissionService;
import com.lodhi.auth.services.RoleService;
import com.lodhi.auth.services.UserService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private RoleService roleService;
    
    @Mock
    private PermissionService permissionService;
    
    @Mock
    private UserService userService;

    @InjectMocks
    private AdminController adminController;

    @Test
    void getAllRoles_ShouldReturnPagedResponse() {
        Page<RoleResponseDTO> page = new PageImpl<>(java.util.Collections.emptyList());
        when(roleService.getAllRoles(any(Pageable.class))).thenReturn(page);
        
        ResponseEntity<PagedResponse<RoleResponseDTO>> entity = adminController.getAllRoles(0, 20, "name");
        
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(0, entity.getBody().getTotalElements());
    }

    @Test
    void createRole_ShouldReturnCreated() {
        CreateRoleRequestDTO req = new CreateRoleRequestDTO();
        RoleResponseDTO res = new RoleResponseDTO();
        when(roleService.createRole(req)).thenReturn(res);
        
        ResponseEntity<RoleResponseDTO> entity = adminController.createRole(req);
        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
        assertEquals(res, entity.getBody());
    }

    @Test
    void deleteRole_ShouldReturnNoContent() {
        UUID id = UUID.randomUUID();
        ResponseEntity<Void> entity = adminController.deleteRole(id);
        verify(roleService).deleteRole(id);
        assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
    }

    @Test
    void assignPermissionsToRole_ShouldReturnOk() {
        UUID id = UUID.randomUUID();
        AssignPermissionsRequestDTO req = new AssignPermissionsRequestDTO();
        req.setPermissionIds(Set.of(UUID.randomUUID()));
        RoleResponseDTO res = new RoleResponseDTO();
        when(roleService.assignPermissionsToRole(id, req.getPermissionIds())).thenReturn(res);
        
        ResponseEntity<RoleResponseDTO> entity = adminController.assignPermissionsToRole(id, req);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    void revokePermissionsFromRole_ShouldReturnOk() {
        UUID id = UUID.randomUUID();
        AssignPermissionsRequestDTO req = new AssignPermissionsRequestDTO();
        req.setPermissionIds(Set.of(UUID.randomUUID()));
        RoleResponseDTO res = new RoleResponseDTO();
        when(roleService.revokePermissionsFromRole(id, req.getPermissionIds())).thenReturn(res);
        
        ResponseEntity<RoleResponseDTO> entity = adminController.revokePermissionsFromRole(id, req);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    void getAllPermissions_ShouldReturnPagedResponse() {
        Page<PermissionResponseDTO> page = new PageImpl<>(java.util.Collections.emptyList());
        when(permissionService.getAllPermissions(any(Pageable.class))).thenReturn(page);
        
        ResponseEntity<PagedResponse<PermissionResponseDTO>> entity = adminController.getAllPermissions(0, 20, "name");
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    void createPermission_ShouldReturnCreated() {
        CreatePermissionRequestDTO req = new CreatePermissionRequestDTO();
        PermissionResponseDTO res = new PermissionResponseDTO();
        when(permissionService.createPermission(req)).thenReturn(res);
        
        ResponseEntity<PermissionResponseDTO> entity = adminController.createPermission(req);
        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
    }

    @Test
    void deletePermission_ShouldReturnNoContent() {
        UUID id = UUID.randomUUID();
        ResponseEntity<Void> entity = adminController.deletePermission(id);
        verify(permissionService).deletePermission(id);
        assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
    }

    @Test
    void getAllUsers_ShouldReturnPagedResponse() {
        Page<CreateUserResponseDTO> page = new PageImpl<>(java.util.Collections.emptyList());
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);
        
        ResponseEntity<PagedResponse<CreateUserResponseDTO>> entity = adminController.getAllUsers(0, 20, "id");
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    void assignRolesToUser_ShouldReturnOk() {
        AssignRolesRequestDTO req = new AssignRolesRequestDTO();
        req.setRoleIds(Set.of(UUID.randomUUID()));
        CreateUserResponseDTO res = new CreateUserResponseDTO();
        when(userService.assignRolesToUser(1L, req.getRoleIds())).thenReturn(res);
        
        ResponseEntity<CreateUserResponseDTO> entity = adminController.assignRolesToUser(1L, req);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }
}
