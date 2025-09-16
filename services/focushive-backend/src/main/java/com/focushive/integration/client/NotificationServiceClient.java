package com.focushive.integration.client;

import com.focushive.integration.dto.NotificationDtos;
import com.focushive.integration.fallback.NotificationServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for Notification Service communication.
 * Configured with circuit breaker and fallback for resilience.
 */
@FeignClient(
    name = "notification-service",
    url = "${notification.service.url:http://localhost:8083}",
    fallback = NotificationServiceFallback.class,
    configuration = IntegrationFeignConfiguration.class
)
public interface NotificationServiceClient {

    @PostMapping("/api/v1/notifications")
    NotificationDtos.NotificationResponse sendNotification(@RequestBody NotificationDtos.NotificationRequest request);

    @PostMapping("/api/v1/notifications/batch")
    List<NotificationDtos.NotificationResponse> sendBatchNotifications(@RequestBody List<NotificationDtos.NotificationRequest> requests);

    @GetMapping("/api/v1/notifications/user/{userId}")
    List<NotificationDtos.NotificationResponse> getUserNotifications(@PathVariable String userId);

    @GetMapping("/api/v1/notifications/{notificationId}")
    NotificationDtos.NotificationResponse getNotification(@PathVariable String notificationId);

    @PutMapping("/api/v1/notifications/{notificationId}/mark-read")
    void markAsRead(@PathVariable String notificationId);

    @DeleteMapping("/api/v1/notifications/{notificationId}")
    void deleteNotification(@PathVariable String notificationId);

    @GetMapping("/actuator/health")
    String healthCheck();
}