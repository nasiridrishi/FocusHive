package com.focushive.buddy.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Configuration class for production-grade logging setup.
 * 
 * Features:
 * - Adds correlation IDs to all requests
 * - Configures structured logging context
 * - Provides utilities for secure logging
 */
@Configuration
public class LoggingConfig implements WebMvcConfigurer {
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String REQUEST_URI_MDC_KEY = "requestUri";
    public static final String HTTP_METHOD_MDC_KEY = "httpMethod";
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor());
    }
    
    /**
     * Interceptor to add logging context to all requests
     */
    public static class LoggingInterceptor implements HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // Add correlation ID for request tracking
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Add correlation ID to response headers
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Add context to MDC for structured logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            MDC.put(REQUEST_URI_MDC_KEY, request.getRequestURI());
            MDC.put(HTTP_METHOD_MDC_KEY, request.getMethod());
            
            // Add user ID if present (but sanitize it)
            String userId = request.getHeader(USER_ID_HEADER);
            if (userId != null && !userId.isEmpty()) {
                // Only log the user ID hash for privacy
                MDC.put(USER_ID_MDC_KEY, "user-" + Math.abs(userId.hashCode()));
            }
            
            return true;
        }
        
        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                  Object handler, Exception ex) {
            // Clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }
    
    /**
     * Utility class for secure logging operations
     */
    public static class SecureLogger {
        
        /**
         * Sanitizes potentially sensitive data for logging
         */
        public static String sanitize(String input) {
            if (input == null) {
                return null;
            }
            
            // Common patterns to mask
            String sanitized = input
                .replaceAll("(?i)(password|token|secret|key|auth)[\"']*\\s*[:=]\\s*[\"']*([^\\s,}\\]\"']+)", 
                           "$1: ***MASKED***")
                .replaceAll("(?i)Bearer\\s+[\\w\\-\\.]+", "Bearer ***MASKED***")
                .replaceAll("(?i)Basic\\s+[\\w\\-\\.=]+", "Basic ***MASKED***");
            
            return sanitized;
        }
        
        /**
         * Sanitizes email addresses for logging (keeps domain but masks local part)
         */
        public static String sanitizeEmail(String email) {
            if (email == null || !email.contains("@")) {
                return email;
            }
            
            String[] parts = email.split("@");
            if (parts.length != 2) {
                return email;
            }
            
            String localPart = parts[0];
            String domain = parts[1];
            
            // Mask local part but keep first and last character if long enough
            if (localPart.length() <= 2) {
                return "***@" + domain;
            } else {
                return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
            }
        }
        
        /**
         * Creates a safe representation of an object for logging
         */
        public static String toSafeString(Object obj) {
            if (obj == null) {
                return "null";
            }
            
            return sanitize(obj.toString());
        }
    }
}