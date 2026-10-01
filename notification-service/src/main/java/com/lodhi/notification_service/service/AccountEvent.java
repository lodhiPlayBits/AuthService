package com.lodhi.notification_service.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Base event for account-related notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEvent {
    
    private String eventId;
    private AccountEventType type;
    private Long userId;
    private String email;
    private String message;
    private Object payload;
    private Instant timestamp;
    
    public enum AccountEventType {
        ACCOUNT_DISABLED,
        ACCOUNT_ENABLED,
        ACCOUNT_DELETED,
        PASSWORD_CHANGED,
        ROLE_CHANGED,
        SESSION_REVOKED
    }
}
