package com.focushive.timer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a focus session is paused.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionPausedEvent {
    private String sessionId;
    private String userId;
    private LocalDateTime pausedAt;
    private Integer minutesCompleted;
    private Integer minutesRemaining;
}