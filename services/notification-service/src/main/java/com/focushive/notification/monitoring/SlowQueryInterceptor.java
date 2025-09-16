package com.focushive.notification.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect for intercepting and monitoring slow database queries.
 * Tracks query execution time and logs slow queries with details.
 */
@Slf4j
@Aspect
@Component
public class SlowQueryInterceptor {

    @Value("${notification.monitoring.slow-query-threshold-ms:1000}")
    private long slowQueryThresholdMs;

    @Value("${notification.monitoring.log-all-queries:false}")
    private boolean logAllQueries;

    // Track query statistics
    private final ConcurrentHashMap<String, QueryStatistics> queryStats = new ConcurrentHashMap<>();

    /**
     * Pointcut for repository methods.
     */
    @Pointcut("@within(org.springframework.stereotype.Repository)")
    public void repositoryMethods() {}

    /**
     * Intercept repository method calls to monitor query performance.
     */
    @Around("repositoryMethods()")
    public Object monitorQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant startTime = Instant.now();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            // Execute the query
            Object result = joinPoint.proceed();

            // Calculate execution time
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();

            // Update statistics
            updateStatistics(methodName, executionTime);

            // Log if slow or if logging all queries
            if (executionTime > slowQueryThresholdMs) {
                logSlowQuery(methodName, executionTime, joinPoint.getArgs());
            } else if (logAllQueries) {
                log.debug("Query executed in {}ms: {}", executionTime, methodName);
            }

            return result;

        } catch (Throwable e) {
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            log.error("Query failed after {}ms: {} - Error: {}", executionTime, methodName, e.getMessage());
            throw e;
        }
    }

    /**
     * Log slow query with detailed information.
     */
    private void logSlowQuery(String methodName, long executionTime, Object[] args) {
        log.warn("=== SLOW QUERY DETECTED ===");
        log.warn("Method: {}", methodName);
        log.warn("Execution time: {}ms (threshold: {}ms)", executionTime, slowQueryThresholdMs);
        log.warn("Parameters: {}", Arrays.toString(args));

        // Get statistics for this query
        QueryStatistics stats = queryStats.get(methodName);
        if (stats != null) {
            log.warn("Statistics - Count: {}, Avg: {}ms, Max: {}ms, Min: {}ms",
                    stats.count.get(),
                    stats.getAverageTime(),
                    stats.maxTime.get(),
                    stats.minTime.get());
        }

        // Provide optimization suggestions
        if (executionTime > slowQueryThresholdMs * 3) {
            log.error("CRITICAL: Query is {}x slower than threshold! Consider optimization:",
                    executionTime / slowQueryThresholdMs);
            log.error("- Check for missing indexes");
            log.error("- Verify query plan with EXPLAIN ANALYZE");
            log.error("- Consider query result caching");
            log.error("- Review fetch strategy (lazy vs eager)");
        }
    }

    /**
     * Update query statistics.
     */
    private void updateStatistics(String methodName, long executionTime) {
        queryStats.compute(methodName, (key, stats) -> {
            if (stats == null) {
                stats = new QueryStatistics();
            }
            stats.update(executionTime);
            return stats;
        });
    }

    /**
     * Get current query statistics.
     */
    public ConcurrentHashMap<String, QueryStatistics> getQueryStatistics() {
        return new ConcurrentHashMap<>(queryStats);
    }

    /**
     * Reset query statistics.
     */
    public void resetStatistics() {
        queryStats.clear();
        log.info("Query statistics have been reset");
    }

    /**
     * Statistics for a specific query.
     */
    public static class QueryStatistics {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);

        public void update(long executionTime) {
            count.incrementAndGet();
            totalTime.addAndGet(executionTime);

            // Update max time
            long currentMax;
            while ((currentMax = maxTime.get()) < executionTime) {
                maxTime.compareAndSet(currentMax, executionTime);
            }

            // Update min time
            long currentMin;
            while ((currentMin = minTime.get()) > executionTime) {
                minTime.compareAndSet(currentMin, executionTime);
            }
        }

        public double getAverageTime() {
            long c = count.get();
            return c > 0 ? (double) totalTime.get() / c : 0;
        }

        @Override
        public String toString() {
            return String.format("QueryStats{count=%d, avg=%.2fms, max=%dms, min=%dms}",
                    count.get(), getAverageTime(), maxTime.get(),
                    minTime.get() == Long.MAX_VALUE ? 0 : minTime.get());
        }
    }
}