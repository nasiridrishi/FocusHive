package com.focushive.api.security;

import com.focushive.api.dto.identity.PersonaDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Custom principal that includes user information and active persona from Identity Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityPrincipal implements Serializable {
    private UUID userId;
    private String username;
    private String email;
    private PersonaDto activePersona;
    
    /**
     * Get the display name from active persona or fallback to username.
     */
    public String getDisplayName() {
        if (activePersona != null && activePersona.getProfile() != null) {
            return activePersona.getProfile().getDisplayName();
        }
        return username;
    }
    
    /**
     * Get the avatar URL from active persona.
     */
    public String getAvatarUrl() {
        if (activePersona != null && activePersona.getProfile() != null) {
            return activePersona.getProfile().getAvatarUrl();
        }
        return null;
    }
}