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

/**
 * Base integration test class providing common setup for all buddy service integration tests.
 * Uses TestContainers with PostgreSQL and Redis for realistic testing environment.
 * Follows TDD approach - write tests first, then implement functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("buddy_service_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresql::getJdbcUrl);
        registry.add("spring.datasource.username", postgresql::getUsername);
        registry.add("spring.datasource.password", postgresql::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() {
        cleanupTestData();
        setupTestData();
    }

    /**
     * Clean up test data before each test
     * This method will be implemented when repositories are created
     */
    protected void cleanupTestData() {
        // Will be implemented when repositories are available
        // Currently no-op to allow tests to run
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
}