package com.lodhi.auth.respositories;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lodhi.auth.model.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
    
    /**
     * Find role with permissions eagerly fetched.
     * Use this when you need role with its permissions to avoid N+1 queries.
     */
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE r.name = :name")
    Optional<Role> findByNameWithPermissions(@org.springframework.data.repository.query.Param("name") String name);
    
    /**
     * Find role with permissions eagerly fetched by ID.
     */
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@org.springframework.data.repository.query.Param("id") UUID id);
}
