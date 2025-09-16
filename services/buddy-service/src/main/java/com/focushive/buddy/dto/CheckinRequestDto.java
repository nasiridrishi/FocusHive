package com.focushive.buddy.dto;

import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a daily check-in
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CheckinRequestDto {
    @NotNull(message = "Partnership ID is required")
    private UUID partnershipId;

    @NotNull(message = "Check-in type is required")
    private CheckInType checkinType;

    @Size(min = 1, max = 1000, message = "Content must be between 1 and 1000 characters")
    private String content;

    @NotNull(message = "Mood is required")
    private MoodType mood;

    @NotNull(message = "Productivity rating is required")
    @Min(value = 1, message = "Productivity rating must be at least 1")
    @Max(value = 10, message = "Productivity rating must not exceed 10")
    private Integer productivityRating;

    @Min(value = 0, message = "Focus hours cannot be negative")
    @Max(value = 24, message = "Focus hours cannot exceed 24")
    private Integer focusHours;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public CheckinRequestDto(UUID partnershipId, CheckInType checkinType, String content,
                           MoodType mood, Integer productivityRating) {
        this.partnershipId = partnershipId;
        this.checkinType = checkinType;
        this.content = content;
        this.mood = mood;
        this.productivityRating = productivityRating;
    }
}