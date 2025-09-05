package com.focushive.timer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for broadcasting session updates to hive members.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionBroadcast {
    private String userId;
    private String username;
    private String action; // started, ended, paused
    private String sessionType;
    private Integer durationMinutes;
    private Integer actualDurationMinutes;
    private String sessionId;
    private Boolean completed;
}