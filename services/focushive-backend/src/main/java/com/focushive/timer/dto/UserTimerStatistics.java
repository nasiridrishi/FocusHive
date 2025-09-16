package com.focushive.timer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user timer statistics and productivity metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTimerStatistics {

    private String userId;

    private Integer totalSessions;

    private Integer totalFocusMinutes;

    private Integer totalBreakMinutes;

    private Double averageProductivityScore;

    private Double completionRate;

    private Integer longestStreak;

    private Integer currentStreak;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    private Integer completedSessions;

    private Integer cancelledSessions;

    private Integer totalPausedMinutes;
}