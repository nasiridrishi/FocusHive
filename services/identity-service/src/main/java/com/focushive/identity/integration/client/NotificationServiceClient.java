package com.focushive.identity.integration.client;

import com.focushive.identity.integration.dto.NotificationRequest;
import com.focushive.identity.integration.dto.NotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for communicating with the notification service.
 * Handles all notification-related operations including sending emails,
 * SMS, and in-app notifications.
 */
@FeignClient(
    name = "notification-service",
    url = "${notification.service.url:http://localhost:8083}",
    fallback = NotificationServiceFallback.class,
    configuration = com.focushive.identity.integration.config.NotificationServiceFeignConfig.class
)
public interface NotificationServiceClient {

    /**
     * Send a notification to the notification service.
     * The notification service will handle delivery based on the notification type
     * and user preferences.
     *
     * @param request the notification request containing recipient and content details
     * @return the response from the notification service with notification ID and status
     */
    @PostMapping("/api/v1/notifications")
    NotificationResponse sendNotification(@RequestBody NotificationRequest request);

    /**
     * Check the health of the notification service.
     * Used for monitoring and circuit breaker health checks.
     *
     * @return health status JSON string
     */
    @GetMapping("/actuator/health")
    String healthCheck();
}