package com.focushive.identity.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Custom health indicator for Identity Service.
 * Provides detailed health status and metrics for monitoring.
 */
@Slf4j
@Component("identity")
public class IdentityServiceHealthIndicator implements HealthIndicator {

    @Value("${spring.application.name:identity-service}")
    private String applicationName;

    @Value("${info.app.version:1.0.0}")
    private String applicationVersion;

    @Value("${health.check.jwt.enabled:true}")
    private boolean jwtCheckEnabled;

    @Value("${health.check.database.timeout:5}")
    private int databaseTimeoutSeconds;

    @Value("${health.check.redis.timeout:3}")
    private int redisTimeoutSeconds;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    @Qualifier("jsonRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    private volatile Instant startTime = Instant.now();
    private volatile long totalHealthChecks = 0;
    private volatile long failedHealthChecks = 0;

    @Override
    public Health health() {
        totalHealthChecks++;
        Health.Builder builder = new Health.Builder();
        Map<String, Object> details = new HashMap<>();

        try {
            // Basic service info
            details.put("service", applicationName);
            details.put("version", applicationVersion);
            details.put("uptime", getUptime());
            details.put("healthChecks", totalHealthChecks);
            details.put("failedChecks", failedHealthChecks);

            // Check critical components
            boolean allHealthy = true;

            // Database health
            if (dataSource != null) {
                HealthCheckResult dbResult = checkDatabase();
                details.put("database", dbResult.toMap());
                if (!dbResult.isHealthy()) {
                    allHealthy = false;
                }
            }

            // Redis health
            if (redisTemplate != null) {
                HealthCheckResult redisResult = checkRedis();
                details.put("redis", redisResult.toMap());
                if (!redisResult.isHealthy()) {
                    allHealthy = false;
                }
            }

            // JWT configuration health
            if (jwtCheckEnabled) {
                HealthCheckResult jwtResult = checkJWTConfiguration();
                details.put("jwt", jwtResult.toMap());
                if (!jwtResult.isHealthy()) {
                    allHealthy = false;
                }
            }

            // Memory health
            HealthCheckResult memoryResult = checkMemory();
            details.put("memory", memoryResult.toMap());
            if (!memoryResult.isHealthy()) {
                allHealthy = false;
            }

            // Thread health
            HealthCheckResult threadResult = checkThreads();
            details.put("threads", threadResult.toMap());

            // Set overall status
            if (allHealthy) {
                builder.up();
                details.put("status", "All systems operational");
            } else {
                builder.down();
                details.put("status", "Some components unhealthy");
                failedHealthChecks++;
            }

            builder.withDetails(details);

        } catch (Exception e) {
            log.error("Health check failed", e);
            failedHealthChecks++;
            builder.down()
                   .withException(e)
                   .withDetail("error", e.getMessage());
        }

        return builder.build();
    }

    /**
     * Check database connectivity and response time.
     */
    private HealthCheckResult checkDatabase() {
        try {
            long startTime = System.currentTimeMillis();

            // Test connection
            try (Connection connection = dataSource.getConnection()) {
                if (jdbcTemplate != null) {
                    // Execute a simple query
                    jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                }
            }

            long responseTime = System.currentTimeMillis() - startTime;

            return HealthCheckResult.healthy()
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("status", "connected")
                .withDetail("timeout", databaseTimeoutSeconds + "s");

        } catch (Exception e) {
            log.error("Database health check failed", e);
            return HealthCheckResult.unhealthy()
                .withDetail("status", "disconnected")
                .withDetail("error", e.getMessage());
        }
    }

    /**
     * Check Redis connectivity and response time.
     */
    private HealthCheckResult checkRedis() {
        try {
            long startTime = System.currentTimeMillis();

            // Test Redis connection with ping
            String pingResult = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection ->
                connection.ping()
            );

            long responseTime = System.currentTimeMillis() - startTime;

            if ("PONG".equals(pingResult)) {
                return HealthCheckResult.healthy()
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("status", "connected")
                    .withDetail("timeout", redisTimeoutSeconds + "s");
            } else {
                return HealthCheckResult.unhealthy()
                    .withDetail("status", "unexpected response")
                    .withDetail("response", pingResult);
            }

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return HealthCheckResult.unhealthy()
                .withDetail("status", "disconnected")
                .withDetail("error", e.getMessage());
        }
    }

    /**
     * Check JWT configuration.
     */
    private HealthCheckResult checkJWTConfiguration() {
        try {
            // Check if JWT secret is configured
            String jwtSecret = System.getenv("JWT_SECRET");
            if (jwtSecret == null || jwtSecret.isEmpty() || "CHANGE_ME".equals(jwtSecret)) {
                return HealthCheckResult.unhealthy()
                    .withDetail("status", "misconfigured")
                    .withDetail("error", "JWT secret not properly configured");
            }

            return HealthCheckResult.healthy()
                .withDetail("status", "configured")
                .withDetail("algorithm", "HS256");

        } catch (Exception e) {
            log.error("JWT configuration check failed", e);
            return HealthCheckResult.unhealthy()
                .withDetail("status", "error")
                .withDetail("error", e.getMessage());
        }
    }

    /**
     * Check memory usage.
     */
    private HealthCheckResult checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double usagePercent = (double) usedMemory / maxMemory * 100;

        HealthCheckResult result;
        if (usagePercent > 90) {
            result = HealthCheckResult.unhealthy()
                .withDetail("status", "critical");
        } else if (usagePercent > 75) {
            result = HealthCheckResult.degraded()
                .withDetail("status", "warning");
        } else {
            result = HealthCheckResult.healthy()
                .withDetail("status", "normal");
        }

        return result
            .withDetail("used", formatBytes(usedMemory))
            .withDetail("max", formatBytes(maxMemory))
            .withDetail("usage", String.format("%.1f%%", usagePercent));
    }

    /**
     * Check thread pool status.
     */
    private HealthCheckResult checkThreads() {
        int activeThreads = Thread.activeCount();
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }

        return HealthCheckResult.healthy()
            .withDetail("active", activeThreads)
            .withDetail("daemon", rootGroup.activeCount())
            .withDetail("peak", ((com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean())
                .getProcessCpuLoad() * 100);
    }

    /**
     * Get service uptime.
     */
    private String getUptime() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long days = uptime.toDays();
        long hours = uptime.toHours() % 24;
        long minutes = uptime.toMinutes() % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    /**
     * Format bytes to human-readable format.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Health check result builder.
     */
    private static class HealthCheckResult {
        private boolean healthy;
        private boolean degraded;
        private Map<String, Object> details = new HashMap<>();

        public static HealthCheckResult healthy() {
            HealthCheckResult result = new HealthCheckResult();
            result.healthy = true;
            return result;
        }

        public static HealthCheckResult unhealthy() {
            HealthCheckResult result = new HealthCheckResult();
            result.healthy = false;
            return result;
        }

        public static HealthCheckResult degraded() {
            HealthCheckResult result = new HealthCheckResult();
            result.healthy = true;
            result.degraded = true;
            return result;
        }

        public HealthCheckResult withDetail(String key, Object value) {
            details.put(key, value);
            return this;
        }

        public boolean isHealthy() {
            return healthy && !degraded;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>(details);
            map.put("healthy", healthy);
            if (degraded) {
                map.put("degraded", true);
            }
            return map;
        }
    }
}