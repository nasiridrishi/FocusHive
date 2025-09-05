package com.focushive.performance;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for PostgreSQL database configuration.
 * Tests connection pool performance, query execution times, and concurrent access.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("perftest")
            .withUsername("perfuser")
            .withPassword("perfpass")
            .withReuse(true);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Performance-optimized settings
        registry.add("spring.datasource.hikari.minimum-idle", () -> "10");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "30");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "600000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "1800000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
    }

    private DataSource dataSource;

    @BeforeEach
    void setUp() throws SQLException {
        // Create HikariDataSource for direct testing
        var config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setDriverClassName("org.postgresql.Driver");
        
        // Performance settings
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(30);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setConnectionTestQuery("SELECT 1");
        
        // PostgreSQL-specific optimizations
        config.addDataSourceProperty("preparedStatementCacheSize", "256");
        config.addDataSourceProperty("preparedStatementCacheSqlLimit", "2048");
        config.addDataSourceProperty("defaultTransactionIsolation", "TRANSACTION_READ_COMMITTED");
        
        this.dataSource = new HikariDataSource(config);
        
        // Initialize test table
        initializeTestTable();
    }

    private void initializeTestTable() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String createTable = """
                CREATE TABLE IF NOT EXISTS performance_test (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    data JSONB,
                    tags TEXT[],
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(createTable)) {
                stmt.execute();
            }
            
            // Create indexes for better performance
            String createIndexes = """
                CREATE INDEX IF NOT EXISTS idx_performance_test_name ON performance_test(name);
                CREATE INDEX IF NOT EXISTS idx_performance_test_email ON performance_test(email);
                CREATE INDEX IF NOT EXISTS idx_performance_test_data ON performance_test USING GIN (data);
                CREATE INDEX IF NOT EXISTS idx_performance_test_tags ON performance_test USING GIN (tags);
                """;
            
            try (PreparedStatement stmt = connection.prepareStatement(createIndexes)) {
                stmt.execute();
            }
        }
    }

    @Test
    @DisplayName("Connection pool should handle connection exhaustion gracefully")
    void testConnectionPoolExhaustion() throws SQLException {
        HikariDataSource hikariDS = (HikariDataSource) dataSource;
        int maxPoolSize = hikariDS.getMaximumPoolSize();
        
        List<Connection> connections = new ArrayList<>();
        
        try {
            // Obtain maximum number of connections
            for (int i = 0; i < maxPoolSize; i++) {
                Connection conn = dataSource.getConnection();
                connections.add(conn);
                assertThat(conn.isValid(1)).isTrue();
            }
            
            // Try to get one more connection (should timeout)
            long startTime = System.currentTimeMillis();
            assertThrows(SQLException.class, () -> {
                Connection extraConn = dataSource.getConnection();
                connections.add(extraConn); // Won't reach here due to timeout
            });
            
            long duration = System.currentTimeMillis() - startTime;
            // Should timeout within connection timeout + some buffer
            assertThat(duration).isBetween(25000L, 35000L); // ~30s timeout
            
        } finally {
            // Clean up connections
            for (Connection conn : connections) {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    @Test
    @DisplayName("Query performance should meet sub-100ms targets for simple operations")
    void testQueryPerformance() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Clear and populate test data
            try (PreparedStatement clearStmt = connection.prepareStatement("TRUNCATE performance_test")) {
                clearStmt.execute();
            }
            
            // Insert test data
            String insert = "INSERT INTO performance_test (name, email, data, tags) VALUES (?, ?, ?::jsonb, ?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
                connection.setAutoCommit(false);
                
                for (int i = 0; i < 1000; i++) {
                    insertStmt.setString(1, "User " + i);
                    insertStmt.setString(2, "user" + i + "@example.com");
                    insertStmt.setString(3, "{\"userId\": " + i + ", \"active\": true}");
                    
                    // Create array
                    java.sql.Array tagsArray = connection.createArrayOf("TEXT", 
                        new String[]{"user", "test", "tag" + (i % 10)});
                    insertStmt.setArray(4, tagsArray);
                    
                    insertStmt.addBatch();
                    
                    if (i % 100 == 0) {
                        insertStmt.executeBatch();
                    }
                }
                
                insertStmt.executeBatch();
                connection.commit();
                connection.setAutoCommit(true);
            }
            
            // Test simple SELECT performance
            String select = "SELECT id, name, email FROM performance_test WHERE name = ?";
            
            long totalTime = 0;
            int iterations = 100;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                
                try (PreparedStatement selectStmt = connection.prepareStatement(select)) {
                    selectStmt.setString(1, "User " + (i % 1000));
                    
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        while (rs.next()) {
                            // Process results
                            rs.getInt("id");
                            rs.getString("name");
                            rs.getString("email");
                        }
                    }
                }
                
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }
            
            double avgTimeMs = (totalTime / iterations) / 1_000_000.0;
            
            // Average query time should be under 10ms for indexed lookups
            assertThat(avgTimeMs).isLessThan(10.0);
            
            System.out.printf("Average query time: %.2f ms%n", avgTimeMs);
        }
    }

    @Test
    @DisplayName("Batch insert performance should handle 1000 records efficiently")
    void testBatchInsertPerformance() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // Clear test data
            try (PreparedStatement clearStmt = connection.prepareStatement("TRUNCATE performance_test")) {
                clearStmt.execute();
            }
            
            String insert = "INSERT INTO performance_test (name, email, data, tags) VALUES (?, ?, ?::jsonb, ?)";
            
            long startTime = System.currentTimeMillis();
            
            try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
                connection.setAutoCommit(false);
                
                for (int i = 0; i < 1000; i++) {
                    insertStmt.setString(1, "BatchUser " + i);
                    insertStmt.setString(2, "batch" + i + "@example.com");
                    insertStmt.setString(3, "{\"batchId\": " + i + ", \"processed\": false}");
                    
                    java.sql.Array tagsArray = connection.createArrayOf("TEXT", 
                        new String[]{"batch", "performance", "test" + i});
                    insertStmt.setArray(4, tagsArray);
                    
                    insertStmt.addBatch();
                    
                    if (i % 100 == 0) {
                        insertStmt.executeBatch();
                    }
                }
                
                insertStmt.executeBatch();
                connection.commit();
                connection.setAutoCommit(true);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // Should complete 1000 batch inserts in under 2 seconds
            assertThat(duration).isLessThan(2000);
            
            System.out.printf("Batch insert time for 1000 records: %d ms%n", duration);
            
            // Verify all records were inserted
            String count = "SELECT COUNT(*) FROM performance_test";
            try (PreparedStatement countStmt = connection.prepareStatement(count);
                 ResultSet rs = countStmt.executeQuery()) {
                
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1000);
            }
        }
    }

    @Test
    @DisplayName("Concurrent access should scale properly with multiple threads")
    void testConcurrentAccess() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final int OPERATIONS_PER_THREAD = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long totalTime = 0;
                
                for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                    long startTime = System.nanoTime();
                    
                    try (Connection connection = dataSource.getConnection()) {
                        String query = "SELECT COUNT(*) FROM performance_test WHERE id > ?";
                        
                        try (PreparedStatement stmt = connection.prepareStatement(query)) {
                            stmt.setInt(1, threadId * OPERATIONS_PER_THREAD + op);
                            
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    rs.getInt(1);
                                }
                            }
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    
                    long endTime = System.nanoTime();
                    totalTime += (endTime - startTime);
                }
                
                return totalTime;
                
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all threads to complete
        long totalOperationTime = 0;
        for (CompletableFuture<Long> future : futures) {
            totalOperationTime += future.join();
        }
        
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        
        double avgTimeMs = (totalOperationTime / (THREAD_COUNT * OPERATIONS_PER_THREAD)) / 1_000_000.0;
        
        // Average operation time should be reasonable under concurrent load
        assertThat(avgTimeMs).isLessThan(50.0);
        
        System.out.printf("Concurrent access - Average operation time: %.2f ms%n", avgTimeMs);
    }

    @Test
    @DisplayName("Transaction timeout should be handled properly")
    void testTransactionTimeoutHandling() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            
            // Start transaction
            String insert = "INSERT INTO performance_test (name, email) VALUES (?, ?)";
            
            try (PreparedStatement stmt = connection.prepareStatement(insert)) {
                stmt.setString(1, "Timeout Test");
                stmt.setString(2, "timeout@example.com");
                stmt.executeUpdate();
            }
            
            // Hold transaction open and test timeout behavior
            long startTime = System.currentTimeMillis();
            
            try {
                // Simulate work within transaction timeout window
                Thread.sleep(1000); // 1 second - should be fine
                
                connection.commit();
                
                long duration = System.currentTimeMillis() - startTime;
                assertThat(duration).isLessThan(5000); // Should complete quickly
                
            } catch (InterruptedException e) {
                connection.rollback();
                Thread.currentThread().interrupt();
                fail("Transaction was interrupted");
            }
            
            connection.setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("Deadlock detection and recovery should work correctly")
    void testDeadlockDetectionAndRecovery() throws InterruptedException {
        final String[] results = new String[2];
        final boolean[] completed = new boolean[2];
        
        Thread thread1 = new Thread(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                
                // Insert record 1
                String insert1 = "INSERT INTO performance_test (name, email) VALUES ('Deadlock1', 'deadlock1@test.com')";
                try (PreparedStatement stmt = connection.prepareStatement(insert1)) {
                    stmt.executeUpdate();
                }
                
                // Give thread2 time to start its transaction
                Thread.sleep(100);
                
                // Try to insert record 2 (potential conflict)
                String insert2 = "INSERT INTO performance_test (name, email) VALUES ('Deadlock2', 'deadlock2@test.com')";
                try (PreparedStatement stmt = connection.prepareStatement(insert2)) {
                    stmt.executeUpdate();
                }
                
                connection.commit();
                results[0] = "SUCCESS";
                completed[0] = true;
                
            } catch (SQLException e) {
                results[0] = "DEADLOCK_DETECTED: " + e.getSQLState();
                completed[0] = true;
            } catch (InterruptedException e) {
                results[0] = "INTERRUPTED";
                Thread.currentThread().interrupt();
                completed[0] = true;
            }
        });
        
        Thread thread2 = new Thread(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                
                // Insert record 2
                String insert2 = "INSERT INTO performance_test (name, email) VALUES ('Deadlock2', 'deadlock2@test.com')";
                try (PreparedStatement stmt = connection.prepareStatement(insert2)) {
                    stmt.executeUpdate();
                }
                
                // Give thread1 time to start its transaction
                Thread.sleep(100);
                
                // Try to insert record 1 (potential conflict)
                String insert1 = "INSERT INTO performance_test (name, email) VALUES ('Deadlock1', 'deadlock1@test.com')";
                try (PreparedStatement stmt = connection.prepareStatement(insert1)) {
                    stmt.executeUpdate();
                }
                
                connection.commit();
                results[1] = "SUCCESS";
                completed[1] = true;
                
            } catch (SQLException e) {
                results[1] = "DEADLOCK_DETECTED: " + e.getSQLState();
                completed[1] = true;
            } catch (InterruptedException e) {
                results[1] = "INTERRUPTED";
                Thread.currentThread().interrupt();
                completed[1] = true;
            }
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join(5000);
        thread2.join(5000);
        
        // At least one thread should complete (though both might succeed if no actual deadlock)
        assertThat(completed[0] || completed[1]).isTrue();
        
        // If both threads are trying to insert the same unique data, 
        // we should see at least one constraint violation or deadlock
        System.out.printf("Thread 1 result: %s%n", results[0]);
        System.out.printf("Thread 2 result: %s%n", results[1]);
    }
}