package com.focushive.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Step 1: Configuration validation tests (SHOULD FAIL INITIALLY)
 * These tests validate that our test configuration is properly set up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.web-application-type=none"
})
class TestConfigurationValidationTest {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Should load test application properties correctly")
    void shouldLoadTestApplicationProperties() {
        // This test should FAIL initially - we need proper test property configuration

        // Verify we're running in test profile
        assertThat(environment.getActiveProfiles()).contains("test");

        // Verify test database configuration (H2 in-memory)
        assertThat(environment.getProperty("spring.datasource.url"))
                .isNotNull()
                .contains("h2:mem:");

        // Verify test-specific properties are loaded
        assertThat(environment.getProperty("app.test.data.cleanup")).isEqualTo("true");
        assertThat(environment.getProperty("app.test.data.mock-external-services")).isEqualTo("true");

        // Verify server port is randomized for parallel testing
        assertThat(environment.getProperty("server.port")).isEqualTo("0");
    }

    @Test
    @DisplayName("Should use mock beans in tests")
    void shouldUseMockBeansInTests() {
        // This test should FAIL initially - we need mock bean configuration

        // Verify Redis is disabled (should not have Redis beans)
        assertThat(environment.getProperty("spring.cache.type")).isEqualTo("simple");

        // Verify external service mocking is enabled
        assertThat(environment.getProperty("identity.service.mock.enabled")).isEqualTo("true");

        // Verify WebSocket is disabled for tests
        assertThat(environment.getProperty("app.features.websocket.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should isolate test contexts properly")
    void shouldIsolateTestContexts() {
        // This test should FAIL initially - we need proper context isolation

        // Verify JPA is configured for test isolation
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");

        // Verify lazy initialization is enabled for faster tests
        assertThat(environment.getProperty("spring.main.lazy-initialization")).isEqualTo("true");

        // Verify bean overriding is allowed for mocking
        assertThat(environment.getProperty("spring.main.allow-bean-definition-overriding")).isEqualTo("true");
    }

    @Test
    @DisplayName("Should disable production features in tests")
    void shouldDisableProductionFeaturesInTests() {
        // This test should FAIL initially - we need feature flag configuration

        // Verify Flyway is disabled (using JPA schema generation instead)
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");

        // Verify circuit breaker is disabled
        assertThat(environment.getProperty("resilience4j.circuitbreaker.instances.identity-service.enabled"))
                .isEqualTo("false");

        // Verify metrics/tracing is disabled
        assertThat(environment.getProperty("management.tracing.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("Should have test-specific security configuration")
    void shouldHaveTestSpecificSecurityConfiguration() {
        // This test should FAIL initially - we need test security config

        // Verify JWT test configuration
        assertThat(environment.getProperty("spring.security.jwt.secret"))
                .isNotNull()
                .contains("test-jwt-secret");

        // Verify short expiration times for tests
        assertThat(environment.getProperty("spring.security.jwt.expiration")).isEqualTo("3600000");
    }
}