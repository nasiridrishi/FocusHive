package com.focushive.backend.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Principal object representing authenticated user from Identity Service.
 * Contains user identity and active persona information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityServicePrincipal implements Serializable {
    private UUID userId;
    private String email;
    private String activePersonaId;
    
    /**
     * Get the active persona ID as UUID.
     * Returns null if no active persona is set.
     */
    public UUID getActivePersonaUUID() {
        if (activePersonaId == null || activePersonaId.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(activePersonaId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}