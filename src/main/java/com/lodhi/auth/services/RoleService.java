package com.lodhi.auth.services;

import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.model.Role;
import org.springframework.stereotype.Service;



public interface RoleService {

    Role getRole(RoleType roleType);

}
