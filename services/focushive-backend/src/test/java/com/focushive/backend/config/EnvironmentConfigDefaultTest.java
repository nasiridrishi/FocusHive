package com.focushive.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for default environment configuration with minimal required values
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
    // Minimal required properties to test defaults
    "DATABASE_PASSWORD=default_test_password",
    "REDIS_PASSWORD=default_redis_password",
    "JWT_SECRET=default_jwt_secret_key_32_characters_minimum_length_for_security"
})
class EnvironmentConfigDefaultTest {

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Test
    void shouldLoadValidConfigurationWithDefaults() {
        // This test verifies that the component can load with minimal configuration
        // and that default values are properly applied for optional properties

        // Act & Assert - No exception should be thrown during @PostConstruct
        assertThat(environmentConfig).isNotNull();

        // Verify default database configuration is used
        assertThat(environmentConfig.getDatabaseUrl())
            .isEqualTo("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        assertThat(environmentConfig.getDatabaseUsername()).isEqualTo("sa");
        assertThat(environmentConfig.getDatabasePassword()).isEqualTo("default_test_password");
        assertThat(environmentConfig.getDatabaseDriver()).isEqualTo("org.h2.Driver");

        // Verify default Redis configuration is used
        assertThat(environmentConfig.getRedisHost()).isEqualTo("localhost");
        assertThat(environmentConfig.getRedisPort()).isEqualTo(6379);
        assertThat(environmentConfig.getRedisPassword()).isEqualTo("default_redis_password");

        // Verify JWT configuration
        assertThat(environmentConfig.getJwtSecret()).hasSize(64);
        assertThat(environmentConfig.getJwtExpiration()).isEqualTo(86400000L); // Default 1 day
        assertThat(environmentConfig.getJwtRefreshExpiration()).isEqualTo(604800000L); // Default 7 days

        // Verify default server configuration
        assertThat(environmentConfig.getServerPort()).isEqualTo(8080);

        // Verify default identity service configuration
        assertThat(environmentConfig.getIdentityServiceUrl()).isEqualTo("http://localhost:8081");
        assertThat(environmentConfig.getIdentityServiceConnectTimeout()).isEqualTo(5000);
        assertThat(environmentConfig.getIdentityServiceReadTimeout()).isEqualTo(10000);

        // Optional API key should be empty by default
        assertThat(environmentConfig.getIdentityServiceApiKey()).isEmpty();
    }
}