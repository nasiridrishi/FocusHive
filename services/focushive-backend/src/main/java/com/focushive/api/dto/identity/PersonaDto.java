package com.focushive.api.dto.identity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDto {
    private UUID id;
    private UUID identityId;
    private String name;
    private PersonaType type;
    private ProfileDto profile;
    private boolean isActive;
    private Map<String, Object> settings;
    private Map<String, Object> metadata;
    
    public enum PersonaType {
        WORK, PERSONAL, GAMING, CUSTOM
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileDto {
        private String displayName;
        private String avatarUrl;
        private String bio;
        private Map<String, Object> customFields;
        private VisibilitySettings visibilitySettings;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisibilitySettings {
        private boolean profileVisible;
        private boolean statsVisible;
        private boolean activityVisible;
        private String discoveryLevel; // PUBLIC, FRIENDS, PRIVATE
    }
}