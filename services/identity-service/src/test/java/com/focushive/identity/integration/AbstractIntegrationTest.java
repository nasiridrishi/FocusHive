package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for integration tests that require full schema migration support.
 * This class enables Flyway migrations and provides comprehensive database setup.
 * 
 * Use this when you need:
 * - Full schema with all constraints and indexes
 * - Flyway migration testing
 * - Production-like database structure
 * 
 * Use BaseIntegrationTest when you need:
 * - Fast test execution with Hibernate DDL
 * - Simple entity testing
 * - Isolated unit-like integration tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("flyway-integration-test")
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
@Testcontainers
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("identity_service_flyway_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false); // Disable reuse for full migration testing

    @Container 
    static RedisContainer redis = new RedisContainer("redis:7-alpine")
            .withReuse(false); // Disable reuse for full isolation

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
     * This configuration enables Flyway migrations for full database testing.
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
                "spring.redis.port=" + redis.getFirstMappedPort(),
                
                // Enable Flyway for full schema migration
                "spring.flyway.enabled=true",
                "spring.flyway.clean-disabled=false",
                "spring.flyway.baseline-on-migrate=true",
                
                // Disable Hibernate DDL to let Flyway handle schema
                "spring.jpa.hibernate.ddl-auto=validate",
                
                // Configure JPA for migration testing
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
                "spring.jpa.properties.hibernate.format_sql=false",
                "spring.jpa.show-sql=false"
            ).applyTo(context.getEnvironment());
        }
    }

    /**
     * Utilities specific to Flyway-based integration tests
     */
    protected static class FlywayTestUtils {
        public static final String TEST_CLIENT_ID = "flyway-test-client";
        public static final String TEST_CLIENT_SECRET = "flyway-test-secret";
        public static final String TEST_USER_EMAIL = "flyway@example.com";
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