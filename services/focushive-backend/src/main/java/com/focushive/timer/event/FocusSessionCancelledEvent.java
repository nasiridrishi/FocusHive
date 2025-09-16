package com.focushive.timer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a focus session is cancelled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionCancelledEvent {
    private String sessionId;
    private String userId;
    private LocalDateTime cancelledAt;
    private String reason;
    private Integer minutesCompleted;
}