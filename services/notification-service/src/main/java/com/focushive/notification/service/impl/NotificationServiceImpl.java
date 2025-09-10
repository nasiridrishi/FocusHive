package com.focushive.notification.service.impl;

import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Implementation of NotificationService.
 * This is a minimal implementation to satisfy dependency injection for testing.
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public NotificationDto createNotification(CreateNotificationRequest request) {
        log.debug("Creating notification for user {}", request.getUserId());
        throw new UnsupportedOperationException("NotificationService implementation is not yet complete");
    }

    @Override
    public NotificationResponse getNotifications(String userId, Pageable pageable) {
        log.debug("Getting notifications for user {}", userId);
        throw new UnsupportedOperationException("NotificationService implementation is not yet complete");
    }

    @Override
    public NotificationResponse getUnreadNotifications(String userId, Pageable pageable) {
        log.debug("Getting unread notifications for user {}", userId);
        throw new UnsupportedOperationException("NotificationService implementation is not yet complete");
    }

    @Override
    public NotificationResponse getNotificationsByType(String userId, String type, Pageable pageable) {
        log.debug("Getting notifications by type {} for user {}", type, userId);
        throw new UnsupportedOperationException("NotificationService implementation is not yet complete");
    }

    @Override
    public long getUnreadCount(String userId) {
        log.debug("Getting unread count for user {}", userId);
        return 0;
    }

    @Override
    public void markAsRead(String notificationId, String userId) {
        log.debug("Marking notification {} as read for user {}", notificationId, userId);
        throw new UnsupportedOperationException("NotificationService implementation is not yet complete");
    }

    @Override
    public int markAllAsRead(String userId) {
        log.debug("Marking all notifications as read for user {}", userId);
        return 0;
    }

    @Override
    public void deleteNotification(String notificationId, String userId) {
        log.debug("Deleting notification {} for user {}", notificationId, userId);
        throw new UnsupportedOperationException("NotificationService implementation is not yet complete");
    }

    @Override
    public void archiveNotification(String notificationId, String userId) {
        log.debug("Archiving notification {} for user {}", notificationId, userId);
        throw new UnsupportedOperationException("NotificationService implementation is not yet complete");
    }

    @Override
    public int cleanupOldNotifications(String userId, int daysToKeep) {
        log.debug("Cleaning up old notifications for user {}", userId);
        return 0;
    }
}