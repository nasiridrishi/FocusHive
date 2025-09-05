package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for authentication operations.
 * Contains JWT tokens and user information including active persona.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    
    // JWT tokens
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // seconds
    
    // User information
    private UUID userId;
    private String username;
    private String email;
    private String displayName;
    
    // Active persona information
    private PersonaInfo activePersona;
    
    // Available personas for this user
    private List<PersonaInfo> availablePersonas;
    
    // Additional OAuth2 fields
    private String scope;
    private Instant issuedAt;
    
    /**
     * Simplified persona information for the response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonaInfo {
        private UUID id;
        private String name;
        private String type; // WORK, PERSONAL, GAMING, CUSTOM
        private boolean isDefault;
        private String avatarUrl;
        private PrivacySettings privacySettings;
    }
    
    /**
     * Privacy settings for a persona.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrivacySettings {
        private boolean showRealName;
        private boolean showEmail;
        private boolean showActivity;
        private boolean allowDirectMessages;
        private String visibilityLevel; // PUBLIC, FRIENDS, PRIVATE
    }
}