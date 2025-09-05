package com.focushive.notification.repository;

import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.Notification.NotificationPriority;
import com.focushive.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Notification entities.
 * Provides comprehensive CRUD operations and custom queries for notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * Find all notifications for a user, ordered by creation date descending.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of notifications
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find unread notifications for a user, ordered by priority and creation date descending.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of unread notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false " +
           "ORDER BY n.priority DESC, n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Find notifications by type for a user, ordered by creation date descending.
     *
     * @param userId the user ID
     * @param type notification type
     * @param pageable pagination information
     * @return page of notifications of the specified type
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, NotificationType type, Pageable pageable);

    /**
     * Find unread notifications by priority for a user.
     *
     * @param userId the user ID
     * @param priority notification priority
     * @return list of unread notifications with specified priority
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.priority = :priority " +
           "AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByPriority(@Param("userId") String userId, @Param("priority") NotificationPriority priority);

    /**
     * Count unread notifications for a user.
     *
     * @param userId the user ID
     * @return count of unread notifications
     */
    long countByUserIdAndIsReadFalse(String userId);

    /**
     * Count unread notifications by priority for a user.
     *
     * @param userId the user ID
     * @param priority notification priority
     * @return count of unread notifications with specified priority
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId " +
           "AND n.isRead = false AND n.priority = :priority")
    long countUnreadByPriority(@Param("userId") String userId, @Param("priority") NotificationPriority priority);

    /**
     * Find a notification by ID and user ID for security.
     *
     * @param id notification ID
     * @param userId user ID
     * @return optional notification if found and belongs to user
     */
    Optional<Notification> findByIdAndUserId(String id, String userId);

    /**
     * Check if notification exists and belongs to user.
     *
     * @param id notification ID
     * @param userId user ID
     * @return true if exists and belongs to user
     */
    boolean existsByIdAndUserId(String id, String userId);

    /**
     * Mark a specific notification as read.
     *
     * @param notificationId notification ID
     * @param userId user ID
     * @param now current timestamp
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now, n.updatedAt = :now " +
           "WHERE n.id = :notificationId AND n.userId = :userId")
    void markAsRead(@Param("notificationId") String notificationId, @Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Mark all unread notifications as read for a user.
     *
     * @param userId the user ID
     * @param now current timestamp
     * @return number of notifications updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now, n.updatedAt = :now " +
           "WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Mark a notification as archived.
     *
     * @param notificationId notification ID
     * @param userId user ID
     * @param now current timestamp
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isArchived = true, n.archivedAt = :now, n.updatedAt = :now " +
           "WHERE n.id = :notificationId AND n.userId = :userId")
    void markAsArchived(@Param("notificationId") String notificationId, @Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Find notifications that are not archived.
     *
     * @param userId the user ID
     * @param isArchived archive status (false for not archived)
     * @param pageable pagination information
     * @return page of non-archived notifications
     */
    Page<Notification> findByUserIdAndIsArchivedOrderByCreatedAtDesc(String userId, Boolean isArchived, Pageable pageable);

    /**
     * Delete old read notifications for cleanup.
     *
     * @param userId the user ID
     * @param cutoffDate date before which to delete read notifications
     * @return number of notifications deleted
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.isRead = true AND n.readAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("userId") String userId, @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find recent notifications for a user.
     *
     * @param userId the user ID
     * @param since timestamp to filter from
     * @return list of recent notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotifications(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Mark multiple notifications as read in batch.
     *
     * @param notificationIds list of notification IDs
     * @param userId user ID
     * @param now current timestamp
     * @return number of notifications updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now, n.updatedAt = :now " +
           "WHERE n.id IN :notificationIds AND n.userId = :userId")
    int markMultipleAsRead(@Param("notificationIds") List<String> notificationIds, @Param("userId") String userId, @Param("now") LocalDateTime now);

    /**
     * Find notifications by action URL pattern.
     *
     * @param userId the user ID
     * @param urlPattern URL pattern to match
     * @return list of notifications matching the pattern
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND n.actionUrl LIKE :urlPattern ORDER BY n.createdAt DESC")
    List<Notification> findByActionUrlPattern(@Param("userId") String userId, @Param("urlPattern") String urlPattern);

    /**
     * Delete archived notifications older than specified date.
     *
     * @param cutoffDate date before which to delete archived notifications
     * @return number of notifications deleted
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isArchived = true AND n.createdAt < :cutoffDate")
    int deleteArchivedNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find failed notifications that need retry.
     *
     * @param maxRetries maximum number of delivery attempts
     * @param retryAfter retry notifications failed after this time
     * @return list of notifications eligible for retry
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryAttempts < :maxRetries " +
           "AND n.failedAt IS NOT NULL AND n.failedAt < :retryAfter " +
           "ORDER BY n.failedAt ASC")
    List<Notification> findFailedNotificationsForRetry(@Param("maxRetries") Integer maxRetries, @Param("retryAfter") LocalDateTime retryAfter);

    /**
     * Mark notification delivery as successful.
     *
     * @param notificationId notification ID
     * @param now current timestamp
     */
    @Modifying
    @Query("UPDATE Notification n SET n.deliveredAt = :now, n.updatedAt = :now " +
           "WHERE n.id = :notificationId")
    void markAsDelivered(@Param("notificationId") String notificationId, @Param("now") LocalDateTime now);

    /**
     * Mark notification delivery as failed.
     *
     * @param notificationId notification ID
     * @param reason failure reason
     * @param now current timestamp
     */
    @Modifying
    @Query("UPDATE Notification n SET n.failedAt = :now, n.failureReason = :reason, " +
           "n.deliveryAttempts = n.deliveryAttempts + 1, n.updatedAt = :now " +
           "WHERE n.id = :notificationId")
    void markAsDeliveryFailed(@Param("notificationId") String notificationId, @Param("reason") String reason, @Param("now") LocalDateTime now);

    /**
     * Count notifications by delivery status.
     *
     * @return array containing [delivered_count, failed_count, pending_count]
     */
    @Query("SELECT " +
           "SUM(CASE WHEN n.deliveredAt IS NOT NULL THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.failedAt IS NOT NULL THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN n.deliveredAt IS NULL AND n.failedAt IS NULL THEN 1 ELSE 0 END) " +
           "FROM Notification n")
    Object[] getDeliveryStatistics();
}