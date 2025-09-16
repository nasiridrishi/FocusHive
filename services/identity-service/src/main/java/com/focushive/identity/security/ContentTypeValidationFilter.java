package com.focushive.identity.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Content Type Validation Filter for OWASP A08: Software and Data Integrity Failures.
 * Prevents deserialization attacks by rejecting non-JSON content types for API endpoints.
 */
@Component
@Slf4j
public class ContentTypeValidationFilter extends OncePerRequestFilter {

    // Allowed content types for API endpoints
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE
    );

    // Endpoints that should be checked for content type
    private static final List<String> API_ENDPOINTS = Arrays.asList(
        "/api/",
        "/oauth2/"
    );

    // Dangerous content types that could contain serialized objects
    private static final List<String> DANGEROUS_CONTENT_TYPES = Arrays.asList(
        MediaType.APPLICATION_OCTET_STREAM_VALUE,
        "application/x-java-serialized-object",
        "application/x-java-object",
        "application/java-archive",
        "application/x-binary"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String contentType = request.getContentType();

        // Only check POST, PUT, PATCH requests with content
        if (!Arrays.asList("POST", "PUT", "PATCH").contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only check API endpoints
        boolean isApiEndpoint = API_ENDPOINTS.stream()
            .anyMatch(requestURI::startsWith);

        if (!isApiEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        // A08: Check for dangerous content types that could contain serialized objects
        if (contentType != null && isDangerousContentType(contentType)) {
            log.warn("A08 Data Integrity: Dangerous content type '{}' blocked for endpoint: {} {}",
                contentType, method, requestURI);

            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"error\":\"Unsupported media type\"," +
                "\"message\":\"Only JSON content type is supported for API endpoints\"}"
            );
            return;
        }

        // A08: Validate content type for requests with body
        if (contentType != null && !isAllowedContentType(contentType)) {
            log.warn("A08 Data Integrity: Unsupported content type '{}' for endpoint: {} {}",
                contentType, method, requestURI);

            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"error\":\"Unsupported media type\"," +
                "\"message\":\"Only JSON content type is supported for API endpoints\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if content type is dangerous (could contain serialized objects).
     */
    private boolean isDangerousContentType(String contentType) {
        // Remove charset and other parameters
        String baseContentType = contentType.split(";")[0].trim().toLowerCase();

        return DANGEROUS_CONTENT_TYPES.stream()
            .anyMatch(dangerous -> baseContentType.equals(dangerous.toLowerCase()));
    }

    /**
     * Check if content type is allowed for API endpoints.
     */
    private boolean isAllowedContentType(String contentType) {
        // Remove charset and other parameters
        String baseContentType = contentType.split(";")[0].trim();

        return ALLOWED_CONTENT_TYPES.stream()
            .anyMatch(allowed -> baseContentType.equals(allowed));
    }
}