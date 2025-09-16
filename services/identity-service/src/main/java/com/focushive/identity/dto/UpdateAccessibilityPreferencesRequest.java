package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccessibilityPreferencesRequest {
    private Boolean highContrast;
    private Boolean screenReaderMode;
    private String fontSize;
    private Boolean keyboardNavigation;
    private Boolean reducedMotion;
    private String colorScheme;
}
