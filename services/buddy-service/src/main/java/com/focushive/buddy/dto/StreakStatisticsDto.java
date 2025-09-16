package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for streak statistics
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StreakStatisticsDto {
    private Integer currentDailyStreak;
    private Integer currentWeeklyStreak;
    private Integer longestDailyStreak;
    private Integer longestWeeklyStreak;
    private LocalDate lastCheckinDate;
    private Boolean isOnStreak;
    private Integer daysUntilStreakBreak;

    public StreakStatisticsDto(Integer currentDailyStreak, Integer currentWeeklyStreak,
                             Integer longestDailyStreak, Integer longestWeeklyStreak) {
        this.currentDailyStreak = currentDailyStreak;
        this.currentWeeklyStreak = currentWeeklyStreak;
        this.longestDailyStreak = longestDailyStreak;
        this.longestWeeklyStreak = longestWeeklyStreak;
    }
}