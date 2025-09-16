package com.focushive.notification.repository;

import com.focushive.notification.entity.DeadLetterMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing dead letter messages.
 * Provides queries for monitoring and managing failed notifications.
 */
@Repository
public interface DeadLetterMessageRepository extends JpaRepository<DeadLetterMessage, Long> {

    /**
     * Find messages by status.
     */
    List<DeadLetterMessage> findByStatus(DeadLetterMessage.Status status);

    /**
     * Find messages by status with pagination.
     */
    Page<DeadLetterMessage> findByStatus(DeadLetterMessage.Status status, Pageable pageable);

    /**
     * Count messages by status.
     */
    long countByStatus(DeadLetterMessage.Status status);

    /**
     * Find messages by recipient.
     */
    List<DeadLetterMessage> findByRecipient(String recipient);

    /**
     * Find messages created before a certain date with specific statuses.
     */
    List<DeadLetterMessage> findByCreatedAtBeforeAndStatusIn(
        LocalDateTime createdAt,
        List<DeadLetterMessage.Status> statuses
    );

    /**
     * Find messages that can be retried.
     */
    @Query("SELECT d FROM DeadLetterMessage d WHERE d.status IN ('PENDING', 'RETRY_FAILED') AND d.retryCount < 3")
    List<DeadLetterMessage> findRetriableMessages();

    /**
     * Find messages by user ID.
     */
    List<DeadLetterMessage> findByUserId(Long userId);

    /**
     * Find critical priority messages that are pending.
     */
    @Query("SELECT d FROM DeadLetterMessage d WHERE d.priority = 'CRITICAL' AND d.status = 'PENDING'")
    List<DeadLetterMessage> findCriticalPendingMessages();

    /**
     * Get statistics by status.
     */
    @Query("SELECT d.status, COUNT(d) FROM DeadLetterMessage d GROUP BY d.status")
    List<Object[]> getStatusStatistics();

    /**
     * Find messages that have been pending for more than specified hours.
     */
    @Query("SELECT d FROM DeadLetterMessage d WHERE d.status = 'PENDING' AND d.createdAt < :cutoffTime")
    List<DeadLetterMessage> findStalePendingMessages(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find messages by notification type.
     */
    List<DeadLetterMessage> findByNotificationType(String notificationType);

    /**
     * Delete messages older than specified date.
     */
    void deleteByCreatedAtBefore(LocalDateTime createdAt);

    /**
     * Find most recent failures.
     */
    List<DeadLetterMessage> findTop10ByOrderByCreatedAtDesc();

    /**
     * Find messages with specific error patterns.
     */
    @Query("SELECT d FROM DeadLetterMessage d WHERE d.errorMessage LIKE %:errorPattern%")
    List<DeadLetterMessage> findByErrorPattern(@Param("errorPattern") String errorPattern);

    /**
     * Get daily failure count for the last N days.
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
                   "FROM dead_letter_messages " +
                   "WHERE created_at >= :startDate " +
                   "GROUP BY DATE(created_at) " +
                   "ORDER BY date DESC",
           nativeQuery = true)
    List<Object[]> getDailyFailureStats(@Param("startDate") LocalDateTime startDate);

    /**
     * Find messages by original queue.
     */
    List<DeadLetterMessage> findByOriginalQueue(String originalQueue);

    /**
     * Check if a message ID already exists.
     */
    boolean existsByMessageId(String messageId);

    /**
     * Find message by message ID.
     */
    Optional<DeadLetterMessage> findByMessageId(String messageId);
}