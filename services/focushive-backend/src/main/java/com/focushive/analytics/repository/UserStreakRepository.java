package com.focushive.analytics.repository;

import com.focushive.analytics.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserStreak entity.
 * Provides data access methods for user streak tracking and analytics.
 */
@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, String> {

    /**
     * Find streak information for a specific user
     */
    Optional<UserStreak> findByUserId(String userId);

    /**
     * Find all users with streaks
     */
    List<UserStreak> findAll();

    /**
     * Find users with current streak greater than or equal to specified days
     */
    List<UserStreak> findByCurrentStreakGreaterThanEqual(int minStreak);

    /**
     * Find users with longest streak greater than or equal to specified days
     */
    List<UserStreak> findByLongestStreakGreaterThanEqual(int minLongestStreak);

    /**
     * Find users active on a specific date
     */
    List<UserStreak> findByLastActiveDate(LocalDate date);

    /**
     * Find users who haven't been active since a specific date (at risk)
     */
    List<UserStreak> findByLastActiveDateBefore(LocalDate date);

    /**
     * Find top users by current streak (leaderboard)
     */
    @Query("SELECT us FROM UserStreak us ORDER BY us.currentStreak DESC, us.longestStreak DESC")
    List<UserStreak> findTopCurrentStreaks();

    /**
     * Find top users by longest streak ever
     */
    @Query("SELECT us FROM UserStreak us ORDER BY us.longestStreak DESC, us.currentStreak DESC")
    List<UserStreak> findTopLongestStreaks();

    /**
     * Find users with streaks at risk (not active yesterday or today)
     */
    @Query("SELECT us FROM UserStreak us " +
           "WHERE us.currentStreak > 0 " +
           "AND us.lastActiveDate < :yesterday")
    List<UserStreak> findStreaksAtRisk(@Param("yesterday") LocalDate yesterday);

    /**
     * Find users who achieved a milestone streak (e.g., 7, 30, 100 days)
     */
    @Query("SELECT us FROM UserStreak us " +
           "WHERE us.currentStreak = :milestoneStreak " +
           "AND us.lastActiveDate = :today")
    List<UserStreak> findUsersWithMilestoneStreak(@Param("milestoneStreak") int milestoneStreak,
                                                  @Param("today") LocalDate today);

    /**
     * Calculate average current streak across all users
     */
    @Query("SELECT AVG(us.currentStreak) FROM UserStreak us WHERE us.currentStreak > 0")
    Double getAverageCurrentStreak();

    /**
     * Calculate average longest streak across all users
     */
    @Query("SELECT AVG(us.longestStreak) FROM UserStreak us WHERE us.longestStreak > 0")
    Double getAverageLongestStreak();

    /**
     * Count users with active streaks (current streak > 0)
     */
    @Query("SELECT COUNT(us) FROM UserStreak us WHERE us.currentStreak > 0")
    Long countUsersWithActiveStreaks();

    /**
     * Count users who were active today
     */
    @Query("SELECT COUNT(us) FROM UserStreak us WHERE us.lastActiveDate = :today")
    Long countUsersActiveToday(@Param("today") LocalDate today);

    /**
     * Find users who broke their longest streak today
     */
    @Query("SELECT us FROM UserStreak us " +
           "WHERE us.longestStreak > us.currentStreak " +
           "AND us.lastActiveDate = :today")
    List<UserStreak> findUsersWhoLostLongestStreak(@Param("today") LocalDate today);

    /**
     * Get streak distribution (count of users for each streak length)
     */
    @Query("SELECT us.currentStreak, COUNT(us) FROM UserStreak us " +
           "WHERE us.currentStreak > 0 " +
           "GROUP BY us.currentStreak " +
           "ORDER BY us.currentStreak")
    List<Object[]> getStreakDistribution();

    /**
     * Find users with available streak freezes
     */
    List<UserStreak> findByAvailableStreakFreezesGreaterThan(int minFreezes);

    /**
     * Find users who used streak freezes recently
     */
    List<UserStreak> findByStreakFreezesUsedGreaterThan(int minUsed);

    /**
     * Get monthly active user count
     */
    @Query("SELECT COUNT(DISTINCT us.userId) FROM UserStreak us " +
           "WHERE us.lastActiveDate BETWEEN :startDate AND :endDate")
    Long getMonthlyActiveUsers(@Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    /**
     * Find users with highest total active days
     */
    @Query("SELECT us FROM UserStreak us ORDER BY us.totalActiveDays DESC")
    List<UserStreak> findMostActiveTotalDays();

    /**
     * Calculate retention rate (users active in last 7 days vs 30 days ago)
     */
    @Query(value = "SELECT CASE " +
           "WHEN (SELECT COUNT(*) FROM user_streaks WHERE last_active_date >= :oldDate) = 0 THEN 0.0 " +
           "ELSE (SELECT COUNT(*) FROM user_streaks WHERE last_active_date >= :recentDate) * 100.0 / " +
           "(SELECT COUNT(*) FROM user_streaks WHERE last_active_date >= :oldDate) " +
           "END", nativeQuery = true)
    Double calculateRetentionRate(@Param("recentDate") LocalDate recentDate,
                                 @Param("oldDate") LocalDate oldDate);

    /**
     * Find users eligible for streak freeze reset (monthly)
     */
    @Query("SELECT us FROM UserStreak us " +
           "WHERE us.streakFreezesUsed > 0 OR us.availableStreakFreezes < 2")
    List<UserStreak> findUsersForStreakFreezeReset();

    /**
     * Get daily streak statistics
     */
    @Query("SELECT " +
           "COUNT(us) as totalUsers, " +
           "COUNT(CASE WHEN us.currentStreak > 0 THEN 1 END) as activeStreaks, " +
           "AVG(us.currentStreak) as avgCurrentStreak, " +
           "MAX(us.currentStreak) as maxCurrentStreak, " +
           "AVG(us.longestStreak) as avgLongestStreak, " +
           "MAX(us.longestStreak) as maxLongestStreak " +
           "FROM UserStreak us")
    Object[] getDailyStreakStatistics();

    /**
     * Check if user exists in streak tracking
     */
    boolean existsByUserId(String userId);

    /**
     * Delete streak data for users who haven't been active for a long time
     */
    void deleteByLastActiveDateBefore(LocalDate cutoffDate);
}