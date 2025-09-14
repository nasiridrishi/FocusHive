package com.focushive.identity.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Environment Configuration Validation for Identity Service
 * 
 * This component validates all required environment variables at application startup
 * and provides clear error messages for missing or invalid configuration.
 * 
 * Features:
 * - Comprehensive validation of all required environment variables
 * - Clear error messages for missing/invalid values
 * - Fail-fast startup behavior to prevent runtime issues
 * - Secure logging that excludes sensitive configuration values
 * - Validation of database, Redis, JWT, and security configurations
 */
@Component
@Validated
@Profile("!test")
public class EnvironmentConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

    // ========================================
    // DATABASE CONFIGURATION
    // ========================================

    @Value("${DB_HOST}")
    @NotBlank(message = "DB_HOST environment variable is required")
    @Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "DB_HOST must be a valid hostname or IP address")
    private String dbHost;

    @Value("${DB_PORT}")
    @NotNull(message = "DB_PORT environment variable is required")
    @Min(value = 1, message = "DB_PORT must be a positive number")
    private Integer dbPort;

    @Value("${DB_NAME}")
    @NotBlank(message = "DB_NAME environment variable is required")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "DB_NAME must contain only alphanumeric characters and underscores")
    private String dbName;

    @Value("${DB_USER}")
    @NotBlank(message = "DB_USER environment variable is required")
    private String dbUser;

    @Value("${DB_PASSWORD}")
    @NotBlank(message = "DB_PASSWORD environment variable is required")
    private String dbPassword;

    // ========================================
    // REDIS CONFIGURATION
    // ========================================

    @Value("${REDIS_HOST}")
    @NotBlank(message = "REDIS_HOST environment variable is required")
    @Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "REDIS_HOST must be a valid hostname or IP address")
    private String redisHost;

    @Value("${REDIS_PORT}")
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

    @Value("${JWT_ACCESS_TOKEN_EXPIRATION:3600000}")
    @NotNull(message = "JWT_ACCESS_TOKEN_EXPIRATION environment variable is required")
    @Min(value = 300000, message = "JWT_ACCESS_TOKEN_EXPIRATION must be at least 5 minutes (300000ms)")
    private Long jwtAccessTokenExpiration;

    @Value("${JWT_REFRESH_TOKEN_EXPIRATION:2592000000}")
    @NotNull(message = "JWT_REFRESH_TOKEN_EXPIRATION environment variable is required")
    @Min(value = 86400000, message = "JWT_REFRESH_TOKEN_EXPIRATION must be at least 1 day (86400000ms)")
    private Long jwtRefreshTokenExpiration;

    // ========================================
    // SECURITY CONFIGURATION
    // ========================================

    @Value("${KEY_STORE_PASSWORD}")
    @NotBlank(message = "KEY_STORE_PASSWORD environment variable is required")
    private String keyStorePassword;

    @Value("${PRIVATE_KEY_PASSWORD}")
    @NotBlank(message = "PRIVATE_KEY_PASSWORD environment variable is required")
    private String privateKeyPassword;

    @Value("${FOCUSHIVE_CLIENT_SECRET}")
    @NotBlank(message = "FOCUSHIVE_CLIENT_SECRET environment variable is required")
    private String focushiveClientSecret;

    @Value("${ENCRYPTION_MASTER_KEY}")
    @NotBlank(message = "ENCRYPTION_MASTER_KEY environment variable is required and must be at least 32 characters")
    private String encryptionMasterKey;

    // ========================================
    // SERVER CONFIGURATION
    // ========================================

    @Value("${SERVER_PORT:8081}")
    @NotNull(message = "SERVER_PORT environment variable is required")
    @Min(value = 1024, message = "SERVER_PORT must be at least 1024")
    private Integer serverPort;

    @Value("${ISSUER_URI:http://localhost:8081}")
    @NotBlank(message = "ISSUER_URI environment variable is required")
    @Pattern(regexp = "^https?://.*", message = "ISSUER_URI must be a valid HTTP or HTTPS URL")
    private String issuerUri;

    // ========================================
    // CORS CONFIGURATION
    // ========================================

    @Value("${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}")
    private String corsOrigins;

    // ========================================
    // OPTIONAL CONFIGURATION WITH DEFAULTS
    // ========================================

    @Value("${LOG_LEVEL:INFO}")
    private String logLevel;

    @Value("${SECURITY_LOG_LEVEL:WARN}")
    private String securityLogLevel;

    @Value("${SQL_LOG_LEVEL:WARN}")
    private String sqlLogLevel;

    @Value("${RATE_LIMITING_ENABLED:true}")
    private Boolean rateLimitingEnabled;

    @Value("${SECURITY_HEADERS_ENABLED:true}")
    private Boolean securityHeadersEnabled;

    /**
     * Validates the environment configuration after properties are loaded
     * 
     * @throws IllegalStateException if any required configuration is missing or invalid
     */
    @PostConstruct
    public void validateEnvironment() {
        logger.info("🔍 Validating Identity Service environment configuration...");

        try {
            // Additional custom validations beyond annotations
            validateJwtSecret();
            validateEncryptionMasterKey();
            validateDatabaseConfiguration();
            validateRedisConfiguration();
            validateSecurityConfiguration();

            logValidatedConfiguration();
            
            logger.info("✅ Environment validation successful - Identity Service ready to start");
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
     * Validates encryption master key meets security requirements
     */
    private void validateEncryptionMasterKey() {
        if (encryptionMasterKey == null || encryptionMasterKey.length() < 32) {
            throw new IllegalArgumentException("ENCRYPTION_MASTER_KEY must be at least 32 characters long");
        }
    }

    /**
     * Validates database configuration consistency
     */
    private void validateDatabaseConfiguration() {
        if (dbPort != null && (dbPort < 1 || dbPort > 65535)) {
            throw new IllegalArgumentException("DB_PORT must be between 1 and 65535");
        }
    }

    /**
     * Validates Redis configuration consistency
     */
    private void validateRedisConfiguration() {
        if (redisPort != null && (redisPort < 1 || redisPort > 65535)) {
            throw new IllegalArgumentException("REDIS_PORT must be between 1 and 65535");
        }
    }

    /**
     * Validates security configuration requirements
     */
    private void validateSecurityConfiguration() {
        // Validate that passwords are not the same as default values
        if ("changeme".equals(keyStorePassword) || "changeme".equals(privateKeyPassword)) {
            throw new IllegalArgumentException("Security passwords cannot use default values in production");
        }
    }

    /**
     * Logs the validated configuration (excluding sensitive values)
     */
    private void logValidatedConfiguration() {
        logger.info("📋 Identity Service Configuration Summary:");
        logger.info("  Database: {}:{}/{}", dbHost, dbPort, dbName);
        logger.info("  Redis: {}:{}", redisHost, redisPort);
        logger.info("  Server Port: {}", serverPort);
        logger.info("  Issuer URI: {}", issuerUri);
        logger.info("  JWT Access Token Expiration: {}ms", jwtAccessTokenExpiration);
        logger.info("  JWT Refresh Token Expiration: {}ms", jwtRefreshTokenExpiration);
        logger.info("  Rate Limiting: {}", rateLimitingEnabled ? "Enabled" : "Disabled");
        logger.info("  Security Headers: {}", securityHeadersEnabled ? "Enabled" : "Disabled");
        logger.info("  Log Level: {}", logLevel);
        logger.info("  CORS Origins: {}", corsOrigins != null ? corsOrigins : "Default");
        
        // Log security status without revealing sensitive values
        logger.info("  Security Configuration: ✅ All secrets configured");
        logger.info("  JWT Secret: ✅ {} characters", jwtSecret != null ? jwtSecret.length() : 0);
        logger.info("  Encryption Key: ✅ {} characters", encryptionMasterKey != null ? encryptionMasterKey.length() : 0);
    }

    // ========================================
    // GETTERS AND SETTERS
    // ========================================

    public String getDbHost() { return dbHost; }
    public void setDbHost(String dbHost) { this.dbHost = dbHost; }

    public Integer getDbPort() { return dbPort; }
    public void setDbPort(Integer dbPort) { this.dbPort = dbPort; }

    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }

    public String getDbUser() { return dbUser; }
    public void setDbUser(String dbUser) { this.dbUser = dbUser; }

    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public Integer getRedisPort() { return redisPort; }
    public void setRedisPort(Integer redisPort) { this.redisPort = redisPort; }

    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public Long getJwtAccessTokenExpiration() { return jwtAccessTokenExpiration; }
    public void setJwtAccessTokenExpiration(Long jwtAccessTokenExpiration) { 
        this.jwtAccessTokenExpiration = jwtAccessTokenExpiration; 
    }

    public Long getJwtRefreshTokenExpiration() { return jwtRefreshTokenExpiration; }
    public void setJwtRefreshTokenExpiration(Long jwtRefreshTokenExpiration) { 
        this.jwtRefreshTokenExpiration = jwtRefreshTokenExpiration; 
    }

    public String getKeyStorePassword() { return keyStorePassword; }
    public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }

    public String getPrivateKeyPassword() { return privateKeyPassword; }
    public void setPrivateKeyPassword(String privateKeyPassword) { this.privateKeyPassword = privateKeyPassword; }

    public String getFocushiveClientSecret() { return focushiveClientSecret; }
    public void setFocushiveClientSecret(String focushiveClientSecret) { 
        this.focushiveClientSecret = focushiveClientSecret; 
    }

    public String getEncryptionMasterKey() { return encryptionMasterKey; }
    public void setEncryptionMasterKey(String encryptionMasterKey) { 
        this.encryptionMasterKey = encryptionMasterKey; 
    }

    public Integer getServerPort() { return serverPort; }
    public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }

    public String getIssuerUri() { return issuerUri; }
    public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }

    public String getCorsOrigins() { return corsOrigins; }
    public void setCorsOrigins(String corsOrigins) { this.corsOrigins = corsOrigins; }

    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }

    public String getSecurityLogLevel() { return securityLogLevel; }
    public void setSecurityLogLevel(String securityLogLevel) { this.securityLogLevel = securityLogLevel; }

    public String getSqlLogLevel() { return sqlLogLevel; }
    public void setSqlLogLevel(String sqlLogLevel) { this.sqlLogLevel = sqlLogLevel; }

    public Boolean getRateLimitingEnabled() { return rateLimitingEnabled; }
    public void setRateLimitingEnabled(Boolean rateLimitingEnabled) { 
        this.rateLimitingEnabled = rateLimitingEnabled; 
    }

    public Boolean getSecurityHeadersEnabled() { return securityHeadersEnabled; }
    public void setSecurityHeadersEnabled(Boolean securityHeadersEnabled) { 
        this.securityHeadersEnabled = securityHeadersEnabled; 
    }
}