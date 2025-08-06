package com.focushive.backend.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private String type; // WORK, PERSONAL, GAMING, STUDY, CUSTOM
    private String displayName;
    private String avatarUrl;
    private String bio;
    private boolean isActive;
    private boolean isDefault;
    private Map<String, Object> profile;
    private Map<String, Object> privacySettings;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}