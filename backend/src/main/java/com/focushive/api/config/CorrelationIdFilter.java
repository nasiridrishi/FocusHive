package com.focushive.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;
import brave.Tracing;
import brave.propagation.TraceContext;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to handle correlation IDs for distributed tracing.
 * Extracts correlation ID from incoming requests or generates a new one.
 */
@Slf4j
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String correlationId = extractOrGenerateCorrelationId(request);
        
        try {
            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add to request attributes for use in services
            request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
            
            // Add to response header for client correlation
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Add trace ID to MDC if available
            try {
                TraceContext traceContext = Tracing.current().currentTraceContext().get();
                if (traceContext != null) {
                    String traceId = traceContext.traceIdString();
                    String spanId = traceContext.spanIdString();
                    MDC.put("traceId", traceId);
                    MDC.put("spanId", spanId);
                    response.setHeader("X-Trace-ID", traceId);
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Tracing not available or not configured");
                }
            }
            
            if (log.isDebugEnabled()) {
                log.debug("Processing request with correlation ID: {}", correlationId);
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            // Clean up MDC
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }
    
    /**
     * Extract correlation ID from request header or generate a new one.
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            if (log.isDebugEnabled()) {
                log.debug("Generated new correlation ID: {}", correlationId);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Using existing correlation ID: {}", correlationId);
            }
        }
        
        return correlationId;
    }
    
    /**
     * Utility method to get current correlation ID from request context.
     */
    public static String getCurrentCorrelationId() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            Object correlationId = attributes.getRequest().getAttribute(CORRELATION_ID_ATTRIBUTE);
            return correlationId != null ? correlationId.toString() : null;
        } catch (Exception e) {
            return MDC.get(CORRELATION_ID_MDC_KEY);
        }
    }
}