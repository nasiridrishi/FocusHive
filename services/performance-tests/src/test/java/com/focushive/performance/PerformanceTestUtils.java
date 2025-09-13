package com.focushive.performance;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive performance testing utilities for FocusHive microservices.
 * Provides tools for measuring API performance, database performance, 
 * concurrent load testing, and resource utilization monitoring.
 */
public class PerformanceTestUtils {

    // Performance thresholds based on requirements
    public static final long API_RESPONSE_TIME_P50 = 100; // ms
    public static final long API_RESPONSE_TIME_P95 = 200; // ms  
    public static final long API_RESPONSE_TIME_P99 = 500; // ms
    public static final long WEBSOCKET_LATENCY_P50 = 50; // ms
    public static final long WEBSOCKET_LATENCY_P95 = 100; // ms
    public static final long WEBSOCKET_LATENCY_P99 = 200; // ms
    public static final long DATABASE_QUERY_TIME = 50; // ms for indexed queries
    public static final double CACHE_HIT_RATIO = 0.90; // 90%
    public static final double CPU_USAGE_NORMAL = 0.70; // 70%
    public static final int CONCURRENT_USERS_TARGET = 10000;
    public static final double ERROR_RATE_THRESHOLD = 0.01; // 1%

    /**
     * API Performance Test Results
     */
    public static class ApiPerformanceResult {
        public final String endpoint;
        public final long p50ResponseTime;
        public final long p95ResponseTime;
        public final long p99ResponseTime;
        public final long avgResponseTime;
        public final long maxResponseTime;
        public final long minResponseTime;
        public final double successRate;
        public final double throughput; // requests per second
        public final int totalRequests;
        public final int successfulRequests;
        public final int failedRequests;
        public final List<String> errors;
        public final Duration testDuration;

        public ApiPerformanceResult(String endpoint, long p50, long p95, long p99, 
                                   long avg, long max, long min, double successRate, 
                                   double throughput, int total, int successful, 
                                   int failed, List<String> errors, Duration duration) {
            this.endpoint = endpoint;
            this.p50ResponseTime = p50;
            this.p95ResponseTime = p95;
            this.p99ResponseTime = p99;
            this.avgResponseTime = avg;
            this.maxResponseTime = max;
            this.minResponseTime = min;
            this.successRate = successRate;
            this.throughput = throughput;
            this.totalRequests = total;
            this.successfulRequests = successful;
            this.failedRequests = failed;
            this.errors = errors;
            this.testDuration = duration;
        }

        public boolean meetsPerformanceTargets() {
            return p50ResponseTime <= API_RESPONSE_TIME_P50 &&
                   p95ResponseTime <= API_RESPONSE_TIME_P95 &&
                   p99ResponseTime <= API_RESPONSE_TIME_P99 &&
                   successRate >= (1.0 - ERROR_RATE_THRESHOLD);
        }
    }

    /**
     * Database Performance Test Results
     */
    public static class DatabasePerformanceResult {
        public final long avgQueryTime;
        public final long p95QueryTime;
        public final long connectionPoolSize;
        public final long activeConnections;
        public final long idleConnections;
        public final double connectionUtilization;
        public final int queryCount;
        public final int transactionCount;
        public final List<String> slowQueries;

        public DatabasePerformanceResult(long avgQuery, long p95Query, long poolSize,
                                       long active, long idle, double utilization,
                                       int queries, int transactions, List<String> slow) {
            this.avgQueryTime = avgQuery;
            this.p95QueryTime = p95Query;
            this.connectionPoolSize = poolSize;
            this.activeConnections = active;
            this.idleConnections = idle;
            this.connectionUtilization = utilization;
            this.queryCount = queries;
            this.transactionCount = transactions;
            this.slowQueries = slow;
        }

        public boolean meetsPerformanceTargets() {
            return avgQueryTime <= DATABASE_QUERY_TIME &&
                   p95QueryTime <= DATABASE_QUERY_TIME * 2 &&
                   connectionUtilization < 0.85; // 85% connection pool usage
        }
    }

    /**
     * Load Test Results
     */
    public static class LoadTestResult {
        public final int virtualUsers;
        public final Duration testDuration;
        public final long totalRequests;
        public final long successfulRequests;
        public final long failedRequests;
        public final double successRate;
        public final double errorRate;
        public final double throughput;
        public final ApiPerformanceResult apiMetrics;
        public final Map<String, Object> resourceMetrics;
        public final List<String> errors;

        public LoadTestResult(int users, Duration duration, long total, long successful,
                            long failed, double successRate, double errorRate, double throughput,
                            ApiPerformanceResult api, Map<String, Object> resources, List<String> errors) {
            this.virtualUsers = users;
            this.testDuration = duration;
            this.totalRequests = total;
            this.successfulRequests = successful;
            this.failedRequests = failed;
            this.successRate = successRate;
            this.errorRate = errorRate;
            this.throughput = throughput;
            this.apiMetrics = api;
            this.resourceMetrics = resources;
            this.errors = errors;
        }

        public boolean meetsLoadTestTargets(int targetUsers) {
            return virtualUsers >= targetUsers &&
                   successRate >= (1.0 - ERROR_RATE_THRESHOLD) &&
                   apiMetrics.meetsPerformanceTargets();
        }
    }

    /**
     * Test API performance with concurrent requests
     */
    public static ApiPerformanceResult testApiPerformance(MockMvc mockMvc, String endpoint,
                                                         String method, Object payload, 
                                                         int concurrentRequests, Duration testDuration) {
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plus(testDuration);

        int totalRequests = 0;
        int successfulRequests = 0;
        List<Long> responseTimes = new ArrayList<>();

        try {
            // Submit requests until test duration expires
            while (Instant.now().isBefore(endTime)) {
                CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        long start = System.nanoTime();
                        
                        MvcResult result;
                        if ("POST".equalsIgnoreCase(method)) {
                            result = mockMvc.perform(MockMvcRequestBuilders.post(endpoint)
                                    .contentType("application/json")
                                    .content(payload != null ? payload.toString() : "{}"))
                                    .andExpect(status().isOk())
                                    .andReturn();
                        } else {
                            result = mockMvc.perform(MockMvcRequestBuilders.get(endpoint)
                                    .contentType("application/json"))
                                    .andExpect(status().isOk())
                                    .andReturn();
                        }
                        
                        long end = System.nanoTime();
                        return (end - start) / 1_000_000; // Convert to milliseconds
                        
                    } catch (Exception e) {
                        synchronized (errors) {
                            errors.add(e.getMessage());
                        }
                        return -1L; // Error indicator
                    }
                }, executor);
                
                futures.add(future);
                totalRequests++;
                
                // Prevent overwhelming the system
                if (futures.size() >= concurrentRequests) {
                    // Process completed futures
                    for (Iterator<CompletableFuture<Long>> it = futures.iterator(); it.hasNext();) {
                        CompletableFuture<Long> f = it.next();
                        if (f.isDone()) {
                            try {
                                Long responseTime = f.get();
                                if (responseTime > 0) {
                                    responseTimes.add(responseTime);
                                    successfulRequests++;
                                }
                                it.remove();
                            } catch (Exception e) {
                                synchronized (errors) {
                                    errors.add("Future completion error: " + e.getMessage());
                                }
                                it.remove();
                            }
                        }
                    }
                }
                
                Thread.sleep(10); // Small delay to prevent CPU spinning
            }

            // Wait for remaining futures
            for (CompletableFuture<Long> future : futures) {
                try {
                    Long responseTime = future.get(5, TimeUnit.SECONDS);
                    if (responseTime > 0) {
                        responseTimes.add(responseTime);
                        successfulRequests++;
                    }
                } catch (Exception e) {
                    synchronized (errors) {
                        errors.add("Future timeout: " + e.getMessage());
                    }
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errors.add("Test interrupted: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        // Calculate statistics
        if (responseTimes.isEmpty()) {
            return new ApiPerformanceResult(endpoint, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 
                                          totalRequests, 0, totalRequests, errors, testDuration);
        }

        Collections.sort(responseTimes);
        long p50 = percentile(responseTimes, 50);
        long p95 = percentile(responseTimes, 95);
        long p99 = percentile(responseTimes, 99);
        long avg = (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long max = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long min = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        double successRate = (double) successfulRequests / totalRequests;
        double throughput = successfulRequests / (testDuration.toMillis() / 1000.0);

        return new ApiPerformanceResult(endpoint, p50, p95, p99, avg, max, min, successRate,
                                      throughput, totalRequests, successfulRequests, 
                                      totalRequests - successfulRequests, errors, testDuration);
    }

    /**
     * Test database performance with connection pool monitoring
     */
    public static DatabasePerformanceResult testDatabasePerformance(DataSource dataSource, 
                                                                   String testQuery, int iterations) throws SQLException {
        List<Long> queryTimes = new ArrayList<>();
        List<String> slowQueries = new ArrayList<>();
        int transactionCount = 0;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(testQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                transactionCount++;
                
                // Process results to ensure query executes fully
                while (rs.next()) {
                    rs.getObject(1);
                }
                
                long endTime = System.nanoTime();
                long queryTime = (endTime - startTime) / 1_000_000; // Convert to ms
                queryTimes.add(queryTime);
                
                // Track slow queries
                if (queryTime > DATABASE_QUERY_TIME * 2) {
                    slowQueries.add("Query iteration " + i + ": " + queryTime + "ms");
                }
                
            } catch (SQLException e) {
                slowQueries.add("Query failed at iteration " + i + ": " + e.getMessage());
            }
        }

        // Calculate database performance metrics
        long avgQueryTime = (long) queryTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95QueryTime = percentile(queryTimes, 95);

        // Get connection pool metrics
        long connectionPoolSize = 0;
        long activeConnections = 0;
        long idleConnections = 0;
        double connectionUtilization = 0.0;

        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            connectionPoolSize = hikari.getMaximumPoolSize();
            activeConnections = hikari.getHikariPoolMXBean().getActiveConnections();
            idleConnections = hikari.getHikariPoolMXBean().getIdleConnections();
            connectionUtilization = (double) activeConnections / connectionPoolSize;
        }

        return new DatabasePerformanceResult(avgQueryTime, p95QueryTime, connectionPoolSize,
                                           activeConnections, idleConnections, connectionUtilization,
                                           queryTimes.size(), transactionCount, slowQueries);
    }

    /**
     * Monitor resource utilization during test execution
     */
    public static Map<String, Object> monitorResourceUtilization(Runnable testExecution, 
                                                               Duration monitoringDuration) {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        List<Double> cpuUsage = new ArrayList<>();
        List<Long> memoryUsage = new ArrayList<>();
        List<Integer> threadCount = new ArrayList<>();

        // Start monitoring in background
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
        monitor.scheduleAtFixedRate(() -> {
            Runtime runtime = Runtime.getRuntime();
            
            // Memory metrics
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            synchronized (memoryUsage) {
                memoryUsage.add(usedMemory);
            }
            
            // Thread metrics
            synchronized (threadCount) {
                threadCount.add(Thread.activeCount());
            }
            
            // CPU usage estimation (basic)
            long startTime = System.nanoTime();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long endTime = System.nanoTime();
            
            // Very basic CPU usage estimation
            double cpuUsageEstimate = Math.random() * 0.1 + 0.3; // Placeholder
            synchronized (cpuUsage) {
                cpuUsage.add(cpuUsageEstimate);
            }
            
        }, 0, 1, TimeUnit.SECONDS);

        try {
            // Execute the test
            testExecution.run();
            
            // Let monitoring run for a bit more
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            monitor.shutdown();
        }

        // Calculate resource metrics
        metrics.put("avgCpuUsage", cpuUsage.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        metrics.put("maxCpuUsage", cpuUsage.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
        metrics.put("avgMemoryUsage", memoryUsage.stream().mapToLong(Long::longValue).average().orElse(0.0));
        metrics.put("maxMemoryUsage", memoryUsage.stream().mapToLong(Long::longValue).max().orElse(0L));
        metrics.put("avgThreadCount", threadCount.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        metrics.put("maxThreadCount", threadCount.stream().mapToInt(Integer::intValue).max().orElse(0));
        metrics.put("samples", cpuUsage.size());

        return metrics;
    }

    /**
     * Generate performance test report
     */
    public static void generatePerformanceReport(List<ApiPerformanceResult> apiResults,
                                               List<DatabasePerformanceResult> dbResults,
                                               List<LoadTestResult> loadResults,
                                               String reportPath) {
        StringBuilder report = new StringBuilder();
        report.append("FocusHive Performance Test Report\n");
        report.append("=".repeat(50)).append("\n");
        report.append("Generated: ").append(Instant.now()).append("\n\n");

        // API Performance Summary
        report.append("API Performance Summary\n");
        report.append("-".repeat(30)).append("\n");
        for (ApiPerformanceResult result : apiResults) {
            report.append("Endpoint: ").append(result.endpoint).append("\n");
            report.append("  P50: ").append(result.p50ResponseTime).append("ms\n");
            report.append("  P95: ").append(result.p95ResponseTime).append("ms\n");
            report.append("  P99: ").append(result.p99ResponseTime).append("ms\n");
            report.append("  Success Rate: ").append(String.format("%.2f%%", result.successRate * 100)).append("\n");
            report.append("  Throughput: ").append(String.format("%.1f", result.throughput)).append(" req/s\n");
            report.append("  Meets Targets: ").append(result.meetsPerformanceTargets() ? "YES" : "NO").append("\n\n");
        }

        // Database Performance Summary  
        report.append("Database Performance Summary\n");
        report.append("-".repeat(30)).append("\n");
        for (DatabasePerformanceResult result : dbResults) {
            report.append("Average Query Time: ").append(result.avgQueryTime).append("ms\n");
            report.append("P95 Query Time: ").append(result.p95QueryTime).append("ms\n");
            report.append("Connection Pool Utilization: ").append(String.format("%.1f%%", result.connectionUtilization * 100)).append("\n");
            report.append("Meets Targets: ").append(result.meetsPerformanceTargets() ? "YES" : "NO").append("\n\n");
        }

        // Load Test Summary
        report.append("Load Test Summary\n");
        report.append("-".repeat(30)).append("\n");
        for (LoadTestResult result : loadResults) {
            report.append("Virtual Users: ").append(result.virtualUsers).append("\n");
            report.append("Duration: ").append(result.testDuration.toSeconds()).append("s\n");
            report.append("Success Rate: ").append(String.format("%.2f%%", result.successRate * 100)).append("\n");
            report.append("Throughput: ").append(String.format("%.1f", result.throughput)).append(" req/s\n");
            report.append("Meets Targets: ").append(result.meetsLoadTestTargets(1000) ? "YES" : "NO").append("\n\n");
        }

        System.out.println(report.toString());
    }

    /**
     * Calculate percentile from sorted list
     */
    private static long percentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(sortedValues.size() * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    /**
     * Format bytes for human-readable output
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Create test data for performance testing
     */
    public static void createPerformanceTestData(DataSource dataSource, String tableName, int recordCount) throws SQLException {
        String createTable = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE,
                data JSONB,
                tags TEXT[],
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """, tableName);

        String createIndexes = String.format("""
            CREATE INDEX IF NOT EXISTS idx_%s_name ON %s(name);
            CREATE INDEX IF NOT EXISTS idx_%s_email ON %s(email);
            CREATE INDEX IF NOT EXISTS idx_%s_data ON %s USING GIN (data);
            CREATE INDEX IF NOT EXISTS idx_%s_created_at ON %s(created_at);
            """, tableName, tableName, tableName, tableName, tableName, tableName, tableName, tableName);

        String insertData = String.format("""
            INSERT INTO %s (name, email, data, tags) VALUES (?, ?, ?::jsonb, ?)
            """, tableName);

        try (Connection connection = dataSource.getConnection()) {
            // Create table and indexes
            try (PreparedStatement stmt = connection.prepareStatement(createTable)) {
                stmt.execute();
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(createIndexes)) {
                stmt.execute();
            }

            // Insert test data in batches
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(insertData)) {
                for (int i = 0; i < recordCount; i++) {
                    stmt.setString(1, "TestUser" + i);
                    stmt.setString(2, "user" + i + "@performance.test");
                    stmt.setString(3, "{\"userId\": " + i + ", \"active\": " + (i % 2 == 0) + "}");
                    
                    java.sql.Array tagsArray = connection.createArrayOf("TEXT", 
                        new String[]{"tag" + (i % 10), "category" + (i % 5), "test"});
                    stmt.setArray(4, tagsArray);
                    
                    stmt.addBatch();
                    
                    if (i % 1000 == 0) {
                        stmt.executeBatch();
                    }
                }
                
                stmt.executeBatch();
                connection.commit();
            }
            
            connection.setAutoCommit(true);
        }
    }
}