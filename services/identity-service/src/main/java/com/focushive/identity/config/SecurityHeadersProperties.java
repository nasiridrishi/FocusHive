package com.focushive.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for security headers.
 * Allows customization of security headers through application.yml properties.
 *
 * Usage in application.yml:
 * <pre>
 * focushive:
 *   security:
 *     headers:
 *       hsts:
 *         enabled: true
 *         max-age: 31536000
 *       csp:
 *         enabled: true
 *         report-only: false
 *         custom-directives:
 *           - "connect-src 'self' <a href="https://api.focushive.com">...</a>"
 * </pre>
 *
 * @author FocusHive Security Team
 * @version 1.0
 * @since 2024-12-12
 */
@Getter
@Component
@ConfigurationProperties(prefix = "focushive.security.headers")
public class SecurityHeadersProperties {

    // Getters and setters
    /**
     * Enable or disable all security headers globally.
     */
    @Setter
    private boolean enabled = true;

    /**
     * Environment-specific configuration mode.
     * Values: AUTO (based on spring.profiles.active), DEVELOPMENT, PRODUCTION
     */
    @Setter
    private SecurityMode mode = SecurityMode.AUTO;

    /**
     * HTTP Strict Transport Security (HSTS) configuration.
     */
    private final Hsts hsts = new Hsts();

    /**
     * Content Security Policy (CSP) configuration.
     */
    private final ContentSecurityPolicy csp = new ContentSecurityPolicy();

    /**
     * Frame options configuration.
     */
    private final FrameOptions frameOptions = new FrameOptions();

    /**
     * Permissions Policy configuration.
     */
    private final PermissionsPolicy permissionsPolicy = new PermissionsPolicy();

    /**
     * Additional security headers configuration.
     */
    private final AdditionalHeaders additionalHeaders = new AdditionalHeaders();

    public enum SecurityMode {
        AUTO, DEVELOPMENT, PRODUCTION
    }

    /**
     * HTTP Strict Transport Security configuration.
     */
    public static class Hsts {
        private boolean enabled = true;
        
        @Min(value = 1, message = "HSTS max-age must be at least 1 second")
        private long maxAge = 31536000; // 1 year in seconds
        
        private boolean includeSubdomains = true;
        private boolean preload = true;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }

        public boolean isIncludeSubdomains() { return includeSubdomains; }
        public void setIncludeSubdomains(boolean includeSubdomains) { this.includeSubdomains = includeSubdomains; }

        public boolean isPreload() { return preload; }
        public void setPreload(boolean preload) { this.preload = preload; }

        public String buildHeaderValue() {
            StringBuilder builder = new StringBuilder();
            builder.append("max-age=").append(maxAge);
            if (includeSubdomains) {
                builder.append("; includeSubDomains");
            }
            if (preload) {
                builder.append("; preload");
            }
            return builder.toString();
        }
    }

    /**
     * Content Security Policy configuration.
     */
    @Getter
    public static class ContentSecurityPolicy {
        // Getters and setters
        @Setter
        private boolean enabled = true;
        @Setter
        private boolean reportOnly = false;
        @Setter
        private String reportUri;
        @Setter
        private List<String> customDirectives = new ArrayList<>();
        
        // Environment-specific CSP directives
        private final Environment development = new Environment();
        private final Environment production = new Environment();

        /**
         * Environment-specific CSP configuration.
         */
        @Setter
        @Getter
        public static class Environment {
            // Getters and setters
            private List<String> scriptSrc = new ArrayList<>();
            private List<String> styleSrc = new ArrayList<>();
            private List<String> imgSrc = new ArrayList<>();
            private List<String> connectSrc = new ArrayList<>();
            private List<String> fontSrc = new ArrayList<>();
            private List<String> objectSrc = List.of("'none'");
            private List<String> frameSrc = new ArrayList<>();

        }
    }

    /**
     * Frame options configuration.
     */
    public static class FrameOptions {
        private boolean enabled = true;
        private FramePolicy policy = FramePolicy.DENY;
        private List<String> allowFrom = new ArrayList<>();

        public enum FramePolicy {
            DENY, SAMEORIGIN, ALLOW_FROM
        }

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public FramePolicy getPolicy() { return policy; }
        public void setPolicy(FramePolicy policy) { this.policy = policy; }

        public List<String> getAllowFrom() { return allowFrom; }
        public void setAllowFrom(List<String> allowFrom) { this.allowFrom = allowFrom; }

        public String buildHeaderValue() {
            return switch (policy) {
                case SAMEORIGIN -> "SAMEORIGIN";
                case ALLOW_FROM -> "ALLOW-FROM " + String.join(" ", allowFrom);
                default -> "DENY";
            };
        }
    }

    /**
     * Permissions Policy configuration.
     */
    @Getter
    public static class PermissionsPolicy {
        // Getters and setters
        @Setter
        private boolean enabled = true;
        private List<String> disabledFeatures = new ArrayList<>(List.of(
            "camera", "microphone", "geolocation", "interest-cohort",
            "payment", "usb", "serial", "bluetooth"
        ));
        private final List<String> allowedFeatures = new ArrayList<>();
        private final List<String> customDirectives = new ArrayList<>();

        public void setDisabledFeatures(List<String> disabledFeatures) {
            this.disabledFeatures = disabledFeatures != null ? disabledFeatures : new ArrayList<>(); 
        }

    }

    /**
     * Additional security headers configuration.
     */
    @Setter
    @Getter
    public static class AdditionalHeaders {
        // Getters and setters
        private boolean xContentTypeOptions = true;
        private boolean xXssProtection = true;
        private boolean xPermittedCrossDomainPolicies = true;
        
        @NotBlank
        private String referrerPolicy = "strict-origin-when-cross-origin";
        
        private boolean crossOriginEmbedderPolicy = false; // Can break iframe compatibility
        private boolean crossOriginOpenerPolicy = false;   // Can break popup functionality
        private boolean crossOriginResourcePolicy = false; // Can break resource loading

    }
}