package com.focushive.notification.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing correlation IDs to track requests across service boundaries.
 * Provides structured logging with correlation IDs for better observability.
 */
@Service
@Slf4j
public class CorrelationIdService {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String OPERATION_MDC_KEY = "operation";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    /**
     * Generate a new correlation ID.
     */
    public String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Set the correlation ID in MDC for the current thread.
     */
    public void setCorrelationId(String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
        }
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        log.debug("Set correlation ID: {}", correlationId);
    }

    /**
     * Get the current correlation ID from MDC.
     */
    public String getCorrelationId() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }

    /**
     * Set user ID in MDC for the current thread.
     */
    public void setUserId(String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(USER_ID_MDC_KEY, userId);
            log.debug("Set user ID in MDC: {}", userId);
        }
    }

    /**
     * Get the current user ID from MDC.
     */
    public String getUserId() {
        return MDC.get(USER_ID_MDC_KEY);
    }

    /**
     * Set operation name in MDC for the current thread.
     */
    public void setOperation(String operation) {
        if (operation != null && !operation.trim().isEmpty()) {
            MDC.put(OPERATION_MDC_KEY, operation);
            log.debug("Set operation in MDC: {}", operation);
        }
    }

    /**
     * Get the current operation from MDC.
     */
    public String getOperation() {
        return MDC.get(OPERATION_MDC_KEY);
    }

    /**
     * Set request ID in MDC for the current thread.
     */
    public void setRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isEmpty()) {
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            log.debug("Set request ID in MDC: {}", requestId);
        }
    }

    /**
     * Get the current request ID from MDC.
     */
    public String getRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }

    /**
     * Set up full tracing context for a request.
     */
    public void setupTracingContext(String correlationId, String userId, String operation, String requestId) {
        setCorrelationId(correlationId);
        setUserId(userId);
        setOperation(operation);
        setRequestId(requestId);
        
        log.info("Set up tracing context - correlationId: {}, userId: {}, operation: {}, requestId: {}", 
                correlationId, userId, operation, requestId);
    }

    /**
     * Clear all tracing context from MDC.
     */
    public void clearTracingContext() {
        String correlationId = getCorrelationId();
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(USER_ID_MDC_KEY);
        MDC.remove(OPERATION_MDC_KEY);
        MDC.remove(REQUEST_ID_MDC_KEY);
        
        log.debug("Cleared tracing context for correlation ID: {}", correlationId);
    }

    /**
     * Execute a runnable with tracing context.
     */
    public void executeWithTracingContext(String correlationId, String userId, String operation, Runnable task) {
        String requestId = generateCorrelationId(); // Generate unique request ID
        setupTracingContext(correlationId, userId, operation, requestId);
        
        try {
            task.run();
        } finally {
            clearTracingContext();
        }
    }

    /**
     * Get current tracing context as a formatted string for logging.
     */
    public String getTracingContextString() {
        String correlationId = getCorrelationId();
        String userId = getUserId();
        String operation = getOperation();
        String requestId = getRequestId();
        
        StringBuilder context = new StringBuilder();
        context.append("[");
        
        if (correlationId != null) {
            context.append("correlationId=").append(correlationId);
        }
        
        if (userId != null) {
            if (context.length() > 1) context.append(", ");
            context.append("userId=").append(userId);
        }
        
        if (operation != null) {
            if (context.length() > 1) context.append(", ");
            context.append("operation=").append(operation);
        }
        
        if (requestId != null) {
            if (context.length() > 1) context.append(", ");
            context.append("requestId=").append(requestId);
        }
        
        context.append("]");
        return context.toString();
    }

    /**
     * Check if correlation ID is set in current context.
     */
    public boolean hasCorrelationId() {
        return getCorrelationId() != null;
    }

    /**
     * Ensure correlation ID is set, generate one if missing.
     */
    public String ensureCorrelationId() {
        String correlationId = getCorrelationId();
        if (correlationId == null) {
            correlationId = generateCorrelationId();
            setCorrelationId(correlationId);
        }
        return correlationId;
    }
}