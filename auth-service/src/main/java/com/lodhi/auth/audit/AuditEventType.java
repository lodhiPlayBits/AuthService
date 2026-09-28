package com.lodhi.auth.audit;

public enum AuditEventType {
    // Authentication events
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_REFRESH,
    
    // Account management events
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    PASSWORD_CHANGED,
    
    // User CRUD events
    USER_REGISTERED,
    USER_UPDATED,
    USER_DELETED,
    
    // Role management events (high privilege)
    ROLE_CREATED,
    ROLE_DELETED,
    ROLE_PERMISSIONS_ASSIGNED,
    ROLE_PERMISSIONS_REVOKED,
    
    // Permission management events (high privilege)
    PERMISSION_CREATED,
    PERMISSION_DELETED,
    
    // User authorization events (high privilege)
    USER_ROLES_ASSIGNED
}
