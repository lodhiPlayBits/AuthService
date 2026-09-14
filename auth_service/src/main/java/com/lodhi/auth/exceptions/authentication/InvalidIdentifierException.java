package com.lodhi.auth.exceptions.authentication;


import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidIdentifierException extends AuthServiceException {
    public InvalidIdentifierException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ErrorCode.INVALID_IDENTIFIER);
    }
}