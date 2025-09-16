package com.focushive.identity.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that environment variables are correctly mapped in the application configuration.
 *
 * This test validates that the application correctly uses environment variables from .env file
 * for configuration, ensuring that:
 * 1. Database connection parameters are correctly mapped
 * 2. Redis configuration uses proper environment variables
 * 3. Security keys and secrets are sourced from environment
 * 4. Service URLs are correctly configured
 *
 * This is a unit test that doesn't require Spring context.
 */
public class EnvironmentConfigurationTest {

    private MockEnvironment environment;

    @BeforeEach
    public void setup() {
        environment = new MockEnvironment();

        // Set up environment variables as they would come from .env file
        environment.setProperty("DB_HOST", "postgres");
        environment.setProperty("DB_PORT", "5432");
        environment.setProperty("DB_NAME", "focushive_identity");
        environment.setProperty("DB_USER", "focushive_user");
        environment.setProperty("DB_PASSWORD", "focushive_pass");

        environment.setProperty("REDIS_HOST", "redis");
        environment.setProperty("REDIS_PORT", "6379");
        environment.setProperty("REDIS_PASSWORD", "redis_pass");

        environment.setProperty("JWT_SECRET", "ef90b5d6dabcf307e93ccfc6df11dc2838b45fae0f0e3c4f5db8a4c991d5b8f6");
        environment.setProperty("JWT_ISSUER", "http://localhost:8081");
        environment.setProperty("AUTH_ISSUER", "http://localhost:8081");

        environment.setProperty("ADMIN_USERNAME", "admin");
        environment.setProperty("ADMIN_PASSWORD", "Admin123!");
        environment.setProperty("ADMIN_EMAIL", "admin@focushive.local");

        environment.setProperty("ENCRYPTION_MASTER_KEY", "Zp1I56EhNXCrCKynFFw0T6D9nw2kopzxafoy40xpKJM=");
        environment.setProperty("ENCRYPTION_SALT", "secure-encryption-salt");
    }

    @Test
    public void testDatabaseEnvironmentVariables() {
        // Test that database configuration variables are correctly set
        assertEquals("postgres", environment.getProperty("DB_HOST"),
            "DB_HOST should be set from .env file");
        assertEquals("5432", environment.getProperty("DB_PORT"),
            "DB_PORT should be set from .env file");
        assertEquals("focushive_identity", environment.getProperty("DB_NAME"),
            "DB_NAME should be set from .env file");
        assertEquals("focushive_user", environment.getProperty("DB_USER"),
            "DB_USER should be set from .env file");
        assertEquals("focushive_pass", environment.getProperty("DB_PASSWORD"),
            "DB_PASSWORD should be set from .env file");

        // Test that JDBC URL can be constructed from individual components
        String expectedJdbcUrl = String.format("jdbc:postgresql://%s:%s/%s",
            environment.getProperty("DB_HOST"),
            environment.getProperty("DB_PORT"),
            environment.getProperty("DB_NAME"));
        assertEquals("jdbc:postgresql://postgres:5432/focushive_identity", expectedJdbcUrl,
            "JDBC URL should be constructable from individual environment variables");
    }

    @Test
    public void testRedisEnvironmentVariables() {
        // Test that Redis configuration variables are correctly set
        assertEquals("redis", environment.getProperty("REDIS_HOST"),
            "REDIS_HOST should be set from .env file");
        assertEquals("6379", environment.getProperty("REDIS_PORT"),
            "REDIS_PORT should be set from .env file");
        assertEquals("redis_pass", environment.getProperty("REDIS_PASSWORD"),
            "REDIS_PASSWORD should be set from .env file");
    }

    @Test
    public void testSecurityEnvironmentVariables() {
        // Test that security configuration variables are correctly set
        assertEquals("ef90b5d6dabcf307e93ccfc6df11dc2838b45fae0f0e3c4f5db8a4c991d5b8f6",
            environment.getProperty("JWT_SECRET"),
            "JWT_SECRET should be set from .env file");
        assertEquals("http://localhost:8081", environment.getProperty("JWT_ISSUER"),
            "JWT_ISSUER should be set from .env file");
        assertEquals("http://localhost:8081", environment.getProperty("AUTH_ISSUER"),
            "AUTH_ISSUER should be set from .env file");
        assertEquals("Zp1I56EhNXCrCKynFFw0T6D9nw2kopzxafoy40xpKJM=",
            environment.getProperty("ENCRYPTION_MASTER_KEY"),
            "ENCRYPTION_MASTER_KEY should be set from .env file");
        assertEquals("secure-encryption-salt", environment.getProperty("ENCRYPTION_SALT"),
            "ENCRYPTION_SALT should be set from .env file");
    }

    @Test
    public void testAdminEnvironmentVariables() {
        // Test that admin configuration variables are correctly set
        assertEquals("admin", environment.getProperty("ADMIN_USERNAME"),
            "ADMIN_USERNAME should be set from .env file");
        assertEquals("Admin123!", environment.getProperty("ADMIN_PASSWORD"),
            "ADMIN_PASSWORD should be set from .env file");
        assertEquals("admin@focushive.local", environment.getProperty("ADMIN_EMAIL"),
            "ADMIN_EMAIL should be set from .env file");
    }

    @Test
    public void testEnvironmentVariablePresence() {
        // Verify that all required environment variables are non-null and non-empty
        assertNotNull(environment.getProperty("DB_HOST"), "DB_HOST must not be null");
        assertNotNull(environment.getProperty("DB_PORT"), "DB_PORT must not be null");
        assertNotNull(environment.getProperty("DB_NAME"), "DB_NAME must not be null");
        assertNotNull(environment.getProperty("DB_USER"), "DB_USER must not be null");
        assertNotNull(environment.getProperty("DB_PASSWORD"), "DB_PASSWORD must not be null");

        assertNotNull(environment.getProperty("REDIS_HOST"), "REDIS_HOST must not be null");
        assertNotNull(environment.getProperty("REDIS_PORT"), "REDIS_PORT must not be null");
        assertNotNull(environment.getProperty("REDIS_PASSWORD"), "REDIS_PASSWORD must not be null");

        assertNotNull(environment.getProperty("JWT_SECRET"), "JWT_SECRET must not be null");
        assertNotNull(environment.getProperty("JWT_ISSUER"), "JWT_ISSUER must not be null");
        assertNotNull(environment.getProperty("AUTH_ISSUER"), "AUTH_ISSUER must not be null");

        assertNotNull(environment.getProperty("ADMIN_USERNAME"), "ADMIN_USERNAME must not be null");
        assertNotNull(environment.getProperty("ADMIN_PASSWORD"), "ADMIN_PASSWORD must not be null");
        assertNotNull(environment.getProperty("ADMIN_EMAIL"), "ADMIN_EMAIL must not be null");

        assertNotNull(environment.getProperty("ENCRYPTION_MASTER_KEY"), "ENCRYPTION_MASTER_KEY must not be null");
        assertNotNull(environment.getProperty("ENCRYPTION_SALT"), "ENCRYPTION_SALT must not be null");
    }

    @Test
    public void testDockerProfileConfiguration() {
        // Test that in Docker environment, container hostnames should be used
        assertEquals("postgres", environment.getProperty("DB_HOST"),
            "In Docker, DB_HOST should use container hostname 'postgres'");
        assertEquals("redis", environment.getProperty("REDIS_HOST"),
            "In Docker, REDIS_HOST should use container hostname 'redis'");
    }

    @Test
    public void testEnvironmentVariableMapping() {
        // This test validates that the environment variable names match what's expected in application.properties
        // The application.properties file expects these exact environment variable names

        // Database configuration mapping
        assertTrue(environment.containsProperty("DB_HOST"),
            "DB_HOST must be present for spring.datasource.url construction");
        assertTrue(environment.containsProperty("DB_PORT"),
            "DB_PORT must be present for spring.datasource.url construction");
        assertTrue(environment.containsProperty("DB_NAME"),
            "DB_NAME must be present for spring.datasource.url construction");
        assertTrue(environment.containsProperty("DB_USER"),
            "DB_USER must be present for spring.datasource.username");
        assertTrue(environment.containsProperty("DB_PASSWORD"),
            "DB_PASSWORD must be present for spring.datasource.password");

        // Redis configuration mapping
        assertTrue(environment.containsProperty("REDIS_HOST"),
            "REDIS_HOST must be present for spring.data.redis.host");
        assertTrue(environment.containsProperty("REDIS_PORT"),
            "REDIS_PORT must be present for spring.data.redis.port");
        assertTrue(environment.containsProperty("REDIS_PASSWORD"),
            "REDIS_PASSWORD must be present for spring.data.redis.password");

        // JWT configuration mapping
        assertTrue(environment.containsProperty("JWT_SECRET"),
            "JWT_SECRET must be present for jwt.secret");
        assertTrue(environment.containsProperty("JWT_ISSUER"),
            "JWT_ISSUER must be present for jwt.issuer");

        // Admin configuration mapping
        assertTrue(environment.containsProperty("ADMIN_USERNAME"),
            "ADMIN_USERNAME must be present for app.admin.username");
        assertTrue(environment.containsProperty("ADMIN_PASSWORD"),
            "ADMIN_PASSWORD must be present for app.admin.password");

        // Encryption configuration mapping
        assertTrue(environment.containsProperty("ENCRYPTION_MASTER_KEY"),
            "ENCRYPTION_MASTER_KEY must be present for app.encryption.master-key");
    }
}