package com.focushive.analytics.repository;

import com.focushive.analytics.entity.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DailyGoal entity.
 * Provides data access methods for daily goal tracking and progress monitoring.
 */
@Repository
public interface DailyGoalRepository extends JpaRepository<DailyGoal, String> {

    /**
     * Find daily goal for a specific user and date
     */
    Optional<DailyGoal> findByUserIdAndDate(String userId, LocalDate date);

    /**
     * Find all goals for a specific user
     */
    List<DailyGoal> findByUserId(String userId);

    /**
     * Find goals for a user within a date range
     */
    List<DailyGoal> findByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * Find goals for a specific date across all users
     */
    List<DailyGoal> findByDate(LocalDate date);

    /**
     * Find goals within a date range across all users
     */
    List<DailyGoal> findByDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find achieved goals for a specific user
     */
    List<DailyGoal> findByUserIdAndAchievedTrue(String userId);

    /**
     * Find unachieved goals for a specific user
     */
    List<DailyGoal> findByUserIdAndAchievedFalse(String userId);

    /**
     * Find today's goal for a user
     */
    @Query(value = "SELECT * FROM daily_goals WHERE user_id = :userId AND date = CURRENT_DATE", nativeQuery = true)
    Optional<DailyGoal> findTodaysGoal(@Param("userId") String userId);

    /**
     * Find overdue goals (past date and not achieved)
     */
    @Query(value = "SELECT * FROM daily_goals " +
           "WHERE date < CURRENT_DATE AND achieved = false", nativeQuery = true)
    List<DailyGoal> findOverdueGoals();

    /**
     * Find goals that need reminders (today's goals not achieved)
     */
    @Query(value = "SELECT * FROM daily_goals " +
           "WHERE date = CURRENT_DATE " +
           "AND achieved = false " +
           "AND reminder_sent = false", nativeQuery = true)
    List<DailyGoal> findGoalsNeedingReminders();

    /**
     * Count achieved goals for a user within a date range
     */
    @Query("SELECT COUNT(dg) FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.date BETWEEN :startDate AND :endDate " +
           "AND dg.achieved = true")
    Long countAchievedGoals(@Param("userId") String userId,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);

    /**
     * Count total goals for a user within a date range
     */
    @Query("SELECT COUNT(dg) FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.date BETWEEN :startDate AND :endDate")
    Long countTotalGoals(@Param("userId") String userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

    /**
     * Calculate goal achievement rate for a user
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN dg.achieved = true THEN 1 END) * 100.0 / COUNT(dg) " +
           "FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.date BETWEEN :startDate AND :endDate")
    Double calculateAchievementRate(@Param("userId") String userId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    /**
     * Find consecutive goal achievement days (streak)
     */
    @Query("SELECT dg FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.achieved = true " +
           "AND dg.streakContribution = true " +
           "ORDER BY dg.date DESC")
    List<DailyGoal> findGoalStreakDays(@Param("userId") String userId);

    /**
     * Get average target minutes for a user
     */
    @Query("SELECT AVG(dg.targetMinutes) FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.date BETWEEN :startDate AND :endDate")
    Double getAverageTargetMinutes(@Param("userId") String userId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Get average completed minutes for a user
     */
    @Query("SELECT AVG(dg.completedMinutes) FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.date BETWEEN :startDate AND :endDate")
    Double getAverageCompletedMinutes(@Param("userId") String userId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /**
     * Find goals by priority level
     */
    List<DailyGoal> findByUserIdAndPriority(String userId, String priority);

    /**
     * Find overachieved goals (completed > target)
     */
    @Query("SELECT dg FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.completedMinutes > dg.targetMinutes")
    List<DailyGoal> findOverachievedGoals(@Param("userId") String userId);

    /**
     * Get goal statistics for a user
     */
    @Query("SELECT " +
           "COUNT(dg) as totalGoals, " +
           "COUNT(CASE WHEN dg.achieved = true THEN 1 END) as achievedGoals, " +
           "AVG(dg.targetMinutes) as avgTarget, " +
           "AVG(dg.completedMinutes) as avgCompleted, " +
           "MAX(dg.completedMinutes) as maxCompleted " +
           "FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "AND dg.date BETWEEN :startDate AND :endDate")
    Object[] getUserGoalStatistics(@Param("userId") String userId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    /**
     * Find top performers by goal achievement rate
     */
    @Query("SELECT dg.userId, " +
           "COUNT(CASE WHEN dg.achieved = true THEN 1 END) * 100.0 / COUNT(dg) as achievementRate " +
           "FROM DailyGoal dg " +
           "WHERE dg.date BETWEEN :startDate AND :endDate " +
           "GROUP BY dg.userId " +
           "HAVING COUNT(dg) >= :minGoals " +
           "ORDER BY achievementRate DESC")
    List<Object[]> findTopPerformersByAchievementRate(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate,
                                                      @Param("minGoals") int minGoals);

    /**
     * Find users with highest total completed minutes
     */
    @Query("SELECT dg.userId, SUM(dg.completedMinutes) as totalCompleted " +
           "FROM DailyGoal dg " +
           "WHERE dg.date BETWEEN :startDate AND :endDate " +
           "GROUP BY dg.userId " +
           "ORDER BY totalCompleted DESC")
    List<Object[]> findTopPerformersByTotalMinutes(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * Get daily platform statistics
     */
    @Query("SELECT " +
           "dg.date, " +
           "COUNT(dg) as totalGoals, " +
           "COUNT(CASE WHEN dg.achieved = true THEN 1 END) as achievedGoals, " +
           "AVG(dg.targetMinutes) as avgTarget, " +
           "SUM(dg.completedMinutes) as totalCompleted " +
           "FROM DailyGoal dg " +
           "WHERE dg.date BETWEEN :startDate AND :endDate " +
           "GROUP BY dg.date " +
           "ORDER BY dg.date DESC")
    List<Object[]> getDailyPlatformStatistics(@Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * Find users who consistently set goals
     */
    @Query("SELECT dg.userId FROM DailyGoal dg " +
           "WHERE dg.date BETWEEN :startDate AND :endDate " +
           "GROUP BY dg.userId " +
           "HAVING COUNT(dg.date) >= :minDays")
    List<String> findConsistentGoalSetters(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("minDays") int minDays);

    /**
     * Find goals achieved in the last hour (for real-time notifications)
     */
    @Query("SELECT dg FROM DailyGoal dg " +
           "WHERE dg.achievedAt >= :oneHourAgo")
    List<DailyGoal> findRecentlyAchievedGoals(@Param("oneHourAgo") LocalDate oneHourAgo);

    /**
     * Get monthly goal summary
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM dg.date) as year, " +
           "EXTRACT(MONTH FROM dg.date) as month, " +
           "COUNT(dg) as totalGoals, " +
           "COUNT(CASE WHEN dg.achieved = true THEN 1 END) as achievedGoals, " +
           "AVG(dg.targetMinutes) as avgTarget, " +
           "SUM(dg.completedMinutes) as totalCompleted " +
           "FROM DailyGoal dg " +
           "WHERE dg.userId = :userId " +
           "GROUP BY EXTRACT(YEAR FROM dg.date), EXTRACT(MONTH FROM dg.date) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlySummary(@Param("userId") String userId);

    /**
     * Check if user has any goals for a date range
     */
    boolean existsByUserIdAndDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    /**
     * Delete old goal data (for data retention)
     */
    void deleteByDateBefore(LocalDate cutoffDate);
}