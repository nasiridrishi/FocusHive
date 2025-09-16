package com.focushive.identity.integration.client;

import com.focushive.identity.integration.dto.NotificationRequest;
import com.focushive.identity.integration.dto.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for NotificationServiceClient.
 * This class provides fallback behavior when the notification service is unavailable
 * or when circuit breaker is triggered. It ensures the identity service can continue
 * to function even when the notification service is down.
 */
@Slf4j
@Component
public class NotificationServiceFallback implements NotificationServiceClient {

    /**
     * Fallback method for sending notifications.
     * Logs the notification failure and returns null to indicate the notification
     * could not be sent. The calling service should handle this gracefully.
     *
     * @param request the notification request that failed to send
     * @return null indicating the notification could not be sent
     */
    @Override
    public NotificationResponse sendNotification(NotificationRequest request) {
        log.error("Failed to send notification to user {} of type {}. Notification service is unavailable.",
                request.getUserId(), request.getType());

        // Log critical notifications with higher severity
        if ("CRITICAL".equals(request.getPriority())) {
            log.error("CRITICAL notification failed for user {}: {}",
                    request.getUserId(), request.getTitle());
        }

        // In production, this could also:
        // 1. Queue the notification for retry
        // 2. Send to a dead letter queue
        // 3. Store in database for later processing
        // 4. Send alert to operations team for critical notifications

        return null; // Caller must handle null response gracefully
    }

    /**
     * Fallback health check method.
     * Returns a DOWN status when the notification service cannot be reached.
     *
     * @return JSON string indicating the service is down
     */
    @Override
    public String healthCheck() {
        log.warn("Notification service health check failed - circuit breaker activated");
        return "{\"status\":\"DOWN\",\"message\":\"Notification service unavailable - fallback activated\"}";
    }
}