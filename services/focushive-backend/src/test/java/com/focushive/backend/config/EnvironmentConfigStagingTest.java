package com.focushive.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Test for Staging Profile Environment Configuration
 *
 * This test will FAIL initially because:
 * 1. Staging profile configuration is newly created
 * 2. Profile-specific properties need to be properly configured
 * 3. Environment validation needs to work with staging profile
 *
 * Following TDD principles - implement after test fails
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
@ActiveProfiles("staging")
@TestPropertySource(properties = {
    // Staging-specific required environment variables
    "STAGING_DB_PASSWORD=staging_test_password",
    "STAGING_REDIS_PASSWORD=staging_redis_password",
    "STAGING_JWT_SECRET=staging_jwt_secret_32_characters_minimum_length_for_security"
})
class EnvironmentConfigStagingTest {

    @Autowired
    private Environment environment;

    @Autowired
    private EnvironmentConfig environmentConfig;

    /**
     * TEST THAT SHOULD FAIL INITIALLY: Staging Profile Loading
     *
     * This test verifies that the staging profile loads correctly
     * with staging-specific configurations.
     */
    @Test
    void shouldLoadStagingProfileSuccessfully() {
        // This test WILL FAIL initially because staging profile was just created

        // Verify profile is active
        String[] activeProfiles = environment.getActiveProfiles();
        assertThat(activeProfiles).contains("staging");

        // Verify staging-specific database configuration is loaded
        // These assertions will FAIL until the staging profile is properly integrated
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
            .isEqualTo("org.postgresql.Driver");

        // Staging should use PostgreSQL URL pattern with staging variables
        assertThat(environment.getProperty("spring.datasource.url"))
            .contains("jdbc:postgresql://")
            .contains("focushive_staging");

        // Verify staging logging configuration
        assertThat(environment.getProperty("logging.level.com.focushive"))
            .isEqualTo("INFO");  // Staging uses INFO level

        // Verify staging shows all health details for testing
        assertThat(environment.getProperty("management.endpoint.health.show-details"))
            .isEqualTo("always");

        // Verify staging feature flags are enabled for testing
        assertThat(environment.getProperty("app.features.forum.enabled"))
            .isEqualTo("true");
        assertThat(environment.getProperty("app.features.analytics.enabled"))
            .isEqualTo("true");
    }

    /**
     * TEST THAT SHOULD FAIL INITIALLY: Staging Rate Limiting Configuration
     */
    @Test
    void shouldConfigureStagingRateLimiting() {
        // Staging should have more lenient rate limiting than production
        assertThat(environment.getProperty("focushive.rate-limit.enabled"))
            .isEqualTo("true");

        // More lenient limits for testing
        assertThat(environment.getProperty("focushive.rate-limit.public-api-requests-per-hour"))
            .isEqualTo("300");
        assertThat(environment.getProperty("focushive.rate-limit.authenticated-api-requests-per-hour"))
            .isEqualTo("3000");
    }

    /**
     * TEST THAT SHOULD FAIL INITIALLY: Staging JWT Configuration
     */
    @Test
    void shouldConfigureStagingJwtWithShorterExpiration() {
        // Staging should have shorter JWT expiration for testing
        assertThat(environment.getProperty("spring.security.jwt.expiration"))
            .isEqualTo("3600000");  // 1 hour instead of 24 hours
    }
}