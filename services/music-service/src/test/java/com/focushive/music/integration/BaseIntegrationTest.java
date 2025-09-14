package com.focushive.music.integration;

import com.focushive.music.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * Base class for integration tests using H2 in-memory database.
 * Follows TDD approach - write tests first, then implement functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
@Import(TestSecurityConfig.class)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @BeforeEach
    void baseSetup() {
        // Clear Redis cache before each test
        // This will be implemented when Redis is integrated
    }

    private static int userIdCounter = 0;

    /**
     * Helper method to create test user ID for music service tests
     */
    protected String createTestUserId() {
        return "test-user-" + System.currentTimeMillis() + "-" + (++userIdCounter);
    }

    /**
     * Helper method to create test Spotify user ID
     */
    protected String createTestSpotifyUserId() {
        return "spotify-user-" + System.currentTimeMillis();
    }

    /**
     * Helper method to create test hive ID for collaborative features
     */
    protected String createTestHiveId() {
        return "test-hive-" + System.currentTimeMillis();
    }

    /**
     * Helper method to create full URL for API endpoints
     */
    protected String createUrl(String path) {
        return "http://localhost:" + port + path;
    }
}