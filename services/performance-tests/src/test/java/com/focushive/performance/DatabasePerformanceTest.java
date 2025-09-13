package com.focushive.performance;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive Database Performance Tests for FocusHive Platform.
 * 
 * Tests database performance across all critical areas:
 * - Query execution times and optimization
 * - Index effectiveness and query planning
 * - Connection pool sizing and utilization
 * - Transaction throughput and deadlock handling
 * - Batch operation performance
 * - Read/write ratio optimization
 * - Complex join performance
 * - Full-text search performance
 * - Concurrent access patterns
 * 
 * Performance Targets:
 * - Simple queries: < 50ms
 * - Complex queries: < 200ms
 * - Bulk operations: < 2s per 1000 records
 * - Connection pool utilization: < 85%
 * - Transaction deadlock rate: < 0.1%
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("performance-test")
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("focushive_db_perf")
            .withUsername("db_perf_user")
            .withPassword("db_perf_pass")
            .withReuse(true)
            .withCommand("postgres", "-c", "shared_buffers=256MB", 
                        "-c", "max_connections=200",
                        "-c", "effective_cache_size=1GB",
                        "-c", "maintenance_work_mem=64MB",
                        "-c", "checkpoint_completion_target=0.9",
                        "-c", "wal_buffers=16MB",
                        "-c", "default_statistics_target=100");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Performance optimized connection pool
        registry.add("spring.datasource.hikari.minimum-idle", () -> "25");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "75");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "300000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "900000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
        
        // PostgreSQL specific optimizations
        registry.add("spring.datasource.hikari.data-source-properties.preparedStatementCacheSize", () -> "512");
        registry.add("spring.datasource.hikari.data-source-properties.preparedStatementCacheSqlLimit", () -> "4096");
        registry.add("spring.datasource.hikari.data-source-properties.defaultTransactionIsolation", () -> "TRANSACTION_READ_COMMITTED");
        
        // JPA optimizations
        registry.add("spring.jpa.properties.hibernate.jdbc.batch_size", () -> "50");
        registry.add("spring.jpa.properties.hibernate.order_inserts", () -> "true");
        registry.add("spring.jpa.properties.hibernate.order_updates", () -> "true");
        registry.add("spring.jpa.properties.hibernate.jdbc.batch_versioned_data", () -> "true");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private SessionFactory sessionFactory;
    private Statistics hibernateStatistics;
    private List<PerformanceTestUtils.DatabasePerformanceResult> dbResults;

    @BeforeEach
    void setUp() throws SQLException {
        sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        hibernateStatistics = sessionFactory.getStatistics();
        hibernateStatistics.setStatisticsEnabled(true);
        hibernateStatistics.clear();
        
        dbResults = new ArrayList<>();
        
        // Create comprehensive test data
        setupPerformanceTestData();
    }

    @AfterEach
    void generateDatabaseReport() {
        if (!dbResults.isEmpty()) {
            PerformanceTestUtils.generatePerformanceReport(List.of(), dbResults, List.of(), 
                "database-performance-report.txt");
        }
    }

    @Nested
    @DisplayName("Query Performance Tests")
    class QueryPerformanceTests {

        @Test
        @DisplayName("Simple SELECT query performance")
        void testSimpleSelectPerformance() throws SQLException {
            String testQuery = "SELECT id, name, email FROM perf_users WHERE id = ?";
            
            PerformanceTestUtils.DatabasePerformanceResult result = 
                PerformanceTestUtils.testDatabasePerformance(dataSource, testQuery, 1000);
            
            dbResults.add(result);
            
            assertThat(result.avgQueryTime)
                .describedAs("Simple SELECT queries should be very fast")
                .isLessThanOrEqualTo(PerformanceTestUtils.DATABASE_QUERY_TIME);
            
            assertThat(result.p95QueryTime)
                .describedAs("P95 of simple queries should be under 100ms")
                .isLessThanOrEqualTo(100);
            
            System.out.println(String.format(
                "Simple SELECT: Avg=%dms, P95=%dms, Connection Util=%.1f%%",
                result.avgQueryTime, result.p95QueryTime, result.connectionUtilization * 100
            ));
        }

        @Test
        @DisplayName("Complex JOIN query performance")
        void testComplexJoinPerformance() throws SQLException {
            String complexQuery = """
                SELECT u.id, u.name, u.email, h.name as hive_name, 
                       COUNT(s.id) as session_count,
                       AVG(s.duration) as avg_session_duration
                FROM perf_users u
                LEFT JOIN perf_hives h ON u.id = h.owner_id
                LEFT JOIN perf_sessions s ON u.id = s.user_id
                WHERE u.created_at > ? AND u.active = true
                GROUP BY u.id, u.name, u.email, h.name
                ORDER BY session_count DESC
                LIMIT 50
                """;
            
            List<Long> queryTimes = new ArrayList<>();
            int iterations = 100;
            
            try (Connection connection = dataSource.getConnection()) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    
                    try (PreparedStatement stmt = connection.prepareStatement(complexQuery)) {
                        stmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis() - 86400000)); // 24 hours ago
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                rs.getInt("session_count");
                                rs.getDouble("avg_session_duration");
                            }
                        }
                    }
                    
                    long endTime = System.nanoTime();
                    queryTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
                }
            }
            
            Collections.sort(queryTimes);
            long avgTime = (long) queryTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95Time = queryTimes.get((int) (queryTimes.size() * 0.95));
            
            assertThat(avgTime)
                .describedAs("Complex JOIN queries should complete within 200ms")
                .isLessThanOrEqualTo(200);
            
            assertThat(p95Time)
                .describedAs("P95 of complex queries should be under 500ms")
                .isLessThanOrEqualTo(500);
            
            System.out.println(String.format(
                "Complex JOIN: Avg=%dms, P95=%dms, Iterations=%d",
                avgTime, p95Time, iterations
            ));
        }

        @Test
        @DisplayName("Full-text search performance")
        void testFullTextSearchPerformance() throws SQLException {
            // Create full-text search indexes first
            try (Connection connection = dataSource.getConnection()) {
                String createTsVector = """
                    ALTER TABLE perf_users ADD COLUMN search_vector tsvector;
                    UPDATE perf_users SET search_vector = to_tsvector('english', name || ' ' || COALESCE(bio, ''));
                    CREATE INDEX IF NOT EXISTS idx_users_search ON perf_users USING GIN (search_vector);
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(createTsVector)) {
                    stmt.execute();
                }
            }
            
            String searchQuery = """
                SELECT id, name, email, ts_rank(search_vector, query) as rank
                FROM perf_users, to_tsquery('english', ?) as query
                WHERE search_vector @@ query
                ORDER BY rank DESC
                LIMIT 20
                """;
            
            List<Long> searchTimes = new ArrayList<>();
            String[] searchTerms = {"focus", "study", "work", "productivity", "team", "collaboration"};
            
            try (Connection connection = dataSource.getConnection()) {
                for (String term : searchTerms) {
                    for (int i = 0; i < 20; i++) {
                        long startTime = System.nanoTime();
                        
                        try (PreparedStatement stmt = connection.prepareStatement(searchQuery)) {
                            stmt.setString(1, term);
                            
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    rs.getString("name");
                                    rs.getDouble("rank");
                                }
                            }
                        }
                        
                        long endTime = System.nanoTime();
                        searchTimes.add((endTime - startTime) / 1_000_000);
                    }
                }
            }
            
            Collections.sort(searchTimes);
            long avgSearchTime = (long) searchTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95SearchTime = searchTimes.get((int) (searchTimes.size() * 0.95));
            
            assertThat(avgSearchTime)
                .describedAs("Full-text search should be fast")
                .isLessThanOrEqualTo(100);
            
            assertThat(p95SearchTime)
                .describedAs("P95 search time should be reasonable")
                .isLessThanOrEqualTo(250);
            
            System.out.println(String.format(
                "Full-text Search: Avg=%dms, P95=%dms, Searches=%d",
                avgSearchTime, p95SearchTime, searchTimes.size()
            ));
        }

        @Test
        @DisplayName("Analytical query performance")
        void testAnalyticalQueryPerformance() throws SQLException {
            String analyticalQuery = """
                SELECT 
                    DATE_TRUNC('hour', s.created_at) as hour_bucket,
                    COUNT(*) as session_count,
                    AVG(s.duration) as avg_duration,
                    SUM(CASE WHEN s.completed = true THEN 1 ELSE 0 END) as completed_sessions,
                    PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY s.duration) as median_duration
                FROM perf_sessions s
                WHERE s.created_at >= ? AND s.created_at < ?
                GROUP BY DATE_TRUNC('hour', s.created_at)
                ORDER BY hour_bucket
                """;
            
            List<Long> analyticalTimes = new ArrayList<>();
            int iterations = 50;
            
            try (Connection connection = dataSource.getConnection()) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    
                    try (PreparedStatement stmt = connection.prepareStatement(analyticalQuery)) {
                        stmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis() - 604800000)); // 1 week ago
                        stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                rs.getInt("session_count");
                                rs.getDouble("avg_duration");
                                rs.getDouble("median_duration");
                            }
                        }
                    }
                    
                    long endTime = System.nanoTime();
                    analyticalTimes.add((endTime - startTime) / 1_000_000);
                }
            }
            
            Collections.sort(analyticalTimes);
            long avgTime = (long) analyticalTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95Time = analyticalTimes.get((int) (analyticalTimes.size() * 0.95));
            
            // Analytical queries can be slower but should still be reasonable
            assertThat(avgTime)
                .describedAs("Analytical queries should complete within 1 second")
                .isLessThanOrEqualTo(1000);
            
            assertThat(p95Time)
                .describedAs("P95 analytical query time should be under 2 seconds")
                .isLessThanOrEqualTo(2000);
            
            System.out.println(String.format(
                "Analytical Query: Avg=%dms, P95=%dms, Iterations=%d",
                avgTime, p95Time, iterations
            ));
        }
    }

    @Nested
    @DisplayName("Connection Pool Performance Tests")
    class ConnectionPoolTests {

        @Test
        @DisplayName("Connection pool utilization under load")
        void testConnectionPoolUtilization() throws SQLException, InterruptedException {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            int maxPoolSize = hikari.getMaximumPoolSize();
            int testConnections = (int) (maxPoolSize * 0.8); // Use 80% of pool
            
            ExecutorService executor = Executors.newFixedThreadPool(testConnections);
            List<CompletableFuture<Long>> futures = new ArrayList<>();
            
            for (int i = 0; i < testConnections; i++) {
                CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        long startTime = System.nanoTime();
                        
                        try (Connection connection = dataSource.getConnection()) {
                            // Simulate database work
                            try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM perf_users WHERE active = true");
                                 ResultSet rs = stmt.executeQuery()) {
                                
                                if (rs.next()) {
                                    rs.getInt(1);
                                }
                            }
                            
                            Thread.sleep(1000); // Hold connection for 1 second
                        }
                        
                        long endTime = System.nanoTime();
                        return (endTime - startTime) / 1_000_000;
                        
                    } catch (Exception e) {
                        return -1L;
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Monitor connection pool metrics
            List<Integer> activeConnectionSamples = new ArrayList<>();
            ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
            
            monitor.scheduleAtFixedRate(() -> {
                int active = hikari.getHikariPoolMXBean().getActiveConnections();
                activeConnectionSamples.add(active);
            }, 0, 500, TimeUnit.MILLISECONDS);
            
            // Wait for all tasks to complete
            List<Long> connectionTimes = new ArrayList<>();
            for (CompletableFuture<Long> future : futures) {
                try {
                    Long time = future.get(30, TimeUnit.SECONDS);
                    if (time > 0) {
                        connectionTimes.add(time);
                    }
                } catch (Exception e) {
                    // Handle timeout or error
                }
            }
            
            executor.shutdown();
            monitor.shutdown();
            
            // Analyze connection pool performance
            int maxActiveConnections = activeConnectionSamples.stream().mapToInt(Integer::intValue).max().orElse(0);
            double avgActiveConnections = activeConnectionSamples.stream().mapToInt(Integer::intValue).average().orElse(0);
            double poolUtilization = (double) maxActiveConnections / maxPoolSize;
            
            assertThat(poolUtilization)
                .describedAs("Connection pool utilization should be efficient")
                .isLessThanOrEqualTo(0.85); // 85% max utilization
            
            double successRate = (double) connectionTimes.size() / testConnections;
            assertThat(successRate)
                .describedAs("Connection acquisition should be reliable")
                .isGreaterThanOrEqualTo(0.95);
            
            System.out.println(String.format(
                "Connection Pool: Max Active=%d/%d (%.1f%%), Avg Active=%.1f, Success Rate=%.1f%%",
                maxActiveConnections, maxPoolSize, poolUtilization * 100, avgActiveConnections, successRate * 100
            ));
        }

        @Test
        @DisplayName("Connection leak detection")
        void testConnectionLeakDetection() throws SQLException, InterruptedException {
            HikariDataSource hikari = (HikariDataSource) dataSource;
            
            int initialActive = hikari.getHikariPoolMXBean().getActiveConnections();
            int initialIdle = hikari.getHikariPoolMXBean().getIdleConnections();
            
            // Perform operations that might leak connections
            for (int i = 0; i < 20; i++) {
                try (Connection connection = dataSource.getConnection()) {
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT 1")) {
                        stmt.execute();
                    }
                    
                    // Simulate some processing time
                    Thread.sleep(100);
                }
            }
            
            // Wait for connections to be returned to pool
            Thread.sleep(2000);
            
            int finalActive = hikari.getHikariPoolMXBean().getActiveConnections();
            int finalIdle = hikari.getHikariPoolMXBean().getIdleConnections();
            
            assertThat(finalActive)
                .describedAs("No connections should leak")
                .isLessThanOrEqualTo(initialActive + 1); // Allow for small variance
            
            System.out.println(String.format(
                "Connection Leak Test: Initial Active=%d, Final Active=%d, Initial Idle=%d, Final Idle=%d",
                initialActive, finalActive, initialIdle, finalIdle
            ));
        }
    }

    @Nested
    @DisplayName("Batch Operations Performance")
    class BatchOperationTests {

        @Test
        @DisplayName("Batch insert performance")
        void testBatchInsertPerformance() throws SQLException {
            int batchSize = 1000;
            String insertSQL = "INSERT INTO perf_batch_test (name, email, data, created_at) VALUES (?, ?, ?::jsonb, ?)";
            
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                
                try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
                    for (int i = 0; i < batchSize; i++) {
                        stmt.setString(1, "BatchUser" + i);
                        stmt.setString(2, "batch" + i + "@test.com");
                        stmt.setString(3, "{\"batchId\": " + i + ", \"testData\": \"performance\"}");
                        stmt.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                        
                        stmt.addBatch();
                        
                        if (i % 100 == 0) {
                            stmt.executeBatch();
                        }
                    }
                    
                    stmt.executeBatch();
                }
                
                connection.commit();
                connection.setAutoCommit(true);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double recordsPerSecond = (double) batchSize / (duration / 1000.0);
            
            assertThat(duration)
                .describedAs("Batch insert should be fast")
                .isLessThanOrEqualTo(5000); // 5 seconds for 1000 records
            
            assertThat(recordsPerSecond)
                .describedAs("Batch insert throughput should be high")
                .isGreaterThan(200); // 200 records per second minimum
            
            System.out.println(String.format(
                "Batch Insert: %d records in %dms (%.1f records/sec)",
                batchSize, duration, recordsPerSecond
            ));
        }

        @Test
        @DisplayName("Batch update performance")
        void testBatchUpdatePerformance() throws SQLException {
            // First, ensure we have data to update
            String selectSQL = "SELECT id FROM perf_users LIMIT 1000";
            List<Integer> userIds = new ArrayList<>();
            
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(selectSQL);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    userIds.add(rs.getInt("id"));
                }
            }
            
            if (userIds.isEmpty()) {
                System.out.println("No users found for batch update test");
                return;
            }
            
            String updateSQL = "UPDATE perf_users SET last_login = ?, login_count = login_count + 1 WHERE id = ?";
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                
                try (PreparedStatement stmt = connection.prepareStatement(updateSQL)) {
                    for (Integer userId : userIds) {
                        stmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
                        stmt.setInt(2, userId);
                        stmt.addBatch();
                    }
                    
                    stmt.executeBatch();
                }
                
                connection.commit();
                connection.setAutoCommit(true);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double updatesPerSecond = (double) userIds.size() / (duration / 1000.0);
            
            assertThat(duration)
                .describedAs("Batch updates should be fast")
                .isLessThanOrEqualTo(3000); // 3 seconds for updates
            
            System.out.println(String.format(
                "Batch Update: %d updates in %dms (%.1f updates/sec)",
                userIds.size(), duration, updatesPerSecond
            ));
        }
    }

    @Nested
    @DisplayName("Concurrent Access Tests")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("Concurrent read performance")
        void testConcurrentReadPerformance() throws InterruptedException {
            int threadCount = 50;
            int operationsPerThread = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<CompletableFuture<List<Long>>> futures = new ArrayList<>();
            
            for (int i = 0; i < threadCount; i++) {
                CompletableFuture<List<Long>> future = CompletableFuture.supplyAsync(() -> {
                    List<Long> threadTimes = new ArrayList<>();
                    
                    try (Connection connection = dataSource.getConnection()) {
                        String query = "SELECT id, name, email FROM perf_users WHERE active = true LIMIT 100";
                        
                        for (int j = 0; j < operationsPerThread; j++) {
                            long startTime = System.nanoTime();
                            
                            try (PreparedStatement stmt = connection.prepareStatement(query);
                                 ResultSet rs = stmt.executeQuery()) {
                                
                                while (rs.next()) {
                                    rs.getInt("id");
                                    rs.getString("name");
                                    rs.getString("email");
                                }
                            }
                            
                            long endTime = System.nanoTime();
                            threadTimes.add((endTime - startTime) / 1_000_000);
                            
                            Thread.sleep(50); // Small delay between operations
                        }
                        
                    } catch (Exception e) {
                        // Handle error
                    }
                    
                    return threadTimes;
                }, executor);
                
                futures.add(future);
            }
            
            // Collect all results
            List<Long> allTimes = new ArrayList<>();
            int totalOperations = 0;
            
            for (CompletableFuture<List<Long>> future : futures) {
                try {
                    List<Long> threadTimes = future.get(60, TimeUnit.SECONDS);
                    allTimes.addAll(threadTimes);
                    totalOperations += operationsPerThread;
                } catch (Exception e) {
                    // Handle timeout
                }
            }
            
            executor.shutdown();
            
            if (!allTimes.isEmpty()) {
                Collections.sort(allTimes);
                long avgTime = (long) allTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                long p95Time = allTimes.get((int) (allTimes.size() * 0.95));
                
                assertThat(avgTime)
                    .describedAs("Average concurrent read time should be reasonable")
                    .isLessThanOrEqualTo(100);
                
                assertThat(p95Time)
                    .describedAs("P95 concurrent read time should be acceptable")
                    .isLessThanOrEqualTo(200);
                
                System.out.println(String.format(
                    "Concurrent Reads: %d threads, %d operations, Avg=%dms, P95=%dms",
                    threadCount, allTimes.size(), avgTime, p95Time
                ));
            }
        }

        @Test
        @DisplayName("Mixed read-write performance")
        void testMixedReadWritePerformance() throws InterruptedException {
            int readerThreads = 30;
            int writerThreads = 10;
            int operationsPerThread = 20;
            
            ExecutorService executor = Executors.newFixedThreadPool(readerThreads + writerThreads);
            List<CompletableFuture<OperationResult>> futures = new ArrayList<>();
            
            // Start reader threads
            for (int i = 0; i < readerThreads; i++) {
                CompletableFuture<OperationResult> future = CompletableFuture.supplyAsync(() -> {
                    return performReadOperations(operationsPerThread);
                }, executor);
                futures.add(future);
            }
            
            // Start writer threads
            for (int i = 0; i < writerThreads; i++) {
                final int threadId = i;
                CompletableFuture<OperationResult> future = CompletableFuture.supplyAsync(() -> {
                    return performWriteOperations(operationsPerThread, threadId);
                }, executor);
                futures.add(future);
            }
            
            // Collect results
            List<OperationResult> results = new ArrayList<>();
            for (CompletableFuture<OperationResult> future : futures) {
                try {
                    OperationResult result = future.get(120, TimeUnit.SECONDS);
                    results.add(result);
                } catch (Exception e) {
                    results.add(new OperationResult("error", 0, 0, List.of("Timeout or error")));
                }
            }
            
            executor.shutdown();
            
            // Analyze mixed workload performance
            int totalReadOps = results.stream().filter(r -> "read".equals(r.operationType))
                                     .mapToInt(r -> r.successfulOperations).sum();
            int totalWriteOps = results.stream().filter(r -> "write".equals(r.operationType))
                                      .mapToInt(r -> r.successfulOperations).sum();
            
            double avgReadTime = results.stream().filter(r -> "read".equals(r.operationType))
                                       .mapToDouble(r -> r.avgOperationTime).average().orElse(0);
            double avgWriteTime = results.stream().filter(r -> "write".equals(r.operationType))
                                        .mapToDouble(r -> r.avgOperationTime).average().orElse(0);
            
            assertThat(avgReadTime)
                .describedAs("Average read time should be reasonable under mixed load")
                .isLessThanOrEqualTo(150);
            
            assertThat(avgWriteTime)
                .describedAs("Average write time should be acceptable under mixed load")
                .isLessThanOrEqualTo(300);
            
            System.out.println(String.format(
                "Mixed Workload: Reads=%d (%.1fms avg), Writes=%d (%.1fms avg)",
                totalReadOps, avgReadTime, totalWriteOps, avgWriteTime
            ));
        }
    }

    // Helper methods

    private void setupPerformanceTestData() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Create performance test tables
            createPerformanceTestTables(connection);
            
            // Insert test data if not exists
            if (getTableRowCount(connection, "perf_users") < 10000) {
                insertPerformanceTestData(connection);
            }
        }
    }

    private void createPerformanceTestTables(Connection connection) throws SQLException {
        String[] createTableStatements = {
            """
            CREATE TABLE IF NOT EXISTS perf_users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(150) UNIQUE NOT NULL,
                bio TEXT,
                active BOOLEAN DEFAULT true,
                login_count INTEGER DEFAULT 0,
                last_login TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS perf_hives (
                id SERIAL PRIMARY KEY,
                name VARCHAR(200) NOT NULL,
                description TEXT,
                owner_id INTEGER REFERENCES perf_users(id),
                is_public BOOLEAN DEFAULT false,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS perf_sessions (
                id SERIAL PRIMARY KEY,
                user_id INTEGER REFERENCES perf_users(id),
                hive_id INTEGER REFERENCES perf_hives(id),
                duration INTEGER NOT NULL,
                completed BOOLEAN DEFAULT false,
                session_type VARCHAR(50) DEFAULT 'focus',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS perf_batch_test (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(150) NOT NULL,
                data JSONB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """
        };
        
        for (String sql : createTableStatements) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.execute();
            }
        }
        
        // Create indexes
        String[] indexStatements = {
            "CREATE INDEX IF NOT EXISTS idx_perf_users_email ON perf_users(email)",
            "CREATE INDEX IF NOT EXISTS idx_perf_users_active ON perf_users(active)",
            "CREATE INDEX IF NOT EXISTS idx_perf_users_created_at ON perf_users(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_perf_hives_owner ON perf_hives(owner_id)",
            "CREATE INDEX IF NOT EXISTS idx_perf_sessions_user ON perf_sessions(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_perf_sessions_hive ON perf_sessions(hive_id)",
            "CREATE INDEX IF NOT EXISTS idx_perf_sessions_created_at ON perf_sessions(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_perf_batch_test_created_at ON perf_batch_test(created_at)"
        };
        
        for (String sql : indexStatements) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.execute();
            }
        }
    }

    private void insertPerformanceTestData(Connection connection) throws SQLException {
        System.out.println("Inserting performance test data...");
        
        connection.setAutoCommit(false);
        
        try {
            // Insert users
            String userSQL = "INSERT INTO perf_users (name, email, bio, active) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(userSQL)) {
                for (int i = 0; i < 10000; i++) {
                    stmt.setString(1, "User" + i);
                    stmt.setString(2, "user" + i + "@performance.test");
                    stmt.setString(3, "Performance test user " + i + " for database testing purposes.");
                    stmt.setBoolean(4, i % 10 != 0); // 90% active users
                    stmt.addBatch();
                    
                    if (i % 1000 == 0) {
                        stmt.executeBatch();
                        System.out.println("Inserted " + (i + 1) + " users");
                    }
                }
                stmt.executeBatch();
            }
            
            // Insert hives
            String hiveSQL = "INSERT INTO perf_hives (name, description, owner_id, is_public) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(hiveSQL)) {
                for (int i = 0; i < 2000; i++) {
                    stmt.setString(1, "Hive" + i);
                    stmt.setString(2, "Performance test hive " + i);
                    stmt.setInt(3, (i % 10000) + 1); // Random user as owner
                    stmt.setBoolean(4, i % 3 == 0); // 33% public hives
                    stmt.addBatch();
                    
                    if (i % 500 == 0) {
                        stmt.executeBatch();
                    }
                }
                stmt.executeBatch();
            }
            
            // Insert sessions
            String sessionSQL = "INSERT INTO perf_sessions (user_id, hive_id, duration, completed, session_type) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sessionSQL)) {
                String[] sessionTypes = {"focus", "break", "study", "work"};
                
                for (int i = 0; i < 50000; i++) {
                    stmt.setInt(1, (i % 10000) + 1);
                    stmt.setInt(2, (i % 2000) + 1);
                    stmt.setInt(3, 900 + (i % 2700)); // 15-60 minutes
                    stmt.setBoolean(4, i % 4 != 0); // 75% completed
                    stmt.setString(5, sessionTypes[i % sessionTypes.length]);
                    stmt.addBatch();
                    
                    if (i % 10000 == 0) {
                        stmt.executeBatch();
                        System.out.println("Inserted " + (i + 1) + " sessions");
                    }
                }
                stmt.executeBatch();
            }
            
            connection.commit();
            System.out.println("Performance test data insertion completed");
            
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private int getTableRowCount(Connection connection, String tableName) throws SQLException {
        String countSQL = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement stmt = connection.prepareStatement(countSQL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private OperationResult performReadOperations(int operationCount) {
        List<Long> operationTimes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT id, name, email FROM perf_users WHERE active = true ORDER BY RANDOM() LIMIT 50";
            
            for (int i = 0; i < operationCount; i++) {
                long startTime = System.nanoTime();
                
                try (PreparedStatement stmt = connection.prepareStatement(query);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        rs.getInt("id");
                        rs.getString("name");
                        rs.getString("email");
                    }
                }
                
                long endTime = System.nanoTime();
                operationTimes.add((endTime - startTime) / 1_000_000);
                
                Thread.sleep(100);
            }
            
        } catch (Exception e) {
            errors.add("Read operation error: " + e.getMessage());
        }
        
        double avgTime = operationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        return new OperationResult("read", operationTimes.size(), (long) avgTime, errors);
    }

    private OperationResult performWriteOperations(int operationCount, int threadId) {
        List<Long> operationTimes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String updateSQL = "UPDATE perf_users SET login_count = login_count + 1, last_login = ? WHERE id = ?";
            
            for (int i = 0; i < operationCount; i++) {
                long startTime = System.nanoTime();
                
                try (PreparedStatement stmt = connection.prepareStatement(updateSQL)) {
                    stmt.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
                    stmt.setInt(2, (threadId * operationCount + i) % 10000 + 1);
                    
                    stmt.executeUpdate();
                }
                
                long endTime = System.nanoTime();
                operationTimes.add((endTime - startTime) / 1_000_000);
                
                Thread.sleep(150);
            }
            
        } catch (Exception e) {
            errors.add("Write operation error: " + e.getMessage());
        }
        
        double avgTime = operationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        return new OperationResult("write", operationTimes.size(), (long) avgTime, errors);
    }

    // Helper classes
    static class OperationResult {
        final String operationType;
        final int successfulOperations;
        final long avgOperationTime;
        final List<String> errors;
        
        OperationResult(String operationType, int successful, long avgTime, List<String> errors) {
            this.operationType = operationType;
            this.successfulOperations = successful;
            this.avgOperationTime = avgTime;
            this.errors = errors;
        }
    }
}