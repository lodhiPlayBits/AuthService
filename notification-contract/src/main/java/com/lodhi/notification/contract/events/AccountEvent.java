package com.lodhi.notification.contract.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Single wire schema for account events on the {@code account-events} topic.
 * Produced by auth-service, consumed by auth-service SSE and notification-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private AccountEventType type;
    private Long userId;
    private String email;
    private String message;
    private transient Object payload;  // Excluded from serialization
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
