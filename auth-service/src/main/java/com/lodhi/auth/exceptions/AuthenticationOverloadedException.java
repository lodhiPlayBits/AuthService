package com.lodhi.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AuthenticationOverloadedException extends RuntimeException {
    public AuthenticationOverloadedException(String message) {
        super(message);
    }
}
