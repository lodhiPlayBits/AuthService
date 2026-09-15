package com.lodhi.auth.exceptions.token;


import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AuthServiceException {
    public InvalidTokenException() {
        super("Token is invalid or malformed", HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN);
    }
}