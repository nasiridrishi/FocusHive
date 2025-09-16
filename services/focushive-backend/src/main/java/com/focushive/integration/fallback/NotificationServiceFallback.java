package com.focushive.integration.fallback;

import com.focushive.integration.client.NotificationServiceClient;
import com.focushive.integration.dto.NotificationDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback implementation for Notification Service.
 * Provides graceful degradation when the service is unavailable.
 */
@Slf4j
@Component
public class NotificationServiceFallback implements NotificationServiceClient {

    @Override
    public NotificationDtos.NotificationResponse sendNotification(NotificationDtos.NotificationRequest request) {
        log.warn("Notification service unavailable. Falling back for notification: {}", request);
        return createFallbackResponse("Notification queued for later delivery");
    }

    @Override
    public List<NotificationDtos.NotificationResponse> sendBatchNotifications(List<NotificationDtos.NotificationRequest> requests) {
        log.warn("Notification service unavailable. Falling back for batch of {} notifications", requests.size());
        return Collections.singletonList(createFallbackResponse("Batch notifications queued for later delivery"));
    }

    @Override
    public List<NotificationDtos.NotificationResponse> getUserNotifications(String userId) {
        log.warn("Notification service unavailable. Cannot retrieve notifications for user: {}", userId);
        return Collections.emptyList();
    }

    @Override
    public NotificationDtos.NotificationResponse getNotification(String notificationId) {
        log.warn("Notification service unavailable. Cannot retrieve notification: {}", notificationId);
        return createFallbackResponse("Service temporarily unavailable");
    }

    @Override
    public void markAsRead(String notificationId) {
        log.warn("Notification service unavailable. Cannot mark notification as read: {}", notificationId);
    }

    @Override
    public void deleteNotification(String notificationId) {
        log.warn("Notification service unavailable. Cannot delete notification: {}", notificationId);
    }

    @Override
    public String healthCheck() {
        return "{\"status\":\"DOWN\",\"message\":\"Notification service unreachable\"}";
    }

    private NotificationDtos.NotificationResponse createFallbackResponse(String message) {
        NotificationDtos.NotificationResponse response = new NotificationDtos.NotificationResponse();
        response.setId(UUID.randomUUID().toString());
        response.setMessage(message);
        response.setStatus("FALLBACK");
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}