package com.focushive.analytics.repository;

import com.focushive.analytics.entity.HiveAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for HiveAnalytics entity.
 * Provides data access methods for hive-level analytics and metrics.
 */
@Repository
public interface HiveAnalyticsRepository extends JpaRepository<HiveAnalytics, String> {

    /**
     * Find hive analytics for a specific hive and date
     */
    Optional<HiveAnalytics> findByHiveIdAndDate(String hiveId, LocalDate date);

    /**
     * Find all analytics for a hive within a date range
     */
    List<HiveAnalytics> findByHiveIdAndDateBetween(String hiveId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all analytics for a hive
     */
    List<HiveAnalytics> findByHiveId(String hiveId);

    /**
     * Find analytics for a specific date across all hives
     */
    List<HiveAnalytics> findByDate(LocalDate date);

    /**
     * Find analytics within a date range across all hives
     */
    List<HiveAnalytics> findByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Calculate total focus time for a hive within a date range
     */
    @Query("SELECT SUM(ha.totalFocusTime) FROM HiveAnalytics ha " +
           "WHERE ha.hiveId = :hiveId AND ha.date BETWEEN :startDate AND :endDate")
    Long getTotalFocusTime(@Param("hiveId") String hiveId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    /**
     * Calculate total active users for a hive within a date range
     */
    @Query("SELECT SUM(ha.activeUsers) FROM HiveAnalytics ha " +
           "WHERE ha.hiveId = :hiveId AND ha.date BETWEEN :startDate AND :endDate")
    Long getTotalActiveUsers(@Param("hiveId") String hiveId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    /**
     * Calculate average productivity score for a hive within a date range
     */
    @Query("SELECT AVG(ha.averageProductivityScore) FROM HiveAnalytics ha " +
           "WHERE ha.hiveId = :hiveId AND ha.date BETWEEN :startDate AND :endDate")
    Double getAverageProductivityScore(@Param("hiveId") String hiveId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    /**
     * Find top performing hives by total focus time for a specific date
     */
    @Query("SELECT ha FROM HiveAnalytics ha " +
           "WHERE ha.date = :date " +
           "ORDER BY ha.totalFocusTime DESC")
    List<HiveAnalytics> findTopPerformingHivesByDate(@Param("date") LocalDate date);

    /**
     * Find most active hives by user count for a specific date
     */
    @Query("SELECT ha FROM HiveAnalytics ha " +
           "WHERE ha.date = :date " +
           "ORDER BY ha.activeUsers DESC")
    List<HiveAnalytics> findMostActiveHivesByDate(@Param("date") LocalDate date);

    /**
     * Get hive activity trends (last N days)
     */
    @Query("SELECT ha FROM HiveAnalytics ha " +
           "WHERE ha.hiveId = :hiveId " +
           "ORDER BY ha.date DESC " +
           "LIMIT :days")
    List<HiveAnalytics> getHiveActivityTrend(@Param("hiveId") String hiveId, @Param("days") int days);

    /**
     * Find hives with highest completion rates
     */
    @Query("SELECT ha FROM HiveAnalytics ha " +
           "WHERE ha.date BETWEEN :startDate AND :endDate " +
           "AND ha.totalSessions > 0 " +
           "ORDER BY (CAST(ha.completedSessions AS DOUBLE) / ha.totalSessions) DESC")
    List<HiveAnalytics> findHivesWithHighestCompletionRates(@Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    /**
     * Calculate average session length for a hive within a date range
     */
    @Query("SELECT AVG(CASE WHEN ha.completedSessions > 0 THEN " +
           "CAST(ha.totalFocusTime AS DOUBLE) / ha.completedSessions ELSE 0 END) " +
           "FROM HiveAnalytics ha " +
           "WHERE ha.hiveId = :hiveId AND ha.date BETWEEN :startDate AND :endDate")
    Double getAverageSessionLength(@Param("hiveId") String hiveId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Find peak concurrent users for a hive within a date range
     */
    @Query("SELECT MAX(ha.peakConcurrentUsers) FROM HiveAnalytics ha " +
           "WHERE ha.hiveId = :hiveId AND ha.date BETWEEN :startDate AND :endDate")
    Integer getPeakConcurrentUsers(@Param("hiveId") String hiveId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Get daily statistics for all hives
     */
    @Query("SELECT " +
           "ha.date, " +
           "COUNT(ha.hiveId) as activeHives, " +
           "SUM(ha.activeUsers) as totalActiveUsers, " +
           "SUM(ha.totalFocusTime) as totalFocusTime, " +
           "AVG(ha.averageProductivityScore) as avgProductivityScore " +
           "FROM HiveAnalytics ha " +
           "WHERE ha.date BETWEEN :startDate AND :endDate " +
           "GROUP BY ha.date " +
           "ORDER BY ha.date DESC")
    List<Object[]> getDailyPlatformStatistics(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Find hives with sustained activity (active for consecutive days)
     */
    @Query("SELECT ha.hiveId FROM HiveAnalytics ha " +
           "WHERE ha.date BETWEEN :startDate AND :endDate " +
           "AND ha.activeUsers > 0 " +
           "GROUP BY ha.hiveId " +
           "HAVING COUNT(ha.date) >= :minDays")
    List<String> findHivesWithSustainedActivity(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("minDays") int minDays);

    /**
     * Get monthly summary for a hive
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM ha.date) as year, " +
           "EXTRACT(MONTH FROM ha.date) as month, " +
           "SUM(ha.totalFocusTime) as totalFocusMinutes, " +
           "AVG(ha.activeUsers) as avgActiveUsers, " +
           "MAX(ha.peakConcurrentUsers) as maxConcurrentUsers, " +
           "AVG(ha.averageProductivityScore) as avgProductivityScore " +
           "FROM HiveAnalytics ha " +
           "WHERE ha.hiveId = :hiveId " +
           "GROUP BY EXTRACT(YEAR FROM ha.date), EXTRACT(MONTH FROM ha.date) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlySummary(@Param("hiveId") String hiveId);

    /**
     * Find hives that achieved their best performance on a specific date
     */
    @Query("SELECT ha FROM HiveAnalytics ha " +
           "WHERE ha.date = :date " +
           "AND ha.totalFocusTime = (" +
           "  SELECT MAX(ha2.totalFocusTime) FROM HiveAnalytics ha2 " +
           "  WHERE ha2.hiveId = ha.hiveId" +
           ")")
    List<HiveAnalytics> findHivesWithDailyRecord(@Param("date") LocalDate date);

    /**
     * Calculate hive engagement score based on multiple factors
     */
    @Query("SELECT ha.hiveId, " +
           "AVG(ha.activeUsers) * 0.3 + " +
           "AVG(ha.totalFocusTime) * 0.4 + " +
           "AVG(ha.averageProductivityScore) * 0.3 as engagementScore " +
           "FROM HiveAnalytics ha " +
           "WHERE ha.date BETWEEN :startDate AND :endDate " +
           "GROUP BY ha.hiveId " +
           "ORDER BY engagementScore DESC")
    List<Object[]> calculateHiveEngagementScores(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * Delete old analytics data (for data retention)
     */
    void deleteByDateBefore(LocalDate cutoffDate);

    /**
     * Check if hive has any analytics for a date range
     */
    boolean existsByHiveIdAndDateBetween(String hiveId, LocalDate startDate, LocalDate endDate);
}