package com.lodhi.auth.exceptions.authorization;

import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class InsufficientPermissionException extends AuthServiceException {
    public InsufficientPermissionException(String requiredPermission) {
        super("Missing required permission: " + requiredPermission, HttpStatus.FORBIDDEN, ErrorCode.INSUFFICIENT_PERMISSION);
    }
}