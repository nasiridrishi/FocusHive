package com.focushive.notification.config;

import com.focushive.notification.service.RateLimitingService;
import com.focushive.notification.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for enforcing API rate limits.
 * Checks rate limits based on user ID or IP address and operation type.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;
    private final UserContextService userContextService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {

        // Skip rate limiting if disabled
        if (!rateLimitingService.isEnabled()) {
            return true;
        }

        // Skip rate limiting for public endpoints
        if (isPublicEndpoint(request.getRequestURI())) {
            return handlePublicEndpoint(request, response);
        }

        // Get user identifier and operation type
        String identifier = getUserIdentifier(request);
        String operationType = getOperationType(request);

        // Check rate limit
        if (!rateLimitingService.isAllowed(identifier, operationType)) {
            return handleRateLimitExceeded(request, response, identifier, operationType);
        }

        // Add rate limit headers to response
        addRateLimitHeaders(response, identifier, operationType);
        
        return true;
    }

    /**
     * Handles rate limiting for public endpoints (based on IP).
     */
    private boolean handlePublicEndpoint(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = getClientIp(request);
        
        if (!rateLimitingService.isAllowed(clientIp, "PUBLIC")) {
            return handleRateLimitExceeded(request, response, clientIp, "PUBLIC");
        }
        
        addRateLimitHeaders(response, clientIp, "PUBLIC");
        return true;
    }

    /**
     * Handles rate limit exceeded scenario.
     */
    private boolean handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                          String identifier, String operationType) {
        log.warn("Rate limit exceeded for {} on {} {}: {}", 
                identifier, request.getMethod(), request.getRequestURI(), operationType);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Content-Type", "application/json");
        
        // Add rate limit headers
        addRateLimitHeaders(response, identifier, operationType);
        
        // Add retry-after header
        long resetTime = rateLimitingService.getResetTime(identifier, operationType);
        long retryAfter = Math.max(1, (resetTime - System.currentTimeMillis()) / 1000);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        
        try {
            String errorResponse = String.format("""
                {
                    "error": "Rate limit exceeded",
                    "message": "Too many requests. Please try again later.",
                    "retryAfter": %d
                }
                """, retryAfter);
            response.getWriter().write(errorResponse);
        } catch (Exception e) {
            log.error("Error writing rate limit response", e);
        }
        
        return false;
    }

    /**
     * Adds rate limit headers to the response.
     */
    private void addRateLimitHeaders(HttpServletResponse response, String identifier, String operationType) {
        try {
            int limit = rateLimitingService.getConfiguredLimit(operationType);
            int remaining = rateLimitingService.getRemainingRequests(identifier, operationType);
            long resetTime = rateLimitingService.getResetTime(identifier, operationType);
            
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime / 1000)); // Unix timestamp
        } catch (Exception e) {
            log.debug("Error adding rate limit headers", e);
        }
    }

    /**
     * Gets user identifier for rate limiting.
     */
    private String getUserIdentifier(HttpServletRequest request) {
        String userId = userContextService.getCurrentUserId();
        return userId != null ? userId : getClientIp(request);
    }

    /**
     * Gets operation type based on request.
     */
    private String getOperationType(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Admin operations
        if (uri.contains("/admin/") || uri.contains("/defaults")) {
            return "ADMIN";
        }
        
        // Write operations
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method)) {
            return "WRITE";
        }
        
        // Read operations
        return "READ";
    }

    /**
     * Checks if the endpoint is public (no authentication required).
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/actuator/") ||
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs") ||
               uri.equals("/swagger-ui.html") ||
               uri.startsWith("/webjars/") ||
               uri.equals("/health");
    }

    /**
     * Gets client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}