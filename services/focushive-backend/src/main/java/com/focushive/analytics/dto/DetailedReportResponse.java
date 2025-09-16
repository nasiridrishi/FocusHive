package com.focushive.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.focushive.analytics.enums.ReportPeriod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for detailed analytics reports.
 * Provides comprehensive analytics data for specified time periods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedReportResponse {

    private String userId;
    private ReportPeriod period;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Summary statistics
    private SummaryStats summary;

    // Daily metrics breakdown
    private List<DailyMetricDetail> dailyMetrics;

    // Productivity analysis
    private ProductivityAnalysis productivityAnalysis;

    // Goal tracking
    private GoalTrackingStats goalStats;

    // Achievement progress
    private AchievementStats achievementStats;

    // Time patterns
    private TimePatternAnalysis timePatterns;

    // Comparative insights
    private ComparativeInsights insights;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryStats {
        private Integer totalFocusMinutes;
        private Integer totalSessions;
        private Integer completedSessions;
        private Double completionRate;
        private Integer averageProductivityScore;
        private Integer totalDistractions;
        private Integer totalBreakMinutes;
        private Integer activeDays;
        private Integer streakDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetricDetail {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private String dayOfWeek;
        private Integer focusMinutes;
        private Integer sessions;
        private Integer completedSessions;
        private Integer productivityScore;
        private Integer distractions;
        private Integer breakMinutes;
        private Boolean goalAchieved;
        private Integer goalTarget;
        private Integer goalCompleted;
        private List<String> achievementsUnlocked;
        private Map<Integer, Integer> hourlyDistribution; // hour -> minutes
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivityAnalysis {
        private Double averageScore;
        private Integer highestScore;
        private Integer lowestScore;
        private String trend; // "IMPROVING", "DECLINING", "STABLE"
        private Double trendPercentage;
        private List<ProductivityTrendPoint> trendData;
        private String mostProductiveDay;
        private String mostProductiveTimeRange;
        private Map<String, Double> factorAnalysis; // factor -> impact percentage
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivityTrendPoint {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Integer score;
        private Integer weeklyAverage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalTrackingStats {
        private Integer totalGoals;
        private Integer achievedGoals;
        private Double achievementRate;
        private Integer averageTarget;
        private Integer averageCompleted;
        private Integer overachievedGoals;
        private Integer streak;
        private List<GoalTrendPoint> goalTrend;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalTrendPoint {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Integer target;
        private Integer completed;
        private Boolean achieved;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementStats {
        private Integer totalUnlocked;
        private Integer newThisPeriod;
        private Integer inProgress;
        private Integer totalPoints;
        private List<AchievementDetail> recentUnlocks;
        private List<AchievementDetail> nearCompletion;
        private Map<String, Integer> categoryBreakdown;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AchievementDetail {
        private String type;
        private String name;
        private String description;
        private Integer progress;
        private Boolean unlocked;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private String unlockedAt;
        private Integer points;
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimePatternAnalysis {
        private Map<String, Integer> dayOfWeekDistribution; // Monday -> minutes
        private Map<Integer, Integer> hourlyDistribution; // hour -> minutes
        private String peakProductivityHour;
        private String peakProductivityDay;
        private List<FocusWindow> focusWindows;
        private SessionLengthAnalysis sessionLengths;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FocusWindow {
        private Integer startHour;
        private Integer endHour;
        private Integer totalMinutes;
        private Double productivityScore;
        private String label; // "Morning Focus", "Afternoon Power", etc.
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionLengthAnalysis {
        private Integer averageLength;
        private Integer shortestSession;
        private Integer longestSession;
        private Map<String, Integer> lengthDistribution; // "0-15min" -> count
        private String preferredLength;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparativeInsights {
        private String performanceVsPrevious; // "BETTER", "WORSE", "SIMILAR"
        private Double improvementPercentage;
        private List<String> strengths;
        private List<String> improvementAreas;
        private List<String> recommendations;
        private Map<String, Object> benchmarks; // Platform averages, percentiles
    }
}