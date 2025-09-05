package com.focushive.timer.repository;

import com.focushive.timer.entity.ProductivityStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductivityStatsRepository extends JpaRepository<ProductivityStats, String> {
    
    // Find stats for a specific date
    Optional<ProductivityStats> findByUserIdAndDate(String userId, LocalDate date);
    
    // Find stats in date range
    @Query("SELECT ps FROM ProductivityStats ps WHERE ps.userId = :userId AND ps.date >= :startDate AND ps.date <= :endDate ORDER BY ps.date DESC")
    List<ProductivityStats> findByUserIdAndDateRange(@Param("userId") String userId, 
                                                     @Param("startDate") LocalDate startDate, 
                                                     @Param("endDate") LocalDate endDate);
    
    // Get weekly stats
    @Query("SELECT ps FROM ProductivityStats ps WHERE ps.userId = :userId AND ps.date >= :startDate ORDER BY ps.date DESC")
    List<ProductivityStats> findWeeklyStats(@Param("userId") String userId, 
                                           @Param("startDate") LocalDate startDate);
    
    // Calculate average daily focus time
    @Query("SELECT AVG(ps.totalFocusMinutes) FROM ProductivityStats ps WHERE ps.userId = :userId AND ps.date >= :startDate AND ps.date <= :endDate")
    Double getAverageDailyFocusMinutes(@Param("userId") String userId, 
                                      @Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);
    
    // Get streak information
    @Query(value = "WITH streaks AS (" +
            "SELECT date, " +
            "date - INTERVAL '1 day' * ROW_NUMBER() OVER (ORDER BY date) AS streak_group " +
            "FROM productivity_stats " +
            "WHERE user_id = :userId AND sessions_completed > 0 " +
            "ORDER BY date DESC) " +
            "SELECT COUNT(*) as streak_length " +
            "FROM streaks " +
            "GROUP BY streak_group " +
            "ORDER BY MIN(date) DESC " +
            "LIMIT 1", nativeQuery = true)
    Integer getCurrentStreak(@Param("userId") String userId);
}