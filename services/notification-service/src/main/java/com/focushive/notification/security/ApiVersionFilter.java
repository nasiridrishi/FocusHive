package com.focushive.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter to handle API versioning via headers and URL paths.
 * Adds version information to responses and handles version negotiation.
 */
@Slf4j
@Component
@Order(3)
public class ApiVersionFilter extends OncePerRequestFilter {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String ACCEPT_VERSION_HEADER = "Accept-Version";
    private static final String API_VERSION_RESPONSE_HEADER = "X-API-Version";
    private static final String API_DEPRECATED_HEADER = "X-API-Deprecated";
    private static final String API_SUNSET_HEADER = "X-API-Sunset";
    private static final String API_SUPPORTED_VERSIONS_HEADER = "X-API-Supported-Versions";

    private static final Pattern VERSION_PATTERN = Pattern.compile("/api/v(\\d+)/");

    @Value("${api.version.current:2}")
    private String currentVersion;

    @Value("${api.version.minimum:1}")
    private String minimumVersion;

    @Value("${api.version.maximum:2}")
    private String maximumVersion;

    @Value("${api.version.supported:1,2}")
    private String supportedVersions;

    @Value("${api.version.deprecated:}")
    private String deprecatedVersions;

    @Value("${api.version.sunset.v1:2025-12-31}")
    private String v1SunsetDate;

    private Set<String> supportedVersionSet;
    private Set<String> deprecatedVersionSet;

    @jakarta.annotation.PostConstruct
    public void init() {
        supportedVersionSet = new HashSet<>(Arrays.asList(supportedVersions.split(",")));
        if (deprecatedVersions != null && !deprecatedVersions.isEmpty()) {
            deprecatedVersionSet = new HashSet<>(Arrays.asList(deprecatedVersions.split(",")));
        } else {
            deprecatedVersionSet = new HashSet<>();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String detectedVersion = null;

        // Try to detect version from URL path
        Matcher matcher = VERSION_PATTERN.matcher(requestPath);
        if (matcher.find()) {
            detectedVersion = matcher.group(1);
        }

        // Check for version in headers (override URL version if present)
        String headerVersion = request.getHeader(API_VERSION_HEADER);
        if (headerVersion == null) {
            headerVersion = request.getHeader(ACCEPT_VERSION_HEADER);
        }
        if (headerVersion != null) {
            detectedVersion = headerVersion;
        }

        // Use current version if not specified
        if (detectedVersion == null) {
            detectedVersion = currentVersion;
        }

        // Validate version
        if (!supportedVersionSet.contains(detectedVersion)) {
            sendVersionNotSupportedResponse(response, detectedVersion);
            return;
        }

        // Add version information to response headers
        response.setHeader(API_VERSION_RESPONSE_HEADER, detectedVersion);
        response.setHeader(API_SUPPORTED_VERSIONS_HEADER, supportedVersions);

        // Add deprecation warnings if applicable
        if (deprecatedVersionSet.contains(detectedVersion)) {
            response.setHeader(API_DEPRECATED_HEADER, "true");
            response.setHeader(API_SUNSET_HEADER, getSunsetDate(detectedVersion));
            response.setHeader("Warning",
                String.format("299 - \"API version %s is deprecated and will be removed on %s\"",
                    detectedVersion, getSunsetDate(detectedVersion)));

            log.warn("Deprecated API version {} used by client: {}",
                detectedVersion, request.getRemoteAddr());
        }

        // Store version in request attributes for downstream use
        request.setAttribute("api.version", detectedVersion);
        request.setAttribute("api.version.deprecated", deprecatedVersionSet.contains(detectedVersion));

        // Log API version usage for monitoring
        log.debug("API request with version {}: {} {}", detectedVersion,
            request.getMethod(), request.getRequestURI());

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Send response for unsupported API version.
     */
    private void sendVersionNotSupportedResponse(HttpServletResponse response, String requestedVersion)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setHeader(API_SUPPORTED_VERSIONS_HEADER, supportedVersions);

        String jsonResponse = String.format("""
            {
                "error": "Unsupported API Version",
                "message": "The requested API version '%s' is not supported",
                "supportedVersions": [%s],
                "currentVersion": "%s",
                "documentation": "/api/docs"
            }
            """,
            requestedVersion,
            supportedVersionSet.stream()
                .map(v -> "\"" + v + "\"")
                .reduce((a, b) -> a + ", " + b)
                .orElse(""),
            currentVersion
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * Get sunset date for a specific API version.
     */
    private String getSunsetDate(String version) {
        switch (version) {
            case "1":
                return v1SunsetDate;
            default:
                return "TBD";
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Don't apply versioning to these endpoints
        return path.equals("/health") ||
               path.equals("/actuator/health") ||
               path.equals("/actuator/prometheus") ||
               path.equals("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/api/docs");
    }
}