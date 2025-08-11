package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBuddyStatsDTO {
    private Long userId;
    private Long totalBuddyRelationships;
    private Long activeBuddies;
    private Long completedRelationships;
    private Long totalGoalsCreated;
    private Long totalGoalsCompleted;
    private Double goalCompletionRate;
    private Long totalSessionsAttended;
    private Integer totalSessionMinutes;
    private Double averageSessionRating;
    private Long totalCheckinsInitiated;
    private Double averageMoodRating;
    private Double averageProgressRating;
    private LocalDateTime memberSince;
    private List<String> topFocusAreas;
    private String preferredCommunicationStyle;
    private Double buddyRating; // Average rating from all buddies
    private String buddyLevel; // "Beginner", "Intermediate", "Advanced", "Expert"
}