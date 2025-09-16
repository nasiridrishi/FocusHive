package com.focushive.notification.security;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Filter that validates request format before Spring Security authentication.
 * This ensures that malformed JSON returns 400 Bad Request instead of 401 Unauthorized.
 *
 * This filter runs early in the chain to catch invalid requests before authentication.
 */
@Slf4j
@Component
public class RequestValidationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Methods that typically have request bodies
    private static final Set<String> METHODS_WITH_BODY = Set.of(
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.PATCH.name()
    );

    // Paths that should skip validation (public endpoints, health checks, etc.)
    private static final Set<String> SKIP_PATHS = Set.of(
        "/health",
        "/api/v1/health",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String contentType = request.getContentType();

        // Skip validation for certain paths
        if (shouldSkipValidation(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only validate requests that should have JSON bodies
        if (METHODS_WITH_BODY.contains(method) && isJsonContentType(contentType)) {
            // Wrap request to allow reading body multiple times
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

            // Validate JSON structure
            if (!isValidJson(cachedRequest)) {
                log.debug("Invalid JSON in request to {} {}", method, path);
                sendBadRequestResponse(response, "Invalid JSON format in request body", path);
                return;
            }

            // Continue with the cached request
            filterChain.doFilter(cachedRequest, response);
        } else {
            // For GET, DELETE, or non-JSON requests, continue without validation
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Checks if the path should skip validation.
     */
    private boolean shouldSkipValidation(String path) {
        return SKIP_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Checks if the content type indicates JSON.
     */
    private boolean isJsonContentType(String contentType) {
        return contentType != null &&
               (contentType.contains("application/json") ||
                contentType.contains("application/vnd.api+json"));
    }

    /**
     * Validates if the request body contains valid JSON.
     */
    private boolean isValidJson(CachedBodyHttpServletRequest request) {
        try {
            String body = request.getCachedBody();

            // Empty body is considered valid for optional body requests
            if (body == null || body.trim().isEmpty()) {
                return true;
            }

            // Try to parse as JSON
            objectMapper.readTree(body);
            return true;
        } catch (JsonParseException | JsonMappingException e) {
            log.debug("JSON parsing failed: {}", e.getMessage());
            return false;
        } catch (IOException e) {
            log.error("Error reading request body: {}", e.getMessage());
            // In case of IO error, let the request continue
            return true;
        }
    }

    /**
     * Sends a 400 Bad Request response for invalid JSON.
     */
    private void sendBadRequestResponse(HttpServletResponse response,
                                         String message,
                                         String path) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("path", path);

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    /**
     * Wrapper class to cache request body for multiple reads.
     */
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final String cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        }

        public String getCachedBody() {
            return cachedBody;
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                cachedBody.getBytes(StandardCharsets.UTF_8));

            return new jakarta.servlet.ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(jakarta.servlet.ReadListener listener) {
                    // Not needed for this implementation
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }
    }
}