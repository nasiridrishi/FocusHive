package com.focushive.notification.service;

import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for notification management.
 */
public interface NotificationService {

    /**
     * Create a new notification for a user.
     *
     * @param request the notification creation request
     * @return the created notification DTO
     */
    NotificationDto createNotification(CreateNotificationRequest request);

    /**
     * Get paginated notifications for a user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return paginated notification response
     */
    NotificationResponse getNotifications(String userId, Pageable pageable);

    /**
     * Get paginated unread notifications for a user.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return paginated notification response with unread notifications
     */
    NotificationResponse getUnreadNotifications(String userId, Pageable pageable);

    /**
     * Get notifications by type for a user.
     *
     * @param userId the user ID
     * @param type the notification type
     * @param pageable pagination information
     * @return paginated notification response
     */
    NotificationResponse getNotificationsByType(String userId, String type, Pageable pageable);

    /**
     * Get count of unread notifications for a user.
     *
     * @param userId the user ID
     * @return count of unread notifications
     */
    long getUnreadCount(String userId);

    /**
     * Mark a notification as read.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for security)
     * @throws IllegalArgumentException if notification not found or doesn't belong to user
     */
    void markAsRead(String notificationId, String userId);

    /**
     * Mark all notifications as read for a user.
     *
     * @param userId the user ID
     * @return number of notifications marked as read
     */
    int markAllAsRead(String userId);

    /**
     * Delete a notification.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for security)
     * @throws IllegalArgumentException if notification not found or doesn't belong to user
     */
    void deleteNotification(String notificationId, String userId);

    /**
     * Archive a notification.
     *
     * @param notificationId the notification ID
     * @param userId the user ID (for security)
     * @throws IllegalArgumentException if notification not found or doesn't belong to user
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
}