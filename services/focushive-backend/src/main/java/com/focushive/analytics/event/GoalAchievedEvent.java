package com.focushive.analytics.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event published when a user achieves their daily goal.
 * This event triggers notifications and real-time updates.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GoalAchievedEvent extends ApplicationEvent {

    private String userId;
    private LocalDate goalDate;
    private int targetMinutes;
    private int completedMinutes;
    private LocalDateTime achievedAt;
    private boolean isOverachieved;
    private String goalDescription;

    public GoalAchievedEvent(Object source) {
        super(source);
    }

    public GoalAchievedEvent(Object source, String userId, int targetMinutes, int completedMinutes) {
        super(source);
        this.userId = userId;
        this.goalDate = LocalDate.now();
        this.targetMinutes = targetMinutes;
        this.completedMinutes = completedMinutes;
        this.achievedAt = LocalDateTime.now();
        this.isOverachieved = completedMinutes > targetMinutes;
    }
}