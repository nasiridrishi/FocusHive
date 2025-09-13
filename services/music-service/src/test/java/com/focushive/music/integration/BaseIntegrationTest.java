package com.focushive.music.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests providing TestContainers setup for PostgreSQL and Redis.
 * Follows TDD approach - write tests first, then implement functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = BaseIntegrationTest.Initializer.class)
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("music_service_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                "spring.redis.host=" + redis.getHost(),
                "spring.redis.port=" + redis.getMappedPort(6379),
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "logging.level.org.springframework.web=DEBUG",
                "logging.level.com.focushive=DEBUG"
            ).applyTo(context.getEnvironment());
        }
    }

    @BeforeEach
    void baseSetup() {
        // Clear Redis cache before each test
        // This will be implemented when Redis is integrated
    }

    /**
     * Helper method to create test user ID for music service tests
     */
    protected String createTestUserId() {
        return "test-user-" + System.currentTimeMillis();
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
}