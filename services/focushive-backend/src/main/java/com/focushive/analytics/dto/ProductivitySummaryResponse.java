package com.focushive.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for user productivity summary.
 * Contains aggregated productivity metrics and key performance indicators.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductivitySummaryResponse {

    private String userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Core metrics
    private Integer totalFocusMinutes;
    private Integer totalCompletedSessions;
    private Integer totalSessions;
    private Double completionRate;

    // Productivity metrics
    private Integer averageProductivityScore;
    private Integer highestProductivityScore;
    private Integer lowestProductivityScore;

    // Streak information
    private Integer currentStreak;
    private Integer longestStreak;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastActiveDate;

    // Goals and achievements
    private Integer goalsAchieved;
    private Integer totalGoals;
    private Double goalAchievementRate;
    private Integer newAchievementsUnlocked;

    // Time analysis
    private Integer averageSessionLength;
    private Integer totalBreakMinutes;
    private Integer totalDistractions;
    private Integer peakPerformanceHour;

    // Weekly breakdown
    private List<DailyMetricDto> dailyBreakdown;

    // Comparative data
    private Map<String, Object> comparison;

    // Trends
    private String trend; // "IMPROVING", "DECLINING", "STABLE"
    private Double trendPercentage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetricDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Integer focusMinutes;
        private Integer completedSessions;
        private Integer productivityScore;
        private Boolean goalAchieved;
    }
}