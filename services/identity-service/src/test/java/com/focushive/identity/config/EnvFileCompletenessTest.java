package com.focushive.identity.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to ensure .env file is the single source of truth for ALL configuration.
 *
 * This test validates:
 * 1. All variables referenced in docker-compose.yml are defined in .env
 * 2. No hardcoded values exist in docker-compose.yml (everything uses ${VAR})
 * 3. All required application environment variables are present
 * 4. No duplicate or conflicting variable definitions
 */
public class EnvFileCompletenessTest {

    private static final String ENV_FILE_PATH = "../../.env";
    private static final String DOCKER_COMPOSE_PATH = "docker-compose.yml";
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([A-Z_]+[A-Z0-9_]*)(?::-[^}]*)?\\}");
    private static final Pattern HARDCODED_VALUE_PATTERN = Pattern.compile("^\\s*[A-Za-z_]+:\\s*(?!\\$\\{)[^#\\s]");

    @Test
    public void testAllDockerComposeVariablesDefinedInEnv() throws IOException {
        // Load .env file
        Map<String, String> envVariables = loadEnvFile();

        // Extract variables from docker-compose.yml
        Set<String> dockerComposeVars = extractVariablesFromDockerCompose();

        // Check that all docker-compose variables are defined in .env
        Set<String> missingVars = new HashSet<>();
        for (String var : dockerComposeVars) {
            if (!envVariables.containsKey(var) && !isOptionalVariable(var)) {
                missingVars.add(var);
            }
        }

        assertTrue(missingVars.isEmpty(),
            "The following variables are used in docker-compose.yml but not defined in .env: " + missingVars);
    }

    @Test
    public void testNoHardcodedValuesInDockerCompose() throws IOException {
        List<String> dockerComposeLines = Files.readAllLines(Paths.get(DOCKER_COMPOSE_PATH));
        List<String> hardcodedLines = new ArrayList<>();

        for (int i = 0; i < dockerComposeLines.size(); i++) {
            String line = dockerComposeLines.get(i);

            // Skip comments and empty lines
            if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                continue;
            }

            // Check for hardcoded ports (e.g., "8080:8080" instead of "${PORT}:${PORT}")
            if (line.contains(":") && line.contains("\"") &&
                line.matches(".*\"\\d+:\\d+\".*") && !line.contains("${")) {
                hardcodedLines.add("Line " + (i + 1) + ": " + line.trim());
            }

            // Check for hardcoded memory/cpu values
            if ((line.contains("memory:") || line.contains("cpus:")) &&
                !line.contains("${") && !line.contains("driver") && !line.contains("external")) {
                hardcodedLines.add("Line " + (i + 1) + ": " + line.trim());
            }

            // Check for hardcoded service URLs
            if (line.contains("http://") && !line.contains("${") && !line.contains("#")) {
                hardcodedLines.add("Line " + (i + 1) + ": " + line.trim());
            }
        }

        assertTrue(hardcodedLines.isEmpty(),
            "Found hardcoded values in docker-compose.yml. Everything should use variables from .env:\n" +
            String.join("\n", hardcodedLines));
    }

    @Test
    public void testRequiredIdentityServiceVariablesPresent() throws IOException {
        Map<String, String> envVariables = loadEnvFile();

        // List of required variables for identity service
        List<String> requiredVars = Arrays.asList(
            // Database
            "IDENTITY_DB_NAME",
            "IDENTITY_DB_USER",
            "IDENTITY_DB_PASSWORD",
            "IDENTITY_DB_HOST",
            "IDENTITY_DB_PORT",

            // Redis
            "IDENTITY_REDIS_PASSWORD",
            "IDENTITY_REDIS_HOST",
            "IDENTITY_REDIS_PORT",

            // JWT
            "JWT_SECRET",
            "JWT_ACCESS_TOKEN_EXPIRATION",
            "JWT_REFRESH_TOKEN_EXPIRATION",
            "JWT_ISSUER",

            // Security
            "ENCRYPTION_MASTER_KEY",
            "KEY_STORE_PASSWORD",
            "PRIVATE_KEY_PASSWORD",

            // Admin
            "ADMIN_USERNAME",
            "ADMIN_PASSWORD",
            "ADMIN_EMAIL",

            // Container Configuration
            "IDENTITY_POSTGRES_CONTAINER_NAME",
            "IDENTITY_REDIS_CONTAINER_NAME",
            "IDENTITY_APP_CONTAINER_NAME",

            // Ports
            "IDENTITY_PORT"
        );

        List<String> missing = new ArrayList<>();
        for (String var : requiredVars) {
            if (!envVariables.containsKey(var)) {
                missing.add(var);
            }
        }

        assertTrue(missing.isEmpty(),
            "The following required variables are missing from .env: " + missing);
    }

    @Test
    public void testNoConflictingVariableDefinitions() throws IOException {
        Map<String, String> envVariables = loadEnvFile();

        // Check for variables that might conflict
        // For example, DB_USER vs IDENTITY_DB_USER when both are present
        Set<String> potentialConflicts = new HashSet<>();

        if (envVariables.containsKey("DB_USER") && envVariables.containsKey("IDENTITY_DB_USER")) {
            if (!envVariables.get("DB_USER").equals(envVariables.get("IDENTITY_DB_USER"))) {
                potentialConflicts.add("DB_USER conflicts with IDENTITY_DB_USER");
            }
        }

        assertTrue(potentialConflicts.isEmpty(),
            "Found potential variable conflicts in .env: " + potentialConflicts);
    }

    @Test
    public void testAllContainerConfigurationExternalized() throws IOException {
        Map<String, String> envVariables = loadEnvFile();

        // Check that all container configuration is externalized
        List<String> containerConfigVars = Arrays.asList(
            // Images
            "IDENTITY_POSTGRES_IMAGE",
            "IDENTITY_REDIS_IMAGE",
            "IDENTITY_APP_IMAGE",

            // Resource limits
            "IDENTITY_POSTGRES_CPU_LIMIT",
            "IDENTITY_POSTGRES_MEMORY_LIMIT",
            "IDENTITY_REDIS_CPU_LIMIT",
            "IDENTITY_REDIS_MEMORY_LIMIT",
            "IDENTITY_APP_CPU_LIMIT",
            "IDENTITY_APP_MEMORY_LIMIT",

            // Health checks
            "IDENTITY_POSTGRES_HEALTHCHECK_INTERVAL",
            "IDENTITY_REDIS_HEALTHCHECK_INTERVAL",
            "IDENTITY_APP_HEALTHCHECK_INTERVAL",

            // Volumes
            "IDENTITY_POSTGRES_DATA_VOLUME",
            "IDENTITY_REDIS_DATA_VOLUME",

            // Network
            "IDENTITY_NETWORK_NAME"
        );

        List<String> missing = containerConfigVars.stream()
            .filter(var -> !envVariables.containsKey(var))
            .collect(Collectors.toList());

        assertTrue(missing.isEmpty(),
            "Container configuration not fully externalized. Missing: " + missing);
    }

    private Map<String, String> loadEnvFile() throws IOException {
        Map<String, String> envVars = new HashMap<>();
        Path envPath = Paths.get(ENV_FILE_PATH);

        if (!Files.exists(envPath)) {
            fail(".env file not found at: " + envPath.toAbsolutePath());
        }

        List<String> lines = Files.readAllLines(envPath);
        for (String line : lines) {
            // Skip comments and empty lines
            if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                continue;
            }

            // Parse KEY=VALUE
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                envVars.put(parts[0].trim(), parts[1].trim());
            }
        }

        return envVars;
    }

    private Set<String> extractVariablesFromDockerCompose() throws IOException {
        Set<String> variables = new HashSet<>();
        Path dockerComposePath = Paths.get(DOCKER_COMPOSE_PATH);

        if (!Files.exists(dockerComposePath)) {
            fail("docker-compose.yml not found at: " + dockerComposePath.toAbsolutePath());
        }

        String content = new String(Files.readAllBytes(dockerComposePath));
        Matcher matcher = ENV_VAR_PATTERN.matcher(content);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    private boolean isOptionalVariable(String var) {
        // Some variables might have defaults in docker-compose.yml
        Set<String> optionalVars = Set.of(
            "SPRING_PROFILES_ACTIVE" // Has default value in docker-compose
        );
        return optionalVars.contains(var);
    }
}