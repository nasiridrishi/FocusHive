package com.focushive.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HTTP request and response logging filter.
 * Logs all HTTP traffic for debugging and monitoring purposes.
 */
@Slf4j
@Component
@Order(2) // After correlation ID filter
public class HttpLoggingFilter extends OncePerRequestFilter {
    
    private static final List<String> SENSITIVE_HEADERS = Arrays.asList(
        "authorization", "cookie", "x-api-key", "x-auth-token"
    );
    
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/actuator/health", "/actuator/metrics", "/actuator/prometheus"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Skip logging for health check endpoints
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Wrap request and response to enable content reading
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        long startTime = System.currentTimeMillis();
        String correlationId = (String) request.getAttribute("correlationId");
        
        try {
            logRequest(wrappedRequest, correlationId);
            
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
            long duration = System.currentTimeMillis() - startTime;
            logResponse(wrappedRequest, wrappedResponse, correlationId, duration);
            
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    private void logRequest(ContentCachingRequestWrapper request, String correlationId) {
        log.info("[{}] Incoming Request: {} {} from {}",
                correlationId,
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());
        
        if (log.isDebugEnabled()) {
            // Log headers
            Collections.list(request.getHeaderNames()).forEach(headerName -> {
                if (!isSensitiveHeader(headerName)) {
                    String headerValue = request.getHeader(headerName);
                    log.debug("[{}] Request Header: {} = {}", 
                            correlationId, headerName, headerValue);
                } else {
                    log.debug("[{}] Request Header: {} = [HIDDEN]", 
                            correlationId, headerName);
                }
            });
            
            // Log query parameters
            if (request.getQueryString() != null) {
                log.debug("[{}] Query String: {}", correlationId, request.getQueryString());
            }
            
            // Log request body for POST/PUT requests
            if (hasBody(request)) {
                logRequestBody(request, correlationId);
            }
        }
    }
    
    private void logResponse(ContentCachingRequestWrapper request,
                           ContentCachingResponseWrapper response,
                           String correlationId,
                           long duration) {
        
        log.info("[{}] Outgoing Response: {} {} - Status: {} - Duration: {}ms",
                correlationId,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
        
        if (log.isDebugEnabled()) {
            // Log response headers
            response.getHeaderNames().forEach(headerName -> {
                if (!isSensitiveHeader(headerName)) {
                    String headerValue = response.getHeader(headerName);
                    log.debug("[{}] Response Header: {} = {}", 
                            correlationId, headerName, headerValue);
                } else {
                    log.debug("[{}] Response Header: {} = [HIDDEN]", 
                            correlationId, headerName);
                }
            });
            
            // Log response body if it's not too large
            logResponseBody(response, correlationId);
        }
        
        // Log performance warnings
        if (duration > 5000) {
            log.warn("[{}] Slow request detected: {} {} took {}ms",
                    correlationId, request.getMethod(), request.getRequestURI(), duration);
        }
        
        // Log error responses
        if (response.getStatus() >= 400) {
            log.warn("[{}] Error response: {} {} returned status {}",
                    correlationId, request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }
    
    private void logRequestBody(ContentCachingRequestWrapper request, String correlationId) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content);
            if (body.length() > 1000) {
                log.debug("[{}] Request Body: {}... (truncated)", 
                        correlationId, body.substring(0, 1000));
            } else {
                log.debug("[{}] Request Body: {}", correlationId, body);
            }
        }
    }
    
    private void logResponseBody(ContentCachingResponseWrapper response, String correlationId) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0 && content.length < 2000) { // Only log small responses
            String body = new String(content);
            log.debug("[{}] Response Body: {}", correlationId, body);
        } else if (content.length >= 2000) {
            log.debug("[{}] Response Body: [Large response - {} bytes]", 
                    correlationId, content.length);
        }
    }
    
    private boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private boolean isSensitiveHeader(String headerName) {
        return SENSITIVE_HEADERS.stream()
                .anyMatch(sensitive -> headerName.toLowerCase().contains(sensitive));
    }
    
    private boolean hasBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}