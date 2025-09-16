package com.focushive.buddy.service;

import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import com.focushive.buddy.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for check-in operations.
 * Provides comprehensive check-in functionality including daily/weekly management,
 * mood tracking, productivity metrics, streak calculations, and accountability scoring.
 */
public interface BuddyCheckinService {

    // Daily Check-in Management
    CheckinResponseDto createDailyCheckin(UUID userId, CheckinRequestDto request);
    CheckinResponseDto updateDailyCheckin(UUID userId, UUID checkinId, CheckinRequestDto request);
    CheckinResponseDto getDailyCheckin(UUID userId, UUID checkinId);
    boolean validateCheckinTime(UUID userId, CheckInType checkinType);
    boolean preventDuplicateCheckin(UUID userId, UUID partnershipId, LocalDate date);
    void handleMissedCheckin(UUID userId, UUID partnershipId, LocalDate date);
    List<CheckinResponseDto> getCheckinHistory(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    List<CheckinResponseDto> getUserCheckins(UUID userId, LocalDate startDate, LocalDate endDate);
    void deleteCheckin(UUID userId, UUID checkinId);

    // Weekly Check-in Management
    WeeklyReviewDto createWeeklyReview(UUID userId, UUID partnershipId, WeeklyReviewDto request);
    WeeklyReviewDto aggregateWeeklyData(UUID userId, UUID partnershipId, LocalDate weekStart);
    ProductivityMetricsDto calculateWeeklyProgress(UUID userId, UUID partnershipId, LocalDate weekStart);
    WeeklyReviewDto compareWithPreviousWeek(UUID userId, UUID partnershipId, LocalDate currentWeekStart);
    String generateWeeklyInsights(UUID userId, UUID partnershipId, LocalDate weekStart);
    void scheduleWeeklyReminder(UUID userId, UUID partnershipId);

    // Mood Tracking
    MoodTrackingDto recordMood(UUID userId, UUID partnershipId, MoodType mood, LocalDate date);
    List<MoodTrackingDto> getMoodHistory(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    String analyzeMoodTrends(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    boolean detectMoodAnomalies(UUID userId, UUID partnershipId, MoodType currentMood);
    String suggestMoodInterventions(UUID userId, MoodType currentMood, Double averageScore);
    Double correlateMoodWithProductivity(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);

    // Productivity Metrics
    ProductivityMetricsDto recordProductivityScore(UUID userId, UUID partnershipId, Integer score, LocalDate date);
    Double calculateAverageProductivity(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    Map<String, Double> identifyProductivePeriods(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    ProductivityMetricsDto trackFocusHours(UUID userId, UUID partnershipId, Integer focusHours, LocalDate date);
    String compareWithGoals(UUID userId, UUID partnershipId, Integer currentScore, Integer targetScore);
    CheckinAnalyticsDto generateProductivityReport(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    Double predictProductivityTrends(UUID userId, UUID partnershipId, Integer daysAhead);

    // Streak Calculations
    StreakStatisticsDto calculateDailyStreak(UUID userId, UUID partnershipId);
    StreakStatisticsDto calculateWeeklyStreak(UUID userId, UUID partnershipId);
    StreakStatisticsDto calculateLongestStreak(UUID userId, UUID partnershipId);
    void handleStreakBreak(UUID userId, UUID partnershipId, LocalDate missedDate);
    StreakStatisticsDto getStreakStatistics(UUID userId, UUID partnershipId);
    List<String> rewardStreakMilestones(UUID userId, Integer streakDays);
    boolean recoverStreak(UUID userId, UUID partnershipId, LocalDate recoverDate, String reason);
    Map<String, Integer> comparePartnerStreaks(UUID partnershipId);

    // Accountability Scoring
    AccountabilityScoreDto calculateAccountabilityScore(UUID userId, UUID partnershipId);
    void updateScoreOnCheckin(UUID userId, UUID partnershipId, CheckInType checkinType);
    void penalizeForMissedCheckin(UUID userId, UUID partnershipId, LocalDate missedDate);
    List<AccountabilityScoreDto> getScoreHistory(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    Map<String, AccountabilityScoreDto> compareWithPartner(UUID partnershipId);
    CheckinAnalyticsDto generateScoreReport(UUID userId, UUID partnershipId);
    List<String> suggestScoreImprovement(UUID userId, AccountabilityScoreDto currentScore);

    // Partner Synchronization
    void syncPartnerCheckins(UUID partnershipId);
    void notifyPartnerOfCheckin(UUID userId, UUID partnershipId, CheckinResponseDto checkin);
    Map<String, CheckinResponseDto> comparePartnerProgress(UUID partnershipId, LocalDate date);
    void celebrateJointMilestones(UUID partnershipId, String milestone);
    void handlePartnerAbsence(UUID partnershipId, UUID absentUserId, LocalDate date);

    // Analytics & Insights
    CheckinAnalyticsDto generateCheckinAnalytics(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> identifyPatterns(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
    LocalDateTime predictNextCheckin(UUID userId, UUID partnershipId);
    String suggestOptimalCheckinTime(UUID userId, UUID partnershipId);
    WeeklyReviewDto generateMonthlyReport(UUID userId, UUID partnershipId, LocalDate monthStart);
    byte[] exportCheckinData(UUID userId, UUID partnershipId, LocalDate startDate, LocalDate endDate);
}