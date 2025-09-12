package com.focushive.identity.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.annotation.RateLimit;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.RateLimitExceededException;
import com.focushive.identity.service.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor that enforces rate limiting based on @RateLimit annotations.
 * Supports IP-based and user-based rate limiting with configurable strategies.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {
    
    private final RedisRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return true;
        }
        
        // Skip rate limiting for authenticated users if configured
        if (rateLimit.skipAuthenticated() && isAuthenticated()) {
            log.debug("Skipping rate limit for authenticated user on endpoint: {}", 
                     request.getRequestURI());
            return true;
        }
        
        try {
            String rateLimitKey = buildRateLimitKey(request, rateLimit);
            
            if (!rateLimiter.isAllowed(rateLimitKey, rateLimit)) {
                // This should not happen as RedisRateLimiter throws exception
                // but keeping as a safety net
                handleRateLimitExceeded(request, response, rateLimit, rateLimitKey);
                return false;
            }
            
            // Add rate limit headers to response
            addRateLimitHeaders(response, rateLimitKey, rateLimit);
            
            log.debug("Rate limit check passed for key: {} on endpoint: {}", 
                     rateLimitKey, request.getRequestURI());
            return true;
            
        } catch (RateLimitExceededException e) {
            handleRateLimitExceeded(request, response, rateLimit, "", e);
            return false;
        } catch (Exception e) {
            log.error("Error in rate limiting interceptor", e);
            // Allow request to proceed on error to maintain service availability
            return true;
        }
    }
    
    /**
     * Builds a unique key for rate limiting based on the strategy.
     */
    private String buildRateLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        String methodName = rateLimit.keyPrefix().isEmpty() ? 
                           request.getRequestURI() : rateLimit.keyPrefix();
        
        String baseKey = "rate_limit:" + methodName + ":";
        
        String ipAddress = getClientIpAddress(request);
        String userId = getCurrentUserId();
        
        return switch (rateLimit.type()) {
            case IP -> baseKey + "ip:" + ipAddress;
            case USER -> baseKey + "user:" + (userId != null ? userId : "anonymous");
            case IP_AND_USER -> baseKey + "ip_user:" + ipAddress + ":" + 
                               (userId != null ? userId : "anonymous");
            case IP_OR_USER -> baseKey + "ip_or_user:" + 
                              (userId != null ? "user:" + userId : "ip:" + ipAddress);
        };
    }
    
    /**
     * Gets the client's IP address, handling proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Get the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        if (xForwardedProto != null) {
            // We're behind a proxy
            String cfConnectingIP = request.getHeader("CF-Connecting-IP");
            if (cfConnectingIP != null && !cfConnectingIP.isEmpty()) {
                return cfConnectingIP; // Cloudflare
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Gets the current authenticated user ID.
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                return user.getId().toString();
            }
        } catch (Exception e) {
            log.debug("Could not extract user ID from security context", e);
        }
        return null;
    }
    
    /**
     * Checks if the current request is from an authenticated user.
     */
    private boolean isAuthenticated() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null && authentication.isAuthenticated()
                   && !(authentication.getPrincipal() instanceof String 
                        && "anonymousUser".equals(authentication.getPrincipal()));
        } catch (Exception e) {
            log.debug("Error checking authentication status", e);
            return false;
        }
    }
    
    /**
     * Adds rate limiting headers to the response.
     */
    private void addRateLimitHeaders(HttpServletResponse response, String key, RateLimit rateLimit) {
        try {
            long remaining = rateLimiter.getRemainingTokens(key, rateLimit);
            long resetTime = System.currentTimeMillis() / 1000 + 
                           rateLimit.timeUnit().toSeconds(rateLimit.window());
            
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.value()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining - 1)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
            response.setHeader("X-RateLimit-Window", rateLimit.window() + " " + 
                              rateLimit.timeUnit().toString().toLowerCase());
            
        } catch (Exception e) {
            log.debug("Error adding rate limit headers", e);
        }
    }
    
    /**
     * Handles rate limit exceeded scenarios.
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                       RateLimit rateLimit, String key) throws IOException {
        handleRateLimitExceeded(request, response, rateLimit, key, null);
    }
    
    /**
     * Handles rate limit exceeded scenarios with exception details.
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                       RateLimit rateLimit, String key, RateLimitExceededException e) 
            throws IOException {
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        long retryAfter = e != null ? e.getRetryAfterSeconds() : 
                         rateLimiter.getSecondsUntilRefill(key, rateLimit);
        
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.value()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", 
                          String.valueOf(System.currentTimeMillis() / 1000 + retryAfter));
        
        String message = e != null ? e.getMessage() : 
                        (rateLimit.message().isEmpty() ? 
                         String.format("Rate limit exceeded. Maximum %d requests per %d %s allowed.", 
                                      rateLimit.value(), rateLimit.window(), 
                                      rateLimit.timeUnit().toString().toLowerCase()) : 
                         rateLimit.message());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "rate_limit_exceeded");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("retryAfterSeconds", retryAfter);
        errorResponse.put("limit", rateLimit.value());
        errorResponse.put("window", rateLimit.window() + " " + rateLimit.timeUnit().toString().toLowerCase());
        
        // Add violation count if available and progressive penalties are enabled
        if (rateLimit.progressivePenalties() && !key.isEmpty()) {
            try {
                int violationCount = rateLimiter.getViolationCount(key);
                if (violationCount > 1) {
                    errorResponse.put("violationCount", violationCount);
                    errorResponse.put("progressivePenalty", true);
                }
            } catch (Exception ex) {
                log.debug("Could not add violation count to response", ex);
            }
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
        
        log.warn("Rate limit exceeded for {} on {} - IP: {}, User: {}", 
                key, request.getRequestURI(), 
                getClientIpAddress(request), getCurrentUserId());
    }
}