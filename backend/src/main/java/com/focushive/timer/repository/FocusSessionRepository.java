package com.focushive.timer.repository;

import com.focushive.timer.entity.FocusSession;
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
    
    // Find active session for user
    Optional<FocusSession> findByUserIdAndCompletedFalse(String userId);
    
    // Find sessions by user
    Page<FocusSession> findByUserIdOrderByStartTimeDesc(String userId, Pageable pageable);
    
    // Find sessions by hive
    Page<FocusSession> findByHiveIdOrderByStartTimeDesc(String hiveId, Pageable pageable);
    
    // Find sessions in date range
    @Query("SELECT fs FROM FocusSession fs WHERE fs.userId = :userId AND fs.startTime >= :startDate AND fs.startTime < :endDate ORDER BY fs.startTime DESC")
    List<FocusSession> findByUserIdAndDateRange(@Param("userId") String userId, 
                                                 @Param("startDate") LocalDateTime startDate, 
                                                 @Param("endDate") LocalDateTime endDate);
    
    // Calculate total focus time for a user in date range
    @Query("SELECT SUM(fs.actualDurationMinutes) FROM FocusSession fs WHERE fs.userId = :userId AND fs.completed = true AND fs.sessionType IN ('WORK', 'STUDY') AND fs.startTime >= :startDate AND fs.startTime < :endDate")
    Integer getTotalFocusMinutes(@Param("userId") String userId, 
                                @Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);
    
    // Count completed sessions
    @Query("SELECT COUNT(fs) FROM FocusSession fs WHERE fs.userId = :userId AND fs.completed = true AND fs.startTime >= :startDate AND fs.startTime < :endDate")
    Integer countCompletedSessions(@Param("userId") String userId, 
                                  @Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);
    
    // Find longest session duration
    @Query("SELECT MAX(fs.actualDurationMinutes) FROM FocusSession fs WHERE fs.userId = :userId AND fs.completed = true AND fs.sessionType IN ('WORK', 'STUDY') AND fs.startTime >= :startDate AND fs.startTime < :endDate")
    Integer getLongestSessionMinutes(@Param("userId") String userId, 
                                    @Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
}