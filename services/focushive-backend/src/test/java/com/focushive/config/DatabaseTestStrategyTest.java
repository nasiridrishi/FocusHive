package com.focushive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TDD Test Suite for Database Strategy Configuration
 *
 * These tests validate our database testing strategy:
 * 1. H2 for unit tests with PostgreSQL compatibility
 * 2. TestContainers for integration tests
 * 3. Database cleanup between tests
 * 4. Schema initialization strategy
 *
 * Written following TDD - these should FAIL initially.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = TestDatabaseConfig.class)
@DisplayName("Database Test Strategy Tests")
class DatabaseTestStrategyTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestDatabaseConfig.DatabaseTestUtils databaseTestUtils;

    @Autowired
    private TestDatabaseConfig.DatabaseValidationUtils databaseValidationUtils;

    @Test
    @DisplayName("Should use H2 database for unit tests")
    void shouldUseH2ForUnitTests() throws SQLException {
        // ARRANGE & ACT
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();

            // ASSERT - This should pass as H2 is already configured
            assertThat(databaseProductName)
                .as("Database should be H2 for unit tests")
                .isEqualTo("H2");
        }
    }

    @Test
    @DisplayName("Should use PostgreSQL compatibility mode in H2")
    void shouldUsePostgreSQLCompatibilityMode() throws SQLException {
        // ARRANGE & ACT
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String url = metaData.getURL();

            // ASSERT - This should pass as PostgreSQL mode is configured
            assertThat(url)
                .as("H2 should be configured with PostgreSQL compatibility mode")
                .contains("MODE=PostgreSQL");
        }
    }

    @Test
    @DisplayName("Should support UUID type in H2")
    void shouldSupportUUIDType() throws SQLException {
        // ARRANGE & ACT
        try (Connection connection = dataSource.getConnection()) {
            // Test that we can create a table with UUID column
            connection.createStatement().execute(
                "CREATE TEMPORARY TABLE test_uuid_table (id UUID PRIMARY KEY, name VARCHAR(100))"
            );

            // Insert a UUID value
            connection.createStatement().execute(
                "INSERT INTO test_uuid_table VALUES ('123e4567-e89b-12d3-a456-426614174000', 'test')"
            );

            // ASSERT - If we get here without exception, UUID is supported
            assertThat(true).as("UUID type should be supported in H2").isTrue();
        }
    }

    @Test
    @DisplayName("Should support JSONB type in H2")
    void shouldSupportJSONBType() throws SQLException {
        // ARRANGE & ACT
        try (Connection connection = dataSource.getConnection()) {
            // Test that we can create a table with JSONB column
            connection.createStatement().execute(
                "CREATE TEMPORARY TABLE test_jsonb_table (id INTEGER, data JSONB)"
            );

            // Insert a JSON value
            connection.createStatement().execute(
                "INSERT INTO test_jsonb_table VALUES (1, '{\"key\": \"value\"}')"
            );

            // ASSERT - If we get here without exception, JSONB is supported
            assertThat(true).as("JSONB type should be supported in H2").isTrue();
        }
    }

    @Test
    @DisplayName("Should have fast connection pool for tests")
    void shouldHaveFastConnectionPoolForTests() throws SQLException {
        // ARRANGE & ACT
        long startTime = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection()) {
            long connectionTime = System.currentTimeMillis() - startTime;

            // ASSERT - Connection should be fast (< 100ms)
            assertThat(connectionTime)
                .as("Database connection should be fast for tests")
                .isLessThan(100L);
        }
    }

    @Test
    @DisplayName("Should cleanup data between tests")
    void shouldCleanupBetweenTests() {
        // ARRANGE - Verify our database utils are available
        assertThat(databaseTestUtils)
            .as("Database test utils should be available")
            .isNotNull();

        // ACT - Test the cleanup functionality
        boolean isReady = databaseTestUtils.isDatabaseReady();

        // ASSERT - Database should be ready for tests
        assertThat(isReady)
            .as("Database should be ready for tests")
            .isTrue();

        // Test cleanup doesn't throw exceptions (even if tables don't exist yet)
        try {
            databaseTestUtils.cleanupTestData();
            // If we get here, cleanup completed without throwing exceptions
            assertThat(true).as("Cleanup should complete without exceptions").isTrue();
        } catch (Exception e) {
            // Cleanup might fail if tables don't exist, which is expected in this phase
            assertThat(e.getMessage())
                .as("Cleanup failure should be related to missing tables")
                .containsAnyOf("table", "does not exist", "not found");
        }
    }

    @Test
    @DisplayName("Should support TestContainers for integration tests - FAILING TEST")
    void shouldUseTestContainersForIntegrationTests() {
        // This test should FAIL initially
        // We need to implement TestContainers configuration

        // This would be implemented in a separate integration test class
        // For now, this test demonstrates what we need to implement
        throw new AssertionError("TestContainers not yet configured - this test should fail initially as per TDD");
    }

    @Test
    @DisplayName("Should have separate test database configuration")
    void shouldHaveSeparateTestDatabaseConfiguration() {
        // ARRANGE & ACT - Check that our test configuration classes are available
        assertThat(databaseTestUtils)
            .as("Database test utils should be configured")
            .isNotNull();

        assertThat(databaseValidationUtils)
            .as("Database validation utils should be configured")
            .isNotNull();

        // Test database info retrieval
        TestDatabaseConfig.DatabaseInfo dbInfo = databaseTestUtils.getDatabaseInfo();

        // ASSERT - Should have H2 database with proper configuration
        assertThat(dbInfo.productName())
            .as("Should be using H2 database")
            .isEqualTo("H2");

        assertThat(dbInfo.url())
            .as("Should have PostgreSQL compatibility mode")
            .contains("MODE=PostgreSQL");
    }

    @Test
    @DisplayName("Should validate database features properly")
    void shouldValidateDatabaseFeatures() {
        // ARRANGE & ACT - Use our validation utilities
        TestDatabaseConfig.ValidationResult result = databaseValidationUtils.validateDatabaseFeatures();

        // ASSERT - Core features should be supported
        assertThat(result.uuidSupported)
            .as("UUID type should be supported in H2")
            .isTrue();

        assertThat(result.jsonbSupported)
            .as("JSONB type should be supported in H2")
            .isTrue();

        assertThat(result.postgresqlModeEnabled)
            .as("PostgreSQL compatibility mode should be enabled")
            .isTrue();

        if (result.hasErrors()) {
            System.out.println("Validation errors: " + result.getErrors());
        }
    }

    @Test
    @DisplayName("Should handle schema initialization properly - FAILING TEST")
    void shouldHandleSchemaInitializationProperly() {
        // This test should verify our schema initialization strategy
        // Should FAIL initially until we implement proper schema handling

        // Currently using JPA create-drop, so tables won't exist until entities are used
        boolean isSchemaInitialized = databaseValidationUtils.isSchemaInitialized();

        // This might fail if no JPA entities have been loaded yet - that's expected in TDD
        // We'll make this pass by ensuring proper schema initialization
        throw new AssertionError("Schema initialization strategy needs validation - current status: " + isSchemaInitialized);
    }
}