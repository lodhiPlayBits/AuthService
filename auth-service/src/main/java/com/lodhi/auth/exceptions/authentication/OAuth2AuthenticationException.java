package com.lodhi.auth.exceptions.authentication;

import com.lodhi.auth.exceptions.base.AuthServiceException;
import com.lodhi.auth.exceptions.base.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when OAuth2 authentication fails (invalid token, unverified email,
 * duplicate email with different provider, etc.)
 */
public class OAuth2AuthenticationException extends AuthServiceException {

    public OAuth2AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, ErrorCode.OAUTH2_ERROR);
    }
}
