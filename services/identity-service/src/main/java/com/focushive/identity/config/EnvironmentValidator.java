package com.focushive.identity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Environment Variable Validator.
 * Validates that all required environment variables are properly configured
 * and fails fast if critical variables are missing in production.
 */
@Slf4j
@Component
public class EnvironmentValidator implements ApplicationRunner {

    @Value("${environment.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${environment.validation.fail-fast:true}")
    private boolean failFast;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private final Environment environment;

    // Configuration categories with their required variables
    private static final Map<String, List<String>> REQUIRED_CONFIG = Map.of(
        "Database", List.of("DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "DB_PASSWORD"),
        "Redis", List.of("REDIS_HOST", "REDIS_PORT"),
        "JWT", List.of("JWT_SECRET", "JWT_ISSUER"),
        "Security", List.of("ADMIN_USERNAME", "ADMIN_PASSWORD"),
        "Server", List.of("SERVER_PORT")
    );

    // Optional but recommended variables
    private static final Map<String, List<String>> OPTIONAL_CONFIG = Map.of(
        "Redis Cluster", List.of("REDIS_PASSWORD", "REDIS_CLUSTER_NODES"),
        "SSL", List.of("SSL_ENABLED", "SSL_KEY_STORE", "SSL_KEY_STORE_PASSWORD"),
        "OAuth2", List.of("OAUTH2_CLIENT_ID", "OAUTH2_CLIENT_SECRET"),
        "Monitoring", List.of("METRICS_ENABLED", "TRACING_ENABLED", "ZIPKIN_ENDPOINT"),
        "Rate Limiting", List.of("AUTH_RATE_LIMIT_RPM", "API_RATE_LIMIT_RPM")
    );

    // Format validators for specific variables
    private static final Map<String, Pattern> FORMAT_VALIDATORS = Map.of(
        "DB_PORT", Pattern.compile("\\d+"),
        "REDIS_PORT", Pattern.compile("\\d+"),
        "SERVER_PORT", Pattern.compile("\\d+"),
        "JWT_ISSUER", Pattern.compile("^https?://.*"),
        "CORS_ORIGINS", Pattern.compile("^https?://.*")
    );

    // Minimum value requirements
    private static final Map<String, Integer> MINIMUM_LENGTHS = Map.of(
        "JWT_SECRET", 32,  // 256 bits
        "DB_PASSWORD", 12,
        "ADMIN_PASSWORD", 12,
        "ENCRYPTION_KEY", 32,
        "OAUTH2_CLIENT_SECRET", 24
    );

    // Weak values that should not be used
    private static final Set<String> WEAK_VALUES = Set.of(
        "password", "123456", "admin", "test", "demo", "secret",
        "changeme", "CHANGE_ME", "password123", "admin123", "default"
    );

    public EnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Validate environment on application startup.
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!validationEnabled) {
            log.warn("⚠️ Environment validation is disabled. Enable it for production!");
            return;
        }

        log.info("🔍 Starting environment variable validation...");

        ValidationResult result = validateEnvironment();

        // Log results
        logValidationResults(result);

        // Fail fast if configured and there are critical errors
        if (failFast && !result.criticalErrors.isEmpty() && isProductionEnvironment()) {
            String errorMessage = String.format(
                "❌ Environment validation failed with %d critical errors. " +
                "Please fix these issues before starting the application.",
                result.criticalErrors.size()
            );
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Perform comprehensive environment validation.
     */
    private ValidationResult validateEnvironment() {
        ValidationResult result = new ValidationResult();

        // Validate required variables
        validateRequiredVariables(result);

        // Validate optional but recommended variables
        validateOptionalVariables(result);

        // Validate variable formats
        validateFormats(result);

        // Validate security requirements
        validateSecurity(result);

        // Production-specific validations
        if (isProductionEnvironment()) {
            validateProductionRequirements(result);
        }

        return result;
    }

    /**
     * Validate required environment variables.
     */
    private void validateRequiredVariables(ValidationResult result) {
        REQUIRED_CONFIG.forEach((category, variables) -> {
            for (String var : variables) {
                String value = getEnvironmentValue(var);
                if (value == null || value.isEmpty()) {
                    result.criticalErrors.add(String.format(
                        "%s: Required variable '%s' is not set", category, var
                    ));
                } else if (WEAK_VALUES.contains(value.toLowerCase())) {
                    result.warnings.add(String.format(
                        "%s: Variable '%s' uses weak value", category, var
                    ));
                }
            }
        });
    }

    /**
     * Validate optional but recommended variables.
     */
    private void validateOptionalVariables(ValidationResult result) {
        OPTIONAL_CONFIG.forEach((category, variables) -> {
            for (String var : variables) {
                String value = getEnvironmentValue(var);
                if (value == null || value.isEmpty()) {
                    result.recommendations.add(String.format(
                        "%s: Recommended variable '%s' is not set", category, var
                    ));
                }
            }
        });
    }

    /**
     * Validate variable formats.
     */
    private void validateFormats(ValidationResult result) {
        FORMAT_VALIDATORS.forEach((var, pattern) -> {
            String value = getEnvironmentValue(var);
            if (value != null && !value.isEmpty() && !pattern.matcher(value).matches()) {
                result.warnings.add(String.format(
                    "Variable '%s' has invalid format. Expected pattern: %s",
                    var, pattern.pattern()
                ));
            }
        });
    }

    /**
     * Validate security requirements.
     */
    private void validateSecurity(ValidationResult result) {
        // Check minimum lengths
        MINIMUM_LENGTHS.forEach((var, minLength) -> {
            String value = getEnvironmentValue(var);
            if (value != null && value.length() < minLength) {
                result.warnings.add(String.format(
                    "Variable '%s' is too short. Minimum length: %d, Current: %d",
                    var, minLength, value.length()
                ));
            }
        });

        // Check for exposed secrets
        checkForExposedSecrets(result);
    }

    /**
     * Production-specific validations.
     */
    private void validateProductionRequirements(ValidationResult result) {
        // SSL must be enabled
        String sslEnabled = getEnvironmentValue("SSL_ENABLED");
        if (!"true".equals(sslEnabled)) {
            result.criticalErrors.add("SSL must be enabled in production");
        }

        // JWT issuer must use HTTPS
        String jwtIssuer = getEnvironmentValue("JWT_ISSUER");
        if (jwtIssuer != null && !jwtIssuer.startsWith("https://")) {
            result.criticalErrors.add("JWT_ISSUER must use HTTPS in production");
        }

        // Database SSL
        String dbUrl = getEnvironmentValue("DATABASE_URL");
        if (dbUrl != null && !dbUrl.contains("sslmode=require")) {
            result.warnings.add("Database connection should use SSL in production");
        }

        // Monitoring should be enabled
        if (!"true".equals(getEnvironmentValue("METRICS_ENABLED"))) {
            result.recommendations.add("Metrics should be enabled in production");
        }

        // Backup should be configured
        if (!"true".equals(getEnvironmentValue("BACKUP_ENABLED"))) {
            result.recommendations.add("Backup should be configured for disaster recovery");
        }
    }

    /**
     * Check for potentially exposed secrets.
     */
    private void checkForExposedSecrets(ValidationResult result) {
        // Check for secrets in common misconfigured locations
        List<String> riskyVariables = List.of(
            "PASSWORD_PLAINTEXT",
            "SECRET_UNENCRYPTED",
            "API_KEY_CLEARTEXT",
            "TOKEN_PLAIN"
        );

        for (String risky : riskyVariables) {
            if (getEnvironmentValue(risky) != null) {
                result.criticalErrors.add(
                    "Potentially exposed secret in variable: " + risky
                );
            }
        }
    }

    /**
     * Log validation results with appropriate formatting.
     */
    private void logValidationResults(ValidationResult result) {
        log.info("=" + "=".repeat(60));
        log.info("ENVIRONMENT VALIDATION RESULTS");
        log.info("=" + "=".repeat(60));
        log.info("Profile: {}", activeProfile);
        log.info("Validation: {}", validationEnabled ? "ENABLED" : "DISABLED");
        log.info("Fail-Fast: {}", failFast ? "ENABLED" : "DISABLED");
        log.info("-" + "-".repeat(60));

        // Log critical errors
        if (!result.criticalErrors.isEmpty()) {
            log.error("❌ CRITICAL ERRORS ({}):", result.criticalErrors.size());
            result.criticalErrors.forEach(error -> log.error("  • {}", error));
        }

        // Log warnings
        if (!result.warnings.isEmpty()) {
            log.warn("⚠️ WARNINGS ({}):", result.warnings.size());
            result.warnings.forEach(warning -> log.warn("  • {}", warning));
        }

        // Log recommendations
        if (!result.recommendations.isEmpty()) {
            log.info("💡 RECOMMENDATIONS ({}):", result.recommendations.size());
            result.recommendations.forEach(rec -> log.info("  • {}", rec));
        }

        // Summary
        if (result.criticalErrors.isEmpty() && result.warnings.isEmpty()) {
            log.info("✅ Environment validation passed successfully!");
        } else if (result.criticalErrors.isEmpty()) {
            log.info("✓ Environment validation passed with {} warnings", result.warnings.size());
        } else {
            log.error("❌ Environment validation failed with {} critical errors",
                result.criticalErrors.size());
        }

        log.info("=" + "=".repeat(60));
    }

    /**
     * Get environment value from system env or Spring property.
     */
    private String getEnvironmentValue(String key) {
        // Try system environment variable first
        String value = System.getenv(key);
        if (value == null) {
            // Try Spring property format
            value = environment.getProperty(key.toLowerCase().replace("_", "."));
        }
        return value;
    }

    /**
     * Check if running in production environment.
     */
    private boolean isProductionEnvironment() {
        return activeProfile.contains("prod") || activeProfile.contains("production");
    }

    /**
     * Validation result container.
     */
    private static class ValidationResult {
        final List<String> criticalErrors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> recommendations = new ArrayList<>();
    }
}