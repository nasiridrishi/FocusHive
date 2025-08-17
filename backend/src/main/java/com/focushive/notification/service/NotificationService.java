package com.focushive.notification.service;

import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;

/**
 * Service interface for managing notifications.
 */
public interface NotificationService {

    /**
     * Create and deliver a new notification.
     *
     * @param request the notification creation request
     * @return the created notification DTO
     */
    NotificationDto createNotification(CreateNotificationRequest request);

    /**
     * Create and deliver a new notification with current time override.
     * Used for testing quiet hours functionality.
     *
     * @param request the notification creation request
     * @param currentTime the current time to use for quiet hours check
     * @return the created notification DTO
     */
    NotificationDto createNotification(CreateNotificationRequest request, LocalTime currentTime);

    /**
     * Get paginated notifications for a user.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return paginated notification response
     */
    NotificationResponse getNotifications(String userId, Pageable pageable);

    /**
     * Get paginated unread notifications for a user.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return paginated notification response
     */
    NotificationResponse getUnreadNotifications(String userId, Pageable pageable);

    /**
     * Mark a notification as read.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for security)
     */
    void markAsRead(String notificationId, String userId);

    /**
     * Mark all unread notifications for a user as read.
     *
     * @param userId the user ID
     * @return number of notifications marked as read
     */
    int markAllAsRead(String userId);

    /**
     * Get the count of unread notifications for a user.
     *
     * @param userId the user ID
     * @return count of unread notifications
     */
    long getUnreadCount(String userId);

    /**
     * Delete a notification.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for security)
     */
    void deleteNotification(String notificationId, String userId);

    /**
     * Archive a notification.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for security)
     */
    void archiveNotification(String notificationId, String userId);

    /**
     * Clean up old read notifications for a user.
     *
     * @param userId the user ID
     * @param daysToKeep number of days to keep read notifications
     * @return number of notifications deleted
     */
    int cleanupOldNotifications(String userId, int daysToKeep);

    /**
     * Get notifications by type for a user.
     *
     * @param userId the user ID
     * @param type the notification type
     * @param pageable pagination parameters
     * @return paginated notification response
     */
    NotificationResponse getNotificationsByType(String userId, String type, Pageable pageable);
}