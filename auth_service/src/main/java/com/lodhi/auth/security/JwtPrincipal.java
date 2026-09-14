package com.lodhi.auth.security;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the authenticated principal extracted from JWT token.
 * This is set as the principal in Spring Security's Authentication object.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The user's unique identifier
     */
    private Long userId;

    /**
     * The user's roles (e.g., USER, ADMIN, MODERATOR)
     */
    private List<String> roles;

    /**
     * The user's permissions (e.g., user:read, admin:create)
     */
    private Set<String> permissions;

    /**
     * JWT token ID (jti claim) for token tracking
     */
    private String jti;

    @Override
    public String toString() {
        return "JwtPrincipal{" +
                "userId=" + userId +
                ", roles=" + roles +
                ", permissions=" + permissions +
                '}';
    }
}
