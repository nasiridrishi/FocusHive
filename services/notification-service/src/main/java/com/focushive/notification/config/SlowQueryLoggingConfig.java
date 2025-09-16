package com.focushive.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for slow query logging and monitoring.
 * Tracks and logs queries that exceed performance thresholds.
 */
@Slf4j
@Configuration
public class SlowQueryLoggingConfig {

    @Value("${notification.monitoring.slow-query-threshold-ms:1000}")
    private long slowQueryThresholdMs;

    @Value("${notification.monitoring.enable-query-statistics:true}")
    private boolean enableQueryStatistics;

    private final Formatter sqlFormatter = FormatStyle.BASIC.getFormatter();

    @Autowired(required = false)
    private EntityManagerFactory entityManagerFactory;

    /**
     * Configure Hibernate statistics for query monitoring.
     */
    @Bean
    @ConditionalOnProperty(name = "notification.monitoring.enable-query-statistics", havingValue = "true")
    public Statistics hibernateStatistics(EntityManagerFactory entityManagerFactory) {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(enableQueryStatistics);

        log.info("Hibernate statistics enabled for slow query monitoring with threshold: {}ms", slowQueryThresholdMs);
        return statistics;
    }

    /**
     * Periodically log slow query statistics.
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void logSlowQueryStatistics() {
        if (!enableQueryStatistics || entityManagerFactory == null) {
            return;
        }

        try {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics statistics = sessionFactory.getStatistics();

            if (statistics.isStatisticsEnabled()) {
                // Log general statistics
                log.info("=== Query Performance Statistics ===");
                log.info("Total queries executed: {}", statistics.getQueryExecutionCount());
                log.info("Slowest query time: {}ms", statistics.getQueryExecutionMaxTime());
                log.info("Average query time: {:.2f}ms",
                        statistics.getQueryExecutionCount() > 0 ?
                                (double) statistics.getQueryExecutionMaxTime() / statistics.getQueryExecutionCount() : 0);

                // Log slow queries
                String slowestQuery = statistics.getQueryExecutionMaxTimeQueryString();
                if (slowestQuery != null && statistics.getQueryExecutionMaxTime() > slowQueryThresholdMs) {
                    log.warn("SLOW QUERY DETECTED - Execution time: {}ms", statistics.getQueryExecutionMaxTime());
                    log.warn("Query: {}", formatSql(slowestQuery));
                }

                // Log cache statistics
                log.info("Second level cache hit ratio: {:.2f}%",
                        statistics.getSecondLevelCacheHitCount() * 100.0 /
                                (statistics.getSecondLevelCacheHitCount() + statistics.getSecondLevelCacheMissCount() + 1));

                // Log entity statistics
                log.info("Entities loaded: {}", statistics.getEntityLoadCount());
                log.info("Entities fetched: {}", statistics.getEntityFetchCount());

                // Clear statistics for next period
                statistics.clear();
            }
        } catch (Exception e) {
            log.error("Error logging query statistics", e);
        }
    }

    /**
     * Format SQL for better readability in logs.
     */
    private String formatSql(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        try {
            return sqlFormatter.format(sql);
        } catch (Exception e) {
            return sql; // Return unformatted if formatting fails
        }
    }
}