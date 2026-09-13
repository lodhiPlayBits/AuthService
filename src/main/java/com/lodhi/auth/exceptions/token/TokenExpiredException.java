package com.lodhi.auth.exceptions.token;

import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AuthServiceException {
    public TokenExpiredException() {
        super("Token has expired", HttpStatus.UNAUTHORIZED, ErrorCode.TOKEN_EXPIRED);
    }
}