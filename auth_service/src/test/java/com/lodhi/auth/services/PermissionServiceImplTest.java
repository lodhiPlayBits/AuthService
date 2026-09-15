package com.lodhi.auth.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lodhi.auth.audit.AuditService;
import com.lodhi.auth.dtos.request.CreatePermissionRequestDTO;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.exceptions.validation.ValidationException;
import com.lodhi.auth.model.Permission;
import com.lodhi.auth.respositories.PermissionRepository;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Permission permission;

    @BeforeEach
    void setUp() {
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
    void testGetPermissionByName_Success() {
        when(permissionRepository.findByName("READ_PRIVILEGES")).thenReturn(Optional.of(permission));
        Permission result = permissionService.getPermissionByName("READ_PRIVILEGES");
        assertEquals("READ_PRIVILEGES", result.getName());
    }

    @Test
    void testGetPermissionByName_NotFound() {
        when(permissionRepository.findByName("READ_PRIVILEGES")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> permissionService.getPermissionByName("READ_PRIVILEGES"));
    }

    @Test
    void testCreatePermission_Success() {
        CreatePermissionRequestDTO req = new CreatePermissionRequestDTO();
        req.setName("WRITE_PRIVILEGES");
        req.setDescription("Can write");

        when(permissionRepository.existsByName("WRITE_PRIVILEGES")).thenReturn(false);
        
        Permission savedPermission = Permission.builder()
                .id(UUID.randomUUID())
                .name("WRITE_PRIVILEGES")
                .description("Can write")
                .build();
                
        when(permissionRepository.save(any(Permission.class))).thenReturn(savedPermission);

        PermissionResponseDTO res = permissionService.createPermission(req);

        assertNotNull(res);
        assertEquals("WRITE_PRIVILEGES", res.getName());
        verify(auditService).logServiceEvent(isNull(), eq("admin_user"), any(), eq(true), anyString());
    }

    @Test
    void testCreatePermission_AlreadyExists() {
        CreatePermissionRequestDTO req = new CreatePermissionRequestDTO();
        req.setName("READ_PRIVILEGES");
        when(permissionRepository.existsByName("READ_PRIVILEGES")).thenReturn(true);
        assertThrows(ValidationException.class, () -> permissionService.createPermission(req));
    }

    @Test
    void testDeletePermission_Success() {
        when(permissionRepository.existsById(permission.getId())).thenReturn(true);
        when(permissionRepository.findById(permission.getId())).thenReturn(Optional.of(permission));
        
        permissionService.deletePermission(permission.getId());
        
        verify(permissionRepository).deleteById(permission.getId());
        verify(auditService).logServiceEvent(isNull(), eq("admin_user"), any(), eq(true), anyString());
    }

    @Test
    void testDeletePermission_NotFound() {
        when(permissionRepository.existsById(permission.getId())).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> permissionService.deletePermission(permission.getId()));
    }
}
