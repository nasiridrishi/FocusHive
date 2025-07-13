package com.focushive.analytics.repository;

import com.focushive.analytics.entity.FocusSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FocusSessionRepository extends JpaRepository<FocusSession, String> {
    
    // Find by user
    Page<FocusSession> findByUserIdOrderByStartTimeDesc(String userId, Pageable pageable);
    
    // Find by user and time range
    @Query("SELECT fs FROM FocusSession fs WHERE fs.user.id = :userId " +
           "AND fs.startTime >= :startTime AND fs.startTime <= :endTime " +
           "ORDER BY fs.startTime DESC")
    List<FocusSession> findByUserIdAndTimeRange(
        @Param("userId") String userId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    // Find active session
    @Query("SELECT fs FROM FocusSession fs WHERE fs.user.id = :userId AND fs.endTime IS NULL")
    Optional<FocusSession> findActiveSessionByUserId(@Param("userId") String userId);
    
    // Find by hive
    Page<FocusSession> findByHiveIdOrderByStartTimeDesc(String hiveId, Pageable pageable);
    
    // Find completed sessions
    @Query("SELECT fs FROM FocusSession fs WHERE fs.user.id = :userId AND fs.completed = true " +
           "ORDER BY fs.startTime DESC")
    Page<FocusSession> findCompletedSessionsByUserId(@Param("userId") String userId, Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COUNT(fs) FROM FocusSession fs WHERE fs.user.id = :userId AND fs.completed = true")
    long countCompletedSessionsByUserId(@Param("userId") String userId);
    
    @Query("SELECT SUM(fs.actualDurationMinutes) FROM FocusSession fs " +
           "WHERE fs.user.id = :userId AND fs.completed = true")
    Long getTotalMinutesByUserId(@Param("userId") String userId);
    
    @Query("SELECT AVG(fs.actualDurationMinutes) FROM FocusSession fs " +
           "WHERE fs.user.id = :userId AND fs.completed = true")
    Double getAverageSessionLengthByUserId(@Param("userId") String userId);
    
    // Daily statistics
    @Query("SELECT fs FROM FocusSession fs WHERE fs.user.id = :userId " +
           "AND DATE(fs.startTime) = DATE(:date)")
    List<FocusSession> findByUserIdAndDate(
        @Param("userId") String userId,
        @Param("date") LocalDateTime date
    );
    
    // Type-based queries
    @Query("SELECT fs FROM FocusSession fs WHERE fs.user.id = :userId AND fs.type = :type " +
           "ORDER BY fs.startTime DESC")
    Page<FocusSession> findByUserIdAndType(
        @Param("userId") String userId,
        @Param("type") FocusSession.SessionType type,
        Pageable pageable
    );
    
    // Productivity analysis
    @Query("SELECT AVG(fs.productivityScore) FROM FocusSession fs " +
           "WHERE fs.user.id = :userId AND fs.productivityScore IS NOT NULL " +
           "AND fs.startTime >= :since")
    Double getAverageProductivityScore(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since
    );
    
    // Streak calculation
    @Query(value = "SELECT COUNT(DISTINCT DATE(start_time)) FROM focus_sessions " +
                   "WHERE user_id = :userId AND completed = true " +
                   "AND start_time >= :since", nativeQuery = true)
    int countDistinctDaysWithSessions(
        @Param("userId") String userId,
        @Param("since") LocalDateTime since
    );
    
    // Hive statistics
    @Query("SELECT SUM(fs.actualDurationMinutes) FROM FocusSession fs " +
           "WHERE fs.hive.id = :hiveId AND fs.completed = true")
    Long getTotalMinutesByHiveId(@Param("hiveId") String hiveId);
    
    // Recent sessions for feed
    @Query("SELECT fs FROM FocusSession fs WHERE fs.hive.id = :hiveId " +
           "AND fs.completed = true ORDER BY fs.endTime DESC")
    Page<FocusSession> findRecentCompletedByHiveId(
        @Param("hiveId") String hiveId,
        Pageable pageable
    );
}