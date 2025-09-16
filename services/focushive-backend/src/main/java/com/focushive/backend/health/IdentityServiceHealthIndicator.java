package com.focushive.backend.health;

import com.focushive.api.dto.identity.HealthResponse;
import com.focushive.api.service.IdentityIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Health indicator for Identity Service integration.
 * Monitors the health and availability of the Identity Service.
 */
@Component("backendIdentityService")
@RequiredArgsConstructor
@ConditionalOnBean(IdentityIntegrationService.class)
@Profile("!test")
public class IdentityServiceHealthIndicator implements HealthIndicator {

    private final IdentityIntegrationService identityIntegrationService;

    @Override
    public Health health() {
        try {
            CompletableFuture<HealthResponse> healthFuture = identityIntegrationService.checkHealthAsync();
            HealthResponse healthResponse = healthFuture.get();

            if (healthResponse != null && "UP".equals(healthResponse.getStatus())) {
                return Health.up()
                        .withDetail("service", "Identity Service")
                        .withDetail("status", "Connected")
                        .withDetail("integration", "Operational")
                        .withDetail("version", healthResponse.getVersion())
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "Identity Service")
                        .withDetail("status", healthResponse != null ? healthResponse.getStatus() : "UNKNOWN")
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