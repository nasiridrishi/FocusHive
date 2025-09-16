package com.focushive.identity.deployment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Test suite for Docker deployment configuration.
 * Ensures all required environment variables are properly configured
 * for successful Docker deployment.
 */
@DisplayName("Docker Deployment Configuration Tests")
public class DockerDeploymentConfigurationTest {

    /**
     * Parse .env file format (not Properties format)
     */
    private Map<String, String> parseEnvFile(Path path) throws IOException {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Parse key=value
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    env.put(key, value);
                }
            }
        }
        return env;
    }

    @Test
    @DisplayName("Should have all required IDENTITY_* prefixed variables in .env file")
    void testIdentityPrefixedVariablesExist() throws IOException {
        // Load the .env file
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            // Skip test if .env file doesn't exist (CI environment)
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        // Required IDENTITY_* prefixed variables for docker-compose
        List<String> requiredIdentityVars = List.of(
            "IDENTITY_NETWORK_NAME",
            "IDENTITY_APP_CONTAINER_NAME",
            "IDENTITY_POSTGRES_CONTAINER_NAME",
            "IDENTITY_REDIS_CONTAINER_NAME",
            "IDENTITY_APP_IMAGE",
            "IDENTITY_POSTGRES_IMAGE",
            "IDENTITY_REDIS_IMAGE",
            "IDENTITY_DB_HOST",
            "IDENTITY_DB_PORT",
            "IDENTITY_DB_NAME",
            "IDENTITY_DB_USER",
            "IDENTITY_DB_PASSWORD",
            "IDENTITY_REDIS_HOST",
            "IDENTITY_REDIS_PORT",
            "IDENTITY_REDIS_PASSWORD",
            "IDENTITY_PORT",
            "IDENTITY_JAVA_OPTS"
        );

        assertAll("All required IDENTITY_* variables should exist",
            requiredIdentityVars.stream()
                .map(var -> () -> assertThat(envVars.get(var))
                    .as("Environment variable " + var)
                    .isNotNull())
        );
    }

    @Test
    @DisplayName("Should have matching application variables for Docker networking")
    void testApplicationVariablesForDocker() throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        // Application needs these for container communication
        Map<String, String> requiredAppVars = Map.of(
            "DB_HOST", "focushive-identity-service-postgres",
            "REDIS_HOST", "focushive-identity-service-redis",
            "DB_NAME", "focushive_identity"
        );

        requiredAppVars.forEach((key, expectedValue) -> {
            assertThat(envVars.get(key))
                .as("Application variable " + key)
                .isEqualTo(expectedValue);
        });
    }

    @Test
    @DisplayName("Should have proper health check configuration for containers")
    void testHealthCheckConfiguration() throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        // Health check variables
        List<String> healthCheckVars = List.of(
            "IDENTITY_APP_HEALTHCHECK_RETRIES",
            "IDENTITY_APP_HEALTHCHECK_INTERVAL",
            "IDENTITY_APP_HEALTHCHECK_TIMEOUT",
            "IDENTITY_APP_HEALTHCHECK_START_PERIOD",
            "IDENTITY_APP_HEALTHCHECK_ENDPOINT",
            "IDENTITY_POSTGRES_HEALTHCHECK_INTERVAL",
            "IDENTITY_POSTGRES_HEALTHCHECK_TIMEOUT",
            "IDENTITY_POSTGRES_HEALTHCHECK_RETRIES",
            "IDENTITY_REDIS_HEALTHCHECK_INTERVAL",
            "IDENTITY_REDIS_HEALTHCHECK_TIMEOUT",
            "IDENTITY_REDIS_HEALTHCHECK_RETRIES"
        );

        assertAll("All health check variables should exist",
            healthCheckVars.stream()
                .map(var -> () -> assertThat(envVars.get(var))
                    .as("Health check variable " + var)
                    .isNotNull())
        );
    }

    @Test
    @DisplayName("Should have proper resource limits for containers")
    void testResourceLimitsConfiguration() throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        // Resource limit variables
        List<String> resourceLimitVars = List.of(
            "IDENTITY_APP_MEMORY_RESERVATION",
            "IDENTITY_APP_MEMORY_LIMIT",
            "IDENTITY_APP_CPU_RESERVATION",
            "IDENTITY_APP_CPU_LIMIT",
            "IDENTITY_POSTGRES_MEMORY_RESERVATION",
            "IDENTITY_POSTGRES_MEMORY_LIMIT",
            "IDENTITY_POSTGRES_CPU_RESERVATION",
            "IDENTITY_POSTGRES_CPU_LIMIT",
            "IDENTITY_REDIS_MEMORY_RESERVATION",
            "IDENTITY_REDIS_MEMORY_LIMIT",
            "IDENTITY_REDIS_CPU_RESERVATION",
            "IDENTITY_REDIS_CPU_LIMIT"
        );

        assertAll("All resource limit variables should exist",
            resourceLimitVars.stream()
                .map(var -> () -> assertThat(envVars.get(var))
                    .as("Resource limit variable " + var)
                    .isNotNull())
        );
    }

    @Test
    @DisplayName("Should have JWT configuration for RSA signing")
    void testJWTConfiguration() throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        // JWT must be configured for RSA
        assertThat(envVars.get("JWT_USE_RSA"))
            .as("JWT_USE_RSA should be enabled")
            .isEqualTo("true");

        assertThat(envVars.get("JWT_RSA_KEY_ID"))
            .as("JWT_RSA_KEY_ID should be set")
            .isNotNull()
            .isNotEmpty();

        assertThat(envVars.get("JWT_SECRET"))
            .as("JWT_SECRET should be set")
            .isNotNull()
            .hasSize(64); // 256-bit secret
    }

    @Test
    @DisplayName("Should have proper OAuth2 issuer configuration")
    void testOAuth2IssuerConfiguration() throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        // OAuth2 issuer should use container hostname
        String authIssuer = envVars.get("AUTH_ISSUER");
        String jwtIssuer = envVars.get("JWT_ISSUER");
        String oauth2Issuer = envVars.get("OAUTH2_ISSUER");

        assertThat(authIssuer)
            .as("AUTH_ISSUER should use container hostname")
            .contains("focushive-identity-service-app");

        assertThat(jwtIssuer)
            .as("JWT_ISSUER should use container hostname")
            .contains("focushive-identity-service-app");

        assertThat(oauth2Issuer)
            .as("OAUTH2_ISSUER should use container hostname")
            .contains("focushive-identity-service-app");
    }

    @Test
    @DisplayName("Should have Spring profile set to docker")
    void testSpringProfileConfiguration() throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        assertThat(envVars.get("SPRING_PROFILES_ACTIVE"))
            .as("Spring profile should be set to docker")
            .isEqualTo("docker");
    }

    @Test
    @DisplayName("Should have notification service integration configured")
    void testNotificationServiceConfiguration() throws IOException {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return;
        }

        Map<String, String> envVars = parseEnvFile(envPath);

        assertThat(envVars.get("NOTIFICATION_SERVICE_URL"))
            .as("NOTIFICATION_SERVICE_URL should be configured")
            .isNotNull();

        assertThat(envVars.get("IDENTITY_NOTIFICATION_SERVICE_URL"))
            .as("IDENTITY_NOTIFICATION_SERVICE_URL should be configured")
            .isNotNull();

        assertThat(envVars.get("NOTIFICATION_SERVICE_API_KEY"))
            .as("NOTIFICATION_SERVICE_API_KEY should be configured")
            .isNotNull();
    }
}