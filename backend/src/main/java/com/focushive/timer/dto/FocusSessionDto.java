package com.focushive.timer.dto;

import com.focushive.timer.entity.FocusSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for focus session information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionDto {
    private String id;
    private String userId;
    private String hiveId;
    private FocusSession.SessionType sessionType;
    private Integer durationMinutes;
    private Integer actualDurationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean completed;
    private Integer interruptions;
    private String notes;
    private LocalDateTime createdAt;
}