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
public class UpdateGlobalPreferencesRequest {
    private String language;
    private String timezone;
    private String dateFormat;
    private String timeFormat;
    private String currency;
    private String measurementUnit;
    private Map<String, Object> customSettings;
}
