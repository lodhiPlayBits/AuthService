package com.lodhi.auth.respositories;


import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository< Role, UUID> {
    Optional<Role> findByRoleType(RoleType roles);
}
