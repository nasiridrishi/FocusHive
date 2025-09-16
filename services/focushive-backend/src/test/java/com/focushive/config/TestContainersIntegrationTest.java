package com.focushive.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests using TestContainers with PostgreSQL
 *
 * This class demonstrates the TestContainers setup for integration testing.
 * It uses a real PostgreSQL database running in a Docker container.
 *
 * Requirements:
 * - Docker must be running
 * - Uses @ActiveProfiles("integration-test") to enable TestContainers config
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@ContextConfiguration(classes = TestContainersConfig.class)
@Testcontainers
@DisplayName("TestContainers Integration Tests")
class TestContainersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("focushive_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private TestContainersConfig.IntegrationDatabaseTestUtils integrationUtils;

    @Test
    @DisplayName("Should start PostgreSQL container successfully")
    void shouldStartPostgreSQLContainer() {
        // ARRANGE & ACT - Container should be started automatically

        // ASSERT - Container should be running
        assertThat(postgres.isRunning())
            .as("PostgreSQL container should be running")
            .isTrue();

        assertThat(postgres.getDatabaseName())
            .as("Database name should be configured correctly")
            .isEqualTo("focushive_test");
    }

    @Test
    @DisplayName("Should connect to PostgreSQL database")
    void shouldConnectToPostgreSQLDatabase() throws Exception {
        // This test might fail if TestContainers configuration is not complete
        // That's expected as part of TDD - we'll make it pass by completing the setup

        if (dataSource == null) {
            throw new AssertionError("DataSource not configured for TestContainers - this should fail initially as per TDD");
        }

        // ARRANGE & ACT - Try to get connection
        try (Connection connection = dataSource.getConnection()) {
            // ASSERT - Should be able to connect to PostgreSQL
            assertThat(connection.getMetaData().getDatabaseProductName())
                .as("Should be connected to PostgreSQL database")
                .isEqualTo("PostgreSQL");
        }
    }

    @Test
    @DisplayName("Should have integration test utilities available")
    void shouldHaveIntegrationTestUtilities() {
        // This test verifies our integration utilities are properly configured

        if (integrationUtils == null) {
            throw new AssertionError("Integration test utilities not configured - should fail initially as per TDD");
        }

        // ARRANGE & ACT - Get container info
        TestContainersConfig.ContainerInfo info = integrationUtils.getContainerInfo();

        // ASSERT - Container info should be available
        assertThat(info.isRunning())
            .as("Container should be reported as running")
            .isTrue();

        assertThat(info.databaseName())
            .as("Database name should match configuration")
            .isEqualTo("focushive_test");
    }

    @Test
    @DisplayName("Should validate PostgreSQL features")
    void shouldValidatePostgreSQLFeatures() throws Exception {
        // This test validates that we can use real PostgreSQL features
        // that might not work exactly the same in H2

        if (dataSource == null) {
            throw new AssertionError("DataSource not available for PostgreSQL feature testing");
        }

        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement()) {

            // Test UUID generation (PostgreSQL specific)
            var result = statement.executeQuery("SELECT gen_random_uuid()");
            assertThat(result.next())
                .as("Should be able to generate UUIDs with PostgreSQL function")
                .isTrue();

            // Test JSONB operations (PostgreSQL specific)
            statement.execute("CREATE TEMPORARY TABLE test_jsonb (id SERIAL, data JSONB)");
            statement.execute("INSERT INTO test_jsonb (data) VALUES ('{\"test\": \"value\"}'::jsonb)");

            var jsonbResult = statement.executeQuery("SELECT data->>'test' as value FROM test_jsonb");
            assertThat(jsonbResult.next())
                .as("Should be able to query JSONB data")
                .isTrue();

            assertThat(jsonbResult.getString("value"))
                .as("JSONB query should return correct value")
                .isEqualTo("value");
        }
    }
}