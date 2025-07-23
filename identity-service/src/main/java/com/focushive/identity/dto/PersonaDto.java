package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.focushive.identity.entity.Persona;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Persona information.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonaDto {
    
    private UUID id;
    private String name;
    private String type;
    private boolean isDefault;
    private boolean isActive;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private String statusMessage;
    private PrivacySettingsDto privacySettings;
    private Map<String, String> customAttributes;
    private String themePreference;
    private String languagePreference;
    private Instant lastActiveAt;
    private Instant createdAt;
    
    /**
     * Privacy settings DTO.
     */
    @Data
    @Builder
    public static class PrivacySettingsDto {
        private boolean showRealName;
        private boolean showEmail;
        private boolean showActivity;
        private boolean allowDirectMessages;
        private String visibilityLevel;
        private boolean searchable;
        private boolean showOnlineStatus;
        private boolean shareFocusSessions;
        private boolean shareAchievements;
    }
    
    /**
     * Convert from entity to DTO.
     */
    public static PersonaDto from(Persona persona) {
        if (persona == null) {
            return null;
        }
        
        PrivacySettingsDto privacyDto = null;
        if (persona.getPrivacySettings() != null) {
            privacyDto = PrivacySettingsDto.builder()
                    .showRealName(persona.getPrivacySettings().isShowRealName())
                    .showEmail(persona.getPrivacySettings().isShowEmail())
                    .showActivity(persona.getPrivacySettings().isShowActivity())
                    .allowDirectMessages(persona.getPrivacySettings().isAllowDirectMessages())
                    .visibilityLevel(persona.getPrivacySettings().getVisibilityLevel())
                    .searchable(persona.getPrivacySettings().isSearchable())
                    .showOnlineStatus(persona.getPrivacySettings().isShowOnlineStatus())
                    .shareFocusSessions(persona.getPrivacySettings().isShareFocusSessions())
                    .shareAchievements(persona.getPrivacySettings().isShareAchievements())
                    .build();
        }
        
        return PersonaDto.builder()
                .id(persona.getId())
                .name(persona.getName())
                .type(persona.getType().name())
                .isDefault(persona.isDefault())
                .isActive(persona.isActive())
                .displayName(persona.getDisplayName())
                .avatarUrl(persona.getAvatarUrl())
                .bio(persona.getBio())
                .statusMessage(persona.getStatusMessage())
                .privacySettings(privacyDto)
                .customAttributes(persona.getCustomAttributes())
                .themePreference(persona.getThemePreference())
                .languagePreference(persona.getLanguagePreference())
                .lastActiveAt(persona.getLastActiveAt())
                .createdAt(persona.getCreatedAt())
                .build();
    }
}