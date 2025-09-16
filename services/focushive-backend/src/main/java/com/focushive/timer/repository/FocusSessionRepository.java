package com.focushive.timer.repository;

import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.entity.FocusSession.SessionStatus;
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
 * Repository for FocusSession entity.
 */
@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, String> {

    /**
     * Find active session for a user.
     */
    @Query("SELECT fs FROM FocusSession fs WHERE fs.userId = :userId AND fs.status = 'ACTIVE'")
    Optional<FocusSession> findActiveSessionByUserId(@Param("userId") String userId);

    /**
     * Find sessions by user ID.
     */
    Page<FocusSession> findByUserIdOrderByStartedAtDesc(String userId, Pageable pageable);

    /**
     * Find sessions by hive ID and status.
     */
    List<FocusSession> findByHiveIdAndStatus(String hiveId, SessionStatus status);

    /**
     * Find sessions by user ID within a date range.
     */
    @Query("SELECT fs FROM FocusSession fs WHERE fs.userId = :userId " +
           "AND fs.completedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY fs.completedAt DESC")
    List<FocusSession> findByUserIdAndCompletedAtBetween(
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find expired active sessions.
     */
    @Query("SELECT fs FROM FocusSession fs WHERE fs.status = 'ACTIVE' " +
           "AND fs.startedAt < :expiryTime")
    List<FocusSession> findExpiredActiveSessions(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * Count completed sessions for a user.
     */
    @Query("SELECT COUNT(fs) FROM FocusSession fs WHERE fs.userId = :userId " +
           "AND fs.status = 'COMPLETED'")
    Long countCompletedSessionsByUserId(@Param("userId") String userId);

    /**
     * Calculate total focus minutes for a user.
     */
    @Query("SELECT SUM(fs.durationMinutes) FROM FocusSession fs WHERE fs.userId = :userId " +
           "AND fs.status = 'COMPLETED'")
    Long getTotalFocusMinutesByUserId(@Param("userId") String userId);

    /**
     * Calculate average productivity score for a user.
     */
    @Query("SELECT AVG(fs.productivityScore) FROM FocusSession fs WHERE fs.userId = :userId " +
           "AND fs.status = 'COMPLETED' AND fs.productivityScore IS NOT NULL")
    Double getAverageProductivityScoreByUserId(@Param("userId") String userId);

    /**
     * Find sessions for a hive within a date range.
     */
    @Query("SELECT fs FROM FocusSession fs WHERE fs.hiveId = :hiveId " +
           "AND fs.startedAt BETWEEN :startDate AND :endDate")
    List<FocusSession> findByHiveIdAndStartedAtBetween(
        @Param("hiveId") String hiveId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find sessions needing reminders.
     */
    @Query("SELECT fs FROM FocusSession fs WHERE fs.status = 'ACTIVE' " +
           "AND fs.reminderEnabled = true AND fs.reminderSent = false " +
           "AND fs.startedAt <= :reminderTime")
    List<FocusSession> findSessionsNeedingReminders(@Param("reminderTime") LocalDateTime reminderTime);

    /**
     * Check if user has active session.
     */
    @Query("SELECT CASE WHEN COUNT(fs) > 0 THEN true ELSE false END " +
           "FROM FocusSession fs WHERE fs.userId = :userId AND fs.status = 'ACTIVE'")
    boolean hasActiveSession(@Param("userId") String userId);

    /**
     * Get user's most productive hour.
     */
    @Query(value = "SELECT EXTRACT(HOUR FROM started_at) as hour, " +
           "AVG(productivity_score) as avg_score " +
           "FROM focus_sessions " +
           "WHERE user_id = :userId AND status = 'COMPLETED' " +
           "AND productivity_score IS NOT NULL " +
           "GROUP BY EXTRACT(HOUR FROM started_at) " +
           "ORDER BY avg_score DESC " +
           "LIMIT 1", nativeQuery = true)
    Object[] getMostProductiveHour(@Param("userId") String userId);

    /**
     * Count sessions by type for a user.
     */
    @Query("SELECT fs.sessionType, COUNT(fs) FROM FocusSession fs " +
           "WHERE fs.userId = :userId GROUP BY fs.sessionType")
    List<Object[]> countSessionsByType(@Param("userId") String userId);

    /**
     * Get recent sessions for sync.
     */
    @Query("SELECT fs FROM FocusSession fs WHERE fs.userId = :userId " +
           "AND fs.lastSyncTime > :lastSyncTime ORDER BY fs.lastSyncTime DESC")
    List<FocusSession> findRecentlyModifiedSessions(
        @Param("userId") String userId,
        @Param("lastSyncTime") LocalDateTime lastSyncTime
    );
}