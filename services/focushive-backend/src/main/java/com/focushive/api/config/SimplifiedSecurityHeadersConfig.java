package com.focushive.api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.HeaderWriter;

/**
 * Simplified Security Headers Configuration
 * Implements essential OWASP security headers
 */
@Configuration
public class SimplifiedSecurityHeadersConfig {

    /**
     * Configure essential security headers for HTTP responses
     */
    public static void configureSecurityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                // X-Content-Type-Options: Prevent MIME type sniffing
                .contentTypeOptions(contentType -> {})

                // X-Frame-Options: Prevent clickjacking
                .frameOptions(frame -> frame.deny())

                // Strict Transport Security (HTTPS)
                .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000) // 1 year
                        .includeSubDomains(true)
                        .preload(true)
                )

                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                        .policyDirectives(buildContentSecurityPolicy())
                )
        );

        // Add custom headers via custom header writer
        http.headers(headers -> headers.addHeaderWriter(new CustomSecurityHeaderWriter()));
    }

    /**
     * Build Content Security Policy directive
     */
    private static String buildContentSecurityPolicy() {
        return String.join("; ",
                "default-src 'self'",
                "script-src 'self'",
                "style-src 'self' 'unsafe-inline'", // Needed for some CSS frameworks
                "img-src 'self' data: https:",
                "font-src 'self' https:",
                "connect-src 'self'", // WebSocket connections
                "media-src 'none'",
                "object-src 'none'",
                "child-src 'none'",
                "frame-ancestors 'none'",
                "base-uri 'self'",
                "form-action 'self'"
        );
    }

    /**
     * Custom header writer for additional security headers
     */
    static class CustomSecurityHeaderWriter implements HeaderWriter {

        @Override
        public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
            String requestURI = request.getRequestURI();

            // X-XSS-Protection
            response.setHeader("X-XSS-Protection", "1; mode=block");

            // Referrer Policy
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions Policy
            response.setHeader("Permissions-Policy", buildPermissionsPolicy());

            // Cache Control for sensitive endpoints
            if (isSensitiveEndpoint(requestURI)) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            } else if (isPublicEndpoint(requestURI)) {
                response.setHeader("Cache-Control", "public, max-age=300"); // 5 minutes
            }
        }

        private String buildPermissionsPolicy() {
            return String.join(", ",
                    "camera=()",
                    "microphone=()",
                    "geolocation=()",
                    "payment=()",
                    "usb=()",
                    "magnetometer=()",
                    "gyroscope=()",
                    "speaker=()",
                    "vibrate=()",
                    "fullscreen=(self)"
            );
        }

        private boolean isSensitiveEndpoint(String uri) {
            return uri.startsWith("/api/v1/") &&
                   !uri.startsWith("/api/v1/auth") &&
                   !uri.startsWith("/api/demo");
        }

        private boolean isPublicEndpoint(String uri) {
            return uri.startsWith("/actuator/health") ||
                   uri.startsWith("/api/demo") ||
                   uri.startsWith("/swagger-ui") ||
                   uri.startsWith("/v3/api-docs");
        }
    }
}