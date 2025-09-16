package com.focushive.analytics.service;

import com.focushive.analytics.dto.*;
import com.focushive.analytics.entity.DailyGoal;
import com.focushive.analytics.enums.AchievementType;
import com.focushive.analytics.enums.ReportPeriod;
import com.focushive.timer.entity.FocusSession;

import java.util.Map;

/**
 * Enhanced Analytics Service interface providing comprehensive productivity tracking,
 * achievement management, goal setting, and detailed reporting capabilities.
 */
public interface EnhancedAnalyticsService {

    // ==================== PRODUCTIVITY METRICS ====================

    /**
     * Get user productivity summary for default period (last 30 days)
     */
    ProductivitySummaryResponse getUserProductivitySummary(String userId);

    /**
     * Get detailed analytics report for specified period
     */
    DetailedReportResponse getUserDetailedReport(String userId, ReportPeriod period);

    /**
     * Calculate productivity score based on various factors
     */
    int calculateProductivityScore(int focusMinutes, int completedSessions, int distractions, int streakBonus);

    /**
     * Update productivity metrics from completed focus session
     */
    void updateProductivityMetrics(FocusSession session);

    // ==================== STREAK MANAGEMENT ====================

    /**
     * Get user streak information
     */
    StreakInfoResponse getStreakInformation(String userId);

    /**
     * Update user streak based on activity
     */
    void updateUserStreak(String userId, boolean activeToday);

    // ==================== ACHIEVEMENT SYSTEM ====================

    /**
     * Get user achievement progress
     */
    AchievementProgressResponse getUserAchievements(String userId);

    /**
     * Check and unlock achievement if criteria met
     */
    boolean checkAndUnlockAchievement(String userId, AchievementType achievementType, int currentValue);

    /**
     * Update progress towards an achievement
     */
    void updateAchievementProgress(String userId, AchievementType achievementType, int newValue);

    // ==================== GOAL SETTING ====================

    /**
     * Set daily goal for user
     */
    DailyGoal setDailyGoal(String userId, DailyGoalRequest request);

    /**
     * Update goal progress
     */
    void updateGoalProgress(String userId, int additionalMinutes);

    /**
     * Get daily goal response with detailed information
     */
    DailyGoalResponse getDailyGoal(String userId);

    // ==================== HIVE ANALYTICS ====================

    /**
     * Get hive analytics summary
     */
    HiveAnalyticsResponse getHiveAnalytics(String hiveId);

    /**
     * Update hive analytics with new session data
     */
    void updateHiveAnalytics(String hiveId, String userId, int sessionMinutes, int productivityScore);

    // ==================== DATA EXPORT ====================

    /**
     * Export user analytics data
     */
    Map<String, Object> exportUserAnalyticsData(String userId);

    // ==================== EVENT PROCESSING ====================

    /**
     * Process focus session completion event (main integration point)
     */
    void processFocusSessionCompletion(FocusSession session);
}