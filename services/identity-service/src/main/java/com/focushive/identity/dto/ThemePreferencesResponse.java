package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemePreferencesResponse {
    private String theme; // light, dark, auto
    private String primaryColor;
    private String secondaryColor;
    private String fontFamily;
    private boolean compactMode;
}
