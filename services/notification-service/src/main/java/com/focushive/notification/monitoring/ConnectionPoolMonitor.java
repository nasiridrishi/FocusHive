package com.focushive.notification.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Connection pool monitoring component.
 * Provides health checks and metrics for HikariCP connection pool.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectionPoolMonitor implements HealthIndicator {

    private final DataSource dataSource;

    /**
     * Provide health status for connection pool.
     */
    @Override
    public Health health() {
        if (!(dataSource instanceof HikariDataSource)) {
            return Health.up()
                    .withDetail("type", "Non-HikariCP DataSource")
                    .build();
        }

        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

        if (poolMXBean == null) {
            return Health.down()
                    .withDetail("error", "Pool MXBean not available")
                    .build();
        }

        Map<String, Object> details = new HashMap<>();
        details.put("poolName", hikariDataSource.getPoolName());
        details.put("totalConnections", poolMXBean.getTotalConnections());
        details.put("activeConnections", poolMXBean.getActiveConnections());
        details.put("idleConnections", poolMXBean.getIdleConnections());
        details.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
        details.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
        details.put("minimumIdle", hikariDataSource.getMinimumIdle());
        details.put("connectionTimeout", hikariDataSource.getConnectionTimeout());

        // Calculate pool usage percentage
        int totalConnections = poolMXBean.getTotalConnections();
        int maxConnections = hikariDataSource.getMaximumPoolSize();
        double usagePercentage = (totalConnections * 100.0) / maxConnections;
        details.put("usagePercentage", String.format("%.2f%%", usagePercentage));

        // Determine health status based on pool metrics
        Health.Builder healthBuilder;

        if (usagePercentage > 95) {
            healthBuilder = Health.down()
                    .withDetail("error", "Pool nearly exhausted");
        } else if (poolMXBean.getThreadsAwaitingConnection() > 0) {
            healthBuilder = Health.status("WARNING")
                    .withDetail("warning", "Threads waiting for connections");
        } else if (usagePercentage > 90) {
            healthBuilder = Health.status("WARNING")
                    .withDetail("warning", "Pool usage above 90%");
        } else {
            healthBuilder = Health.up();
        }

        return healthBuilder.withDetails(details).build();
    }

    /**
     * Log connection pool metrics periodically.
     */
    @Scheduled(fixedDelay = 60000) // Every minute
    public void logPoolMetrics() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

            if (poolMXBean != null) {
                log.info("Connection Pool Metrics - Total: {}, Active: {}, Idle: {}, Waiting: {}",
                        poolMXBean.getTotalConnections(),
                        poolMXBean.getActiveConnections(),
                        poolMXBean.getIdleConnections(),
                        poolMXBean.getThreadsAwaitingConnection());

                // Log warning if pool is under pressure
                if (poolMXBean.getThreadsAwaitingConnection() > 0) {
                    log.warn("Connection pool under pressure! {} threads waiting for connections",
                            poolMXBean.getThreadsAwaitingConnection());
                }

                // Log error if pool is nearly exhausted
                double usagePercentage = (poolMXBean.getTotalConnections() * 100.0) /
                        hikariDataSource.getMaximumPoolSize();
                if (usagePercentage > 90) {
                    log.warn("Connection pool usage at {:.2f}% - consider increasing pool size", usagePercentage);
                }
            }
        }
    }

    /**
     * Get current pool statistics.
     */
    public Map<String, Object> getPoolStatistics() {
        Map<String, Object> stats = new HashMap<>();

        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

            if (poolMXBean != null) {
                stats.put("poolName", hikariDataSource.getPoolName());
                stats.put("totalConnections", poolMXBean.getTotalConnections());
                stats.put("activeConnections", poolMXBean.getActiveConnections());
                stats.put("idleConnections", poolMXBean.getIdleConnections());
                stats.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                stats.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                stats.put("minimumIdle", hikariDataSource.getMinimumIdle());

                // Calculate additional metrics
                int totalConnections = poolMXBean.getTotalConnections();
                int maxConnections = hikariDataSource.getMaximumPoolSize();
                double usagePercentage = (totalConnections * 100.0) / maxConnections;
                stats.put("usagePercentage", usagePercentage);

                // Connection efficiency
                int activeConnections = poolMXBean.getActiveConnections();
                double efficiency = totalConnections > 0 ?
                        (activeConnections * 100.0) / totalConnections : 0;
                stats.put("connectionEfficiency", efficiency);
            }
        }

        return stats;
    }
}