package com.focushive.health;

import com.focushive.hive.repository.HiveRepository;
import com.focushive.hive.service.HiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Hive Service functionality.
 * Monitors hive management system health and provides metrics.
 *
 * Phase 1, Task 1.3: Health Check Implementation
 */
@Slf4j
@Component("hiveService")
@RequiredArgsConstructor
@Profile("!test")
public class HiveServiceHealthIndicator implements HealthIndicator {

    private final HiveService hiveService;
    private final HiveRepository hiveRepository;

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            // Check basic repository connectivity
            long totalHives = hiveRepository.count();

            // Get active hive count
            long activeHives = hiveService.getActiveHiveCount();

            long responseTime = System.currentTimeMillis() - startTime;

            return Health.up()
                .withDetail("service", "hive-service")
                .withDetail("totalHives", totalHives)
                .withDetail("activeHives", activeHives)
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("status", "Hive management system operational")
                .build();

        } catch (Exception e) {
            log.error("Hive service health check failed", e);
            return Health.down()
                .withDetail("service", "hive-service")
                .withDetail("error", e.getMessage())
                .withDetail("errorType", e.getClass().getSimpleName())
                .withDetail("status", "Hive management system unavailable")
                .build();
        }
    }

}