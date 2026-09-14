package com.lodhi.auth.enums;

public enum PermissionType {
    // User permissions
    USER_READ("user:read", "Read user profile"),
    USER_UPDATE("user:update", "Update own profile"),
    USER_DELETE("user:delete", "Delete own account"),
    
    // Admin permissions
    ADMIN_READ("admin:read", "Read all users"),
    ADMIN_CREATE("admin:create", "Create new users"),
    ADMIN_UPDATE("admin:update", "Update any user"),
    ADMIN_DELETE("admin:delete", "Delete any user"),
    ADMIN_ROLE_MANAGE("admin:role:manage", "Manage user roles"),
    
    // Audit permissions
    AUDIT_READ("audit:read", "Read audit logs"),
    AUDIT_MANAGE("audit:manage", "Manage audit logs");

    private final String permission;
    private final String description;

    PermissionType(String permission, String description) {
        this.permission = permission;
        this.description = description;
    }

    public String getPermission() {
        return permission;
    }

    public String getDescription() {
        return description;
    }
}
