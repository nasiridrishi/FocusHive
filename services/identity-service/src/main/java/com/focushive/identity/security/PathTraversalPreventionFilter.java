package com.focushive.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Security filter to prevent path traversal attacks.
 *
 * OWASP A01:2021 – Broken Access Control Compliance:
 * - Detects and blocks path traversal attempts (../, ..\, %2e%2e%2f, etc.)
 * - Logs security violations for monitoring
 * - Returns 404 to avoid revealing internal structure
 * - Handles both URL-encoded and plain path traversal patterns
 *
 * This filter runs before authentication to prevent unauthorized file system access.
 */
@Component
public class PathTraversalPreventionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(PathTraversalPreventionFilter.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY.PATH_TRAVERSAL");

    // Path traversal patterns to detect
    private static final Pattern[] PATH_TRAVERSAL_PATTERNS = {
        // Standard path traversal
        Pattern.compile(".*\\.\\./.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.\\.\\\\.*", Pattern.CASE_INSENSITIVE),

        // URL-encoded variations
        Pattern.compile(".*%2e%2e%2f.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%2e%2e%5c.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%2e%2e/.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.%2e%2f.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*%2e\\./.*", Pattern.CASE_INSENSITIVE),

        // Double URL-encoded
        Pattern.compile(".*%252e%252e%252f.*", Pattern.CASE_INSENSITIVE),

        // Unicode variations
        Pattern.compile(".*\\u002e\\u002e\\u002f.*", Pattern.CASE_INSENSITIVE),

        // Absolute path attempts to sensitive locations
        Pattern.compile(".*/etc/passwd.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/etc/shadow.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/windows/system32.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\\\windows\\\\system32.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*/proc/.*", Pattern.CASE_INSENSITIVE),

        // Null byte injection
        Pattern.compile(".*%00.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\x00.*", Pattern.CASE_INSENSITIVE)
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();

        // Check for path traversal in URI
        if (isPathTraversalAttempt(requestURI)) {
            handlePathTraversalAttempt(request, response, requestURI, "URI");
            return;
        }

        // Check for path traversal in query parameters
        if (queryString != null && isPathTraversalAttempt(queryString)) {
            handlePathTraversalAttempt(request, response, queryString, "Query String");
            return;
        }

        // Continue with the filter chain if no path traversal detected
        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the given input contains path traversal patterns.
     *
     * @param input The input string to check (URI, query string, etc.)
     * @return true if path traversal is detected, false otherwise
     */
    private boolean isPathTraversalAttempt(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // URL decode the input to catch encoded attempts
        String decodedInput;
        try {
            decodedInput = URLDecoder.decode(input, StandardCharsets.UTF_8);
            // Double decode to catch double-encoded attempts
            decodedInput = URLDecoder.decode(decodedInput, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // If decoding fails, check the original input
            decodedInput = input;
        }

        // Check both original and decoded input against all patterns
        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(input).matches() || pattern.matcher(decodedInput).matches()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles detected path traversal attempts by logging and returning 404.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param suspiciousInput The input that triggered the detection
     * @param inputType The type of input (URI, Query String, etc.)
     */
    private void handlePathTraversalAttempt(HttpServletRequest request,
                                          HttpServletResponse response,
                                          String suspiciousInput,
                                          String inputType) throws IOException {

        String clientIP = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");

        // Log security violation for monitoring and alerting
        securityLogger.warn("Path Traversal Attack Detected - IP: {}, User-Agent: {}, {} Input: {}, Method: {}",
                           clientIP, userAgent, inputType, suspiciousInput, request.getMethod());

        // For OWASP compliance testing, return 404 Not Found
        // This prevents revealing internal directory structure
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Not Found\"}");
    }

    /**
     * Gets the real client IP address, considering proxy headers.
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty() && !"unknown".equalsIgnoreCase(xfHeader)) {
            return xfHeader.split(",")[0];
        }

        String xrHeader = request.getHeader("X-Real-IP");
        if (xrHeader != null && !xrHeader.isEmpty() && !"unknown".equalsIgnoreCase(xrHeader)) {
            return xrHeader;
        }

        return request.getRemoteAddr();
    }
}