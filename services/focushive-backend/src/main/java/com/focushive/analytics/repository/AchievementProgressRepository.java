package com.focushive.analytics.repository;

import com.focushive.analytics.entity.AchievementProgress;
import com.focushive.analytics.enums.AchievementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AchievementProgress entity.
 * Provides data access methods for achievement tracking and progress monitoring.
 */
@Repository
public interface AchievementProgressRepository extends JpaRepository<AchievementProgress, String> {

    /**
     * Find achievement progress for a specific user and achievement type
     */
    Optional<AchievementProgress> findByUserIdAndAchievementType(String userId, AchievementType achievementType);

    /**
     * Find all achievement progress for a specific user
     */
    List<AchievementProgress> findByUserId(String userId);

    /**
     * Find all progress for a specific achievement type across all users
     */
    List<AchievementProgress> findByAchievementType(AchievementType achievementType);

    /**
     * Find unlocked achievements for a specific user
     */
    List<AchievementProgress> findByUserIdAndUnlockedAtIsNotNull(String userId);

    /**
     * Find in-progress achievements for a specific user
     */
    List<AchievementProgress> findByUserIdAndUnlockedAtIsNullAndProgressGreaterThan(String userId, int minProgress);

    /**
     * Find recently unlocked achievements (for notifications)
     */
    List<AchievementProgress> findByUnlockedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find achievements unlocked today
     */
    @Query(value = "SELECT * FROM achievement_progress " +
           "WHERE DATE(unlocked_at) = CURRENT_DATE", nativeQuery = true)
    List<AchievementProgress> findAchievementsUnlockedToday();

    /**
     * Find achievements that need notifications sent
     */
    List<AchievementProgress> findByUnlockedAtIsNotNullAndNotificationSentFalse();

    /**
     * Count total unlocked achievements for a user
     */
    @Query("SELECT COUNT(ap) FROM AchievementProgress ap " +
           "WHERE ap.userId = :userId AND ap.unlockedAt IS NOT NULL")
    Long countUnlockedAchievements(@Param("userId") String userId);

    /**
     * Count in-progress achievements for a user
     */
    @Query("SELECT COUNT(ap) FROM AchievementProgress ap " +
           "WHERE ap.userId = :userId AND ap.unlockedAt IS NULL AND ap.progress > 0")
    Long countInProgressAchievements(@Param("userId") String userId);

    /**
     * Calculate total points earned by a user
     */
    @Query("SELECT SUM(" +
           "CASE ap.achievementType " +
           "WHEN 'FIRST_FOCUS' THEN 10 " +
           "WHEN 'EARLY_BIRD' THEN 10 " +
           "WHEN 'NIGHT_OWL' THEN 10 " +
           "WHEN 'TEN_SESSIONS' THEN 25 " +
           "WHEN 'THREE_DAY_STREAK' THEN 25 " +
           "WHEN 'FIFTY_SESSIONS' THEN 50 " +
           "WHEN 'WEEK_WARRIOR' THEN 50 " +
           "WHEN 'MARATHON_RUNNER' THEN 50 " +
           "WHEN 'HIGH_PERFORMER' THEN 50 " +
           "WHEN 'TEAM_PLAYER' THEN 50 " +
           "WHEN 'HUNDRED_SESSIONS' THEN 100 " +
           "WHEN 'MONTH_MASTER' THEN 100 " +
           "WHEN 'ULTRA_RUNNER' THEN 100 " +
           "WHEN 'PEAK_PERFORMER' THEN 100 " +
           "WHEN 'HIVE_LEADER' THEN 100 " +
           "WHEN 'CENTURY_STREAK' THEN 200 " +
           "WHEN 'ENDURANCE_MASTER' THEN 200 " +
           "WHEN 'PERFECT_SCORE' THEN 200 " +
           "WHEN 'SOCIAL_BUTTERFLY' THEN 200 " +
           "WHEN 'DISTRACTION_FREE' THEN 75 " +
           "WHEN 'GOAL_CRUSHER' THEN 75 " +
           "WHEN 'WEEKEND_WARRIOR' THEN 75 " +
           "ELSE 0 END" +
           ") FROM AchievementProgress ap " +
           "WHERE ap.userId = :userId AND ap.unlockedAt IS NOT NULL")
    Long calculateTotalPoints(@Param("userId") String userId);

    /**
     * Find top achievers by total unlocked achievements
     */
    @Query("SELECT ap.userId, COUNT(ap) as achievementCount FROM AchievementProgress ap " +
           "WHERE ap.unlockedAt IS NOT NULL " +
           "GROUP BY ap.userId " +
           "ORDER BY achievementCount DESC")
    List<Object[]> findTopAchieversByCount();

    /**
     * Find achievements close to completion (90%+ progress)
     */
    List<AchievementProgress> findByUnlockedAtIsNullAndProgressGreaterThanEqual(int minProgress);

    /**
     * Find users who unlocked a specific achievement
     */
    List<AchievementProgress> findByAchievementTypeAndUnlockedAtIsNotNull(AchievementType achievementType);

    /**
     * Get achievement statistics for a specific achievement type
     */
    @Query("SELECT " +
           "COUNT(ap) as totalUsers, " +
           "COUNT(CASE WHEN ap.unlockedAt IS NOT NULL THEN 1 END) as unlockedCount, " +
           "AVG(ap.progress) as averageProgress " +
           "FROM AchievementProgress ap " +
           "WHERE ap.achievementType = :achievementType")
    Object[] getAchievementStatistics(@Param("achievementType") AchievementType achievementType);

    /**
     * Find users with fastest unlock time for an achievement
     */
    @Query(value = "SELECT * FROM achievement_progress " +
           "WHERE achievement_type = :achievementType " +
           "AND unlocked_at IS NOT NULL " +
           "ORDER BY (unlocked_at - first_progress_at) ASC", nativeQuery = true)
    List<AchievementProgress> findFastestUnlocks(@Param("achievementType") String achievementType);

    /**
     * Get daily achievement unlock statistics
     */
    @Query(value = "SELECT " +
           "DATE(unlocked_at) as unlock_date, " +
           "COUNT(*) as unlocks_count, " +
           "COUNT(DISTINCT user_id) as unique_users " +
           "FROM achievement_progress " +
           "WHERE unlocked_at BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(unlocked_at) " +
           "ORDER BY unlock_date DESC", nativeQuery = true)
    List<Object[]> getDailyUnlockStatistics(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Find achievements unlocked by category
     */
    @Query("SELECT ap FROM AchievementProgress ap " +
           "WHERE ap.userId = :userId " +
           "AND ap.unlockedAt IS NOT NULL " +
           "ORDER BY ap.unlockedAt DESC")
    List<AchievementProgress> findUserAchievementsChronological(@Param("userId") String userId);

    /**
     * Check if user has unlocked any achievement in a category
     */
    @Query("SELECT COUNT(ap) > 0 FROM AchievementProgress ap " +
           "WHERE ap.userId = :userId " +
           "AND ap.unlockedAt IS NOT NULL " +
           "AND ap.achievementType IN :achievementTypes")
    boolean hasUnlockedInCategory(@Param("userId") String userId,
                                 @Param("achievementTypes") List<AchievementType> achievementTypes);

    /**
     * Find rarest achievements (least unlocked)
     */
    @Query("SELECT ap.achievementType, COUNT(ap) as unlockCount FROM AchievementProgress ap " +
           "WHERE ap.unlockedAt IS NOT NULL " +
           "GROUP BY ap.achievementType " +
           "ORDER BY unlockCount ASC")
    List<Object[]> findRarestAchievements();

    /**
     * Find most common achievements (most unlocked)
     */
    @Query("SELECT ap.achievementType, COUNT(ap) as unlockCount FROM AchievementProgress ap " +
           "WHERE ap.unlockedAt IS NOT NULL " +
           "GROUP BY ap.achievementType " +
           "ORDER BY unlockCount DESC")
    List<Object[]> findMostCommonAchievements();

    /**
     * Find users eligible for streak-based achievements
     */
    @Query("SELECT ap.userId FROM AchievementProgress ap " +
           "WHERE ap.achievementType IN ('THREE_DAY_STREAK', 'WEEK_WARRIOR', 'MONTH_MASTER', 'CENTURY_STREAK') " +
           "AND ap.unlockedAt IS NULL")
    List<String> findUsersEligibleForStreakAchievements();

    /**
     * Get user's completion rate across all achievements
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN ap.unlockedAt IS NOT NULL THEN 1 END) * 100.0 / COUNT(ap) " +
           "FROM AchievementProgress ap " +
           "WHERE ap.userId = :userId")
    Double getUserCompletionRate(@Param("userId") String userId);

    /**
     * Check if user has specific achievement
     */
    boolean existsByUserIdAndAchievementTypeAndUnlockedAtIsNotNull(String userId, AchievementType achievementType);

    /**
     * Delete old achievement progress data
     */
    void deleteByFirstProgressAtBefore(LocalDateTime cutoffDate);
}