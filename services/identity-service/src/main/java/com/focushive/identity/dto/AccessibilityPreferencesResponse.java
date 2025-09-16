package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessibilityPreferencesResponse {
    private boolean highContrast;
    private boolean screenReaderMode;
    private String fontSize;
    private boolean keyboardNavigation;
    private boolean reducedMotion;
    private String colorScheme;
}
