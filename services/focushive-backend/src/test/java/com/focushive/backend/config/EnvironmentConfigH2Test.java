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
 * Tests for H2 development environment configuration
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
    "DATABASE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "DATABASE_USERNAME=sa",
    "DATABASE_PASSWORD=test_password",
    "DATABASE_DRIVER=org.h2.Driver",
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD=test_redis_password",
    "JWT_SECRET=test_jwt_secret_that_meets_minimum_length_requirements_for_testing",
    "JWT_EXPIRATION=300000",
    "JWT_REFRESH_EXPIRATION=86400000",
    "SERVER_PORT=8080",
    "IDENTITY_SERVICE_URL=http://localhost:8081",
    "IDENTITY_SERVICE_CONNECT_TIMEOUT=1000",
    "IDENTITY_SERVICE_READ_TIMEOUT=1000"
})
class EnvironmentConfigH2Test {

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Test
    void shouldValidateDevelopmentH2Configuration() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getDatabaseUrl()).contains("h2:mem");
        assertThat(environmentConfig.getDatabaseDriver()).isEqualTo("org.h2.Driver");
        assertThat(environmentConfig.getDatabaseUsername()).isEqualTo("sa");
        assertThat(environmentConfig.getIdentityServiceConnectTimeout()).isEqualTo(1000);
        assertThat(environmentConfig.getIdentityServiceReadTimeout()).isEqualTo(1000);
    }
}