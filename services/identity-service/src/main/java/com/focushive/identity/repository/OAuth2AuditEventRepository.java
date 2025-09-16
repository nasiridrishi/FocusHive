package com.focushive.identity.repository;

import com.focushive.identity.audit.OAuth2AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OAuth2 audit events stored in PostgreSQL.
 * Provides comprehensive querying capabilities for security monitoring.
 */
@Repository
public interface OAuth2AuditEventRepository extends JpaRepository<OAuth2AuditEvent, UUID> {

    /**
     * Find audit events by user ID.
     */
    Page<OAuth2AuditEvent> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find audit events by client ID.
     */
    Page<OAuth2AuditEvent> findByClientIdOrderByTimestampDesc(String clientId, Pageable pageable);

    /**
     * Find audit events within a time range.
     */
    List<OAuth2AuditEvent> findByTimestampBetweenOrderByTimestampDesc(
        Instant startTime, Instant endTime);

    /**
     * Find failed authentication attempts for a client.
     */
    @Query("SELECT e FROM OAuth2AuditEvent e WHERE e.clientId = :clientId " +
           "AND e.success = false AND e.timestamp > :since " +
           "ORDER BY e.timestamp DESC")
    List<OAuth2AuditEvent> findFailedAttemptsForClient(
        @Param("clientId") String clientId,
        @Param("since") Instant since);

    /**
     * Find suspicious activities.
     */
    @Query("SELECT e FROM OAuth2AuditEvent e WHERE e.suspiciousActivity = true " +
           "AND e.timestamp > :since ORDER BY e.riskLevel DESC, e.timestamp DESC")
    List<OAuth2AuditEvent> findSuspiciousActivitiesSince(@Param("since") Instant since);

    /**
     * Count events by type within a time range.
     */
    @Query("SELECT e.eventType, COUNT(e) FROM OAuth2AuditEvent e " +
           "WHERE e.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY e.eventType")
    List<Object[]> countEventsByTypeInRange(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime);

    /**
     * Find high-risk events.
     */
    @Query("SELECT e FROM OAuth2AuditEvent e WHERE e.riskLevel IN ('HIGH', 'CRITICAL') " +
           "AND e.timestamp > :since ORDER BY e.timestamp DESC")
    List<OAuth2AuditEvent> findHighRiskEventsSince(@Param("since") Instant since);

    /**
     * Find events by IP address for threat detection.
     */
    List<OAuth2AuditEvent> findByIpAddressAndTimestampAfterOrderByTimestampDesc(
        String ipAddress, Instant since);

    /**
     * Count failed login attempts for a user.
     */
    @Query("SELECT COUNT(e) FROM OAuth2AuditEvent e " +
           "WHERE e.userId = :userId AND e.success = false " +
           "AND e.eventType IN ('AUTHORIZATION_FAILURE', 'CLIENT_AUTHENTICATION_FAILURE') " +
           "AND e.timestamp > :since")
    long countFailedLoginAttempts(
        @Param("userId") UUID userId,
        @Param("since") Instant since);

    /**
     * Find rate limit violations.
     */
    @Query("SELECT e FROM OAuth2AuditEvent e " +
           "WHERE e.eventType = 'RATE_LIMIT_EXCEEDED' " +
           "AND e.timestamp > :since " +
           "ORDER BY e.timestamp DESC")
    List<OAuth2AuditEvent> findRateLimitViolationsSince(@Param("since") Instant since);

    /**
     * Cleanup old audit events.
     */
    @Query("DELETE FROM OAuth2AuditEvent e WHERE e.timestamp < :cutoffTime")
    void deleteOldAuditEvents(@Param("cutoffTime") Instant cutoffTime);
}