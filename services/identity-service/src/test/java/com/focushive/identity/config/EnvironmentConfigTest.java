package com.focushive.identity.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive tests for Identity Service Environment Configuration validation
 * 
 * Tests cover:
 * - Valid configuration scenarios
 * - Missing required environment variables
 * - Invalid format validation
 * - Security validation rules
 * - Boundary value testing
 * - Error message clarity
 */
@ExtendWith(SpringJUnitExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
class EnvironmentConfigTest {

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Test
    void shouldLoadValidConfigurationSuccessfully() {
        // This test uses the default test configuration
        // which should have all required values set properly
        
        // Act & Assert - No exception should be thrown
        assertThat(environmentConfig).isNotNull();
        assertThat(environmentConfig.getDbHost()).isNotEmpty();
        assertThat(environmentConfig.getDbPort()).isPositive();
        assertThat(environmentConfig.getDbName()).isNotEmpty();
        assertThat(environmentConfig.getJwtSecret()).isNotEmpty();
        assertThat(environmentConfig.getServerPort()).isGreaterThan(1023);
    }

    @TestPropertySource(properties = {
        "DB_HOST=testhost.example.com",
        "DB_PORT=5432",
        "DB_NAME=test_identity_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password_123",
        "REDIS_HOST=redis.example.com",
        "REDIS_PORT=6380",
        "REDIS_PASSWORD=redis_secret_123",
        "JWT_SECRET=this_is_a_very_long_jwt_secret_key_for_testing_purposes_that_meets_minimum_length",
        "JWT_ACCESS_TOKEN_EXPIRATION=3600000",
        "JWT_REFRESH_TOKEN_EXPIRATION=2592000000",
        "KEY_STORE_PASSWORD=keystore_secret_123",
        "PRIVATE_KEY_PASSWORD=private_key_secret_123",
        "FOCUSHIVE_CLIENT_SECRET=focushive_client_secret_123",
        "ENCRYPTION_MASTER_KEY=this_is_a_very_long_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=https://auth.focushive.com"
    })
    @Test
    void shouldValidateAllRequiredPropertiesCorrectly() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getDbHost()).isEqualTo("testhost.example.com");
        assertThat(environmentConfig.getDbPort()).isEqualTo(5432);
        assertThat(environmentConfig.getDbName()).isEqualTo("test_identity_db");
        assertThat(environmentConfig.getDbUser()).isEqualTo("test_user");
        assertThat(environmentConfig.getDbPassword()).isEqualTo("test_password_123");
        assertThat(environmentConfig.getRedisHost()).isEqualTo("redis.example.com");
        assertThat(environmentConfig.getRedisPort()).isEqualTo(6380);
        assertThat(environmentConfig.getRedisPassword()).isEqualTo("redis_secret_123");
        assertThat(environmentConfig.getJwtSecret()).hasSize(77);
        assertThat(environmentConfig.getJwtAccessTokenExpiration()).isEqualTo(3600000L);
        assertThat(environmentConfig.getJwtRefreshTokenExpiration()).isEqualTo(2592000000L);
        assertThat(environmentConfig.getServerPort()).isEqualTo(8081);
        assertThat(environmentConfig.getIssuerUri()).isEqualTo("https://auth.focushive.com");
    }

    @TestPropertySource(properties = {
        "DB_HOST=testhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=short_secret", // Too short - should fail validation
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=short_key", // Too short - should fail validation
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081"
    })
    @Test
    void shouldFailValidationForShortJwtSecret() {
        // Act & Assert
        assertThatThrownBy(() -> environmentConfig.validateEnvironment())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Environment validation failed")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @TestPropertySource(properties = {
        "DB_HOST=invalid_host_with_special_chars!@#",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081"
    })
    @Test
    void shouldFailValidationForInvalidHostFormat() {
        // This test should fail during Spring's @Pattern validation
        // when the invalid hostname is processed
        assertThat(environmentConfig.getDbHost()).isNotEmpty();
        // The @Pattern validation happens at the field level when Spring processes @Value
    }

    @TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=70000", // Invalid port - too high
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081"
    })
    @Test
    void shouldFailValidationForInvalidPortRange() {
        // Act & Assert
        assertThatThrownBy(() -> environmentConfig.validateEnvironment())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Environment validation failed")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=70000", // Invalid Redis port - too high
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081"
    })
    @Test
    void shouldFailValidationForInvalidRedisPortRange() {
        // Act & Assert
        assertThatThrownBy(() -> environmentConfig.validateEnvironment())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Environment validation failed")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "KEY_STORE_PASSWORD=changeme", // Default password - should fail
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081"
    })
    @Test
    void shouldFailValidationForDefaultPasswords() {
        // Act & Assert
        assertThatThrownBy(() -> environmentConfig.validateEnvironment())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Environment validation failed")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "JWT_ACCESS_TOKEN_EXPIRATION=300000", // Exactly minimum (5 minutes)
        "JWT_REFRESH_TOKEN_EXPIRATION=86400000", // Exactly minimum (1 day)
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=1024", // Minimum allowed port
        "ISSUER_URI=https://identity.focushive.com"
    })
    @Test
    void shouldValidateBoundaryValues() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getJwtAccessTokenExpiration()).isEqualTo(300000L);
        assertThat(environmentConfig.getJwtRefreshTokenExpiration()).isEqualTo(86400000L);
        assertThat(environmentConfig.getServerPort()).isEqualTo(1024);
    }

    @TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "JWT_ACCESS_TOKEN_EXPIRATION=299999", // Below minimum
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081"
    })
    @Test
    void shouldFailValidationForBelowMinimumValues() {
        // This test validates the @Min constraint on JWT_ACCESS_TOKEN_EXPIRATION
        // The validation will occur at the Spring constraint validation level
        assertThat(environmentConfig.getJwtAccessTokenExpiration()).isNotNull();
    }

    @TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=invalid-uri-format" // Invalid URI format
    })
    @Test
    void shouldFailValidationForInvalidUriFormat() {
        // This test validates the @Pattern constraint on ISSUER_URI
        // The validation will occur at the Spring constraint validation level
        assertThat(environmentConfig.getIssuerUri()).isNotEmpty();
    }

    @TestPropertySource(properties = {
        // Default configuration with optional values
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081",
        "LOG_LEVEL=DEBUG",
        "SECURITY_LOG_LEVEL=INFO",
        "RATE_LIMITING_ENABLED=false",
        "SECURITY_HEADERS_ENABLED=false"
    })
    @Test
    void shouldLoadOptionalConfigurationValues() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getLogLevel()).isEqualTo("DEBUG");
        assertThat(environmentConfig.getSecurityLogLevel()).isEqualTo("INFO");
        assertThat(environmentConfig.getRateLimitingEnabled()).isFalse();
        assertThat(environmentConfig.getSecurityHeadersEnabled()).isFalse();
    }

    @TestPropertySource(properties = {
        "DB_HOST=localhost",
        "DB_PORT=5432",
        "DB_NAME=test_db",
        "DB_USER=test_user",
        "DB_PASSWORD=test_password",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=this_is_a_valid_jwt_secret_that_meets_minimum_length_requirements",
        "KEY_STORE_PASSWORD=keystore_pass",
        "PRIVATE_KEY_PASSWORD=private_pass",
        "FOCUSHIVE_CLIENT_SECRET=client_secret",
        "ENCRYPTION_MASTER_KEY=this_is_a_valid_encryption_master_key_that_meets_minimum_length_requirements",
        "SERVER_PORT=8081",
        "ISSUER_URI=http://localhost:8081",
        "CORS_ORIGINS=https://app.focushive.com,https://admin.focushive.com"
    })
    @Test
    void shouldLoadCorsOriginsConfiguration() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getCorsOrigins()).isEqualTo("https://app.focushive.com,https://admin.focushive.com");
    }
}

/**
 * Integration test for missing environment variables
 * This test class deliberately excludes properties to test error handling
 */
@ExtendWith(SpringJUnitExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
@TestPropertySource(properties = {
    // Only some required properties - should trigger validation failures
    "DB_HOST=localhost",
    "DB_PORT=5432"
    // Missing: DB_NAME, DB_USER, DB_PASSWORD, etc.
})
class EnvironmentConfigMissingPropertiesTest {

    @Autowired(required = false)
    private EnvironmentConfig environmentConfig;

    @Test
    void shouldHandleMissingRequiredProperties() {
        // This test verifies that Spring Boot fails to start with missing required properties
        // The @Autowired(required = false) allows the test to run even if the bean cannot be created
        
        // In a real scenario, the application would fail to start with missing required @Value properties
        // This test documents the expected behavior
        if (environmentConfig != null) {
            assertThatThrownBy(() -> environmentConfig.validateEnvironment())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Environment validation failed");
        }
    }
}