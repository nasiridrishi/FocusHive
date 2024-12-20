package com.focushive.identity.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;

/**
 * Input validation configuration to prevent injection attacks.
 * Implements OWASP recommendations for input validation and sanitization.
 */
@Slf4j
@Configuration
public class InputValidationConfig {

    @Bean
    public InputValidationFilter inputValidationFilter() {
        return new InputValidationFilter();
    }

    /**
     * Filter to validate and sanitize all incoming requests.
     */
    public static class InputValidationFilter extends OncePerRequestFilter {

        // Patterns for detecting potential injection attacks
        private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|FROM|WHERE)\\b)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(--|#|;)", Pattern.CASE_INSENSITIVE),  // Removed /* and */ to allow Accept: */*
            Pattern.compile("('|(\\-\\-)|(;)|(\\|\\|))", Pattern.CASE_INSENSITIVE),  // Removed /* and */ to allow Accept: */*
            Pattern.compile("(\\bOR\\b\\s*\\d+\\s*=\\s*\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\bOR\\b\\s*'[^']*'\\s*=\\s*'[^']*')", Pattern.CASE_INSENSITIVE),
            Pattern.compile("/\\*.*\\*/", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)  // Only flag actual SQL comments with content
        );

        private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
            Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<object[^>]*>.*?</object>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE)
        );

        private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = Arrays.asList(
            Pattern.compile("\\.\\./"),
            Pattern.compile("\\.\\.\\\\"),
            Pattern.compile("%2e%2e%2f", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%2e%2e/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.\\.%2f", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%2e%2e\\\\", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\.\\./\\.\\."),
            Pattern.compile("\\\\\\.\\.\\\\")
        );

        private static final List<Pattern> COMMAND_INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("[;&|`]"),
            Pattern.compile("\\$\\("),
            // Only flag command patterns when they appear in suspicious contexts
            // Avoid flagging legitimate user agent strings like "curl/7.68.0"
            Pattern.compile("\\b(cat|ls|rm|wget)\\s+[/\\\\\\w]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(sh|bash|cmd|powershell)\\s*-[ec]", Pattern.CASE_INSENSITIVE)
        );

        private static final List<Pattern> LDAP_INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("[\\*\\(\\)\\\\,]"),
            Pattern.compile("\\)(\\(|\\|)")
        );

        // Maximum allowed parameter length
        private static final int MAX_PARAMETER_LENGTH = 1000;
        private static final int MAX_HEADER_LENGTH = 500;

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain filterChain) throws ServletException, IOException {

            try {
                // Validate request parameters
                if (!validateParameters(request)) {
                    log.warn("Invalid parameters detected from IP: {}", request.getRemoteAddr());
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input detected");
                    return;
                }

                // Validate headers
                if (!validateHeaders(request)) {
                    log.warn("Invalid headers detected from IP: {}", request.getRemoteAddr());
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid headers detected");
                    return;
                }

                // Validate URI
                if (!validateUri(request)) {
                    log.warn("Invalid URI detected from IP: {}", request.getRemoteAddr());
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URI detected");
                    return;
                }

                filterChain.doFilter(request, response);

            } catch (Exception e) {
                log.error("Error in input validation filter", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        /**
         * Validate request parameters for injection attacks.
         */
        private boolean validateParameters(HttpServletRequest request) {
            var parameterMap = request.getParameterMap();

            for (var entry : parameterMap.entrySet()) {
                String paramName = entry.getKey();
                String[] paramValues = entry.getValue();

                // Check parameter name
                if (!isValidParameterName(paramName)) {
                    log.warn("Invalid parameter name: {}", paramName);
                    return false;
                }

                // Check parameter values
                for (String value : paramValues) {
                    if (value == null) continue;

                    // Check length
                    if (value.length() > MAX_PARAMETER_LENGTH) {
                        log.warn("Parameter value too long: {} characters", value.length());
                        return false;
                    }

                    // Check for injection patterns
                    if (containsInjectionPattern(value)) {
                        log.warn("Potential injection detected in parameter: {}", paramName);
                        return false;
                    }
                }
            }

            return true;
        }

        /**
         * Validate request headers.
         */
        private boolean validateHeaders(HttpServletRequest request) {
            var headerNames = request.getHeaderNames();

            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);

                if (headerValue == null) continue;

                // Skip validation for certain headers
                if (isExemptHeader(headerName)) {
                    continue;
                }

                // Check length
                if (headerValue.length() > MAX_HEADER_LENGTH) {
                    log.warn("Header value too long: {} = {} characters", headerName, headerValue.length());
                    return false;
                }

                // Check for injection patterns in custom headers only
                // Standard headers like User-Agent are exempt from command injection checks
                if (!isStandardHeader(headerName) && !isExemptHeader(headerName) && containsInjectionPattern(headerValue)) {
                    log.warn("Potential injection in header: {}", headerName);
                    return false;
                }
            }

            return true;
        }

        /**
         * Validate request URI.
         */
        private boolean validateUri(HttpServletRequest request) {
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();

            // Check URI for path traversal
            if (containsPathTraversal(uri)) {
                log.warn("Path traversal detected in URI: {}", uri);
                return false;
            }

            // Check query string
            if (queryString != null && containsInjectionPattern(queryString)) {
                log.warn("Potential injection in query string");
                return false;
            }

            return true;
        }

        /**
         * Check if parameter name is valid.
         */
        private boolean isValidParameterName(String name) {
            // Allow only alphanumeric, underscore, and dash
            return name.matches("^[a-zA-Z0-9_-]+$");
        }

        /**
         * Check if header should be exempted from validation.
         */
        private boolean isExemptHeader(String headerName) {
            return headerName.equalsIgnoreCase("Authorization") ||
                   headerName.equalsIgnoreCase("Cookie") ||
                   headerName.equalsIgnoreCase("User-Agent") ||
                   headerName.equalsIgnoreCase("X-CSRF-Token");
        }

        /**
         * Check if header is a standard HTTP header.
         */
        private boolean isStandardHeader(String headerName) {
            String lowerHeader = headerName.toLowerCase();
            return lowerHeader.startsWith("accept") ||
                   lowerHeader.startsWith("content-") ||
                   lowerHeader.equals("host") ||
                   lowerHeader.equals("user-agent") ||
                   lowerHeader.equals("referer") ||
                   lowerHeader.equals("origin");
        }

        /**
         * Check if value contains injection patterns.
         */
        private boolean containsInjectionPattern(String value) {
            if (value == null) return false;

            // Check SQL injection
            for (Pattern pattern : SQL_INJECTION_PATTERNS) {
                if (pattern.matcher(value).find()) {
                    log.debug("SQL injection pattern detected: {}", pattern.pattern());
                    return true;
                }
            }

            // Check XSS
            for (Pattern pattern : XSS_PATTERNS) {
                if (pattern.matcher(value).find()) {
                    log.debug("XSS pattern detected: {}", pattern.pattern());
                    return true;
                }
            }

            // Check Command injection
            for (Pattern pattern : COMMAND_INJECTION_PATTERNS) {
                if (pattern.matcher(value).find()) {
                    log.debug("Command injection pattern detected: {}", pattern.pattern());
                    return true;
                }
            }

            // Check LDAP injection (if applicable)
            if (value.contains("uid=") || value.contains("cn=")) {
                for (Pattern pattern : LDAP_INJECTION_PATTERNS) {
                    if (pattern.matcher(value).find()) {
                        log.debug("LDAP injection pattern detected: {}", pattern.pattern());
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Check for path traversal patterns.
         */
        private boolean containsPathTraversal(String path) {
            for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
                if (pattern.matcher(path).find()) {
                    log.debug("Path traversal pattern detected: {}", pattern.pattern());
                    return true;
                }
            }
            return false;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();

            // Don't filter static resources
            if (path.startsWith("/static/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.endsWith(".ico")) {
                return true;
            }

            // Don't filter API endpoints that have their own validation
            // Registration and authentication endpoints use @Valid annotations
            if (path.startsWith("/api/v1/auth/")) {
                return true;
            }

            // Don't filter OAuth2 endpoints
            if (path.startsWith("/oauth2/") || path.startsWith("/.well-known/")) {
                return true;
            }

            // Don't filter actuator endpoints
            if (path.startsWith("/actuator/")) {
                return true;
            }
            
            // Don't filter health check endpoints
            if (path.startsWith("/api/v1/health") || path.equals("/health")) {
                return true;
            }

            // Don't filter OpenAPI/Swagger endpoints
            if (path.startsWith("/api-docs/") || path.startsWith("/swagger-ui/")) {
                return true;
            }

            return false;
        }
    }
}