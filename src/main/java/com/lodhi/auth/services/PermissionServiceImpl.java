package com.lodhi.auth.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lodhi.auth.dtos.request.CreatePermissionRequestDTO;
import com.lodhi.auth.dtos.response.PermissionResponseDTO;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.exceptions.validation.ValidationException;
import com.lodhi.auth.model.Permission;
import com.lodhi.auth.respositories.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    public Permission getPermissionByName(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", name));
    }

    @Override
    public Permission getPermissionById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }

    @Override
    public List<PermissionResponseDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToPermissionResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PermissionResponseDTO createPermission(CreatePermissionRequestDTO requestDTO) {
        if (permissionRepository.existsByName(requestDTO.getName())) {
            throw new ValidationException("Permission already exists: " + requestDTO.getName());
        }
        
        Permission permission = new Permission();
        permission.setName(requestDTO.getName());
        permission.setDescription(requestDTO.getDescription());
        
        Permission savedPermission = permissionRepository.save(permission);
        return mapToPermissionResponseDTO(savedPermission);
    }

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission", id);
        }
        permissionRepository.deleteById(id);
    }
    
    private PermissionResponseDTO mapToPermissionResponseDTO(Permission permission) {
        return PermissionResponseDTO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }
}
