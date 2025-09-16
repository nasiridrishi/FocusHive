package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePersonaPreferencesRequest {
    private Map<String, Object> preferences;
    private String theme;
    private String workspaceLayout;
    private Map<String, Boolean> features;
}
