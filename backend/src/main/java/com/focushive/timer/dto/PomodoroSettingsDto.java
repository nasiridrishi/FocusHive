package com.focushive.timer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Pomodoro settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PomodoroSettingsDto {
    private String id;
    private String userId;
    private Integer workDurationMinutes;
    private Integer shortBreakMinutes;
    private Integer longBreakMinutes;
    private Integer sessionsUntilLongBreak;
    private Boolean autoStartBreaks;
    private Boolean autoStartWork;
    private Boolean notificationEnabled;
    private Boolean soundEnabled;
}