package com.focushive.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Rate Limiting Filter
 * Implements basic in-memory rate limiting for demonstration and testing purposes
 * Note: In production, use Redis-based distributed rate limiting
 */
@Component
@Slf4j
public class SimpleRateLimitingFilter extends OncePerRequestFilter {

    // Simple in-memory rate limiting (for testing purposes)
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> resetTimes = new ConcurrentHashMap<>();

    // Rate limits
    private static final int PUBLIC_LIMIT_PER_HOUR = 100;
    private static final int AUTHENTICATED_LIMIT_PER_HOUR = 1000;
    private static final int ADMIN_LIMIT_PER_HOUR = 10000;
    private static final long HOUR_IN_MILLIS = 3600000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Skip rate limiting for certain endpoints
        if (shouldSkipRateLimiting(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String clientId = getClientIdentifier(request);
            RateLimitType rateLimitType = determineRateLimitType(request);

            if (!isWithinRateLimit(clientId, rateLimitType)) {
                handleRateLimitExceeded(response, rateLimitType);
                return;
            }

            // Add rate limiting headers
            addRateLimitHeaders(response, clientId, rateLimitType);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Rate limiting filter error", e);
            // Fail open - continue with request
            filterChain.doFilter(request, response);
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Use IP address as client identifier (simplified)
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

    private RateLimitType determineRateLimitType(HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        // Check if admin endpoint
        if (requestURI.startsWith("/api/v1/admin/") ||
            (requestURI.startsWith("/actuator/") && !requestURI.equals("/actuator/health"))) {
            return RateLimitType.ADMIN;
        }

        // Check if public endpoint
        if (requestURI.startsWith("/api/v1/auth/") ||
            requestURI.startsWith("/api/demo/") ||
            requestURI.equals("/actuator/health")) {
            return RateLimitType.PUBLIC;
        }

        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getName())) {

            // Check if admin role
            if (authentication.getAuthorities().stream()
                    .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()))) {
                return RateLimitType.ADMIN;
            }

            return RateLimitType.AUTHENTICATED;
        }

        return RateLimitType.PUBLIC;
    }

    private boolean isWithinRateLimit(String clientId, RateLimitType rateLimitType) {
        String key = rateLimitType.name() + ":" + clientId;
        long now = System.currentTimeMillis();

        // Check if we need to reset the counter
        Long resetTime = resetTimes.get(key);
        if (resetTime == null || now > resetTime) {
            requestCounts.put(key, new AtomicInteger(0));
            resetTimes.put(key, now + HOUR_IN_MILLIS);
        }

        AtomicInteger count = requestCounts.get(key);
        int currentCount = count.incrementAndGet();

        int limit = switch (rateLimitType) {
            case PUBLIC -> PUBLIC_LIMIT_PER_HOUR;
            case AUTHENTICATED -> AUTHENTICATED_LIMIT_PER_HOUR;
            case ADMIN -> ADMIN_LIMIT_PER_HOUR;
        };

        return currentCount <= limit;
    }

    private void addRateLimitHeaders(HttpServletResponse response, String clientId, RateLimitType rateLimitType) {
        String key = rateLimitType.name() + ":" + clientId;
        AtomicInteger count = requestCounts.get(key);
        Long resetTime = resetTimes.get(key);

        int limit = switch (rateLimitType) {
            case PUBLIC -> PUBLIC_LIMIT_PER_HOUR;
            case AUTHENTICATED -> AUTHENTICATED_LIMIT_PER_HOUR;
            case ADMIN -> ADMIN_LIMIT_PER_HOUR;
        };

        int remaining = Math.max(0, limit - (count != null ? count.get() : 0));

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime != null ? resetTime : 0));
    }

    private void handleRateLimitExceeded(HttpServletResponse response, RateLimitType rateLimitType) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        String responseBody = String.format(
                "{\"error\": \"Too Many Requests\", " +
                "\"message\": \"Rate limit exceeded for %s endpoints\", " +
                "\"type\": \"%s\"}",
                rateLimitType.name().toLowerCase(),
                rateLimitType.name()
        );

        response.getWriter().write(responseBody);
        log.warn("Rate limit exceeded for {} endpoints", rateLimitType.name());
    }

    private boolean shouldSkipRateLimiting(String requestURI) {
        return requestURI.equals("/error") ||
               requestURI.startsWith("/swagger-ui") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.startsWith("/webjars");
    }

    enum RateLimitType {
        PUBLIC, AUTHENTICATED, ADMIN
    }
}