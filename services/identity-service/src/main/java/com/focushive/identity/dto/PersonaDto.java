package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.focushive.identity.entity.Persona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for Persona information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonaDto {
    
    private UUID id;
    
    @NotBlank(message = "Persona name is required")
    @Size(min = 1, max = 100, message = "Persona name must be between 1 and 100 characters")
    private String name;
    
    @NotNull(message = "Persona type is required")
    private Persona.PersonaType type;
    
    private boolean isDefault;
    private boolean isActive;
    private String displayName;
    private String avatarUrl;
    
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;
    
    @Size(max = 200, message = "Status message must not exceed 200 characters")
    private String statusMessage;
    private PrivacySettingsDto privacySettings;
    private Map<String, String> customAttributes;
    private String themePreference;
    private String languagePreference;
    private Instant lastActiveAt;
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Privacy settings DTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
                .type(persona.getType())
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
                .updatedAt(persona.getUpdatedAt())
                .build();
    }
}