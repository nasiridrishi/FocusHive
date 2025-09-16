package com.focushive.health;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

/**
 * Health indicator for database migration status.
 * Monitors Flyway migration health and database schema version.
 *
 * Phase 1, Task 1.3: Health Check Implementation
 */
@Slf4j
@Profile("!test")
@Component("migration")
@ConditionalOnBean(Flyway.class)
public class MigrationHealthIndicator implements HealthIndicator {

    private final Flyway flyway;

    public MigrationHealthIndicator(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();

            if (flyway == null) {
                // Flyway is disabled or not configured
                return Health.up()
                    .withDetail("service", "migration")
                    .withDetail("status", "Flyway disabled - using schema validation")
                    .withDetail("currentVersion", "N/A")
                    .withDetail("pendingMigrations", 0)
                    .withDetail("note", "Running in demo mode without migrations")
                    .build();
            }

            // Get migration information
            var migrationInfos = flyway.info().all();
            var currentMigration = flyway.info().current();

            // Count pending migrations
            long pendingMigrations = Arrays.stream(migrationInfos)
                .filter(info -> info.getState() == MigrationState.PENDING)
                .count();

            // Count failed migrations
            long failedMigrations = Arrays.stream(migrationInfos)
                .filter(info -> info.getState() == MigrationState.FAILED)
                .count();

            long responseTime = System.currentTimeMillis() - startTime;

            // Determine health status
            if (failedMigrations > 0) {
                return Health.down()
                    .withDetail("service", "migration")
                    .withDetail("status", "Failed migrations detected")
                    .withDetail("currentVersion", currentMigration != null ? currentMigration.getVersion().toString() : "Unknown")
                    .withDetail("pendingMigrations", pendingMigrations)
                    .withDetail("failedMigrations", failedMigrations)
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("reason", "Database schema has failed migrations that need attention")
                    .build();
            }

            if (pendingMigrations > 0) {
                return Health.down()
                    .withDetail("service", "migration")
                    .withDetail("status", "Pending migrations detected")
                    .withDetail("currentVersion", currentMigration != null ? currentMigration.getVersion().toString() : "Unknown")
                    .withDetail("pendingMigrations", pendingMigrations)
                    .withDetail("failedMigrations", failedMigrations)
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("reason", "Database schema is out of date - migrations needed")
                    .build();
            }

            // All migrations are up to date
            String lastMigrationDate = "Unknown";
            if (currentMigration != null && currentMigration.getInstalledOn() != null) {
                lastMigrationDate = LocalDateTime.ofInstant(
                    currentMigration.getInstalledOn().toInstant(),
                    ZoneId.systemDefault()
                ).toString();
            }

            return Health.up()
                .withDetail("service", "migration")
                .withDetail("status", "All migrations up to date")
                .withDetail("currentVersion", currentMigration != null ? currentMigration.getVersion().toString() : "Unknown")
                .withDetail("pendingMigrations", pendingMigrations)
                .withDetail("failedMigrations", failedMigrations)
                .withDetail("totalMigrations", migrationInfos.length)
                .withDetail("lastMigrationDate", lastMigrationDate)
                .withDetail("responseTime", responseTime + "ms")
                .build();

        } catch (Exception e) {
            log.error("Migration health check failed", e);
            return Health.down()
                .withDetail("service", "migration")
                .withDetail("error", e.getMessage())
                .withDetail("errorType", e.getClass().getSimpleName())
                .withDetail("status", "Migration system unavailable")
                .withDetail("reason", "Could not access Flyway migration information")
                .build();
        }
    }
}