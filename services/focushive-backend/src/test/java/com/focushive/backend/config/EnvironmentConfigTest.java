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
 * Basic tests for FocusHive Backend Environment Configuration
 * More specific test scenarios are in separate test classes.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
@TestPropertySource(locations = "classpath:test-environment-config.properties")
class EnvironmentConfigTest {

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Test
    void shouldLoadValidConfigurationSuccessfully() {
        // This test uses the test configuration file
        // which provides all required environment variables

        // Act & Assert - No exception should be thrown during @PostConstruct
        assertThat(environmentConfig).isNotNull();

        // Verify database configuration
        assertThat(environmentConfig.getDatabaseUrl()).contains("h2:mem:testdb");
        assertThat(environmentConfig.getDatabaseUsername()).isEqualTo("sa");
        assertThat(environmentConfig.getDatabasePassword()).isEqualTo("test_h2_password_for_testing");
        assertThat(environmentConfig.getDatabaseDriver()).isEqualTo("org.h2.Driver");

        // Verify Redis configuration
        assertThat(environmentConfig.getRedisHost()).isEqualTo("localhost");
        assertThat(environmentConfig.getRedisPort()).isEqualTo(6379);
        assertThat(environmentConfig.getRedisPassword()).isEqualTo("test_redis_password_123");

        // Verify JWT configuration
        assertThat(environmentConfig.getJwtSecret())
            .hasSize(76)  // Length of our test JWT secret
            .contains("test_jwt_secret_key");
        assertThat(environmentConfig.getJwtExpiration()).isEqualTo(300000L);
        assertThat(environmentConfig.getJwtRefreshExpiration()).isEqualTo(86400000L);

        // Verify server configuration
        assertThat(environmentConfig.getServerPort()).isEqualTo(8080);

        // Verify identity service configuration
        assertThat(environmentConfig.getIdentityServiceUrl()).isEqualTo("http://localhost:8081");
        assertThat(environmentConfig.getIdentityServiceConnectTimeout()).isEqualTo(1000);
        assertThat(environmentConfig.getIdentityServiceReadTimeout()).isEqualTo(2000);
        assertThat(environmentConfig.getIdentityServiceApiKey()).isEqualTo("test_api_key_123");
    }
}