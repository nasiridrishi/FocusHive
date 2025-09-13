package com.focushive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.redis.testcontainers.RedisContainer;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for cross-service integration tests.
 * 
 * Provides:
 * - Multiple service instances via TestContainers
 * - WireMock for external service simulation
 * - REST Assured configuration for API testing
 * - Retry logic for eventual consistency testing
 * - Test data factories and utilities
 * - Performance timing and assertions
 * 
 * Following TDD approach:
 * 1. Write failing test
 * 2. Implement minimal functionality
 * 3. Verify test passes
 * 4. Refactor and optimize
 */
@Testcontainers
@ActiveProfiles("integration-test")
@ContextConfiguration(initializers = AbstractCrossServiceIntegrationTest.Initializer.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractCrossServiceIntegrationTest {

    // Shared network for all containers
    protected static final Network SHARED_NETWORK = Network.newNetwork();

    // Database containers
    protected static PostgreSQLContainer<?> postgresql;
    protected static RedisContainer redis;

    // WireMock servers for external services
    protected static Map<String, WireMockServer> wireMockServers = new HashMap<>();

    // Service ports mapping
    protected static final Map<String, Integer> SERVICE_PORTS = Map.of(
        "focushive-backend", 8080,
        "identity-service", 8081,
        "music-service", 8082,
        "notification-service", 8083,
        "chat-service", 8084,
        "analytics-service", 8085,
        "forum-service", 8086,
        "buddy-service", 8087
    );

    // Jackson ObjectMapper for JSON processing
    protected static ObjectMapper objectMapper;

    // Test configuration
    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    protected static final Duration EVENTUAL_CONSISTENCY_TIMEOUT = Duration.ofSeconds(10);
    protected static final int MAX_RETRY_ATTEMPTS = 5;

    @BeforeAll
    static void setUpInfrastructure() {
        // Initialize ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Start infrastructure containers
        startDatabaseContainers();
        startWireMockServers();
        configureRestAssured();
        configureAwaitility();
    }

    @AfterAll
    static void tearDownInfrastructure() {
        // Stop WireMock servers
        wireMockServers.values().forEach(WireMockServer::stop);
        
        // Stop containers
        if (redis != null && redis.isRunning()) {
            redis.stop();
        }
        if (postgresql != null && postgresql.isRunning()) {
            postgresql.stop();
        }
        
        // Close network
        SHARED_NETWORK.close();
    }

    @BeforeEach
    void setUp() {
        // Reset WireMock servers for clean test state
        wireMockServers.values().forEach(WireMock::reset);
    }

    @AfterEach
    void tearDown() {
        // Clean up any test-specific state
        // This will be implemented by subclasses as needed
    }

    /**
     * Start database containers with shared network
     */
    private static void startDatabaseContainers() {
        postgresql = new PostgreSQLContainer<>("postgres:16-alpine")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("postgresql")
            .withDatabaseName("focushive_integration_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);
        postgresql.start();

        redis = new RedisContainer("redis:7-alpine")
            .withNetwork(SHARED_NETWORK)
            .withNetworkAliases("redis")
            .withReuse(false);
        redis.start();
    }

    /**
     * Start WireMock servers for external service simulation
     */
    private static void startWireMockServers() {
        // Spotify API mock
        WireMockServer spotifyMock = new WireMockServer(
            WireMockConfiguration.options()
                .port(0) // Random port
                .usingFilesUnderDirectory("src/test/resources/wiremock/spotify")
        );
        spotifyMock.start();
        wireMockServers.put("spotify", spotifyMock);

        // Email service mock
        WireMockServer emailMock = new WireMockServer(
            WireMockConfiguration.options()
                .port(0) // Random port
                .usingFilesUnderDirectory("src/test/resources/wiremock/email")
        );
        emailMock.start();
        wireMockServers.put("email", emailMock);

        // Push notification service mock
        WireMockServer pushMock = new WireMockServer(
            WireMockConfiguration.options()
                .port(0) // Random port
                .usingFilesUnderDirectory("src/test/resources/wiremock/push")
        );
        pushMock.start();
        wireMockServers.put("push", pushMock);
    }

    /**
     * Configure REST Assured for API testing
     */
    private static void configureRestAssured() {
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory((type, s) -> objectMapper)
            );
        
        // Default timeouts
        RestAssured.config = RestAssured.config()
            .connectionConfig(
                RestAssured.config().getConnectionConfig()
                    .closeIdleConnectionsAfterEachResponse()
            );
    }

    /**
     * Configure Awaitility for async testing
     */
    private static void configureAwaitility() {
        Awaitility.setDefaultTimeout(DEFAULT_TIMEOUT);
        Awaitility.setDefaultPollInterval(Duration.ofMillis(500));
        Awaitility.setDefaultPollDelay(Duration.ofMillis(100));
    }

    /**
     * Spring application context initializer
     */
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                // Database configuration
                "spring.datasource.url=" + postgresql.getJdbcUrl(),
                "spring.datasource.username=" + postgresql.getUsername(),
                "spring.datasource.password=" + postgresql.getPassword(),
                "spring.datasource.driver-class-name=" + postgresql.getDriverClassName(),
                
                // Redis configuration
                "spring.redis.host=" + redis.getHost(),
                "spring.redis.port=" + redis.getFirstMappedPort(),
                
                // Service URLs for cross-service communication
                "services.identity-service.url=http://localhost:" + SERVICE_PORTS.get("identity-service"),
                "services.analytics-service.url=http://localhost:" + SERVICE_PORTS.get("analytics-service"),
                "services.notification-service.url=http://localhost:" + SERVICE_PORTS.get("notification-service"),
                "services.buddy-service.url=http://localhost:" + SERVICE_PORTS.get("buddy-service"),
                "services.chat-service.url=http://localhost:" + SERVICE_PORTS.get("chat-service"),
                "services.music-service.url=http://localhost:" + SERVICE_PORTS.get("music-service"),
                
                // External service mocks
                "external.spotify.api.url=http://localhost:" + wireMockServers.get("spotify").port(),
                "external.email.service.url=http://localhost:" + wireMockServers.get("email").port(),
                "external.push.service.url=http://localhost:" + wireMockServers.get("push").port(),
                
                // Test profile settings
                "spring.profiles.active=integration-test",
                "logging.level.com.focushive=DEBUG",
                "logging.level.org.springframework.web=DEBUG",
                
                // Performance settings for testing
                "spring.jpa.properties.hibernate.jdbc.batch_size=20",
                "spring.jpa.properties.hibernate.order_inserts=true",
                "spring.jpa.properties.hibernate.order_updates=true",
                "spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true"
                
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Test utilities for cross-service testing
     */
    protected static class CrossServiceTestUtils {
        
        /**
         * Wait for eventual consistency across services
         */
        public static void waitForEventualConsistency() {
            try {
                Thread.sleep(1000); // Give services time to process async events
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Retry operation with exponential backoff
         */
        public static <T> T retryWithBackoff(ThrowingSupplier<T> operation, int maxAttempts) {
            Exception lastException = null;
            
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return operation.get();
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < maxAttempts) {
                        try {
                            Thread.sleep(1000 * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            throw new RuntimeException("Operation failed after " + maxAttempts + " attempts", lastException);
        }

        /**
         * Measure operation execution time
         */
        public static <T> TimedResult<T> measureExecutionTime(ThrowingSupplier<T> operation) {
            long startTime = System.nanoTime();
            try {
                T result = operation.get();
                long duration = System.nanoTime() - startTime;
                return new TimedResult<>(result, Duration.ofNanos(duration));
            } catch (Exception e) {
                throw new RuntimeException("Operation failed", e);
            }
        }

        /**
         * Verify response time is within acceptable limits
         */
        public static void assertResponseTime(Duration actualTime, Duration maxTime) {
            if (actualTime.compareTo(maxTime) > 0) {
                throw new AssertionError(
                    String.format("Response time %dms exceeded maximum %dms", 
                        actualTime.toMillis(), maxTime.toMillis())
                );
            }
        }
    }

    /**
     * Functional interface for operations that can throw exceptions
     */
    @FunctionalInterface
    protected interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Result wrapper that includes timing information
     */
    protected static class TimedResult<T> {
        private final T result;
        private final Duration duration;

        public TimedResult(T result, Duration duration) {
            this.result = result;
            this.duration = duration;
        }

        public T getResult() { return result; }
        public Duration getDuration() { return duration; }
    }
}