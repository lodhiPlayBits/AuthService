package com.lodhi.notification_service.websocket;

import java.security.Principal;
import java.util.List;

/**
 * STOMP session principal derived from the JWT handshake.
 *
 * <p>The name is the user id so that user destinations ({@code /user/queue/**})
 * resolve to one user, and {@link #isAdmin()} drives subscription authorization.
 */
public record StompPrincipal(Long userId, List<String> roles) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }
}
