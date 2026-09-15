package com.lodhi.auth.respositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lodhi.auth.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByPhoneNumber(String phone);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phone);
    
    /**
     * Find user with roles and permissions eagerly fetched.
     * Use this for authentication/authorization where roles/permissions are needed.
     * Uses JOIN FETCH to avoid N+1 queries.
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithRolesAndPermissions(@org.springframework.data.repository.query.Param("email") String email);
    
    /**
     * Find user with roles and permissions eagerly fetched by username.
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.username = :username")
    Optional<User> findByUsernameWithRolesAndPermissions(@org.springframework.data.repository.query.Param("username") String username);
    
    /**
     * Find user with roles and permissions eagerly fetched by ID.
     */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@org.springframework.data.repository.query.Param("id") Long id);
}
