package com.focushive.health;

import com.focushive.websocket.service.PresenceTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Presence Tracking Service.
 * Monitors real-time presence system health and WebSocket connections.
 *
 * Phase 1, Task 1.3: Health Check Implementation
 */
@Slf4j
@Component("presenceService")
@RequiredArgsConstructor
@Profile("!test")
public class PresenceServiceHealthIndicator implements HealthIndicator {

    private final PresenceTrackingService presenceTrackingService;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // Get presence service metrics
            int activeConnections = presenceTrackingService.getActiveConnectionCount();
            long totalPresenceUpdates = presenceTrackingService.getTotalPresenceUpdates();

            long responseTime = System.currentTimeMillis() - startTime;

            return Health.up()
                .withDetail("service", "presence-service")
                .withDetail("activeConnections", activeConnections)
                .withDetail("totalPresenceUpdates", totalPresenceUpdates)
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("status", "Presence tracking system operational")
                .build();

        } catch (Exception e) {
            log.error("Presence service health check failed", e);
            return Health.down()
                .withDetail("service", "presence-service")
                .withDetail("error", e.getMessage())
                .withDetail("errorType", e.getClass().getSimpleName())
                .withDetail("status", "Presence tracking system unavailable")
                .build();
        }
    }

}