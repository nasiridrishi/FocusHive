package com.focushive.timer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for productivity statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityStatsDto {
    private String id;
    private String userId;
    private LocalDate date;
    private Integer totalFocusMinutes;
    private Integer totalBreakMinutes;
    private Integer sessionsCompleted;
    private Integer sessionsStarted;
    private Integer longestStreakMinutes;
    private Integer dailyGoalMinutes;
    private Double completionPercentage;
    
    // Calculated fields
    private Integer totalMinutes;
    private Double focusRatio; // Focus time / Total time
    private Double averageSessionLength;
}