package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyCheckinStatsDTO {
    private String relationshipId;
    private Long totalCheckins;
    private Long checkinsLast7Days;
    private Long checkinsLast30Days;
    private Double averageMoodRating;
    private Double averageProgressRating;
    private LocalDateTime lastCheckinDate;
    private Long user1CheckinCount;
    private Long user2CheckinCount;
    private Double moodTrend; // Positive or negative trend
    private Double progressTrend; // Positive or negative trend
}