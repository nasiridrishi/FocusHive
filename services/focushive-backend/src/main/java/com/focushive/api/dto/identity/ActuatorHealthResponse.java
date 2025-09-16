package com.focushive.api.dto.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Simple wrapper for Spring Boot Actuator health endpoint responses.
 * Avoids generic type erasure issues with Feign deserialization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActuatorHealthResponse {
    private String status;
    private Map<String, Object> components;
    private String[] groups;
    
    /**
     * Convert to Map for compatibility with existing code.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "status", status != null ? status : "UNKNOWN",
            "components", components != null ? components : Map.of(),
            "groups", groups != null ? groups : new String[0]
        );
    }
}