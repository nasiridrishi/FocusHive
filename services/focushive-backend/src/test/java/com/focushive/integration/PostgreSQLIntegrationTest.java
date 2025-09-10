package com.focushive.integration;

import org.flywaydb.core.Flyway;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using Testcontainers to validate PostgreSQL configuration
 * with real PostgreSQL database instance.
 */
@SpringBootTest
@Testcontainers
@DisplayName("PostgreSQL Testcontainers Integration Tests")
class PostgreSQLIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    @DisplayName("PostgreSQL container should be running and accessible")
    void testPostgreSQLContainerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getDatabaseName()).isEqualTo("testdb");
        assertThat(postgres.getUsername()).isEqualTo("testuser");
        assertThat(postgres.getPassword()).isEqualTo("testpass");
    }

    @Test
    @DisplayName("Should be able to connect to PostgreSQL container")
    void testConnectionToPostgreSQL() throws SQLException {
        String jdbcUrl = postgres.getJdbcUrl();
        try (Connection connection = java.sql.DriverManager.getConnection(
                jdbcUrl, postgres.getUsername(), postgres.getPassword())) {
            
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(5)).isTrue();
            
            // Verify PostgreSQL version
            try (PreparedStatement stmt = connection.prepareStatement("SELECT version()")) {
                ResultSet rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                String version = rs.getString(1);
                assertThat(version).contains("PostgreSQL 15");
            }
        }
    }

    @Test
    @DisplayName("Flyway migrations should run successfully on PostgreSQL")
    void testFlywayMigrationsOnPostgreSQL() {
        assertDoesNotThrow(() -> {
            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)  // Explicitly enable clean for this test
                    .load();
            
            // Clean and migrate
            flyway.clean();
            var result = flyway.migrate();
            
            assertThat(result.success).isTrue();
            assertThat(result.migrationsExecuted).isGreaterThan(0);
        });
    }

    @Test
    @DisplayName("JSONB columns should work correctly in PostgreSQL")
    void testJSONBSupport() throws SQLException {
        try (Connection connection = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            // Create a test table with JSONB column
            String createTable = """
                CREATE TEMP TABLE json_test (
                    id SERIAL PRIMARY KEY,
                    data JSONB
                )
                """;
            
            try (PreparedStatement createStmt = connection.prepareStatement(createTable)) {
                createStmt.execute();
            }
            
            // Insert JSONB data
            String insertJson = "INSERT INTO json_test (data) VALUES (?::jsonb)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertJson)) {
                insertStmt.setString(1, "{\"name\": \"test\", \"count\": 42}");
                int rowsAffected = insertStmt.executeUpdate();
                assertThat(rowsAffected).isEqualTo(1);
            }
            
            // Query JSONB data
            String selectJson = "SELECT data->>'name' as name, data->>'count' as count FROM json_test";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectJson);
                 ResultSet rs = selectStmt.executeQuery()) {
                
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("test");
                assertThat(rs.getString("count")).isEqualTo("42");
            }
        }
    }

    @Test
    @DisplayName("Array columns should work correctly in PostgreSQL")
    void testArraySupport() throws SQLException {
        try (Connection connection = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            // Create a test table with array column
            String createTable = """
                CREATE TEMP TABLE array_test (
                    id SERIAL PRIMARY KEY,
                    tags TEXT[]
                )
                """;
            
            try (PreparedStatement createStmt = connection.prepareStatement(createTable)) {
                createStmt.execute();
            }
            
            // Insert array data
            String insertArray = "INSERT INTO array_test (tags) VALUES (?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertArray)) {
                java.sql.Array array = connection.createArrayOf("TEXT", new String[]{"java", "spring", "postgresql"});
                insertStmt.setArray(1, array);
                int rowsAffected = insertStmt.executeUpdate();
                assertThat(rowsAffected).isEqualTo(1);
            }
            
            // Query array data
            String selectArray = "SELECT tags FROM array_test";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectArray);
                 ResultSet rs = selectStmt.executeQuery()) {
                
                assertThat(rs.next()).isTrue();
                java.sql.Array resultArray = rs.getArray("tags");
                String[] tags = (String[]) resultArray.getArray();
                assertThat(tags).containsExactly("java", "spring", "postgresql");
            }
        }
    }

    @Test
    @DisplayName("Transaction isolation should work correctly")
    void testTransactionIsolation() throws SQLException {
        try (Connection connection = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            // Set transaction isolation to READ_COMMITTED
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
            
            // Test transaction
            connection.setAutoCommit(false);
            
            String createTable = "CREATE TEMP TABLE tx_test (id SERIAL PRIMARY KEY, value TEXT)";
            try (PreparedStatement createStmt = connection.prepareStatement(createTable)) {
                createStmt.execute();
            }
            
            String insert = "INSERT INTO tx_test (value) VALUES (?)";
            try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
                insertStmt.setString(1, "test_value");
                insertStmt.executeUpdate();
            }
            
            connection.commit();
            connection.setAutoCommit(true);
            
            // Verify data was committed
            String select = "SELECT COUNT(*) FROM tx_test";
            try (PreparedStatement selectStmt = connection.prepareStatement(select);
                 ResultSet rs = selectStmt.executeQuery()) {
                
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    @DisplayName("Concurrent connections should be handled properly")
    void testConcurrentConnections() throws SQLException, InterruptedException {
        final int CONNECTION_COUNT = 5;
        Thread[] threads = new Thread[CONNECTION_COUNT];
        final boolean[] results = new boolean[CONNECTION_COUNT];
        
        for (int i = 0; i < CONNECTION_COUNT; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try (Connection connection = java.sql.DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                    
                    // Simulate some work
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT pg_sleep(0.1)")) {
                        stmt.execute();
                    }
                    
                    results[index] = connection.isValid(1);
                } catch (SQLException e) {
                    results[index] = false;
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout
        }
        
        // All connections should have been successful
        for (int i = 0; i < CONNECTION_COUNT; i++) {
            assertThat(results[i]).isTrue();
        }
    }

    @Test
    @DisplayName("Performance should be adequate for common operations")
    void testPerformance() throws SQLException {
        try (Connection connection = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            
            // Create test table
            String createTable = """
                CREATE TEMP TABLE perf_test (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;
            
            try (PreparedStatement createStmt = connection.prepareStatement(createTable)) {
                createStmt.execute();
            }
            
            // Time batch insert
            long startTime = System.currentTimeMillis();
            String insert = "INSERT INTO perf_test (name) VALUES (?)";
            
            try (PreparedStatement insertStmt = connection.prepareStatement(insert)) {
                connection.setAutoCommit(false);
                
                for (int i = 0; i < 1000; i++) {
                    insertStmt.setString(1, "test_name_" + i);
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
            
            // Should complete 1000 inserts in reasonable time (< 5 seconds)
            assertThat(duration).isLessThan(5000);
            
            // Verify all records were inserted
            String count = "SELECT COUNT(*) FROM perf_test";
            try (PreparedStatement countStmt = connection.prepareStatement(count);
                 ResultSet rs = countStmt.executeQuery()) {
                
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1000);
            }
        }
    }
}