package com.focushive.integration.service;

import com.focushive.integration.client.NotificationServiceClient;
import com.focushive.integration.dto.NotificationDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for integrating with the Notification microservice.
 * Handles all notification-related operations through Feign client.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.features.notification.enabled", havingValue = "true")
public class NotificationIntegrationService {

    private final NotificationServiceClient notificationClient;

    @Autowired
    public NotificationIntegrationService(NotificationServiceClient notificationClient) {
        this.notificationClient = notificationClient;
        log.info("Notification Integration Service initialized");
    }

    /**
     * Send a notification to a user.
     */
    public NotificationDtos.NotificationResponse sendNotification(String userId, String type, String title,
                                                  String message, String channel) {
        log.debug("Sending notification to user: {} via channel: {}", userId, channel);

        NotificationDtos.NotificationRequest request = new NotificationDtos.NotificationRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setTitle(title);
        request.setMessage(message);
        request.setChannel(channel);

        try {
            NotificationDtos.NotificationResponse response = notificationClient.sendNotification(request);
            log.info("Notification sent successfully to user: {}, id: {}", userId, response.getId());
            return response;
        } catch (Exception e) {
            log.error("Failed to send notification to user: {}", userId, e);
            // Fallback response is handled by NotificationServiceFallback
            throw e;
        }
    }

    /**
     * Send batch notifications to multiple users.
     */
    public List<NotificationDtos.NotificationResponse> sendBatchNotifications(List<NotificationDtos.NotificationRequest> requests) {
        log.debug("Sending batch notifications: {} notifications", requests.size());

        try {
            List<NotificationDtos.NotificationResponse> responses = notificationClient.sendBatchNotifications(requests);
            log.info("Batch notifications sent: {} successful", responses.size());
            return responses;
        } catch (Exception e) {
            log.error("Failed to send batch notifications", e);
            throw e;
        }
    }

    /**
     * Send a hive-related notification.
     */
    public NotificationDtos.NotificationResponse sendHiveNotification(String userId, String hiveId,
                                                      String event, String message) {
        log.debug("Sending hive notification - User: {}, Hive: {}, Event: {}", userId, hiveId, event);

        NotificationDtos.NotificationRequest request = new NotificationDtos.NotificationRequest();
        request.setUserId(userId);
        request.setType("HIVE_EVENT");
        request.setTitle("Hive Activity: " + event);
        request.setMessage(message);
        request.setChannel("IN_APP");
        request.setMetadata(Map.of(
            "hiveId", hiveId,
            "event", event
        ));

        return sendNotification(request);
    }

    /**
     * Send a buddy system notification.
     */
    public NotificationDtos.NotificationResponse sendBuddyNotification(String userId, String buddyId,
                                                       String event, String message) {
        log.debug("Sending buddy notification - User: {}, Buddy: {}, Event: {}", userId, buddyId, event);

        NotificationDtos.NotificationRequest request = new NotificationDtos.NotificationRequest();
        request.setUserId(userId);
        request.setType("BUDDY_EVENT");
        request.setTitle("Buddy System: " + event);
        request.setMessage(message);
        request.setChannel("PUSH");
        request.setMetadata(Map.of(
            "buddyId", buddyId,
            "event", event
        ));

        return sendNotification(request);
    }

    /**
     * Send a focus session reminder.
     */
    public NotificationDtos.NotificationResponse sendFocusSessionReminder(String userId, String sessionId,
                                                          int minutesBefore) {
        log.debug("Sending focus session reminder - User: {}, Session: {}", userId, sessionId);

        NotificationDtos.NotificationRequest request = new NotificationDtos.NotificationRequest();
        request.setUserId(userId);
        request.setType("REMINDER");
        request.setTitle("Focus Session Starting Soon");
        request.setMessage("Your focus session starts in " + minutesBefore + " minutes");
        request.setChannel("PUSH");
        request.setMetadata(Map.of(
            "sessionId", sessionId,
            "minutesBefore", minutesBefore
        ));

        return sendNotification(request);
    }

    /**
     * Get user notifications.
     */
    public List<NotificationDtos.NotificationResponse> getUserNotifications(String userId) {
        log.debug("Retrieving notifications for user: {}", userId);

        try {
            List<NotificationDtos.NotificationResponse> notifications = notificationClient.getUserNotifications(userId);
            log.info("Retrieved {} notifications for user: {}", notifications.size(), userId);
            return notifications;
        } catch (Exception e) {
            log.error("Failed to retrieve notifications for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Mark notification as read.
     */
    public void markNotificationAsRead(String notificationId) {
        log.debug("Marking notification as read: {}", notificationId);

        try {
            notificationClient.markAsRead(notificationId);
            log.info("Notification marked as read: {}", notificationId);
        } catch (Exception e) {
            log.error("Failed to mark notification as read: {}", notificationId, e);
            throw e;
        }
    }

    /**
     * Delete a notification.
     */
    public void deleteNotification(String notificationId) {
        log.debug("Deleting notification: {}", notificationId);

        try {
            notificationClient.deleteNotification(notificationId);
            log.info("Notification deleted: {}", notificationId);
        } catch (Exception e) {
            log.error("Failed to delete notification: {}", notificationId, e);
            throw e;
        }
    }

    /**
     * Check notification service health.
     */
    public boolean isNotificationServiceHealthy() {
        try {
            String health = notificationClient.healthCheck();
            return health != null && health.contains("UP");
        } catch (Exception e) {
            log.warn("Notification service health check failed", e);
            return false;
        }
    }

    private NotificationDtos.NotificationResponse sendNotification(NotificationDtos.NotificationRequest request) {
        try {
            return notificationClient.sendNotification(request);
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            throw e;
        }
    }
}