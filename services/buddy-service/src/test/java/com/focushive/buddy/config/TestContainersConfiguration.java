package com.focushive.buddy.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * TestContainers configuration for integration tests.
 * Provides PostgreSQL and Redis containers for realistic testing environment.
 */
@TestConfiguration
@Profile("integration")
public class TestContainersConfiguration {

    public static final String POSTGRES_IMAGE = "postgres:15";
    public static final String REDIS_IMAGE = "redis:7-alpine";

    /**
     * PostgreSQL container for integration tests
     */
    @Bean
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName("buddy_service_integration")
                .withUsername("integration_user")
                .withPassword("integration_password")
                .withInitScript("init-test-db.sql"); // Optional: custom init script

        container.start();
        return container;
    }

    /**
     * Redis container for integration tests
     */
    @Bean
    public GenericContainer<?> redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                .withExposedPorts(6379)
                .withCommand("redis-server", "--requirepass", "test-password"); // Optional password

        container.start();
        return container;
    }

    /**
     * Configure application properties dynamically based on container ports
     */
    public static void configureProperties(
            DynamicPropertyRegistry registry,
            PostgreSQLContainer<?> postgres,
            GenericContainer<?> redis) {

        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA/Hibernate configuration for integration tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "test-password");
        registry.add("spring.data.redis.timeout", () -> "2000ms");
        registry.add("spring.data.redis.database", () -> "0");

        // Cache configuration
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.cache.redis.time-to-live", () -> "300000"); // 5 minutes

        // Flyway configuration for integration tests
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.validate-on-migrate", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    /**
     * Container lifecycle management utilities
     */
    public static class ContainerLifecycle {

        /**
         * Start all containers for integration tests
         */
        public static void startContainers(PostgreSQLContainer<?> postgres, GenericContainer<?> redis) {
            if (!postgres.isRunning()) {
                postgres.start();
            }
            if (!redis.isRunning()) {
                redis.start();
            }
        }

        /**
         * Stop all containers after integration tests
         */
        public static void stopContainers(PostgreSQLContainer<?> postgres, GenericContainer<?> redis) {
            if (postgres.isRunning()) {
                postgres.stop();
            }
            if (redis.isRunning()) {
                redis.stop();
            }
        }

        /**
         * Reset container state for clean tests
         */
        public static void resetContainers(PostgreSQLContainer<?> postgres, GenericContainer<?> redis) {
            // Execute cleanup SQL or Redis commands
            try {
                // This could execute cleanup scripts if needed
                postgres.execInContainer("psql", "-U", postgres.getUsername(),
                        "-d", postgres.getDatabaseName(),
                        "-c", "TRUNCATE TABLE buddy_partnerships, buddy_requests, matching_preferences RESTART IDENTITY CASCADE;");
            } catch (Exception e) {
                // Log warning but don't fail tests
                System.err.println("Warning: Could not reset PostgreSQL container: " + e.getMessage());
            }

            try {
                // Clear Redis data
                redis.execInContainer("redis-cli", "-a", "test-password", "FLUSHDB");
            } catch (Exception e) {
                // Log warning but don't fail tests
                System.err.println("Warning: Could not reset Redis container: " + e.getMessage());
            }
        }

        /**
         * Get PostgreSQL JDBC URL with parameters
         */
        public static String getPostgresJdbcUrl(PostgreSQLContainer<?> postgres) {
            return postgres.getJdbcUrl() + "?stringtype=unspecified&ApplicationName=buddy-service-test";
        }

        /**
         * Check if containers are healthy
         */
        public static boolean areContainersHealthy(PostgreSQLContainer<?> postgres, GenericContainer<?> redis) {
            return postgres.isRunning() && postgres.isHealthy() &&
                   redis.isRunning() && redis.isHealthy();
        }
    }
}