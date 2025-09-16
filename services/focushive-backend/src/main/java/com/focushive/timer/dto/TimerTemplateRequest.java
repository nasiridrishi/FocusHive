package com.focushive.timer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating timer templates (Pomodoro patterns).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    @Min(value = 1, message = "Focus duration must be at least 1 minute")
    @Max(value = 180, message = "Focus duration cannot exceed 180 minutes")
    private Integer focusDuration;

    @Min(value = 1, message = "Short break duration must be at least 1 minute")
    @Max(value = 30, message = "Short break duration cannot exceed 30 minutes")
    private Integer shortBreakDuration;

    @Min(value = 1, message = "Long break duration must be at least 1 minute")
    @Max(value = 60, message = "Long break duration cannot exceed 60 minutes")
    private Integer longBreakDuration;

    @Min(value = 1, message = "Sessions before long break must be at least 1")
    @Max(value = 10, message = "Sessions before long break cannot exceed 10")
    private Integer sessionsBeforeLongBreak;

    private Boolean isDefault;

    private String description;
}