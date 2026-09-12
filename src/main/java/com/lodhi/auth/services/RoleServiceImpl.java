package com.lodhi.auth.services;

import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.respositories.RoleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role getRole(RoleType roleType){
        return roleRepository.findByRoleType(roleType)
                .orElseThrow(() ->
                        new IllegalStateException("Role not found: " + roleType));

    }


}
