package com.focushive.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Security configuration properties for the Notification Service.
 * Centralizes all security-related configuration parameters for easy management and tuning.
 * All properties can be overridden via application properties or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "notification.security")
@Data
public class SecurityProperties {

    /**
     * Authentication configuration.
     */
    private AuthenticationConfig authentication = new AuthenticationConfig();

    /**
     * Rate limiting configuration.
     */
    private RateLimitingConfig rateLimiting = new RateLimitingConfig();

    /**
     * JWT token configuration.
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * Security headers configuration.
     */
    private HeadersConfig headers = new HeadersConfig();

    /**
     * Audit logging configuration.
     */
    private AuditConfig audit = new AuditConfig();

    /**
     * CORS configuration.
     */
    private CorsConfig cors = new CorsConfig();

    /**
     * Authentication-related configuration.
     */
    @Data
    public static class AuthenticationConfig {
        /**
         * Maximum number of failed authentication attempts before lockout.
         * Default: 5 attempts
         */
        private int maxFailedAttempts = 5;

        /**
         * Duration for which an account/IP is locked after max failed attempts.
         * Default: 15 minutes
         */
        private Duration lockoutDuration = Duration.ofMinutes(15);

        /**
         * Whether to track failed attempts by IP address.
         * Default: true
         */
        private boolean trackByIp = true;

        /**
         * Whether to track failed attempts by username.
         * Default: true
         */
        private boolean trackByUsername = true;

        /**
         * Time window for counting failed attempts.
         * Default: 5 minutes
         */
        private Duration failedAttemptsWindow = Duration.ofMinutes(5);

        /**
         * Whether to automatically unlock accounts after lockout duration.
         * Default: true
         */
        private boolean autoUnlock = true;
    }

    /**
     * Rate limiting configuration.
     */
    @Data
    public static class RateLimitingConfig {
        /**
         * Whether rate limiting is enabled.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Default requests per minute for authenticated users.
         * Default: 60
         */
        private int defaultRequestsPerMinute = 60;

        /**
         * Requests per minute for anonymous users.
         * Default: 20
         */
        private int anonymousRequestsPerMinute = 20;

        /**
         * Requests per minute for admin users.
         * Default: 300
         */
        private int adminRequestsPerMinute = 300;

        /**
         * Burst capacity for handling temporary spikes.
         * Default: 10
         */
        private int burstCapacity = 10;

        /**
         * Cache TTL for rate limit counters.
         * Default: 1 minute
         */
        private Duration cacheTtl = Duration.ofMinutes(1);

        /**
         * Endpoints to exclude from rate limiting.
         */
        private List<String> excludedPaths = List.of(
            "/health",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
        );
    }

    /**
     * JWT token configuration.
     */
    @Data
    public static class JwtConfig {
        /**
         * JWT token expiration time.
         * Default: 1 hour
         */
        private Duration accessTokenExpiration = Duration.ofHours(1);

        /**
         * Refresh token expiration time.
         * Default: 7 days
         */
        private Duration refreshTokenExpiration = Duration.ofDays(7);

        /**
         * Whether to validate token issuer.
         * Default: true
         */
        private boolean validateIssuer = true;

        /**
         * Whether to validate token audience.
         * Default: true
         */
        private boolean validateAudience = true;

        /**
         * Clock skew tolerance for token validation.
         * Default: 30 seconds
         */
        private Duration clockSkew = Duration.ofSeconds(30);

        /**
         * Whether to enable token blacklisting for logout.
         * Default: true
         */
        private boolean blacklistingEnabled = true;

        /**
         * TTL for blacklisted tokens in cache.
         * Should be at least as long as access token expiration.
         * Default: 2 hours
         */
        private Duration blacklistTtl = Duration.ofHours(2);
    }

    /**
     * Security headers configuration.
     */
    @Data
    public static class HeadersConfig {
        /**
         * X-Frame-Options header value.
         * Default: DENY
         */
        private String frameOptions = "DENY";

        /**
         * X-Content-Type-Options header value.
         * Default: nosniff
         */
        private String contentTypeOptions = "nosniff";

        /**
         * X-XSS-Protection header value.
         * Default: 1; mode=block
         */
        private String xssProtection = "1; mode=block";

        /**
         * Referrer-Policy header value.
         * Default: strict-origin-when-cross-origin
         */
        private String referrerPolicy = "strict-origin-when-cross-origin";

        /**
         * Content-Security-Policy header value.
         * Default: default-src 'self'
         */
        private String contentSecurityPolicy = "default-src 'self'";

        /**
         * Strict-Transport-Security header value.
         * Default: max-age=31536000; includeSubDomains
         */
        private String strictTransportSecurity = "max-age=31536000; includeSubDomains";

        /**
         * Whether to add security headers.
         * Default: true
         */
        private boolean enabled = true;
    }

    /**
     * Audit logging configuration.
     */
    @Data
    public static class AuditConfig {
        /**
         * Whether audit logging is enabled.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Whether to log successful authentication attempts.
         * Default: true
         */
        private boolean logSuccessfulAuth = true;

        /**
         * Whether to log failed authentication attempts.
         * Default: true
         */
        private boolean logFailedAuth = true;

        /**
         * Whether to log admin actions.
         * Default: true
         */
        private boolean logAdminActions = true;

        /**
         * Whether to log data access events.
         * Default: false (for performance)
         */
        private boolean logDataAccess = false;

        /**
         * Whether to include request body in audit logs.
         * Default: false (may contain sensitive data)
         */
        private boolean includeRequestBody = false;

        /**
         * Whether to include response body in audit logs.
         * Default: false (may contain sensitive data)
         */
        private boolean includeResponseBody = false;

        /**
         * Maximum days to retain audit logs.
         * Default: 90 days
         */
        private int retentionDays = 90;
    }

    /**
     * CORS configuration.
     */
    @Data
    public static class CorsConfig {
        /**
         * Whether CORS is enabled.
         * Default: true
         */
        private boolean enabled = true;

        /**
         * Allowed origins for CORS.
         */
        private List<String> allowedOrigins = List.of(
            "http://localhost:3000",
            "http://localhost:5173"
        );

        /**
         * Allowed HTTP methods.
         */
        private List<String> allowedMethods = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        );

        /**
         * Allowed headers.
         */
        private List<String> allowedHeaders = List.of(
            "Content-Type", "Authorization", "X-Requested-With",
            "Accept", "X-Correlation-Id"
        );

        /**
         * Exposed headers.
         */
        private List<String> exposedHeaders = List.of(
            "X-Total-Count", "X-Correlation-Id"
        );

        /**
         * Whether to allow credentials.
         * Default: true
         */
        private boolean allowCredentials = true;

        /**
         * Max age for preflight cache.
         * Default: 3600 seconds (1 hour)
         */
        private long maxAge = 3600;
    }
}