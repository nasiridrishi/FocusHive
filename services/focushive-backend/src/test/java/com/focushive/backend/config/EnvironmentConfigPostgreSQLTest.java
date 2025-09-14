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
 * Tests for PostgreSQL environment configuration
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
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
class EnvironmentConfigPostgreSQLTest {

    @Autowired
    private EnvironmentConfig environmentConfig;

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
        assertThat(environmentConfig.getJwtSecret()).hasSize(83);
        assertThat(environmentConfig.getJwtExpiration()).isEqualTo(3600000L);
        assertThat(environmentConfig.getServerPort()).isEqualTo(8080);
        assertThat(environmentConfig.getIdentityServiceUrl()).isEqualTo("https://identity.focushive.com");
        assertThat(environmentConfig.getIdentityServiceApiKey()).isEqualTo("secure_api_key_123");
    }
}