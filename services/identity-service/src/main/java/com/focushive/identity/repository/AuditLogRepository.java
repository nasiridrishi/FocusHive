package com.focushive.identity.repository;

import com.focushive.identity.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by user ID.
     */
    List<AuditLog> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find audit logs by user ID (direct query for when user relationship might be null).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    /**
     * Delete all audit logs for a specific user.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    /**
     * Find audit logs by event type.
     */
    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);

    /**
     * Find audit logs within a time range.
     */
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);

    /**
     * Find audit logs by user and event type.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.user.id = :userId AND a.eventType = :eventType ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserIdAndEventType(@Param("userId") UUID userId, @Param("eventType") String eventType);

    /**
     * Find audit logs by event category.
     */
    List<AuditLog> findByEventCategoryOrderByCreatedAtDesc(String eventCategory);

    /**
     * Find audit logs by outcome (SUCCESS, FAILURE, etc.).
     */
    List<AuditLog> findByOutcomeOrderByCreatedAtDesc(String outcome);

    /**
     * Find audit logs by severity level.
     */
    List<AuditLog> findBySeverityOrderByCreatedAtDesc(String severity);

    /**
     * Find audit logs with risk score above threshold.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.riskScore >= :threshold ORDER BY a.createdAt DESC")
    List<AuditLog> findByRiskScoreGreaterThanEqualOrderByCreatedAtDesc(@Param("threshold") Integer threshold);

    /**
     * Find audit logs for a specific IP address.
     */
    List<AuditLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * Count audit logs by user.
     */
    long countByUser_Id(UUID userId);

    /**
     * Count failed events for a user.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.user.id = :userId AND a.outcome = 'FAILURE'")
    long countFailedEventsByUserId(@Param("userId") UUID userId);

    /**
     * Find recent audit logs for security monitoring.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :since AND a.eventCategory IN ('SECURITY', 'AUTHENTICATION') ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentSecurityEvents(@Param("since") Instant since);
}