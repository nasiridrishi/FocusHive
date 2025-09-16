package com.focushive.notification.repository;

import com.focushive.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Custom repository interface for optimized notification queries.
 * Provides methods with proper fetch strategies to avoid N+1 problems.
 */
public interface NotificationRepositoryCustom {

    /**
     * Find notifications with optimized batch fetching.
     * Uses batch size hints to prevent N+1 queries.
     */
    Page<Notification> findNotificationsWithOptimizedFetch(String userId, Pageable pageable);

    /**
     * Find recent notifications with query hints for performance.
     */
    List<Notification> findRecentNotificationsOptimized(String userId, LocalDateTime since);

    /**
     * Batch load notifications by IDs to prevent multiple queries.
     */
    List<Notification> findByIdsOptimized(List<String> ids);
}