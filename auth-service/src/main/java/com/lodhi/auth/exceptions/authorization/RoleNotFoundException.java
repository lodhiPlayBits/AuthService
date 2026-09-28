package com.lodhi.auth.exceptions.authorization;

import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends AuthServiceException {
    public RoleNotFoundException(String roleName) {
        super("Role not found: " + roleName, HttpStatus.NOT_FOUND, ErrorCode.ROLE_NOT_FOUND);
    }
}