package com.focushive.notification.monitoring;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to handle correlation IDs in HTTP requests and responses.
 * Extracts correlation ID from request headers and sets up tracing context.
 */
@Component
@Order(1) // Highest priority to ensure correlation ID is set early
@RequiredArgsConstructor
@Slf4j
public class CorrelationIdFilter implements Filter {

    private final CorrelationIdService correlationIdService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Extract or generate correlation ID
        String correlationId = httpRequest.getHeader(CorrelationIdService.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = correlationIdService.generateCorrelationId();
        }
        
        // Set up tracing context
        String requestId = correlationIdService.generateCorrelationId();
        String operation = determineOperation(httpRequest);
        
        correlationIdService.setupTracingContext(correlationId, null, operation, requestId);
        
        try {
            // Add correlation ID to response headers
            httpResponse.setHeader(CorrelationIdService.CORRELATION_ID_HEADER, correlationId);
            httpResponse.setHeader("X-Request-ID", requestId);
            
            log.debug("Processing request {} {} with correlation ID: {}", 
                    httpRequest.getMethod(), httpRequest.getRequestURI(), correlationId);
            
            chain.doFilter(request, response);
            
        } finally {
            // Clean up tracing context
            correlationIdService.clearTracingContext();
        }
    }
    
    private String determineOperation(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Simplify operation names for better grouping
        if (uri.startsWith("/api/notifications")) {
            if (method.equals("GET")) {
                return "get_notifications";
            } else if (method.equals("POST")) {
                return "create_notification";
            } else if (method.equals("PUT")) {
                return "update_notification";
            } else if (method.equals("DELETE")) {
                return "delete_notification";
            }
        } else if (uri.startsWith("/api/preferences")) {
            if (method.equals("GET")) {
                return "get_preferences";
            } else if (method.equals("POST")) {
                return "create_preference";
            } else if (method.equals("PUT")) {
                return "update_preference";
            }
        } else if (uri.startsWith("/api/templates")) {
            if (method.equals("GET")) {
                return "get_templates";
            } else if (method.equals("POST")) {
                return "create_template";
            } else if (method.equals("PUT")) {
                return "update_template";
            }
        } else if (uri.startsWith("/api/admin")) {
            return "admin_operation";
        } else if (uri.startsWith("/actuator")) {
            return "actuator_" + uri.substring("/actuator/".length());
        }
        
        // Default operation name
        return method.toLowerCase() + "_" + uri.replaceAll("/", "_").replaceAll("[^a-zA-Z0-9_]", "");
    }
}