package com.focushive.timer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a focus session is resumed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionResumedEvent {
    private String sessionId;
    private String userId;
    private LocalDateTime resumedAt;
    private Integer minutesRemaining;
}