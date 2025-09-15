package com.focushive.api.dto.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents the actual health check response from Spring Boot Actuator.
 * Handles the complex structure returned by /actuator/health endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActuatorHealthResponse {
    private String status; // "UP", "DOWN", "OUT_OF_SERVICE", "UNKNOWN"
    private Map<String, ComponentHealth> components;

    /**
     * Nested component health information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ComponentHealth {
        private String status;
        private Map<String, Object> details;
    }

    /**
     * Convert to simplified HealthResponse for backward compatibility.
     */
    public HealthResponse toSimpleHealthResponse() {
        return HealthResponse.builder()
            .status(this.status != null ? this.status : "UNKNOWN")
            .version("unknown") // Version not included in actuator response
            .build();
    }

    /**
     * Check if the service is healthy.
     */
    public boolean isHealthy() {
        return "UP".equalsIgnoreCase(status);
    }
}
