package com.focushive.backend.health;

import com.focushive.backend.service.IdentityIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Identity Service integration.
 * Monitors the health and availability of the Identity Service.
 */
@Component("backendIdentityService")
@RequiredArgsConstructor
public class IdentityServiceHealthIndicator implements HealthIndicator {

    private final IdentityIntegrationService identityIntegrationService;

    @Override
    public Health health() {
        try {
            boolean isHealthy = identityIntegrationService.isIdentityServiceHealthy();
            
            if (isHealthy) {
                return Health.up()
                        .withDetail("service", "Identity Service")
                        .withDetail("status", "Connected")
                        .withDetail("integration", "Operational")
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "Identity Service")
                        .withDetail("status", "Disconnected")
                        .withDetail("integration", "Degraded - Using fallback")
                        .build();
            }
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("service", "Identity Service")
                    .withDetail("status", "Error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}