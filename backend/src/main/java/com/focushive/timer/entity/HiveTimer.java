package com.focushive.timer.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a synchronized timer for a hive.
 */
@Entity
@Table(name = "hive_timers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiveTimer extends BaseEntity {
    
    @Column(name = "hive_id", nullable = false)
    private String hiveId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "timer_type", nullable = false)
    private TimerType timerType;
    
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
    
    @Column(name = "remaining_seconds", nullable = false)
    private Integer remainingSeconds;
    
    @Column(name = "is_running", nullable = false)
    @Builder.Default
    private Boolean isRunning = false;
    
    @Column(name = "started_by", nullable = false)
    private String startedBy;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "paused_at")
    private LocalDateTime pausedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    public enum TimerType {
        POMODORO,
        COUNTDOWN,
        STOPWATCH
    }
}