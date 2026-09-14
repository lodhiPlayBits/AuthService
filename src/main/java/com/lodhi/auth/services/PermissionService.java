package com.lodhi.auth.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lodhi.auth.dtos.request.CreatePermissionRequestDTO;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.model.Permission;

public interface PermissionService {
    Permission getPermissionByName(String name);
    Permission getPermissionById(UUID id);
    
    /**
     * Get all permissions with pagination support.
     * Maximum page size is enforced to prevent unbounded queries.
     * 
     * @param pageable pagination parameters (page number, size, sort)
     * @return paginated list of permissions
     */
    Page<PermissionResponseDTO> getAllPermissions(Pageable pageable);
    
    PermissionResponseDTO createPermission(CreatePermissionRequestDTO requestDTO);
    void deletePermission(UUID id);
}
