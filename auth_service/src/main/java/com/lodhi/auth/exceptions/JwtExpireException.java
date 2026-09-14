package com.lodhi.auth.exceptions;

public class JwtExpireException extends RuntimeException {
    public JwtExpireException(String message) {
        super(message);
    }

    public JwtExpireException() {
        super("JWT token has expired");
    }
}
