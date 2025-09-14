package com.focushive.identity.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic tests for Identity Service Environment Configuration
 * DISABLED: EnvironmentConfig is excluded from test profile to avoid environment variable dependencies
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EnvironmentConfig.class})
@EnableConfigurationProperties
@Disabled("EnvironmentConfig tests are disabled because EnvironmentConfig is excluded from test profile")
@TestPropertySource(properties = {
    "DB_HOST=testhost.example.com",
    "DB_PORT=5432",
    "DB_NAME=testdb",
    "DB_USER=testuser",
    "DB_PASSWORD=testpass123",
    "JWT_SECRET=test-jwt-secret-key-that-is-long-enough-for-security",
    "JWT_ACCESS_TOKEN_EXPIRATION=3600000",
    "JWT_REFRESH_TOKEN_EXPIRATION=86400000",
    "SERVER_PORT=8081",
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD=testredispass",
    "KEYSTORE_PASSWORD=test-keystore-pass",
    "PRIVATE_KEY_PASSWORD=test-key-pass",
    "FOCUSHIVE_CLIENT_SECRET=test-client-secret",
    "ENCRYPTION_MASTER_KEY=test-encryption-key-32-characters!!",
    "ISSUER_URI=http://localhost:8081"
})
class EnvironmentConfigTest {

    @Autowired
    private EnvironmentConfig environmentConfig;

    @Test
    void shouldLoadValidConfigurationSuccessfully() {
        // Act & Assert - Configuration should be loaded
        assertThat(environmentConfig).isNotNull();
        assertThat(environmentConfig.getDbHost()).isEqualTo("testhost.example.com");
        assertThat(environmentConfig.getDbPort()).isEqualTo(5432);
        assertThat(environmentConfig.getDbName()).isEqualTo("testdb");
        assertThat(environmentConfig.getJwtSecret()).isNotEmpty();
        assertThat(environmentConfig.getServerPort()).isEqualTo(8081);
    }

    @Test
    void shouldLoadDatabaseConfigurationCorrectly() {
        // Assert database configuration
        assertThat(environmentConfig.getDbHost()).isEqualTo("testhost.example.com");
        assertThat(environmentConfig.getDbPort()).isEqualTo(5432);
        assertThat(environmentConfig.getDbName()).isEqualTo("testdb");
        assertThat(environmentConfig.getDbUser()).isEqualTo("testuser");
        assertThat(environmentConfig.getDbPassword()).isEqualTo("testpass123");
    }

    @Test
    void shouldLoadJwtConfigurationCorrectly() {
        // Assert JWT configuration
        assertThat(environmentConfig.getJwtSecret()).isEqualTo("test-jwt-secret-key-that-is-long-enough-for-security");
        assertThat(environmentConfig.getJwtAccessTokenExpiration()).isNotNull();
        assertThat(environmentConfig.getJwtRefreshTokenExpiration()).isNotNull();
    }

    @Test
    void shouldLoadRedisConfigurationCorrectly() {
        // Assert Redis configuration
        assertThat(environmentConfig.getRedisHost()).isEqualTo("localhost");
        assertThat(environmentConfig.getRedisPort()).isEqualTo(6379);
    }

    @Test
    void shouldLoadOAuth2ConfigurationCorrectly() {
        // Assert OAuth2 configuration
        assertThat(environmentConfig.getIssuerUri()).isNotNull();
        assertThat(environmentConfig.getFocushiveClientSecret()).isNotNull();
    }

    @Test
    void shouldLoadSecurityConfigurationCorrectly() {
        // Assert Security configuration
        assertThat(environmentConfig.getKeyStorePassword()).isNotNull();
        assertThat(environmentConfig.getPrivateKeyPassword()).isNotNull();
        assertThat(environmentConfig.getEncryptionMasterKey()).isNotNull();
    }

    @Test
    void shouldLoadRedisPasswordIfProvided() {
        // Assert Redis password can be loaded
        // Note: Redis password might be optional
        assertThat(environmentConfig.getRedisPassword()).isNotNull();
    }
}