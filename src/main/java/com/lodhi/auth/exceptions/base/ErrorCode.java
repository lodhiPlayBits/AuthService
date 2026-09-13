package com.lodhi.auth.exceptions.base;

public enum ErrorCode {
    // Authentication
    UNAUTHORIZED,
    ACCOUNT_LOCKED,
    INVALID_IDENTIFIER,
    
    // Authorization
    INSUFFICIENT_PERMISSION,
    ROLE_NOT_FOUND,
    
    // Registration
    USER_ALREADY_EXISTS,
    WEAK_PASSWORD,
    
    // Token
    INVALID_TOKEN,
    TOKEN_EXPIRED,
    REFRESH_TOKEN_REUSED,
    
    // Validation
    VALIDATION_ERROR,
    
    // Resource
    RESOURCE_NOT_FOUND,
    
    // General
    INTERNAL_SERVER_ERROR
}