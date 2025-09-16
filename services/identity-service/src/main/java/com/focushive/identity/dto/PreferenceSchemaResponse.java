package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceSchemaResponse {
    private Map<String, PreferenceCategory> categories;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceCategory {
        private String name;
        private String description;
        private List<PreferenceField> fields;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreferenceField {
        private String name;
        private String type;
        private Object defaultValue;
        private List<String> allowedValues;
        private String description;
    }
}
