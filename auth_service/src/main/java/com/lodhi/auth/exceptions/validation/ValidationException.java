package com.lodhi.auth.exceptions.validation;

import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class ValidationException extends AuthServiceException {
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
    }
}
