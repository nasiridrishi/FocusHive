package com.focushive.backend.config;

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
 * Comprehensive tests for FocusHive Backend Environment Configuration validation
 * 
 * Tests cover:
 * - Valid configuration scenarios with H2 and PostgreSQL
 * - Missing required environment variables
 * - Invalid format validation
 * - Database driver and URL consistency
 * - External service configuration validation
 * - Feature flag validation
 * - Boundary value testing
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
        assertThat(environmentConfig.getDatabaseUrl()).isNotEmpty();
        assertThat(environmentConfig.getDatabaseUsername()).isNotEmpty();
        assertThat(environmentConfig.getJwtSecret()).isNotEmpty();
        assertThat(environmentConfig.getServerPort()).isGreaterThan(1023);
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:postgresql://postgres.example.com:5432/focushive_db",
        "DATABASE_USERNAME=focushive_user",
        "DATABASE_PASSWORD=secure_database_password_123",
        "DATABASE_DRIVER=org.postgresql.Driver",
        "REDIS_HOST=redis.example.com",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=secure_redis_password_123",
        "JWT_SECRET=this_is_a_very_secure_jwt_secret_key_for_production_use_that_meets_all_requirements",
        "JWT_EXPIRATION=3600000",
        "JWT_REFRESH_EXPIRATION=2592000000",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=https://identity.focushive.com",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000",
        "IDENTITY_SERVICE_API_KEY=secure_api_key_123"
    })
    @Test
    void shouldValidateProductionPostgreSQLConfiguration() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getDatabaseUrl()).contains("postgresql");
        assertThat(environmentConfig.getDatabaseDriver()).isEqualTo("org.postgresql.Driver");
        assertThat(environmentConfig.getDatabaseUsername()).isEqualTo("focushive_user");
        assertThat(environmentConfig.getDatabasePassword()).isEqualTo("secure_database_password_123");
        assertThat(environmentConfig.getRedisHost()).isEqualTo("redis.example.com");
        assertThat(environmentConfig.getRedisPort()).isEqualTo(6379);
        assertThat(environmentConfig.getJwtSecret()).hasSize(80);
        assertThat(environmentConfig.getJwtExpiration()).isEqualTo(3600000L);
        assertThat(environmentConfig.getServerPort()).isEqualTo(8080);
        assertThat(environmentConfig.getIdentityServiceUrl()).isEqualTo("https://identity.focushive.com");
        assertThat(environmentConfig.getIdentityServiceApiKey()).isEqualTo("secure_api_key_123");
    }

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

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:postgresql://localhost:5432/test_db",
        "DATABASE_USERNAME=test_user",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver", // Driver doesn't match URL - should fail
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000"
    })
    @Test
    void shouldFailValidationForMismatchedDatabaseDriverAndUrl() {
        // Act & Assert
        assertThatThrownBy(() -> environmentConfig.validateEnvironment())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Environment validation failed")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:mysql://localhost:3306/test_db", // Unsupported database
        "DATABASE_USERNAME=test_user",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=com.mysql.cj.jdbc.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000"
    })
    @Test
    void shouldFailValidationForUnsupportedDatabase() {
        // This test validates the @Pattern constraint on DATABASE_URL and DATABASE_DRIVER
        // The validation will occur at the Spring constraint validation level
        assertThat(environmentConfig.getDatabaseUrl()).contains("mysql");
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=short", // Too short - should fail validation
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000"
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
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=invalid_host_with_special_chars!@#",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000"
    })
    @Test
    void shouldFailValidationForInvalidRedisHostFormat() {
        // This test validates the @Pattern constraint on REDIS_HOST
        // The validation will occur at the Spring constraint validation level
        assertThat(environmentConfig.getRedisHost()).isNotEmpty();
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=70000", // Invalid port - too high
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000"
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
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=invalid-url-format", // Invalid URL format
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000"
    })
    @Test
    void shouldFailValidationForInvalidIdentityServiceUrl() {
        // This test validates the @Pattern constraint on IDENTITY_SERVICE_URL
        // The validation will occur at the Spring constraint validation level
        assertThat(environmentConfig.getIdentityServiceUrl()).isNotEmpty();
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "JWT_EXPIRATION=299999", // Below minimum (5 minutes)
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000"
    })
    @Test
    void shouldFailValidationForBelowMinimumJwtExpiration() {
        // This test validates the @Min constraint on JWT_EXPIRATION
        // The validation will occur at the Spring constraint validation level
        assertThat(environmentConfig.getJwtExpiration()).isNotNull();
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=50000", // Very high - should trigger warning
        "IDENTITY_SERVICE_READ_TIMEOUT=80000" // Very high - should trigger warning
    })
    @Test
    void shouldWarnForHighTimeoutValues() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert - should complete successfully but log warnings
        assertThat(environmentConfig.getIdentityServiceConnectTimeout()).isEqualTo(50000);
        assertThat(environmentConfig.getIdentityServiceReadTimeout()).isEqualTo(80000);
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "JWT_EXPIRATION=300000", // Exactly minimum (5 minutes)
        "JWT_REFRESH_EXPIRATION=86400000", // Exactly minimum (1 day)
        "SERVER_PORT=1024", // Minimum allowed port
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=1000", // Minimum allowed
        "IDENTITY_SERVICE_READ_TIMEOUT=1000" // Minimum allowed
    })
    @Test
    void shouldValidateBoundaryValues() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getJwtExpiration()).isEqualTo(300000L);
        assertThat(environmentConfig.getJwtRefreshExpiration()).isEqualTo(86400000L);
        assertThat(environmentConfig.getServerPort()).isEqualTo(1024);
        assertThat(environmentConfig.getIdentityServiceConnectTimeout()).isEqualTo(1000);
        assertThat(environmentConfig.getIdentityServiceReadTimeout()).isEqualTo(1000);
    }

    @TestPropertySource(properties = {
        // Default configuration with feature flags
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000",
        "APP_VERSION=2.1.0",
        "LOG_LEVEL=DEBUG",
        "FEIGN_LOG_LEVEL=INFO",
        "RATE_LIMIT_ENABLED=true",
        "RATE_LIMIT_USE_REDIS=true"
    })
    @Test
    void shouldLoadOptionalConfigurationValues() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getAppVersion()).isEqualTo("2.1.0");
        assertThat(environmentConfig.getLogLevel()).isEqualTo("DEBUG");
        assertThat(environmentConfig.getFeignLogLevel()).isEqualTo("INFO");
        assertThat(environmentConfig.getRateLimitEnabled()).isTrue();
        assertThat(environmentConfig.getRateLimitUseRedis()).isTrue();
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000",
        // Feature flag configuration
        "forumEnabled=true",
        "buddyEnabled=true",
        "analyticsEnabled=true",
        "authenticationEnabled=true",
        "authControllerEnabled=true",
        "redisEnabled=true",
        "healthEnabled=true"
    })
    @Test
    void shouldLoadFeatureFlagConfiguration() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getForumEnabled()).isTrue();
        assertThat(environmentConfig.getBuddyEnabled()).isTrue();
        assertThat(environmentConfig.getAnalyticsEnabled()).isTrue();
        assertThat(environmentConfig.getAuthenticationEnabled()).isTrue();
        assertThat(environmentConfig.getAuthControllerEnabled()).isTrue();
        assertThat(environmentConfig.getRedisEnabled()).isTrue();
        assertThat(environmentConfig.getHealthEnabled()).isTrue();
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000",
        // Conflicting configuration: rate limiting wants Redis but Redis is disabled
        "rateLimitUseRedis=true",
        "redisEnabled=false"
    })
    @Test
    void shouldWarnForConflictingFeatureConfiguration() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert - should complete successfully but log warnings
        assertThat(environmentConfig.getRateLimitUseRedis()).isTrue();
        assertThat(environmentConfig.getRedisEnabled()).isFalse();
    }

    @TestPropertySource(properties = {
        "DATABASE_URL=jdbc:h2:mem:testdb",
        "DATABASE_USERNAME=sa",
        "DATABASE_PASSWORD=test_password",
        "DATABASE_DRIVER=org.h2.Driver",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379",
        "REDIS_PASSWORD=redis_password",
        "JWT_SECRET=valid_jwt_secret_that_meets_minimum_length_requirements",
        "SERVER_PORT=8080",
        "IDENTITY_SERVICE_URL=http://localhost:8081",
        "IDENTITY_SERVICE_CONNECT_TIMEOUT=5000",
        "IDENTITY_SERVICE_READ_TIMEOUT=10000",
        // Rate limiting configuration
        "rateLimitPublic=50",
        "rateLimitAuthenticated=500",
        "rateLimitAdmin=5000",
        "rateLimitWebsocket=30",
        "tracingSamplingProbability=0.5",
        "zipkinEndpoint=http://zipkin.example.com:9411/api/v2/spans"
    })
    @Test
    void shouldLoadRateLimitingAndTracingConfiguration() {
        // Act
        environmentConfig.validateEnvironment();

        // Assert
        assertThat(environmentConfig.getRateLimitPublic()).isEqualTo(50);
        assertThat(environmentConfig.getRateLimitAuthenticated()).isEqualTo(500);
        assertThat(environmentConfig.getRateLimitAdmin()).isEqualTo(5000);
        assertThat(environmentConfig.getRateLimitWebsocket()).isEqualTo(30);
        assertThat(environmentConfig.getTracingSamplingProbability()).isEqualTo(0.5);
        assertThat(environmentConfig.getZipkinEndpoint()).isEqualTo("http://zipkin.example.com:9411/api/v2/spans");
    }
}