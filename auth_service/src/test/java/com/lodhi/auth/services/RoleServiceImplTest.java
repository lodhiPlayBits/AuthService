package com.lodhi.auth.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lodhi.auth.audit.AuditService;
import com.lodhi.auth.dtos.request.CreateRoleRequestDTO;
import com.lodhi.auth.dtos.response.RoleResponseDTO;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.exceptions.validation.ValidationException;
import com.lodhi.auth.model.Permission;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.respositories.PermissionRepository;
import com.lodhi.auth.respositories.RoleRepository;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
    private Permission permission;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(UUID.randomUUID())
                .name("MANAGER")
                .description("Manager Role")
                .systemRole(false)
                .permissions(new HashSet<>())
                .build();

        permission = Permission.builder()
                .id(UUID.randomUUID())
                .name("READ_PRIVILEGES")
                .description("Can read")
                .build();

        // Mock Security Context
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("admin_user");
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetRoleByName_Success() {
        when(roleRepository.findByNameWithPermissions("MANAGER")).thenReturn(Optional.of(role));
        Role result = roleService.getRoleByName("MANAGER");
        assertEquals("MANAGER", result.getName());
    }

    @Test
    void testGetRoleByName_NotFound() {
        when(roleRepository.findByNameWithPermissions("MANAGER")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> roleService.getRoleByName("MANAGER"));
    }

    @Test
    void testCreateRole_Success() {
        CreateRoleRequestDTO req = new CreateRoleRequestDTO();
        req.setRoleName("NEW_ROLE");
        req.setDescription("New Role Desc");

        when(roleRepository.existsByName("NEW_ROLE")).thenReturn(false);
        
        Role savedRole = Role.builder()
                .id(UUID.randomUUID())
                .name("NEW_ROLE")
                .description("New Role Desc")
                .systemRole(false)
                .permissions(new HashSet<>())
                .build();
                
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        RoleResponseDTO res = roleService.createRole(req);

        assertNotNull(res);
        assertEquals("NEW_ROLE", res.getRoleName());
        verify(auditService).logServiceEvent(isNull(), eq("admin_user"), any(), eq(true), anyString());
    }

    @Test
    void testCreateRole_InvalidFormat() {
        CreateRoleRequestDTO req = new CreateRoleRequestDTO();
        req.setRoleName("invalid name");
        assertThrows(ValidationException.class, () -> roleService.createRole(req));
    }

    @Test
    void testCreateRole_AlreadyExists() {
        CreateRoleRequestDTO req = new CreateRoleRequestDTO();
        req.setRoleName("MANAGER");
        when(roleRepository.existsByName("MANAGER")).thenReturn(true);
        assertThrows(ValidationException.class, () -> roleService.createRole(req));
    }

    @Test
    void testDeleteRole_Success() {
        when(roleRepository.findByIdWithPermissions(role.getId())).thenReturn(Optional.of(role));
        roleService.deleteRole(role.getId());
        verify(roleRepository).delete(role);
        verify(auditService).logServiceEvent(isNull(), eq("admin_user"), any(), eq(true), anyString());
    }

    @Test
    void testDeleteRole_SystemRole() {
        role.setSystemRole(true);
        when(roleRepository.findByIdWithPermissions(role.getId())).thenReturn(Optional.of(role));
        assertThrows(ValidationException.class, () -> roleService.deleteRole(role.getId()));
    }

    @Test
    void testAssignPermissionsToRole_Success() {
        when(roleRepository.findByIdWithPermissions(role.getId())).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        when(roleRepository.save(role)).thenReturn(role);

        RoleResponseDTO res = roleService.assignPermissionsToRole(role.getId(), Set.of(permission.getId()));
        
        assertEquals(1, role.getPermissions().size());
        assertTrue(role.getPermissions().contains(permission));
        assertEquals(1, res.getPermissions().size());
    }
}
