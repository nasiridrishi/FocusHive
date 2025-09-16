package com.focushive.timer.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for timer statistics response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimerStatisticsResponse {
    private String userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalSessions;
    private Long completedSessions;
    private Long cancelledSessions;
    private Long totalFocusMinutes;
    private Double averageSessionDuration;
    private Double averageProductivityScore;
    private Integer longestSession;
    private Integer mostProductiveHour;
    private Map<String, Long> sessionsByType;
    private Map<String, Double> productivityByDay;
    private Integer currentStreak;
    private Integer longestStreak;
    private Long totalTasksCompleted;
    private Long totalNotesCreated;
    private Double completionRate;
}