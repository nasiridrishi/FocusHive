package com.focushive.notification.repository;

import com.focushive.notification.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for SecurityAuditLog entity operations.
 * Provides methods for querying audit logs for security monitoring and reporting.
 */
@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {

    /**
     * Find audit logs by action type
     */
    List<SecurityAuditLog> findByActionOrderByTimestampDesc(String action);

    /**
     * Find audit logs by username
     */
    List<SecurityAuditLog> findByUsernameOrderByTimestampDesc(String username);

    /**
     * Find audit logs by action and username within a time period
     */
    List<SecurityAuditLog> findByActionAndUsernameAndTimestampAfter(
            String action, String username, LocalDateTime after);

    /**
     * Find failed audit logs (success = false)
     */
    List<SecurityAuditLog> findBySuccessFalseOrderByTimestampDesc();

    /**
     * Find audit logs within a date range
     */
    List<SecurityAuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start, LocalDateTime end);

    /**
     * Count total audit events
     */
    @Query("SELECT COUNT(s) FROM SecurityAuditLog s")
    long countTotalEvents();

    /**
     * Count successful events
     */
    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.success = true")
    long countSuccessfulEvents();

    /**
     * Count failed events
     */
    @Query("SELECT COUNT(s) FROM SecurityAuditLog s WHERE s.success = false")
    long countFailedEvents();

    /**
     * Find recent audit logs (last N hours)
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findRecentLogs(@Param("since") LocalDateTime since);

    /**
     * Find audit logs by IP address
     */
    List<SecurityAuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress);

    /**
     * Find suspicious activities (multiple failed attempts from same IP or user)
     */
    @Query("SELECT s FROM SecurityAuditLog s WHERE s.success = false " +
           "AND s.timestamp >= :since " +
           "GROUP BY s.ipAddress, s.username " +
           "HAVING COUNT(s) >= :threshold " +
           "ORDER BY s.timestamp DESC")
    List<SecurityAuditLog> findSuspiciousActivity(
            @Param("since") LocalDateTime since,
            @Param("threshold") long threshold);
}