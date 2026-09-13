package com.lodhi.auth.respositories;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.model.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByRoleType(RoleType roles);
    boolean existsByRoleType(RoleType roleType);
}
