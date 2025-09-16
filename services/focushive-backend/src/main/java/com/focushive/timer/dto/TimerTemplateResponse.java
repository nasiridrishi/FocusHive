package com.focushive.timer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for timer templates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerTemplateResponse {

    private String id;

    private String userId;

    private String name;

    private Integer focusDuration;

    private Integer shortBreakDuration;

    private Integer longBreakDuration;

    private Integer sessionsBeforeLongBreak;

    private Boolean isDefault;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}