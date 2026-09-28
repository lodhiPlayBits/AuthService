package com.lodhi.auth.dtos.cache;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lightweight, Redis-safe DTO for caching Role data.
 *
 * Why this exists:
 *   Caching the JPA Role entity directly causes SerializationException because
 *   Role.permissions is fetched LAZY. When Redis deserializes the cached object
 *   there is no active Hibernate session, so any access to the permissions
 *   PersistentSet throws LazyInitializationException.
 *
 *   By mapping to this DTO (plain Java, no Hibernate dependencies) BEFORE
 *   handing the object to Redis, serialization and deserialization always work.
 */
public class RoleCache implements Serializable {

    private final UUID id;
    private final String name;
    private final String description;
    private final boolean systemRole;
    private final Set<String> permissionNames;

    @JsonCreator
    public RoleCache(
            @JsonProperty("id")              UUID id,
            @JsonProperty("name")            String name,
            @JsonProperty("description")     String description,
            @JsonProperty("systemRole")      boolean systemRole,
            @JsonProperty("permissionNames") Set<String> permissionNames) {
        this.id              = id;
        this.name            = name;
        this.description     = description;
        this.systemRole      = systemRole;
        this.permissionNames = permissionNames;
    }

    public UUID    getId()              { return id;              }
    public String  getName()            { return name;            }
    public String  getDescription()     { return description;     }
    public boolean isSystemRole()       { return systemRole;      }
    public Set<String> getPermissionNames() { return permissionNames; }
}
