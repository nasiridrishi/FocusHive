package com.focushive.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for environment configuration validation logic.
 * These tests create EnvironmentConfig instances manually to test validation
 * without the Spring context, allowing us to test validation failure scenarios.
 */
class EnvironmentConfigValidationTest {

    private EnvironmentConfig environmentConfig;

    @BeforeEach
    void setUp() {
        environmentConfig = new EnvironmentConfig();
    }

    @Test
    void shouldFailValidationForMismatchedDatabaseDriverAndUrl() {
        // Arrange - Set up configuration with mismatched driver and URL
        environmentConfig.setDatabaseUrl("jdbc:postgresql://localhost:5432/test_db");
        environmentConfig.setDatabaseDriver("org.h2.Driver"); // Mismatch!
        environmentConfig.setDatabaseUsername("test_user");
        environmentConfig.setDatabasePassword("test_password");
        environmentConfig.setRedisHost("localhost");
        environmentConfig.setRedisPort(6379);
        environmentConfig.setRedisPassword("redis_password");
        environmentConfig.setJwtSecret("valid_jwt_secret_that_meets_minimum_length_requirements_32_chars");
        environmentConfig.setServerPort(8080);
        environmentConfig.setIdentityServiceUrl("http://localhost:8081");
        environmentConfig.setIdentityServiceConnectTimeout(5000);
        environmentConfig.setIdentityServiceReadTimeout(10000);

        // Act & Assert
        assertThatThrownBy(() -> environmentConfig.validateEnvironment())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Environment validation failed")
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("DATABASE_URL must match DATABASE_DRIVER (PostgreSQL or H2)");
    }

    @Test
    void shouldFailValidationForShortJwtSecret() {
        // Arrange - Set up configuration with JWT secret that's too short
        environmentConfig.setDatabaseUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        environmentConfig.setDatabaseDriver("org.h2.Driver");
        environmentConfig.setDatabaseUsername("sa");
        environmentConfig.setDatabasePassword("test_password");
        environmentConfig.setRedisHost("localhost");
        environmentConfig.setRedisPort(6379);
        environmentConfig.setRedisPassword("redis_password");
        environmentConfig.setJwtSecret("short_key"); // Too short!
        environmentConfig.setServerPort(8080);
        environmentConfig.setIdentityServiceUrl("http://localhost:8081");
        environmentConfig.setIdentityServiceConnectTimeout(5000);
        environmentConfig.setIdentityServiceReadTimeout(10000);

        // Act & Assert
        assertThatThrownBy(() -> environmentConfig.validateEnvironment())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Environment validation failed")
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasRootCauseMessage("JWT_SECRET must be at least 32 characters long for security");
    }

    @Test
    void shouldPassValidationForH2Configuration() {
        // Arrange - Set up valid H2 configuration
        environmentConfig.setDatabaseUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        environmentConfig.setDatabaseDriver("org.h2.Driver");
        environmentConfig.setDatabaseUsername("sa");
        environmentConfig.setDatabasePassword("test_password");
        environmentConfig.setRedisHost("localhost");
        environmentConfig.setRedisPort(6379);
        environmentConfig.setRedisPassword("redis_password");
        environmentConfig.setJwtSecret("valid_jwt_secret_that_meets_minimum_32_character_requirement_for_security");
        environmentConfig.setServerPort(8080);
        environmentConfig.setIdentityServiceUrl("http://localhost:8081");
        environmentConfig.setIdentityServiceConnectTimeout(5000);
        environmentConfig.setIdentityServiceReadTimeout(10000);

        // Act & Assert - Should not throw any exception
        assertThatNoException().isThrownBy(() -> environmentConfig.validateEnvironment());
    }

    @Test
    void shouldPassValidationForPostgreSQLConfiguration() {
        // Arrange - Set up valid PostgreSQL configuration
        environmentConfig.setDatabaseUrl("jdbc:postgresql://localhost:5432/focushive_db");
        environmentConfig.setDatabaseDriver("org.postgresql.Driver");
        environmentConfig.setDatabaseUsername("focushive_user");
        environmentConfig.setDatabasePassword("secure_database_password");
        environmentConfig.setRedisHost("redis.example.com");
        environmentConfig.setRedisPort(6379);
        environmentConfig.setRedisPassword("redis_password");
        environmentConfig.setJwtSecret("secure_jwt_secret_that_meets_minimum_32_character_requirement_for_production");
        environmentConfig.setServerPort(8080);
        environmentConfig.setIdentityServiceUrl("https://identity.example.com");
        environmentConfig.setIdentityServiceConnectTimeout(5000);
        environmentConfig.setIdentityServiceReadTimeout(10000);

        // Act & Assert - Should not throw any exception
        assertThatNoException().isThrownBy(() -> environmentConfig.validateEnvironment());
    }
}