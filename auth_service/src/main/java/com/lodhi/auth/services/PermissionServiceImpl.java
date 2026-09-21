package com.lodhi.auth.services;

import java.util.UUID;

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
    private final AuditService auditService;

    @Override
    @Cacheable(value={"permissions"}, key="#name")
    public Permission getPermissionByName(String name) {
        return permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", name));
    }

    @Override
    @Cacheable(value={"permissions"}, key="#id")
    public Permission getPermissionById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }

    @Override
    @Cacheable(value={"permissions:all"})
    public Page<PermissionResponseDTO> getAllPermissions(Pageable pageable) {
        // Enforce maximum page size to prevent unbounded queries
        int maxPageSize = 100;
        if (pageable.getPageSize() > maxPageSize) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                maxPageSize, 
                pageable.getSort()
            );
        }
        
        Page<Permission> permissionPage = permissionRepository.findAll(pageable);
        return permissionPage.map(this::mapToPermissionResponseDTO);
    }

    @Override
    @Transactional
    @Caching(evict={@CacheEvict(value={"permissions"}, allEntries=true), @CacheEvict(value={"permissions:all"}, allEntries=true)})
    public PermissionResponseDTO createPermission(CreatePermissionRequestDTO requestDTO) {
        if (permissionRepository.existsByName(requestDTO.getName())) {
            throw new ValidationException("Permission already exists: " + requestDTO.getName());
        }
        
        Permission permission = new Permission();
        permission.setName(requestDTO.getName());
        permission.setDescription(requestDTO.getDescription());
        
        Permission savedPermission = permissionRepository.save(permission);
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.PERMISSION_CREATED, true,
            "Permission created: " + requestDTO.getName());
        
        return mapToPermissionResponseDTO(savedPermission);
    }

    @Override
    @Transactional
    @Caching(evict={@CacheEvict(value={"permissions"}, key="#id"), @CacheEvict(value={"permissions:all"}, allEntries=true), @CacheEvict(value={"user-permissions"}, allEntries=true), @CacheEvict(value={"roles"}, allEntries=true)})
    public void deletePermission(UUID id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission", id);
        }
        
        Permission permission = getPermissionById(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.PERMISSION_DELETED, true,
            "Permission deleted: " + permission.getName());
        
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
