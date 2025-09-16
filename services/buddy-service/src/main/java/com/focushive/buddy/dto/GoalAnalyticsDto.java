package com.focushive.buddy.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for goal analytics and insights.
 * Contains comprehensive analytics data for goals and milestone performance.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoalAnalyticsDto {

    private UUID goalId;
    private String goalTitle;
    private LocalDateTime analyticsGeneratedAt;
    private String analyticsVersion;

    /**
     * Overall goal performance metrics
     */
    private OverallPerformanceDto overallPerformance;

    /**
     * Progress trend analysis
     */
    private ProgressTrendDto progressTrend;

    /**
     * Milestone performance analytics
     */
    private MilestonePerformanceDto milestonePerformance;

    /**
     * Time-based analytics
     */
    private TimeAnalyticsDto timeAnalytics;

    /**
     * Collaboration analytics (for shared goals)
     */
    private CollaborationAnalyticsDto collaborationAnalytics;

    /**
     * Predictive analytics
     */
    private PredictiveAnalyticsDto predictiveAnalytics;

    /**
     * Comparative analytics (vs. similar goals)
     */
    private ComparativeAnalyticsDto comparativeAnalytics;

    /**
     * Success patterns and insights
     */
    private List<InsightDto> insights;

    /**
     * Recommendations for improvement
     */
    private List<RecommendationDto> recommendations;

    /**
     * DTO for overall performance metrics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallPerformanceDto {
        private Integer currentProgress;
        private Double completionRate; // percentage completed per day
        private Integer totalDaysActive;
        private Integer daysWithProgress;
        private Integer stagnantDays;
        private Double averageDailyProgress;
        private String performanceGrade; // A, B, C, D, F
        private Double velocityScore; // 0-100 score
    }

    /**
     * DTO for progress trend analysis
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressTrendDto {
        private String trendDirection; // ACCELERATING, STEADY, SLOWING, STAGNANT
        private Double trendSlope; // mathematical trend slope
        private List<DailyProgressDto> dailyProgress;
        private List<WeeklyProgressDto> weeklyProgress;
        private LocalDate lastSignificantProgress;
        private Integer longestStreakDays;
        private Integer currentStreakDays;
    }

    /**
     * DTO for milestone performance
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestonePerformanceDto {
        private Integer totalMilestones;
        private Integer completedMilestones;
        private Integer overdueMilestones;
        private Double averageCompletionTime; // days
        private Double milestoneVelocity; // milestones per week
        private List<MilestoneEfficiencyDto> milestoneEfficiency;
        private String milestoneCompletionPattern; // FRONT_LOADED, BACK_LOADED, STEADY
    }

    /**
     * DTO for time-based analytics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeAnalyticsDto {
        private LocalDate estimatedCompletionDate;
        private Integer daysUntilTarget;
        private Boolean onTrackForTarget;
        private Double timeUtilizationRate; // how well time is being used
        private Map<String, Integer> progressByDayOfWeek;
        private Map<String, Integer> progressByTimeOfDay;
        private List<ProductivityPeriodDto> highProductivityPeriods;
    }

    /**
     * DTO for collaboration analytics (shared goals)
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollaborationAnalyticsDto {
        private UUID partnerId;
        private String partnerName;
        private Integer userContributionPercentage;
        private Integer partnerContributionPercentage;
        private LocalDateTime lastPartnerActivity;
        private Double collaborationScore; // 0-100
        private String collaborationPattern; // BALANCED, USER_DRIVEN, PARTNER_DRIVEN
        private List<CollaborationEventDto> recentCollaborationEvents;
    }

    /**
     * DTO for predictive analytics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictiveAnalyticsDto {
        private LocalDate predictedCompletionDate;
        private Double completionProbability; // 0-100
        private String riskLevel; // LOW, MEDIUM, HIGH
        private List<String> riskFactors;
        private List<String> successFactors;
        private String recommendedAction; // STAY_COURSE, ACCELERATE, ADJUST_TARGET
        private Double confidenceInterval; // 0-100
    }

    /**
     * DTO for comparative analytics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparativeAnalyticsDto {
        private Double communityAverageProgress;
        private String performanceVsCommunity; // ABOVE_AVERAGE, AVERAGE, BELOW_AVERAGE
        private Integer rankingPercentile; // 0-100
        private List<BenchmarkDto> similarGoalsBenchmarks;
        private String categoryAverageCompletion;
        private Double successRateForCategory;
    }

    /**
     * DTO for daily progress data
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyProgressDto {
        private LocalDate date;
        private Integer progressChange;
        private Integer cumulativeProgress;
        private String activityLevel; // HIGH, MEDIUM, LOW, NONE
    }

    /**
     * DTO for weekly progress data
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyProgressDto {
        private LocalDate weekStartDate;
        private Integer progressChange;
        private Integer cumulativeProgress;
        private Integer activeDays;
        private String weeklyTrend; // ACCELERATING, STEADY, SLOWING
    }

    /**
     * DTO for milestone efficiency
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneEfficiencyDto {
        private UUID milestoneId;
        private String milestoneTitle;
        private Integer plannedDays;
        private Integer actualDays;
        private Double efficiencyRatio; // actual/planned
        private String efficiencyGrade; // A, B, C, D, F
    }

    /**
     * DTO for productivity periods
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivityPeriodDto {
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer progressMade;
        private String productivityLevel; // HIGH, MEDIUM, LOW
        private List<String> contributingFactors;
    }

    /**
     * DTO for collaboration events
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollaborationEventDto {
        private LocalDateTime timestamp;
        private String eventType; // PROGRESS_UPDATE, MILESTONE_COMPLETION, COMMENT
        private UUID userId;
        private String userName;
        private String description;
    }

    /**
     * DTO for benchmarks
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenchmarkDto {
        private String benchmarkType; // CATEGORY, DIFFICULTY, DURATION
        private String benchmarkValue;
        private Double averageProgress;
        private Double averageCompletionTime;
        private Double successRate;
    }

    /**
     * DTO for insights
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightDto {
        private String insightType; // PATTERN, TREND, ACHIEVEMENT, WARNING
        private String title;
        private String description;
        private String severity; // INFO, LOW, MEDIUM, HIGH, CRITICAL
        private LocalDateTime discoveredAt;
        private Map<String, Object> metadata;
    }

    /**
     * DTO for recommendations
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationDto {
        private String recommendationType; // GOAL_ADJUSTMENT, MILESTONE_RESTRUCTURE, TIME_MANAGEMENT
        private String title;
        private String description;
        private String priority; // LOW, MEDIUM, HIGH, CRITICAL
        private String actionRequired; // IMMEDIATE, SOON, OPTIONAL
        private List<String> actionSteps;
        private String expectedImpact;
        private Double confidenceScore; // 0-100
    }
}