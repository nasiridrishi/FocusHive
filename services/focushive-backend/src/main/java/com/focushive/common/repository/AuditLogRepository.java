package com.focushive.common.repository;

import com.focushive.common.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    
    // Find by user
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    // Find by user and time range
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.createdAt >= :startTime AND al.createdAt <= :endTime " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findByUserIdAndTimeRange(
        @Param("userId") String userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable);
    
    // Find by entity
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        String entityType, String entityId, Pageable pageable);
    
    // Find by action
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.action = :action " +
           "AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findByActionSince(
        @Param("action") String action,
        @Param("since") LocalDateTime since);
    
    // Find by session
    List<AuditLog> findBySessionIdOrderByCreatedAt(String sessionId);
    
    // Find by IP address
    @Query("SELECT al FROM AuditLog al WHERE al.ipAddress = :ipAddress " +
           "AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findByIpAddressSince(
        @Param("ipAddress") String ipAddress,
        @Param("since") LocalDateTime since);
    
    // Search audit logs
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(al.userId = :userId OR :userId IS NULL) AND " +
           "(al.action = :action OR :action IS NULL) AND " +
           "(al.entityType = :entityType OR :entityType IS NULL) AND " +
           "al.createdAt >= :startTime AND al.createdAt <= :endTime " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> searchAuditLogs(
        @Param("userId") String userId,
        @Param("action") String action,
        @Param("entityType") String entityType,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable);
    
    // Find failed login attempts
    @Query("SELECT al FROM AuditLog al WHERE al.action = 'LOGIN_FAILED' " +
           "AND al.userId = :userId AND al.createdAt >= :since")
    List<AuditLog> findFailedLoginAttempts(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since);
    
    // Find security events
    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.action IN ('LOGIN_FAILED', 'UNAUTHORIZED_ACCESS', 'PASSWORD_CHANGED', " +
           "'PERMISSION_DENIED', 'SUSPICIOUS_ACTIVITY') " +
           "AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findSecurityEvents(@Param("since") LocalDateTime since);
    
    // Statistics
    @Query("SELECT al.action, COUNT(al) FROM AuditLog al " +
           "WHERE al.createdAt >= :since GROUP BY al.action")
    List<Object[]> getActionStatistics(@Param("since") LocalDateTime since);
    
    @Query("SELECT al.entityType, COUNT(al) FROM AuditLog al " +
           "WHERE al.createdAt >= :since GROUP BY al.entityType")
    List<Object[]> getEntityStatistics(@Param("since") LocalDateTime since);
    
    // Count by criteria
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.createdAt >= :since")
    long countByUserIdSince(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since);
    
    // Find by request ID
    List<AuditLog> findByRequestIdOrderByCreatedAt(String requestId);
    
    // Admin queries
    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.action IN ('USER_CREATED', 'USER_DELETED', 'USER_SUSPENDED', " +
           "'ROLE_CHANGED', 'PERMISSION_GRANTED', 'PERMISSION_REVOKED') " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findAdminActions(Pageable pageable);
    
    // Data modification queries
    @Query("SELECT al FROM AuditLog al WHERE " +
           "al.action IN ('CREATE', 'UPDATE', 'DELETE') AND " +
           "al.entityType = :entityType AND al.createdAt >= :since " +
           "ORDER BY al.createdAt DESC")
    List<AuditLog> findDataModifications(
        @Param("entityType") String entityType,
        @Param("since") LocalDateTime since);
    
    // Cleanup old logs (for scheduled jobs)
    @Query("DELETE FROM AuditLog al WHERE al.createdAt < :before")
    int deleteOldLogs(@Param("before") LocalDateTime before);
}