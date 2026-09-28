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
import com.lodhi.auth.dtos.cache.RoleCache;
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

/**
 * Role service implementation.
 *
 * CACHING STRATEGY — why we cache RoleCache, not Role:
 *
 *   Role.permissions is fetched LAZY. If we cached the JPA entity directly,
 *   Redis deserialization (which happens outside a Hibernate session) would hit
 *   the PersistentSet and throw LazyInitializationException → SerializationException.
 *
 *   Instead we map to RoleCache (plain Java DTO, no Hibernate dependencies) inside
 *   the @Transactional boundary while the session is still open, then return the
 *   RoleCache to Redis. On a cache hit Redis returns RoleCache directly without
 *   ever touching Hibernate.
 *
 *   Callers that need a full Role entity for persistence (e.g. GoogleOAuth2Service)
 *   use roleCacheToEntity() to reassemble a detached Role from the cache.
 */
@RequiredArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    // ─── Cache-safe lookup methods ──────────────────────────────────────────

    /**
     * Returns a RoleCache (stored in Redis) instead of a JPA entity.
     * All fields — including permissions — are plain Java types.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#roleName")
    public RoleCache getRoleByName(String roleName) {
        Role role = roleRepository.findByNameWithPermissions(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
        return toRoleCache(role);
    }

    /**
     * Returns a RoleCache (stored in Redis) by UUID.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#roleId")
    public RoleCache getRoleById(UUID roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        return toRoleCache(role);
    }

    // ─── Entity reconstruction for internal callers ─────────────────────────

    /**
     * Convenience method: fetch (or use cached) RoleCache, then turn it back into
     * a detached Role entity that is safe to attach to new JPA entities.
     *
     * The returned Role has its permissions eagerly populated from the cache,
     * so no Hibernate session is needed downstream.
     */
    @Transactional(readOnly = true)
    public Role getRoleEntityByName(String roleName) {
        RoleCache cache = getRoleByName(roleName);
        return roleCacheToEntity(cache);
    }

    @Transactional(readOnly = true)
    public Role getRoleEntityById(UUID roleId) {
        RoleCache cache = getRoleById(roleId);
        return roleCacheToEntity(cache);
    }

    // ─── Paginated listing ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponseDTO> getAllRoles(Pageable pageable) {
        int maxPageSize = 100;
        if (pageable.getPageSize() > maxPageSize) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    maxPageSize,
                    pageable.getSort());
        }
        Page<Role> rolePage = roleRepository.findAll(pageable);
        return rolePage.map(this::mapToRoleResponseDTO);
    }

    // ─── Mutation methods ───────────────────────────────────────────────────

    @Override
    @Transactional
    @Caching(evict = {
            // Evict ALL role cache entries (both name-keyed and UUID-keyed).
            // Evicting only by roleId would leave the name-keyed entry stale.
            // e.g. roles::USER can persist while roles::<UUID> is evicted.
            // Since permissions are security-sensitive, we always wipe the whole cache.
            @CacheEvict(value = "roles", allEntries = true)
    })
    public RoleResponseDTO createRole(CreateRoleRequestDTO requestDTO) {
        String roleName = requestDTO.getRoleName().toUpperCase().trim();

        if (!roleName.matches("^[A-Z_]+$")) {
            throw new ValidationException("Role name must contain only uppercase letters and underscores");
        }
        if (roleRepository.existsByName(roleName)) {
            throw new ValidationException("Role already exists: " + roleName);
        }

        Role role = Role.builder()
                .name(roleName)
                .description(requestDTO.getDescription())
                .systemRole(false)
                .permissions(new HashSet<>())
                .build();

        Role savedRole = roleRepository.save(role);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.ROLE_CREATED, true,
                "Role created: " + roleName);

        return mapToRoleResponseDTO(savedRole);
    }

    @Override
    @Transactional
    @Caching(evict = {
            // Wipe entire roles cache — a deleted role may have been cached under both
            // its name key and its UUID key. allEntries ensures no stale entry remains.
            @CacheEvict(value = "roles", allEntries = true)
    })
    public void deleteRole(UUID roleId) {
        RoleCache cache = getRoleById(roleId);

        if (cache.isSystemRole()) {
            throw new ValidationException("Cannot delete system role: " + cache.getName());
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.ROLE_DELETED, true,
                "Role deleted: " + cache.getName());

        roleRepository.deleteById(roleId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            // P0 security fix: permissions changed → evict ALL role cache entries.
            // If we only evict by roleId (UUID key), the name-keyed entry (e.g. roles::USER)
            // remains stale and callers will see outdated permissions until TTL expires.
            @CacheEvict(value = "roles", allEntries = true)
    })
    public RoleResponseDTO assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds) {
        // Fetch fresh entity from DB (not from cache) for mutation
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        Set<Permission> permissions = permissionIds.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", id)))
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
    @Caching(evict = {
            // P0 security fix: same reasoning as assignPermissionsToRole above.
            // allEntries evicts both name-keyed (roles::USER) and UUID-keyed (roles::<uuid>) entries.
            @CacheEvict(value = "roles", allEntries = true)
    })
    public RoleResponseDTO revokePermissionsFromRole(UUID roleId, Set<UUID> permissionIds) {
        // Fetch fresh entity from DB (not from cache) for mutation
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));

        Set<Permission> permissionsToRevoke = permissionIds.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", id)))
                .collect(Collectors.toSet());

        role.getPermissions().removeAll(permissionsToRevoke);
        Role updatedRole = roleRepository.save(role);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        auditService.logServiceEvent(null, username, AuditEventType.ROLE_PERMISSIONS_REVOKED, true,
                "Revoked " + permissionsToRevoke.size() + " permissions from role: " + role.getName());

        return mapToRoleResponseDTO(updatedRole);
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    /**
     * Map JPA Role (session open) → RoleCache (plain Java, session-independent).
     * Called inside @Transactional so permissions are accessible.
     */
    private RoleCache toRoleCache(Role role) {
        Set<String> permissionNames = role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return new RoleCache(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                permissionNames);
    }

    /**
     * Reconstruct a detached Role entity from a RoleCache.
     * Permissions are loaded from the DB so the entity can be persisted safely
     * (e.g. when assigning to a new User).
     */
    private Role roleCacheToEntity(RoleCache cache) {
        Set<Permission> permissions = permissionRepository.findByNameIn(cache.getPermissionNames());

        return Role.builder()
                .id(cache.getId())
                .name(cache.getName())
                .description(cache.getDescription())
                .systemRole(cache.isSystemRole())
                .permissions(new HashSet<>(permissions))
                .build();
    }

    private RoleResponseDTO mapToRoleResponseDTO(Role role) {
        Set<PermissionResponseDTO> permissionDTOs = role.getPermissions().stream()
                .map(p -> PermissionResponseDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
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
