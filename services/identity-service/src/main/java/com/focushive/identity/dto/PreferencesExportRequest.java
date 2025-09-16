package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesExportRequest {
    private String format; // json, yaml
    private List<String> categories;
    private boolean includePersonaPreferences;
}
