package com.focushive.timer.dto;

import com.focushive.timer.entity.HiveTimer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for real-time timer state updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerStateDto {
    private String timerId;
    private String hiveId;
    private HiveTimer.TimerType timerType;
    private Integer durationMinutes;
    private Integer remainingSeconds;
    private Boolean isRunning;
    private String startedBy;
    private String startedByUsername;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    
    // For client-side display
    private String phase; // "work", "shortBreak", "longBreak"
    private Integer currentSession; // Current Pomodoro session number
}