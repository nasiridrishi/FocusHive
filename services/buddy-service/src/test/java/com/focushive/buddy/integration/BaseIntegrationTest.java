package com.focushive.buddy.integration;

import com.focushive.buddy.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base integration test class providing common setup for all buddy service integration tests.
 * Uses TestContainers with PostgreSQL and Redis for realistic testing environment.
 * Follows TDD approach - write tests first, then implement functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"integration", "test"})
@Import(TestSecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("buddy_service_integration_test")
            .withUsername("integration_user")
            .withPassword("integration_password")
            .withInitScript("db/init/test-schema.sql")
            .withReuse(false)
            .withStartupTimeout(java.time.Duration.ofSeconds(60));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "test-password")
            .withReuse(false)
            .withStartupTimeout(java.time.Duration.ofSeconds(30));

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA/Hibernate configuration
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "test-password");
        registry.add("spring.data.redis.database", () -> "0");
        registry.add("spring.data.redis.timeout", () -> "2000ms");

        // Cache configuration
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.cache.redis.time-to-live", () -> "300000");

        // Flyway configuration
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.validate-on-migrate", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @BeforeEach
    void setUp() {
        // Verify containers are running
        assertTrue(postgresql.isRunning(), "PostgreSQL container should be running");
        assertTrue(redis.isRunning(), "Redis container should be running");

        cleanupTestData();
        setupTestData();
    }

    /**
     * Clean up test data before each test
     * This method will be implemented when repositories are created
     */
    protected void cleanupTestData() {
        try {
            // Clean up Redis cache
            redis.execInContainer("redis-cli", "-a", "test-password", "FLUSHDB");
        } catch (Exception e) {
            System.err.println("Warning: Could not clean Redis data: " + e.getMessage());
        }

        // Database cleanup will be implemented when repositories are available
        // For now, rely on @DirtiesContext to recreate application context
    }

    /**
     * Setup basic test data for all tests
     * This method will be implemented when entities are created
     */
    protected void setupTestData() {
        // Will be implemented when entities and repositories are available
        // Currently no-op to allow tests to run
    }

    // Entity creation methods will be implemented when entities and repositories are created
    // These are placeholder methods for the TDD infrastructure

    /**
     * Helper method to create test user IDs
     */
    protected String createTestUserId() {
        return "test-user-" + System.currentTimeMillis() + "-" + System.nanoTime();
    }

    /**
     * Helper method to create full URL for API endpoints
     */
    protected String createUrl(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * Helper method to create API v1 URL
     */
    protected String createApiV1Url(String path) {
        return createUrl("/api/v1" + (path.startsWith("/") ? path : "/" + path));
    }

    /**
     * Waits for asynchronous operations to complete
     */
    protected void waitForAsyncOperations() {
        try {
            Thread.sleep(100); // Small delay for async operations
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits for scheduled operations (like reminders) to complete
     */
    protected void waitForScheduledOperations() {
        try {
            Thread.sleep(500); // Longer delay for scheduled operations
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Partnership helper methods will be implemented when entities are available

    /**
     * Creates a realistic compatibility score for testing
     */
    protected double calculateTestCompatibilityScore(String interests1, String interests2) {
        // Simple overlap calculation for testing
        String[] int1 = interests1.split(",");
        String[] int2 = interests2.split(",");

        int overlap = 0;
        for (String i1 : int1) {
            for (String i2 : int2) {
                if (i1.trim().equals(i2.trim())) {
                    overlap++;
                    break;
                }
            }
        }

        return Math.min(1.0, overlap / (double) Math.max(int1.length, int2.length));
    }

    /**
     * Verify that containers are running
     */
    protected void verifyContainersHealth() {
        assertTrue(postgresql.isRunning(), "PostgreSQL container should be running");
        assertTrue(redis.isRunning(), "Redis container should be running");
    }

    /**
     * Get PostgreSQL container JDBC URL with additional parameters
     */
    protected String getPostgresqlJdbcUrl() {
        return postgresql.getJdbcUrl() + "?stringtype=unspecified&ApplicationName=buddy-service-integration-test";
    }

    /**
     * Execute SQL commands in PostgreSQL container
     */
    protected void executePostgresqlCommand(String sql) throws Exception {
        postgresql.execInContainer("psql", "-U", postgresql.getUsername(),
                "-d", postgresql.getDatabaseName(), "-c", sql);
    }

    /**
     * Execute Redis commands in Redis container
     */
    protected void executeRedisCommand(String... command) throws Exception {
        String[] fullCommand = new String[command.length + 2];
        fullCommand[0] = "redis-cli";
        fullCommand[1] = "-a";
        fullCommand[2] = "test-password";
        System.arraycopy(command, 0, fullCommand, 3, command.length);
        redis.execInContainer(fullCommand);
    }

    /**
     * Reset all test data - useful for cleanup between tests
     */
    protected void resetAllTestData() {
        try {
            // Clear Redis
            executeRedisCommand("FLUSHDB");
        } catch (Exception e) {
            System.err.println("Warning: Could not clear Redis: " + e.getMessage());
        }

        // Database reset will be implemented when repositories are available
    }
}