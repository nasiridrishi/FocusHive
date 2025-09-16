package com.focushive.analytics.event;

import com.focushive.timer.entity.FocusSession;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Event published when a focus session is completed.
 * This event triggers analytics processing and real-time updates.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class FocusSessionCompletedEvent extends ApplicationEvent {

    private FocusSession session;
    private String userId;
    private String hiveId;
    private Integer sessionMinutes;
    private LocalDateTime completedAt;
    private boolean wasSuccessful;

    public FocusSessionCompletedEvent(Object source, FocusSession session) {
        super(source);
        this.session = session;
        this.userId = session.getUserId();
        this.hiveId = session.getHiveId();
        this.sessionMinutes = session.getElapsedMinutes();
        this.completedAt = session.getCompletedAt();
        this.wasSuccessful = session.getStatus() == FocusSession.SessionStatus.COMPLETED;
    }

    public FocusSessionCompletedEvent(Object source) {
        super(source);
    }
}