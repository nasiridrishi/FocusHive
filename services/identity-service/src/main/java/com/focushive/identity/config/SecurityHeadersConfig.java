package com.focushive.identity.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comprehensive security headers configuration for the Identity Service.
 * Provides environment-aware security headers including CSP, HSTS, and anti-clickjacking protection.
 * 
 * This configuration implements OWASP security recommendations and can be reused across microservices.
 * Features:
 * - Environment-aware CSP (development vs production)
 * - Configurable security headers via application.yml
 * - HSTS with configurable max-age and subdomain inclusion
 * - Permissions Policy for modern browser feature control
 * - Anti-clickjacking protection with X-Frame-Options
 * 
 * @author FocusHive Security Team
 * @version 2.0
 * @since 2024-12-12
 */
@Configuration
@Profile("!test")
@EnableConfigurationProperties(SecurityHeadersProperties.class)
public class SecurityHeadersConfig {

    private final SecurityHeadersProperties properties;
    private final Environment environment;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Autowired
    public SecurityHeadersConfig(SecurityHeadersProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * Registers the security headers filter with high priority to ensure headers are applied
     * to all requests before other filters process them.
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("securityHeadersFilter");
        return registration;
    }

    /**
     * Security headers filter that adds comprehensive security headers to all HTTP responses.
     * Headers are configured based on the active Spring profile and SecurityHeadersProperties.
     */
    public class SecurityHeadersFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {

            // Skip security headers if disabled
            if (!properties.isEnabled()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Determine security mode
            SecurityHeadersProperties.SecurityMode mode = determineSecurityMode();

            // Apply security headers based on determined mode
            switch (mode) {
                case PRODUCTION:
                    applyProductionSecurityHeaders(response);
                    break;
                case DEVELOPMENT:
                    applyDevelopmentSecurityHeaders(response);
                    break;
                case AUTO:
                default:
                    if (isProductionProfile()) {
                        applyProductionSecurityHeaders(response);
                    } else {
                        applyDevelopmentSecurityHeaders(response);
                    }
                    break;
            }

            filterChain.doFilter(request, response);
        }

        /**
         * Determines the security mode based on configuration and active profiles.
         */
        private SecurityHeadersProperties.SecurityMode determineSecurityMode() {
            if (properties.getMode() != SecurityHeadersProperties.SecurityMode.AUTO) {
                return properties.getMode();
            }
            return SecurityHeadersProperties.SecurityMode.AUTO;
        }

        /**
         * Checks if the current profile is production-like.
         */
        private boolean isProductionProfile() {
            String[] activeProfiles = environment.getActiveProfiles();
            for (String profile : activeProfiles) {
                if ("prod".equals(profile) || "production".equals(profile)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Applies production-grade security headers with strict policies.
         */
        private void applyProductionSecurityHeaders(HttpServletResponse response) {
            // Content Security Policy
            if (properties.getCsp().isEnabled()) {
                String csp = buildCSP(true);
                String headerName = properties.getCsp().isReportOnly() ? 
                    "Content-Security-Policy-Report-Only" : "Content-Security-Policy";
                response.setHeader(headerName, csp);
            }

            // HTTP Strict Transport Security
            if (properties.getHsts().isEnabled() && sslEnabled) {
                response.setHeader("Strict-Transport-Security", properties.getHsts().buildHeaderValue());
            }

            // Frame Options
            if (properties.getFrameOptions().isEnabled()) {
                response.setHeader("X-Frame-Options", properties.getFrameOptions().buildHeaderValue());
            }

            // Content Type Options
            if (properties.getAdditionalHeaders().isXContentTypeOptions()) {
                response.setHeader("X-Content-Type-Options", "nosniff");
            }

            // XSS Protection
            if (properties.getAdditionalHeaders().isXXssProtection()) {
                response.setHeader("X-XSS-Protection", "1; mode=block");
            }

            // Referrer Policy
            response.setHeader("Referrer-Policy", properties.getAdditionalHeaders().getReferrerPolicy());

            // Permissions Policy
            if (properties.getPermissionsPolicy().isEnabled()) {
                String permissionsPolicy = buildPermissionsPolicy(true);
                response.setHeader("Permissions-Policy", permissionsPolicy);
            }

            // Cross-Origin Policies (only if enabled in properties)
            if (properties.getAdditionalHeaders().isCrossOriginEmbedderPolicy()) {
                response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
            }
            if (properties.getAdditionalHeaders().isCrossOriginOpenerPolicy()) {
                response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
            }
            if (properties.getAdditionalHeaders().isCrossOriginResourcePolicy()) {
                response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
            }

            // Additional security headers for production
            if (properties.getAdditionalHeaders().isXPermittedCrossDomainPolicies()) {
                response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
            }
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("X-Robots-Tag", "noindex, nofollow, nosnippet, noarchive");
        }

        /**
         * Applies development-friendly security headers with relaxed policies for local testing.
         */
        private void applyDevelopmentSecurityHeaders(HttpServletResponse response) {
            // Content Security Policy
            if (properties.getCsp().isEnabled()) {
                String csp = buildCSP(false);
                String headerName = properties.getCsp().isReportOnly() ? 
                    "Content-Security-Policy-Report-Only" : "Content-Security-Policy";
                response.setHeader(headerName, csp);
            }

            // Frame Options - More permissive for development
            if (properties.getFrameOptions().isEnabled()) {
                response.setHeader("X-Frame-Options", "SAMEORIGIN");
            }

            // Content Type Options
            if (properties.getAdditionalHeaders().isXContentTypeOptions()) {
                response.setHeader("X-Content-Type-Options", "nosniff");
            }

            // XSS Protection
            if (properties.getAdditionalHeaders().isXXssProtection()) {
                response.setHeader("X-XSS-Protection", "1; mode=block");
            }

            // Referrer Policy
            response.setHeader("Referrer-Policy", properties.getAdditionalHeaders().getReferrerPolicy());

            // Permissions Policy - More permissive for development
            if (properties.getPermissionsPolicy().isEnabled()) {
                String permissionsPolicy = buildPermissionsPolicy(false);
                response.setHeader("Permissions-Policy", permissionsPolicy);
            }

            // Development-specific headers
            response.setHeader("X-Development-Mode", "true");
        }

        /**
         * Builds Content Security Policy based on environment and configuration.
         * @param isProduction true for production environment, false for development
         */
        private String buildCSP(boolean isProduction) {
            List<String> directives = new java.util.ArrayList<>();

            if (isProduction) {
                // Production CSP - Strict policy
                directives.add("default-src 'self'");
                directives.add("script-src 'self'");
                directives.add("style-src 'self' 'unsafe-inline'"); // Allow inline CSS for frameworks
                directives.add("img-src 'self' data: https:");
                directives.add("font-src 'self' https: data:");
                directives.add("connect-src 'self' https: wss:");
                directives.add("media-src 'self'");
                directives.add("object-src 'none'");
                directives.add("child-src 'none'");
                directives.add("frame-src 'none'");
                directives.add("worker-src 'none'");
                directives.add("frame-ancestors 'none'");
                directives.add("form-action 'self'");
                directives.add("base-uri 'self'");
                directives.add("manifest-src 'self'");
                directives.add("upgrade-insecure-requests");
            } else {
                // Development CSP - Relaxed policy
                directives.add("default-src 'self' localhost:* 127.0.0.1:*");
                directives.add("script-src 'self' 'unsafe-eval' 'unsafe-inline' localhost:* 127.0.0.1:*");
                directives.add("style-src 'self' 'unsafe-inline' localhost:* 127.0.0.1:*");
                directives.add("img-src 'self' data: https: http: localhost:* 127.0.0.1:*");
                directives.add("font-src 'self' https: data: localhost:* 127.0.0.1:*");
                directives.add("connect-src 'self' https: http: ws: wss: localhost:* 127.0.0.1:*");
                directives.add("media-src 'self' localhost:* 127.0.0.1:*");
                directives.add("object-src 'none'");
                directives.add("frame-src 'self' localhost:* 127.0.0.1:*");
                directives.add("frame-ancestors 'self' localhost:* 127.0.0.1:*");
                directives.add("form-action 'self' localhost:* 127.0.0.1:*");
                directives.add("base-uri 'self'");
            }

            // Add custom directives from configuration
            directives.addAll(properties.getCsp().getCustomDirectives());

            // Add report-uri if configured
            if (properties.getCsp().getReportUri() != null && !properties.getCsp().getReportUri().isEmpty()) {
                directives.add("report-uri " + properties.getCsp().getReportUri());
            }

            return String.join("; ", directives);
        }

        /**
         * Builds Permissions Policy based on environment and configuration.
         * @param isProduction true for production environment, false for development
         */
        private String buildPermissionsPolicy(boolean isProduction) {
            List<String> directives = new java.util.ArrayList<>();

            // Add disabled features
            List<String> disabledFeatures = properties.getPermissionsPolicy().getDisabledFeatures();
            for (String feature : disabledFeatures) {
                directives.add(feature + "=()");
            }

            if (isProduction) {
                // Production - Strict permissions
                directives.addAll(List.of(
                    "autoplay=(self)",
                    "fullscreen=(self)",
                    "picture-in-picture=(self)",
                    "screen-wake-lock=()",
                    "web-share=(self)"
                ));
            } else {
                // Development - More permissive
                directives.addAll(List.of(
                    "autoplay=(self)",
                    "fullscreen=(self)",
                    "picture-in-picture=(self)",
                    "screen-wake-lock=(self)",
                    "web-share=(self)"
                ));
            }

            // Add custom directives from configuration
            directives.addAll(properties.getPermissionsPolicy().getCustomDirectives());

            return String.join(", ", directives);
        }
    }

}