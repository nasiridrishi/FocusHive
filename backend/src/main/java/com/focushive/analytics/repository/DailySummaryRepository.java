package com.focushive.analytics.repository;

import com.focushive.analytics.entity.DailySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, String> {
    
    // Find by user and date
    Optional<DailySummary> findByUserIdAndDate(String userId, LocalDate date);
    
    // Find range of summaries
    @Query("SELECT ds FROM DailySummary ds WHERE ds.userId = :userId " +
           "AND ds.date >= :startDate AND ds.date <= :endDate " +
           "ORDER BY ds.date DESC")
    List<DailySummary> findByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Find recent summaries
    @Query("SELECT ds FROM DailySummary ds WHERE ds.userId = :userId " +
           "ORDER BY ds.date DESC")
    List<DailySummary> findRecentByUserId(@Param("userId") String userId, Pageable pageable);
    
    // Calculate streak - This requires PostgreSQL-specific date functions
    // For H2 tests, this method won't work. Consider implementing in service layer
    // @Query(value = "WITH consecutive_days AS (" +
    //                "  SELECT date, " +
    //                "    date - INTERVAL '1 day' * ROW_NUMBER() OVER (ORDER BY date) AS grp " +
    //                "  FROM daily_summaries " +
    //                "  WHERE user_id = :userId AND total_minutes > 0 " +
    //                "  ORDER BY date DESC" +
    //                ") " +
    //                "SELECT COUNT(*) as streak_length " +
    //                "FROM consecutive_days " +
    //                "WHERE grp = (SELECT grp FROM consecutive_days LIMIT 1)",
    //        nativeQuery = true)
    // int getCurrentStreak(@Param("userId") String userId);
    
    // Statistics
    @Query("SELECT SUM(ds.totalMinutes) FROM DailySummary ds " +
           "WHERE ds.userId = :userId AND ds.date >= :since")
    Long getTotalMinutesSince(
        @Param("userId") String userId,
        @Param("since") LocalDate since
    );
    
    @Query("SELECT AVG(ds.totalMinutes) FROM DailySummary ds " +
           "WHERE ds.userId = :userId AND ds.date >= :since")
    Double getAverageDailyMinutesSince(
        @Param("userId") String userId,
        @Param("since") LocalDate since
    );
    
    // Best day
    @Query("SELECT ds FROM DailySummary ds WHERE ds.userId = :userId " +
           "ORDER BY ds.totalMinutes DESC")
    Page<DailySummary> findBestDayPage(@Param("userId") String userId, Pageable pageable);
    
    default Optional<DailySummary> findBestDay(String userId) {
        Page<DailySummary> page = findBestDayPage(userId, PageRequest.of(0, 1));
        return page.hasContent() ? Optional.of(page.getContent().get(0)) : Optional.empty();
    }
    
    // Weekly summary
    @Query("SELECT SUM(ds.totalMinutes) as total, " +
           "SUM(ds.sessionsCount) as sessions, " +
           "AVG(ds.productivityScore) as avgScore " +
           "FROM DailySummary ds " +
           "WHERE ds.userId = :userId " +
           "AND ds.date >= :weekStart AND ds.date <= :weekEnd")
    WeeklySummaryProjection getWeeklySummary(
        @Param("userId") String userId,
        @Param("weekStart") LocalDate weekStart,
        @Param("weekEnd") LocalDate weekEnd
    );
    
    interface WeeklySummaryProjection {
        Long getTotal();
        Long getSessions();
        Double getAvgScore();
    }
}