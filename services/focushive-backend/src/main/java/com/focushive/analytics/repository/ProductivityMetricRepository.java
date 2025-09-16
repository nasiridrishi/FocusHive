package com.focushive.analytics.repository;

import com.focushive.analytics.entity.ProductivityMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProductivityMetric entity.
 * Provides data access methods for productivity metrics and analytics.
 */
@Repository
public interface ProductivityMetricRepository extends JpaRepository<ProductivityMetric, String> {

    /**
     * Find productivity metric for a specific user and date
     */
    Optional<ProductivityMetric> findByUserIdAndDate(String userId, LocalDate date);

    /**
     * Find all productivity metrics for a user within a date range
     */
    List<ProductivityMetric> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all productivity metrics for a user
     */
    List<ProductivityMetric> findByUserId(String userId);

    /**
     * Find productivity metrics for a specific date across all users
     */
    List<ProductivityMetric> findByDate(LocalDate date);

    /**
     * Find productivity metrics within a date range across all users
     */
    List<ProductivityMetric> findByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Calculate total focus minutes for a user within a date range
     */
    @Query("SELECT SUM(pm.focusMinutes) FROM ProductivityMetric pm " +
           "WHERE pm.userId = :userId AND pm.date BETWEEN :startDate AND :endDate")
    Long getTotalFocusMinutes(@Param("userId") String userId,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate);

    /**
     * Calculate total completed sessions for a user within a date range
     */
    @Query("SELECT SUM(pm.completedSessions) FROM ProductivityMetric pm " +
           "WHERE pm.userId = :userId AND pm.date BETWEEN :startDate AND :endDate")
    Long getTotalCompletedSessions(@Param("userId") String userId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Calculate average productivity score for a user within a date range
     */
    @Query("SELECT AVG(pm.productivityScore) FROM ProductivityMetric pm " +
           "WHERE pm.userId = :userId AND pm.date BETWEEN :startDate AND :endDate")
    Double getAverageProductivityScore(@Param("userId") String userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    /**
     * Find top performers by productivity score for a specific date
     */
    @Query("SELECT pm FROM ProductivityMetric pm " +
           "WHERE pm.date = :date " +
           "ORDER BY pm.productivityScore DESC")
    List<ProductivityMetric> findTopPerformersByDate(@Param("date") LocalDate date);

    /**
     * Find users with consecutive days of activity
     */
    @Query("SELECT pm.userId FROM ProductivityMetric pm " +
           "WHERE pm.date BETWEEN :startDate AND :endDate " +
           "AND pm.completedSessions > 0 " +
           "GROUP BY pm.userId " +
           "HAVING COUNT(pm.date) >= :minDays")
    List<String> findUsersWithConsecutiveActivity(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate,
                                                  @Param("minDays") int minDays);

    /**
     * Get productivity trends for a user (last N days)
     */
    @Query("SELECT pm FROM ProductivityMetric pm " +
           "WHERE pm.userId = :userId " +
           "ORDER BY pm.date DESC " +
           "LIMIT :days")
    List<ProductivityMetric> getProductivityTrend(@Param("userId") String userId, @Param("days") int days);

    /**
     * Find users who achieved their highest productivity score on a specific date
     */
    @Query("SELECT pm FROM ProductivityMetric pm " +
           "WHERE pm.date = :date " +
           "AND pm.productivityScore = (" +
           "  SELECT MAX(pm2.productivityScore) FROM ProductivityMetric pm2 " +
           "  WHERE pm2.userId = pm.userId" +
           ")")
    List<ProductivityMetric> findUsersWithPersonalBest(@Param("date") LocalDate date);

    /**
     * Calculate completion rate for a user within a date range
     */
    @Query("SELECT AVG(CASE WHEN pm.totalSessions > 0 THEN " +
           "CAST(pm.completedSessions AS DOUBLE) / pm.totalSessions * 100 ELSE 0 END) " +
           "FROM ProductivityMetric pm " +
           "WHERE pm.userId = :userId AND pm.date BETWEEN :startDate AND :endDate")
    Double getAverageCompletionRate(@Param("userId") String userId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * Find metrics where goals were achieved
     */
    List<ProductivityMetric> findByUserIdAndGoalsAchievedGreaterThan(String userId, int minGoals);

    /**
     * Get monthly summary statistics
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM pm.date) as year, " +
           "EXTRACT(MONTH FROM pm.date) as month, " +
           "SUM(pm.focusMinutes) as totalFocusMinutes, " +
           "SUM(pm.completedSessions) as totalCompletedSessions, " +
           "AVG(pm.productivityScore) as avgProductivityScore " +
           "FROM ProductivityMetric pm " +
           "WHERE pm.userId = :userId " +
           "GROUP BY EXTRACT(YEAR FROM pm.date), EXTRACT(MONTH FROM pm.date) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlySummary(@Param("userId") String userId);

    /**
     * Delete old metrics (for data retention)
     */
    void deleteByDateBefore(LocalDate cutoffDate);

    /**
     * Check if user has any metrics for a date range
     */
    boolean existsByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);
}