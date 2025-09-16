package com.focushive.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers Configuration for Integration Tests
 *
 * This configuration provides PostgreSQL containers for integration testing.
 * It's part of our TDD database strategy to test against real PostgreSQL behavior.
 *
 * Usage:
 * - Use @ActiveProfiles("integration-test") to enable
 * - Requires Docker to be running
 * - Provides actual PostgreSQL database for testing
 */
@TestConfiguration
@Profile("integration-test")
public class TestContainersConfig {

    /**
     * PostgreSQL container for integration tests
     * Configured manually for Spring Boot 3.3.0 compatibility
     */
    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("focushive_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("testcontainers/init-test-db.sql")
            .withReuse(true); // Reuse container across tests for performance
    }

    /**
     * Database test utilities configured for PostgreSQL
     */
    @Bean
    public IntegrationDatabaseTestUtils integrationDatabaseTestUtils(PostgreSQLContainer<?> container) {
        return new IntegrationDatabaseTestUtils(container);
    }

    /**
     * Integration test utilities for PostgreSQL
     */
    public static class IntegrationDatabaseTestUtils {
        private final PostgreSQLContainer<?> container;

        public IntegrationDatabaseTestUtils(PostgreSQLContainer<?> container) {
            this.container = container;
        }

        /**
         * Get container information
         */
        public ContainerInfo getContainerInfo() {
            return new ContainerInfo(
                container.getDatabaseName(),
                container.getJdbcUrl(),
                container.getUsername(),
                container.isRunning()
            );
        }

        /**
         * Wait for container to be ready
         */
        public boolean waitForReady(int timeoutSeconds) {
            try {
                long startTime = System.currentTimeMillis();
                while ((System.currentTimeMillis() - startTime) < timeoutSeconds * 1000) {
                    if (container.isRunning()) {
                        return true;
                    }
                    Thread.sleep(100);
                }
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        /**
         * Execute SQL script for test setup
         */
        public void executeScript(String script) {
            // This would execute SQL scripts for test data setup
            // Implementation depends on test requirements
        }

        /**
         * Clean up test data in PostgreSQL
         */
        public void cleanupPostgreSQLTestData() {
            // PostgreSQL-specific cleanup logic
            // Different from H2 cleanup due to different SQL syntax
        }
    }

    /**
     * Container information record
     */
    public static record ContainerInfo(
        String databaseName,
        String jdbcUrl,
        String username,
        boolean isRunning
    ) {}
}