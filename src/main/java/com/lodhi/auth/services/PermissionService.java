package com.lodhi.auth.services;

import java.util.List;
import java.util.UUID;

import com.lodhi.auth.dtos.request.CreatePermissionRequestDTO;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.model.Permission;

public interface PermissionService {
    Permission getPermissionByName(String name);
    Permission getPermissionById(UUID id);
    List<PermissionResponseDTO> getAllPermissions();
    PermissionResponseDTO createPermission(CreatePermissionRequestDTO requestDTO);
    void deletePermission(UUID id);
}
