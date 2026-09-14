package com.lodhi.auth.exceptions.registration;


import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends AuthServiceException {
    public UserAlreadyExistsException(String identifier) {
        super("An account already exists for: " + identifier, HttpStatus.CONFLICT, ErrorCode.USER_ALREADY_EXISTS);
    }
}
