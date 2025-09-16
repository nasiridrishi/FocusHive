package com.focushive.identity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Secrets Management Configuration.
 * Provides integration with environment variables, Vault, or AWS Secrets Manager.
 * Ensures no hardcoded secrets in production.
 */
@Slf4j
@Configuration
public class SecretsManagerConfig {

    @Value("${secrets.provider:environment}")
    private String secretsProvider;

    @Value("${secrets.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${secrets.rotation.enabled:false}")
    private boolean rotationEnabled;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private final Environment environment;

    // Required secrets that must be provided in production
    private static final List<String> REQUIRED_SECRETS = List.of(
        "JWT_SECRET",
        "DB_PASSWORD",
        "DATABASE_PASSWORD"
    );

    // Optional but recommended secrets
    private static final List<String> RECOMMENDED_SECRETS = List.of(
        "REDIS_PASSWORD",
        "ADMIN_PASSWORD",
        "ENCRYPTION_KEY",
        "OAUTH_CLIENT_SECRET"
    );

    public SecretsManagerConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validate that all required secrets are present on startup.
     */
    @PostConstruct
    public void validateSecrets() {
        if (!validationEnabled) {
            log.warn("Secret validation is disabled. This should only be done in development!");
            return;
        }

        if (isProductionProfile()) {
            log.info("Validating required secrets for production environment...");

            List<String> missingSecrets = new ArrayList<>();
            List<String> weakSecrets = new ArrayList<>();

            // Check required secrets
            for (String secret : REQUIRED_SECRETS) {
                String value = environment.getProperty(secret);
                if (value == null || value.isEmpty()) {
                    // Also check lowercase version
                    value = environment.getProperty(secret.toLowerCase().replace("_", "."));
                }

                if (value == null || value.isEmpty() || value.equals("CHANGE_ME")) {
                    missingSecrets.add(secret);
                } else if (isWeakSecret(secret, value)) {
                    weakSecrets.add(secret);
                }
            }

            // Check recommended secrets
            for (String secret : RECOMMENDED_SECRETS) {
                String value = environment.getProperty(secret);
                if (value == null || value.isEmpty()) {
                    value = environment.getProperty(secret.toLowerCase().replace("_", "."));
                }

                if (value == null || value.isEmpty()) {
                    log.warn("Recommended secret '{}' is not configured", secret);
                }
            }

            // Fail fast if critical secrets are missing in production
            if (!missingSecrets.isEmpty()) {
                String errorMsg = String.format(
                    "CRITICAL: Missing required secrets in production: %s. " +
                    "Please configure these secrets via environment variables or secret manager.",
                    String.join(", ", missingSecrets)
                );
                log.error(errorMsg);

                if (isStrictProductionMode()) {
                    throw new IllegalStateException(errorMsg);
                }
            }

            if (!weakSecrets.isEmpty()) {
                log.warn("SECURITY WARNING: Weak secrets detected: {}. Please use stronger values.",
                    String.join(", ", weakSecrets));
            }

            log.info("Secret validation completed. Provider: {}", secretsProvider);
        }
    }

    /**
     * Secret provider bean for retrieving secrets from configured source.
     */
    @Bean
    public SecretProvider secretProvider() {
        log.info("Initializing secret provider: {}", secretsProvider);

        return switch (secretsProvider.toLowerCase()) {
            case "vault" -> new VaultSecretProvider(environment);
            case "aws-secrets-manager", "aws" -> new AwsSecretsManagerProvider(environment);
            case "kubernetes", "k8s" -> new KubernetesSecretProvider(environment);
            default -> new EnvironmentSecretProvider(environment);
        };
    }

    /**
     * Command line runner to log secret management status on startup.
     */
    @Bean
    public CommandLineRunner secretManagementStatus() {
        return args -> {
            if (isProductionProfile()) {
                log.info("================== SECRET MANAGEMENT STATUS ==================");
                log.info("Provider: {}", secretsProvider);
                log.info("Validation: {}", validationEnabled ? "ENABLED" : "DISABLED");
                log.info("Rotation: {}", rotationEnabled ? "ENABLED" : "DISABLED");
                log.info("Profile: {}", activeProfile);
                log.info("=============================================================");

                // Log masked values for verification
                logMaskedSecret("JWT_SECRET", environment.getProperty("JWT_SECRET"));
                logMaskedSecret("DB_PASSWORD", environment.getProperty("DB_PASSWORD"));
            }
        };
    }

    /**
     * Check if a secret value is considered weak.
     */
    private boolean isWeakSecret(String name, String value) {
        if (value == null) return true;

        // Check for common weak values
        Set<String> weakValues = Set.of(
            "password", "123456", "admin", "secret", "changeme",
            "password123", "admin123", "test", "demo"
        );

        if (weakValues.contains(value.toLowerCase())) {
            return true;
        }

        // Check minimum length based on secret type
        if (name.contains("JWT") && value.length() < 32) {
            return true; // JWT secrets should be at least 256 bits
        }

        if (name.contains("PASSWORD") && value.length() < 12) {
            return true; // Passwords should be at least 12 characters
        }

        if (name.contains("KEY") && value.length() < 16) {
            return true; // Encryption keys should be at least 128 bits
        }

        return false;
    }

    /**
     * Check if running in production profile.
     */
    private boolean isProductionProfile() {
        return activeProfile.contains("prod") || activeProfile.contains("production");
    }

    /**
     * Check if running in strict production mode (fail on missing secrets).
     */
    private boolean isStrictProductionMode() {
        return isProductionProfile() &&
               !"false".equals(environment.getProperty("secrets.strict.mode", "true"));
    }

    /**
     * Log a masked version of a secret for verification.
     */
    private void logMaskedSecret(String name, String value) {
        if (value == null || value.isEmpty()) {
            log.warn("{}: NOT SET", name);
        } else {
            String masked = maskSecret(value);
            log.info("{}: {} (length: {})", name, masked, value.length());
        }
    }

    /**
     * Mask sensitive values for logging.
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 4) {
            return "***";
        }
        return secret.substring(0, 2) + "*".repeat(Math.min(secret.length() - 4, 10)) +
               secret.substring(secret.length() - 2);
    }

    /**
     * Interface for secret providers.
     */
    public interface SecretProvider {
        String getSecret(String key);
        void refreshSecret(String key);
        boolean supportsRotation();
    }

    /**
     * Environment variable based secret provider (default).
     */
    public static class EnvironmentSecretProvider implements SecretProvider {
        private final Environment environment;

        public EnvironmentSecretProvider(Environment environment) {
            this.environment = environment;
        }

        @Override
        public String getSecret(String key) {
            // Try environment variable first (uppercase with underscores)
            String value = environment.getProperty(key);
            if (value == null) {
                // Try property format (lowercase with dots)
                value = environment.getProperty(key.toLowerCase().replace("_", "."));
            }
            return value;
        }

        @Override
        public void refreshSecret(String key) {
            // Environment variables don't support runtime refresh
            log.debug("Environment provider does not support secret refresh for key: {}", key);
        }

        @Override
        public boolean supportsRotation() {
            return false;
        }
    }

    /**
     * HashiCorp Vault secret provider.
     */
    public static class VaultSecretProvider implements SecretProvider {
        private final Environment environment;

        public VaultSecretProvider(Environment environment) {
            this.environment = environment;
            log.info("Vault secret provider initialized");
        }

        @Override
        public String getSecret(String key) {
            // In production, this would integrate with Vault API
            // For now, fallback to environment
            return environment.getProperty(key);
        }

        @Override
        public void refreshSecret(String key) {
            log.info("Refreshing secret from Vault: {}", key);
            // Implement Vault lease renewal
        }

        @Override
        public boolean supportsRotation() {
            return true;
        }
    }

    /**
     * AWS Secrets Manager provider.
     */
    public static class AwsSecretsManagerProvider implements SecretProvider {
        private final Environment environment;

        public AwsSecretsManagerProvider(Environment environment) {
            this.environment = environment;
            log.info("AWS Secrets Manager provider initialized");
        }

        @Override
        public String getSecret(String key) {
            // In production, this would use AWS SDK
            // For now, fallback to environment
            return environment.getProperty(key);
        }

        @Override
        public void refreshSecret(String key) {
            log.info("Refreshing secret from AWS Secrets Manager: {}", key);
            // Implement AWS Secrets Manager refresh
        }

        @Override
        public boolean supportsRotation() {
            return true;
        }
    }

    /**
     * Kubernetes secrets provider.
     */
    public static class KubernetesSecretProvider implements SecretProvider {
        private final Environment environment;

        public KubernetesSecretProvider(Environment environment) {
            this.environment = environment;
            log.info("Kubernetes secret provider initialized");
        }

        @Override
        public String getSecret(String key) {
            // In production, this would read from mounted secrets
            // For now, fallback to environment
            return environment.getProperty(key);
        }

        @Override
        public void refreshSecret(String key) {
            log.info("Kubernetes secrets are refreshed via pod restart");
        }

        @Override
        public boolean supportsRotation() {
            return false; // Requires pod restart
        }
    }
}