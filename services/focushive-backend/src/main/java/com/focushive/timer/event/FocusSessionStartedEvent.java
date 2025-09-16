package com.focushive.timer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Event published when a focus session is started.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FocusSessionStartedEvent {
    private String sessionId;
    private String userId;
    private String hiveId;
    private String title;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private String sessionType;
}