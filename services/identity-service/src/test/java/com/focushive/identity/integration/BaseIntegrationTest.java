package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * Provides TestContainers setup for PostgreSQL and Redis, common configuration, and utilities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@ContextConfiguration(initializers = BaseIntegrationTest.Initializer.class)
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:15.7-alpine")
            .withDatabaseName("identity_service_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container 
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeAll
    static void configureProperties() {
        postgresql.start();
        redis.start();
    }

    /**
     * Spring application context initializer to configure test properties from containers.
     */
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgresql.getJdbcUrl(),
                "spring.datasource.username=" + postgresql.getUsername(),
                "spring.datasource.password=" + postgresql.getPassword(),
                "spring.datasource.driver-class-name=" + postgresql.getDriverClassName(),
                "spring.redis.host=" + redis.getHost(),
                "spring.redis.port=" + redis.getMappedPort(6379),
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Common utilities for integration tests.
     */
    protected static class TestUtils {
        public static final String TEST_CLIENT_ID = "test-client";
        public static final String TEST_CLIENT_SECRET = "test-secret";
        public static final String TEST_USER_EMAIL = "test@example.com";
        public static final String TEST_USER_PASSWORD = "testpassword";
        
        /**
         * Create Basic Auth header for OAuth2 client credentials.
         */
        public static String createBasicAuthHeader(String clientId, String clientSecret) {
            String credentials = clientId + ":" + clientSecret;
            return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
        }
    }
}