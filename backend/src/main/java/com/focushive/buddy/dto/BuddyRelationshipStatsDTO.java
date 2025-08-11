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
public class BuddyRelationshipStatsDTO {
    private Long relationshipId;
    private LocalDateTime startDate;
    private Integer daysActive;
    private Long totalGoals;
    private Long completedGoals;
    private Double goalCompletionRate;
    private Long totalSessions;
    private Long completedSessions;
    private Double sessionCompletionRate;
    private Integer totalSessionMinutes;
    private Double averageSessionDuration;
    private Double averageSessionRating;
    private Long totalCheckins;
    private Double averageMoodRating;
    private Double averageProgressRating;
    private LocalDateTime lastInteraction;
    private String relationshipHealth; // "Excellent", "Good", "Fair", "Needs Attention"
}