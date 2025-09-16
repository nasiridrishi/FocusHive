package com.focushive.buddy.validation;

import com.focushive.buddy.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Filter to validate the presence and format of X-User-ID header for API endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdHeaderValidator extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Endpoints that require X-User-ID header
    private static final List<String> PROTECTED_PATTERNS = Arrays.asList(
        "/api/v1/buddy/**"
    );

    // Endpoints that are excluded from validation
    private static final List<String> EXCLUDED_PATTERNS = Arrays.asList(
        "/api/v1/health",
        "/api/v1/health/**",
        "/actuator/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Check if the request path is excluded
        if (isExcluded(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if the request path requires validation
        if (!requiresValidation(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate X-User-ID header
        String userId = request.getHeader("X-User-ID");

        if (userId == null || userId.trim().isEmpty()) {
            sendErrorResponse(response, "User ID header is required");
            return;
        }

        // Validate UUID format
        try {
            UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, "Invalid X-User-ID format. Must be a valid UUID");
            return;
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    private boolean requiresValidation(String requestPath) {
        return PROTECTED_PATTERNS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    private boolean isExcluded(String requestPath) {
        return EXCLUDED_PATTERNS.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Object> errorResponse = ApiResponse.builder()
            .success(false)
            .message(message)
            .error(message)
            .timestamp(LocalDateTime.now())
            .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}