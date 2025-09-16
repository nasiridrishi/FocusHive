package com.focushive.timer.dto;

import com.focushive.timer.entity.FocusSession;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for starting a timer session request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartTimerRequest {

    @NotNull(message = "User ID is required")
    private String userId;

    private String hiveId;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 240, message = "Duration cannot exceed 240 minutes")
    private Integer durationMinutes;

    @Builder.Default
    private FocusSession.SessionType sessionType = FocusSession.SessionType.FOCUS;

    private String title;

    private String description;

    private String templateId;

    @Builder.Default
    private Boolean reminderEnabled = false;

    @Min(value = 0, message = "Reminder minutes before must be non-negative")
    @Max(value = 60, message = "Reminder cannot be more than 60 minutes before")
    private Integer reminderMinutesBefore;

    private String deviceId;

    private String tags;

    @Builder.Default
    private Boolean autoComplete = false;
}