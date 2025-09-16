package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateThemePreferencesRequest {
    private String theme;
    private String primaryColor;
    private String secondaryColor;
    private String fontFamily;
    private Boolean compactMode;
}
