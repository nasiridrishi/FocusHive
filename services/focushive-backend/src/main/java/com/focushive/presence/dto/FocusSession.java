package com.focushive.presence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO representing an active focus session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSession {
    private String sessionId;
    private String userId;
    private String username;
    private String hiveId;
    private Instant startTime;
    private Instant endTime;
    private int plannedDurationMinutes;
    private Integer actualDurationMinutes;
    private SessionType type;
    private SessionStatus status;
    
    public enum SessionType {
        FOCUS, BREAK, MEDITATION, PLANNING
    }
    
    public enum SessionStatus {
        ACTIVE, PAUSED, COMPLETED, ABANDONED
    }
}