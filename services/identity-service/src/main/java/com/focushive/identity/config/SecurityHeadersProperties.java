package com.focushive.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
 *           - "connect-src 'self' https://api.focushive.com"
 * </pre>
 * 
 * @author FocusHive Security Team
 * @version 1.0
 * @since 2024-12-12
 */
@Configuration
@ConfigurationProperties(prefix = "focushive.security.headers")
public class SecurityHeadersProperties {

    /**
     * Enable or disable all security headers globally.
     */
    private boolean enabled = true;

    /**
     * Environment-specific configuration mode.
     * Values: AUTO (based on spring.profiles.active), DEVELOPMENT, PRODUCTION
     */
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

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public SecurityMode getMode() { return mode; }
    public void setMode(SecurityMode mode) { this.mode = mode; }

    public Hsts getHsts() { return hsts; }
    public ContentSecurityPolicy getCsp() { return csp; }
    public FrameOptions getFrameOptions() { return frameOptions; }
    public PermissionsPolicy getPermissionsPolicy() { return permissionsPolicy; }
    public AdditionalHeaders getAdditionalHeaders() { return additionalHeaders; }

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
    public static class ContentSecurityPolicy {
        private boolean enabled = true;
        private boolean reportOnly = false;
        private String reportUri;
        private List<String> customDirectives = new ArrayList<>();
        
        // Environment-specific CSP directives
        private final Environment development = new Environment();
        private final Environment production = new Environment();

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isReportOnly() { return reportOnly; }
        public void setReportOnly(boolean reportOnly) { this.reportOnly = reportOnly; }

        public String getReportUri() { return reportUri; }
        public void setReportUri(String reportUri) { this.reportUri = reportUri; }

        public List<String> getCustomDirectives() { return customDirectives; }
        public void setCustomDirectives(List<String> customDirectives) { this.customDirectives = customDirectives; }

        public Environment getDevelopment() { return development; }
        public Environment getProduction() { return production; }

        /**
         * Environment-specific CSP configuration.
         */
        public static class Environment {
            private List<String> scriptSrc = new ArrayList<>();
            private List<String> styleSrc = new ArrayList<>();
            private List<String> imgSrc = new ArrayList<>();
            private List<String> connectSrc = new ArrayList<>();
            private List<String> fontSrc = new ArrayList<>();
            private List<String> objectSrc = List.of("'none'");
            private List<String> frameSrc = new ArrayList<>();

            // Getters and setters
            public List<String> getScriptSrc() { return scriptSrc; }
            public void setScriptSrc(List<String> scriptSrc) { this.scriptSrc = scriptSrc; }

            public List<String> getStyleSrc() { return styleSrc; }
            public void setStyleSrc(List<String> styleSrc) { this.styleSrc = styleSrc; }

            public List<String> getImgSrc() { return imgSrc; }
            public void setImgSrc(List<String> imgSrc) { this.imgSrc = imgSrc; }

            public List<String> getConnectSrc() { return connectSrc; }
            public void setConnectSrc(List<String> connectSrc) { this.connectSrc = connectSrc; }

            public List<String> getFontSrc() { return fontSrc; }
            public void setFontSrc(List<String> fontSrc) { this.fontSrc = fontSrc; }

            public List<String> getObjectSrc() { return objectSrc; }
            public void setObjectSrc(List<String> objectSrc) { this.objectSrc = objectSrc; }

            public List<String> getFrameSrc() { return frameSrc; }
            public void setFrameSrc(List<String> frameSrc) { this.frameSrc = frameSrc; }
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
            switch (policy) {
                case DENY:
                    return "DENY";
                case SAMEORIGIN:
                    return "SAMEORIGIN";
                case ALLOW_FROM:
                    return "ALLOW-FROM " + String.join(" ", allowFrom);
                default:
                    return "DENY";
            }
        }
    }

    /**
     * Permissions Policy configuration.
     */
    public static class PermissionsPolicy {
        private boolean enabled = true;
        private List<String> disabledFeatures = new ArrayList<>(List.of(
            "camera", "microphone", "geolocation", "interest-cohort",
            "payment", "usb", "serial", "bluetooth"
        ));
        private final List<String> allowedFeatures = new ArrayList<>();
        private final List<String> customDirectives = new ArrayList<>();

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<String> getDisabledFeatures() { return disabledFeatures; }
        public void setDisabledFeatures(List<String> disabledFeatures) { 
            this.disabledFeatures = disabledFeatures != null ? disabledFeatures : new ArrayList<>(); 
        }
        
        public List<String> getAllowedFeatures() { return allowedFeatures; }
        public List<String> getCustomDirectives() { return customDirectives; }
    }

    /**
     * Additional security headers configuration.
     */
    public static class AdditionalHeaders {
        private boolean xContentTypeOptions = true;
        private boolean xXssProtection = true;
        private boolean xPermittedCrossDomainPolicies = true;
        
        @NotBlank
        private String referrerPolicy = "strict-origin-when-cross-origin";
        
        private boolean crossOriginEmbedderPolicy = false; // Can break iframe compatibility
        private boolean crossOriginOpenerPolicy = false;   // Can break popup functionality
        private boolean crossOriginResourcePolicy = false; // Can break resource loading

        // Getters and setters
        public boolean isXContentTypeOptions() { return xContentTypeOptions; }
        public void setXContentTypeOptions(boolean xContentTypeOptions) { 
            this.xContentTypeOptions = xContentTypeOptions; 
        }

        public boolean isXXssProtection() { return xXssProtection; }
        public void setXXssProtection(boolean xXssProtection) { this.xXssProtection = xXssProtection; }

        public boolean isXPermittedCrossDomainPolicies() { return xPermittedCrossDomainPolicies; }
        public void setXPermittedCrossDomainPolicies(boolean xPermittedCrossDomainPolicies) { 
            this.xPermittedCrossDomainPolicies = xPermittedCrossDomainPolicies; 
        }

        public String getReferrerPolicy() { return referrerPolicy; }
        public void setReferrerPolicy(String referrerPolicy) { this.referrerPolicy = referrerPolicy; }

        public boolean isCrossOriginEmbedderPolicy() { return crossOriginEmbedderPolicy; }
        public void setCrossOriginEmbedderPolicy(boolean crossOriginEmbedderPolicy) { 
            this.crossOriginEmbedderPolicy = crossOriginEmbedderPolicy; 
        }

        public boolean isCrossOriginOpenerPolicy() { return crossOriginOpenerPolicy; }
        public void setCrossOriginOpenerPolicy(boolean crossOriginOpenerPolicy) { 
            this.crossOriginOpenerPolicy = crossOriginOpenerPolicy; 
        }

        public boolean isCrossOriginResourcePolicy() { return crossOriginResourcePolicy; }
        public void setCrossOriginResourcePolicy(boolean crossOriginResourcePolicy) { 
            this.crossOriginResourcePolicy = crossOriginResourcePolicy; 
        }
    }
}