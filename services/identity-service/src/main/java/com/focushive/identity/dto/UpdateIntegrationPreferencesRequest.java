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
public class UpdateIntegrationPreferencesRequest {
    private Map<String, Boolean> enabledIntegrations;
    private Map<String, Map<String, String>> integrationSettings;
}
