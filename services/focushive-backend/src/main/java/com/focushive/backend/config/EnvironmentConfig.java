package com.focushive.backend.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Environment Configuration Validation for FocusHive Backend Service
 * 
 * This component validates all required environment variables at application startup
 * and provides clear error messages for missing or invalid configuration.
 * 
 * Features:
 * - Comprehensive validation of all required environment variables
 * - Clear error messages for missing/invalid values
 * - Fail-fast startup behavior to prevent runtime issues
 * - Secure logging that excludes sensitive configuration values
 * - Validation of database, Redis, JWT, and external service configurations
 */
@Component
@Validated
public class EnvironmentConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

    // ========================================
    // DATABASE CONFIGURATION
    // ========================================

    @Value("${DATABASE_URL:jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE}")
    @NotBlank(message = "DATABASE_URL environment variable is required")
    @Pattern(regexp = "^jdbc:(postgresql|h2):.*", message = "DATABASE_URL must be a valid JDBC URL (PostgreSQL or H2)")
    private String databaseUrl;

    @Value("${DATABASE_USERNAME:sa}")
    @NotBlank(message = "DATABASE_USERNAME environment variable is required")
    private String databaseUsername;

    @Value("${DATABASE_PASSWORD}")
    @NotBlank(message = "DATABASE_PASSWORD environment variable is required")
    private String databasePassword;

    @Value("${DATABASE_DRIVER:org.h2.Driver}")
    @NotBlank(message = "DATABASE_DRIVER environment variable is required")
    @Pattern(regexp = "^(org\\.postgresql\\.Driver|org\\.h2\\.Driver)$", 
             message = "DATABASE_DRIVER must be either org.postgresql.Driver or org.h2.Driver")
    private String databaseDriver;

    // ========================================
    // REDIS CONFIGURATION
    // ========================================

    @Value("${REDIS_HOST:localhost}")
    @NotBlank(message = "REDIS_HOST environment variable is required")
    @Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "REDIS_HOST must be a valid hostname or IP address")
    private String redisHost;

    @Value("${REDIS_PORT:6379}")
    @NotNull(message = "REDIS_PORT environment variable is required")
    @Min(value = 1, message = "REDIS_PORT must be a positive number")
    private Integer redisPort;

    @Value("${REDIS_PASSWORD}")
    @NotBlank(message = "REDIS_PASSWORD environment variable is required for security")
    private String redisPassword;

    // ========================================
    // JWT CONFIGURATION
    // ========================================

    @Value("${JWT_SECRET}")
    @NotBlank(message = "JWT_SECRET environment variable is required")
    private String jwtSecret;

    @Value("${JWT_EXPIRATION:86400000}")
    @NotNull(message = "JWT_EXPIRATION environment variable is required")
    @Min(value = 300000, message = "JWT_EXPIRATION must be at least 5 minutes (300000ms)")
    private Long jwtExpiration;

    @Value("${JWT_REFRESH_EXPIRATION:604800000}")
    @NotNull(message = "JWT_REFRESH_EXPIRATION environment variable is required")
    @Min(value = 86400000, message = "JWT_REFRESH_EXPIRATION must be at least 1 day (86400000ms)")
    private Long jwtRefreshExpiration;

    // ========================================
    // SERVER CONFIGURATION
    // ========================================

    @Value("${SERVER_PORT:8080}")
    @NotNull(message = "SERVER_PORT environment variable is required")
    @Min(value = 1024, message = "SERVER_PORT must be at least 1024")
    private Integer serverPort;

    // ========================================
    // EXTERNAL SERVICES CONFIGURATION
    // ========================================

    @Value("${IDENTITY_SERVICE_URL:http://localhost:8081}")
    @NotBlank(message = "IDENTITY_SERVICE_URL environment variable is required")
    @Pattern(regexp = "^https?://.*", message = "IDENTITY_SERVICE_URL must be a valid HTTP or HTTPS URL")
    private String identityServiceUrl;

    @Value("${IDENTITY_SERVICE_CONNECT_TIMEOUT:5000}")
    @NotNull(message = "IDENTITY_SERVICE_CONNECT_TIMEOUT environment variable is required")
    @Min(value = 1000, message = "IDENTITY_SERVICE_CONNECT_TIMEOUT must be at least 1000ms")
    private Integer identityServiceConnectTimeout;

    @Value("${IDENTITY_SERVICE_READ_TIMEOUT:10000}")
    @NotNull(message = "IDENTITY_SERVICE_READ_TIMEOUT environment variable is required")
    @Min(value = 1000, message = "IDENTITY_SERVICE_READ_TIMEOUT must be at least 1000ms")
    private Integer identityServiceReadTimeout;

    // Optional API key for identity service
    @Value("${IDENTITY_SERVICE_API_KEY:}")
    private String identityServiceApiKey;

    // ========================================
    // OPTIONAL CONFIGURATION WITH DEFAULTS
    // ========================================

    private String appVersion = "1.0.0";
    private String logLevel = "INFO";
    private String feignLogLevel = "DEBUG";
    private Boolean rateLimitEnabled = false;
    private Boolean rateLimitUseRedis = false;
    private Integer rateLimitPublic = 100;
    private Integer rateLimitAuthenticated = 1000;
    private Integer rateLimitAdmin = 10000;
    private Integer rateLimitWebsocket = 60;
    private Double tracingSamplingProbability = 1.0;
    private String zipkinEndpoint = "http://localhost:9411/api/v2/spans";

    // ========================================
    // FEATURE FLAGS
    // ========================================

    private Boolean forumEnabled = false;
    private Boolean buddyEnabled = false;
    private Boolean analyticsEnabled = false;
    private Boolean authenticationEnabled = false;
    private Boolean authControllerEnabled = false;
    private Boolean redisEnabled = false;
    private Boolean healthEnabled = false;

    /**
     * Validates the environment configuration after properties are loaded
     * 
     * @throws IllegalStateException if any required configuration is missing or invalid
     */
    @PostConstruct
    public void validateEnvironment() {
        logger.info("🔍 Validating FocusHive Backend environment configuration...");

        try {
            // Additional custom validations beyond annotations
            validateJwtSecret();
            validateDatabaseConfiguration();
            validateRedisConfiguration();
            validateExternalServiceConfiguration();
            validateFeatureConfiguration();

            logValidatedConfiguration();
            
            logger.info("✅ Environment validation successful - FocusHive Backend ready to start");
        } catch (Exception e) {
            logger.error("❌ Environment validation failed: {}", e.getMessage());
            logger.error("📖 Please check your environment variables and try again");
            throw new IllegalStateException("Environment validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validates JWT secret meets security requirements
     */
    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters long for security");
        }
    }

    /**
     * Validates database configuration consistency
     */
    private void validateDatabaseConfiguration() {
        // Check if database URL matches driver
        if (databaseDriver != null && databaseUrl != null) {
            boolean isPostgres = databaseDriver.contains("postgresql");
            boolean isH2 = databaseDriver.contains("h2");
            boolean urlMatchesDriver = 
                (isPostgres && databaseUrl.contains("postgresql")) ||
                (isH2 && databaseUrl.contains("h2"));
            
            if (!urlMatchesDriver) {
                throw new IllegalArgumentException("DATABASE_URL must match DATABASE_DRIVER (PostgreSQL or H2)");
            }
        }
    }

    /**
     * Validates Redis configuration consistency
     */
    private void validateRedisConfiguration() {
        if (redisPort != null && (redisPort < 1 || redisPort > 65535)) {
            throw new IllegalArgumentException("REDIS_PORT must be between 1 and 65535");
        }

        // Warn if Redis is disabled but rate limiting tries to use it
        if (Boolean.TRUE.equals(rateLimitUseRedis) && Boolean.FALSE.equals(redisEnabled)) {
            logger.warn("⚠️  Rate limiting configured to use Redis but Redis is disabled");
        }
    }

    /**
     * Validates external service configuration
     */
    private void validateExternalServiceConfiguration() {
        // Validate timeout values are reasonable
        if (identityServiceConnectTimeout != null && identityServiceConnectTimeout > 30000) {
            logger.warn("⚠️  IDENTITY_SERVICE_CONNECT_TIMEOUT is very high ({}ms)", identityServiceConnectTimeout);
        }

        if (identityServiceReadTimeout != null && identityServiceReadTimeout > 60000) {
            logger.warn("⚠️  IDENTITY_SERVICE_READ_TIMEOUT is very high ({}ms)", identityServiceReadTimeout);
        }
    }

    /**
     * Validates feature configuration consistency
     */
    private void validateFeatureConfiguration() {
        // Warn about feature dependencies
        if (Boolean.TRUE.equals(authControllerEnabled) && Boolean.FALSE.equals(authenticationEnabled)) {
            logger.warn("⚠️  Auth controller enabled but authentication feature is disabled");
        }

        if (Boolean.TRUE.equals(rateLimitUseRedis) && Boolean.FALSE.equals(redisEnabled)) {
            logger.warn("⚠️  Rate limiting configured to use Redis but Redis feature is disabled");
        }
    }

    /**
     * Logs the validated configuration (excluding sensitive values)
     */
    private void logValidatedConfiguration() {
        logger.info("📋 FocusHive Backend Configuration Summary:");
        logger.info("  Application Version: {}", appVersion);
        logger.info("  Server Port: {}", serverPort);
        logger.info("  Database: {} (Driver: {})", 
                   databaseUrl != null ? databaseUrl.replaceAll("password=[^&;]*", "password=***") : "Not set",
                   databaseDriver);
        logger.info("  Redis: {}:{}", redisHost, redisPort);
        logger.info("  Identity Service: {}", identityServiceUrl);
        logger.info("  Identity Service Timeouts: Connect={}ms, Read={}ms", 
                   identityServiceConnectTimeout, identityServiceReadTimeout);
        logger.info("  JWT Expiration: Access={}ms, Refresh={}ms", jwtExpiration, jwtRefreshExpiration);
        logger.info("  Rate Limiting: {} (Redis: {})", 
                   rateLimitEnabled ? "Enabled" : "Disabled",
                   rateLimitUseRedis ? "Enabled" : "Disabled");
        logger.info("  Log Level: {} (Feign: {})", logLevel, feignLogLevel);
        logger.info("  Tracing: {} (Zipkin: {})", 
                   tracingSamplingProbability, 
                   zipkinEndpoint != null ? zipkinEndpoint : "Disabled");
        
        // Feature flags summary
        logger.info("  Feature Flags:");
        logger.info("    Forum: {}", forumEnabled ? "Enabled" : "Disabled");
        logger.info("    Buddy System: {}", buddyEnabled ? "Enabled" : "Disabled");
        logger.info("    Analytics: {}", analyticsEnabled ? "Enabled" : "Disabled");
        logger.info("    Authentication: {}", authenticationEnabled ? "Enabled" : "Disabled");
        logger.info("    Auth Controller: {}", authControllerEnabled ? "Enabled" : "Disabled");
        logger.info("    Redis: {}", redisEnabled ? "Enabled" : "Disabled");
        logger.info("    Health Checks: {}", healthEnabled ? "Enabled" : "Disabled");
        
        // Log security status without revealing sensitive values
        logger.info("  Security Configuration: ✅ All secrets configured");
        logger.info("  JWT Secret: ✅ {} characters", jwtSecret != null ? jwtSecret.length() : 0);
        logger.info("  Database Password: ✅ Configured");
        logger.info("  Redis Password: ✅ Configured");
        if (identityServiceApiKey != null && !identityServiceApiKey.isEmpty()) {
            logger.info("  Identity Service API Key: ✅ Configured");
        }
    }

    // ========================================
    // GETTERS AND SETTERS
    // ========================================

    public String getDatabaseUrl() { return databaseUrl; }
    public void setDatabaseUrl(String databaseUrl) { this.databaseUrl = databaseUrl; }

    public String getDatabaseUsername() { return databaseUsername; }
    public void setDatabaseUsername(String databaseUsername) { this.databaseUsername = databaseUsername; }

    public String getDatabasePassword() { return databasePassword; }
    public void setDatabasePassword(String databasePassword) { this.databasePassword = databasePassword; }

    public String getDatabaseDriver() { return databaseDriver; }
    public void setDatabaseDriver(String databaseDriver) { this.databaseDriver = databaseDriver; }

    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public Integer getRedisPort() { return redisPort; }
    public void setRedisPort(Integer redisPort) { this.redisPort = redisPort; }

    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public Long getJwtExpiration() { return jwtExpiration; }
    public void setJwtExpiration(Long jwtExpiration) { this.jwtExpiration = jwtExpiration; }

    public Long getJwtRefreshExpiration() { return jwtRefreshExpiration; }
    public void setJwtRefreshExpiration(Long jwtRefreshExpiration) { 
        this.jwtRefreshExpiration = jwtRefreshExpiration; 
    }

    public Integer getServerPort() { return serverPort; }
    public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }

    public String getIdentityServiceUrl() { return identityServiceUrl; }
    public void setIdentityServiceUrl(String identityServiceUrl) { 
        this.identityServiceUrl = identityServiceUrl; 
    }

    public Integer getIdentityServiceConnectTimeout() { return identityServiceConnectTimeout; }
    public void setIdentityServiceConnectTimeout(Integer identityServiceConnectTimeout) { 
        this.identityServiceConnectTimeout = identityServiceConnectTimeout; 
    }

    public Integer getIdentityServiceReadTimeout() { return identityServiceReadTimeout; }
    public void setIdentityServiceReadTimeout(Integer identityServiceReadTimeout) { 
        this.identityServiceReadTimeout = identityServiceReadTimeout; 
    }

    public String getIdentityServiceApiKey() { return identityServiceApiKey; }
    public void setIdentityServiceApiKey(String identityServiceApiKey) { 
        this.identityServiceApiKey = identityServiceApiKey; 
    }

    // Optional configuration getters/setters
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getFeignLogLevel() { return feignLogLevel; }
    public void setFeignLogLevel(String feignLogLevel) { this.feignLogLevel = feignLogLevel; }

    public Boolean getRateLimitEnabled() { return rateLimitEnabled; }
    public void setRateLimitEnabled(Boolean rateLimitEnabled) { this.rateLimitEnabled = rateLimitEnabled; }

    public Boolean getRateLimitUseRedis() { return rateLimitUseRedis; }
    public void setRateLimitUseRedis(Boolean rateLimitUseRedis) { this.rateLimitUseRedis = rateLimitUseRedis; }

    // Feature flag getters/setters
    public Boolean getForumEnabled() { return forumEnabled; }
    public void setForumEnabled(Boolean forumEnabled) { this.forumEnabled = forumEnabled; }

    public Boolean getBuddyEnabled() { return buddyEnabled; }
    public void setBuddyEnabled(Boolean buddyEnabled) { this.buddyEnabled = buddyEnabled; }

    public Boolean getAnalyticsEnabled() { return analyticsEnabled; }
    public void setAnalyticsEnabled(Boolean analyticsEnabled) { this.analyticsEnabled = analyticsEnabled; }

    public Boolean getAuthenticationEnabled() { return authenticationEnabled; }
    public void setAuthenticationEnabled(Boolean authenticationEnabled) { 
        this.authenticationEnabled = authenticationEnabled; 
    }

    public Boolean getAuthControllerEnabled() { return authControllerEnabled; }
    public void setAuthControllerEnabled(Boolean authControllerEnabled) { 
        this.authControllerEnabled = authControllerEnabled; 
    }

    public Boolean getRedisEnabled() { return redisEnabled; }
    public void setRedisEnabled(Boolean redisEnabled) { this.redisEnabled = redisEnabled; }

    public Boolean getHealthEnabled() { return healthEnabled; }
    public void setHealthEnabled(Boolean healthEnabled) { this.healthEnabled = healthEnabled; }

    // Rate limiting configuration getters/setters
    public Integer getRateLimitPublic() { return rateLimitPublic; }
    public void setRateLimitPublic(Integer rateLimitPublic) { this.rateLimitPublic = rateLimitPublic; }

    public Integer getRateLimitAuthenticated() { return rateLimitAuthenticated; }
    public void setRateLimitAuthenticated(Integer rateLimitAuthenticated) { 
        this.rateLimitAuthenticated = rateLimitAuthenticated; 
    }

    public Integer getRateLimitAdmin() { return rateLimitAdmin; }
    public void setRateLimitAdmin(Integer rateLimitAdmin) { this.rateLimitAdmin = rateLimitAdmin; }

    public Integer getRateLimitWebsocket() { return rateLimitWebsocket; }
    public void setRateLimitWebsocket(Integer rateLimitWebsocket) { 
        this.rateLimitWebsocket = rateLimitWebsocket; 
    }

    public Double getTracingSamplingProbability() { return tracingSamplingProbability; }
    public void setTracingSamplingProbability(Double tracingSamplingProbability) { 
        this.tracingSamplingProbability = tracingSamplingProbability; 
    }

    public String getZipkinEndpoint() { return zipkinEndpoint; }
    public void setZipkinEndpoint(String zipkinEndpoint) { this.zipkinEndpoint = zipkinEndpoint; }
}