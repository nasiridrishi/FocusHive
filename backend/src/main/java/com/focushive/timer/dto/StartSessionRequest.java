package com.focushive.timer.dto;

import com.focushive.timer.entity.FocusSession;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting a new focus session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionRequest {
    
    private String hiveId; // Optional - for hive-based sessions
    
    @NotNull(message = "Session type is required")
    private FocusSession.SessionType sessionType;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;
    
    private String notes; // Optional session notes/goals
}