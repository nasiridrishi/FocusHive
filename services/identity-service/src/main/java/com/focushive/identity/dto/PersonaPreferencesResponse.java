package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaPreferencesResponse {
    private UUID personaId;
    private String personaName;
    private Map<String, Object> preferences;
    private String theme;
    private String workspaceLayout;
    private Map<String, Boolean> features;
    private Instant updatedAt;
}
