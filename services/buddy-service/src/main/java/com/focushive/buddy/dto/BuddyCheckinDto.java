package com.focushive.buddy.dto;

import com.focushive.buddy.constant.CheckinFrequency;
import com.focushive.buddy.constant.MoodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for buddy check-in information.
 * Used for daily and weekly check-ins between buddies.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BuddyCheckinDto {

    private UUID id;

    @NotBlank(message = "User ID is required")
    private String userId;

    private UUID partnershipId;

    @Min(value = 1, message = "Mood must be at least 1")
    @Max(value = 10, message = "Mood cannot exceed 10")
    private Integer mood;

    @Min(value = 1, message = "Energy level must be at least 1")
    @Max(value = 10, message = "Energy level cannot exceed 10")
    private Integer energyLevel;

    @Min(value = 1, message = "Productivity must be at least 1")
    @Max(value = 10, message = "Productivity cannot exceed 10")
    private Integer productivity;

    @Min(value = 1, message = "Stress level must be at least 1")
    @Max(value = 10, message = "Stress level cannot exceed 10")
    private Integer stressLevel;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @NotNull(message = "Check-in frequency is required")
    @Builder.Default
    private CheckinFrequency frequency = CheckinFrequency.DAILY;

    private Map<String, Integer> goalsProgress;

    private List<String> challenges;

    private List<String> wins;

    private String weeklyReflection;

    private LocalDateTime checkinDate;

    private LocalDateTime createdAt;

    // Partnership-specific fields
    private String buddyUserId;
    private String buddyName;
    private Boolean buddyCheckedIn;

    // Goal-specific fields
    private List<UUID> goalIds;
    private Map<UUID, Integer> goalProgressMap;

    // Metrics
    private Integer streakDays;
    private Double complianceRate;
    private Integer totalCheckins;

    // Mood as enum (optional, can coexist with mood score)
    private MoodType moodType;

    /**
     * Calculates the overall wellness score based on check-in metrics.
     */
    public double getWellnessScore() {
        if (mood != null && energyLevel != null && productivity != null && stressLevel != null) {
            // Higher mood, energy, productivity is good; lower stress is good
            double adjustedStress = 11 - stressLevel; // Invert stress score
            return (mood + energyLevel + productivity + adjustedStress) / 4.0;
        }
        return 5.0; // Default neutral score
    }

    /**
     * Determines if this is a positive check-in.
     */
    public boolean isPositiveCheckin() {
        return getWellnessScore() >= 7.0;
    }

    /**
     * Checks if the check-in needs attention from buddy.
     */
    public boolean needsBuddySupport() {
        return mood != null && mood <= 3
               || stressLevel != null && stressLevel >= 8
               || (challenges != null && challenges.size() > 3);
    }

    /**
     * Validates that at least one metric is provided.
     */
    public boolean hasMetrics() {
        return mood != null || energyLevel != null
               || productivity != null || stressLevel != null
               || (notes != null && !notes.isEmpty());
    }

    /**
     * Gets the check-in type as a string.
     */
    public String getCheckinType() {
        return frequency == CheckinFrequency.WEEKLY ? "WEEKLY" : "DAILY";
    }
}