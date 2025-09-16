package com.focushive.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for Configuration Hierarchy and Environment Variable Fallback
 *
 * These tests validate that configuration values are loaded in the correct
 * precedence order and that fallback mechanisms work properly.
 *
 * Following TDD principles - tests that should initially FAIL
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
    // Minimal required configuration for test
    "DATABASE_PASSWORD=hierarchy_test_password",
    "REDIS_PASSWORD=hierarchy_redis_password",
    "JWT_SECRET=hierarchy_jwt_secret_32_characters_minimum_length_for_security"
})
class EnvironmentConfigProfileTest {

    @Autowired
    private Environment environment;

    @Autowired
    private EnvironmentConfig environmentConfig;

    /**
     * TEST THAT SHOULD PASS: Configuration Hierarchy and Fallback
     *
     * This test verifies that configuration values are loaded correctly
     * with proper fallback to defaults.
     */
    @Test
    void shouldFallbackToDefaultsGracefully() {
        // Verify that EnvironmentConfig loaded successfully
        assertThat(environmentConfig).isNotNull();

        // Should fall back to H2 defaults
        assertThat(environment.getProperty("spring.datasource.url")).contains("h2:mem:testdb");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("sa");

        // Should fall back to localhost Redis
        assertThat(environment.getProperty("spring.redis.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("spring.redis.port")).isEqualTo("6379");

        // Should use test properties for required values
        assertThat(environmentConfig.getDatabasePassword()).isEqualTo("hierarchy_test_password");
        assertThat(environmentConfig.getRedisPassword()).isEqualTo("hierarchy_redis_password");

        // Should fall back to default server port
        assertThat(environment.getProperty("server.port")).isEqualTo("8080");
    }

    /**
     * TEST THAT DOCUMENTS FUTURE REQUIREMENTS
     *
     * This test documents what needs to be implemented for Task 1.2
     * Profile-specific tests are in separate test classes due to Spring limitations
     */
    @Test
    void shouldDocumentProfileRequirements() {
        // NOTE: Profile-specific configuration tests are implemented in:
        // - EnvironmentConfigDefaultTest (default profile)
        // - EnvironmentConfigH2Test (H2 specific)
        // - EnvironmentConfigPostgreSQLTest (PostgreSQL specific)
        // - EnvironmentConfigStagingTest (staging profile) - NEW

        // This test documents the requirement for profile-aware configuration
        // The actual implementation should be verified through the profile-specific test classes
        assertThat(true).isTrue(); // Placeholder assertion
    }
}