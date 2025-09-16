package com.focushive.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for hive analytics summary.
 * Contains aggregated metrics and insights for hive performance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HiveAnalyticsResponse {

    private String hiveId;
    private String hiveName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Core metrics
    private Integer totalActiveUsers;
    private Integer totalFocusTime;
    private Integer totalSessions;
    private Integer completedSessions;
    private Double completionRate;

    // Performance metrics
    private Integer averageProductivityScore;
    private Integer peakConcurrentUsers;
    private Double averageSessionLength;
    private Integer totalBreakTime;
    private Integer totalDistractions;

    // Member engagement
    private List<MemberStats> topPerformers;
    private List<MemberStats> mostActiveMembers;
    private Double memberEngagementScore;

    // Time analysis
    private Integer mostProductiveHour;
    private Map<Integer, Integer> hourlyActivity; // hour -> total minutes
    private Map<String, Integer> dailyActivity; // day -> total minutes

    // Trends and comparisons
    private List<DailyHiveMetric> dailyTrends;
    private String performanceTrend; // "IMPROVING", "DECLINING", "STABLE"
    private Double trendPercentage;

    // Comparative insights
    private HiveComparison comparison;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberStats {
        private String userId;
        private String username;
        private Integer focusMinutes;
        private Integer sessionsCompleted;
        private Integer productivityScore;
        private Double completionRate;
        private Integer rank;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyHiveMetric {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Integer activeUsers;
        private Integer totalFocusTime;
        private Integer completedSessions;
        private Integer averageProductivityScore;
        private Integer peakConcurrentUsers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HiveComparison {
        private Double percentileRank; // 0-100, where this hive ranks among all hives
        private String sizeCategory; // "SMALL", "MEDIUM", "LARGE"
        private Integer platformAvgFocusTime;
        private Double platformAvgProductivityScore;
        private String relativePerfomance; // "ABOVE_AVERAGE", "AVERAGE", "BELOW_AVERAGE"
        private List<String> strengths;
        private List<String> improvementAreas;
    }
}