package com.focushive.notification.service;

import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Cached implementation of NotificationService.
 * Provides query result caching to improve performance and reduce database load.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@CacheConfig(cacheNames = "notifications")
public class CachedNotificationService implements NotificationService {

    private final NotificationServiceImpl delegate;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#request.userId"),
            @CacheEvict(value = "notificationCount", key = "#request.userId")
    })
    public NotificationDto createNotification(CreateNotificationRequest request) {
        log.debug("Creating notification and evicting cache for user: {}", request.getUserId());
        return delegate.createNotification(request);
    }

    @Override
    @Cacheable(
            value = "userNotifications",
            key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()",
            condition = "#pageable.pageNumber < 5" // Only cache first 5 pages
    )
    public NotificationResponse getNotifications(String userId, Pageable pageable) {
        log.debug("Fetching notifications for user: {} with pagination: {}", userId, pageable);
        return delegate.getNotifications(userId, pageable);
    }

    @Override
    @Cacheable(
            value = "userNotifications",
            key = "'unread:' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
            condition = "#pageable.pageNumber < 3" // Only cache first 3 pages of unread
    )
    public NotificationResponse getUnreadNotifications(String userId, Pageable pageable) {
        log.debug("Fetching unread notifications for user: {} with pagination: {}", userId, pageable);
        return delegate.getUnreadNotifications(userId, pageable);
    }

    @Override
    @Cacheable(
            value = "userNotifications",
            key = "'type:' + #type + ':' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
            condition = "#pageable.pageNumber < 3"
    )
    public NotificationResponse getNotificationsByType(String userId, String type, Pageable pageable) {
        log.debug("Fetching notifications by type: {} for user: {}", type, userId);
        return delegate.getNotificationsByType(userId, type, pageable);
    }

    @Override
    @Cacheable(value = "notificationCount", key = "#userId")
    public long getUnreadCount(String userId) {
        log.debug("Fetching unread count for user: {}", userId);
        return delegate.getUnreadCount(userId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#userId", allEntries = true),
            @CacheEvict(value = "notificationCount", key = "#userId")
    })
    public void markAsRead(String notificationId, String userId) {
        log.debug("Marking notification {} as read for user: {}, evicting cache", notificationId, userId);
        delegate.markAsRead(notificationId, userId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#userId", allEntries = true),
            @CacheEvict(value = "notificationCount", key = "#userId")
    })
    public int markAllAsRead(String userId) {
        log.debug("Marking all notifications as read for user: {}, evicting cache", userId);
        return delegate.markAllAsRead(userId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#userId", allEntries = true),
            @CacheEvict(value = "notificationCount", key = "#userId")
    })
    public void deleteNotification(String notificationId, String userId) {
        log.debug("Deleting notification {} for user: {}, evicting cache", notificationId, userId);
        delegate.deleteNotification(notificationId, userId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#userId", allEntries = true),
            @CacheEvict(value = "notificationCount", key = "#userId")
    })
    public void archiveNotification(String notificationId, String userId) {
        log.debug("Archiving notification {} for user: {}, evicting cache", notificationId, userId);
        delegate.archiveNotification(notificationId, userId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "userNotifications", key = "#userId", allEntries = true),
            @CacheEvict(value = "notificationCount", key = "#userId")
    })
    public int cleanupOldNotifications(String userId, int daysToKeep) {
        log.debug("Cleaning up old notifications for user: {}, keeping last {} days, evicting cache", userId, daysToKeep);
        return delegate.cleanupOldNotifications(userId, daysToKeep);
    }

    @Override
    public int bulkMarkAsRead(java.util.List<Long> notificationIds) {
        log.debug("Bulk marking {} notifications as read", notificationIds.size());
        return delegate.bulkMarkAsRead(notificationIds);
    }

    @Override
    public int bulkDelete(java.util.List<Long> notificationIds) {
        log.debug("Bulk deleting {} notifications", notificationIds.size());
        return delegate.bulkDelete(notificationIds);
    }

    @Override
    public NotificationStatistics getStatistics(String userId) {
        log.debug("Getting statistics for user: {}", userId);
        return delegate.getStatistics(userId);
    }
}