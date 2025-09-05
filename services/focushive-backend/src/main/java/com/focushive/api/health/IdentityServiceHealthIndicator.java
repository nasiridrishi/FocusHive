package com.focushive.api.health;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.dto.identity.HealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Identity Service connectivity.
 * Integrates with Spring Boot Actuator health endpoint.
 */
@Slf4j
@Component("apiIdentityService")
@RequiredArgsConstructor
public class IdentityServiceHealthIndicator implements HealthIndicator {
    
    private final IdentityServiceClient identityServiceClient;
    
    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            HealthResponse response = identityServiceClient.checkHealth();
            long duration = System.currentTimeMillis() - startTime;
            
            if ("UP".equals(response.getStatus())) {
                return Health.up()
                    .withDetail("service", "identity-service")
                    .withDetail("status", response.getStatus())
                    .withDetail("version", response.getVersion())
                    .withDetail("responseTime", duration + "ms")
                    .build();
            } else {
                return Health.down()
                    .withDetail("service", "identity-service")
                    .withDetail("status", response.getStatus())
                    .withDetail("responseTime", duration + "ms")
                    .withDetail("reason", "Service reported DOWN status")
                    .build();
            }
            
        } catch (Exception e) {
            log.warn("Identity Service health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("service", "identity-service")
                .withDetail("error", e.getMessage())
                .withDetail("errorType", e.getClass().getSimpleName())
                .withDetail("message", "Identity Service is unavailable - operating in degraded mode")
                .build();
        }
    }
}