package com.focushive.timer.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing user's Pomodoro timer preferences.
 */
@Entity
@Table(name = "pomodoro_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PomodoroSettings extends BaseEntity {
    
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    @Column(name = "work_duration_minutes", nullable = false)
    @Builder.Default
    private Integer workDurationMinutes = 25;
    
    @Column(name = "short_break_minutes", nullable = false)
    @Builder.Default
    private Integer shortBreakMinutes = 5;
    
    @Column(name = "long_break_minutes", nullable = false)
    @Builder.Default
    private Integer longBreakMinutes = 15;
    
    @Column(name = "sessions_until_long_break", nullable = false)
    @Builder.Default
    private Integer sessionsUntilLongBreak = 4;
    
    @Column(name = "auto_start_breaks", nullable = false)
    @Builder.Default
    private Boolean autoStartBreaks = false;
    
    @Column(name = "auto_start_work", nullable = false)
    @Builder.Default
    private Boolean autoStartWork = false;
    
    @Column(name = "notification_enabled", nullable = false)
    @Builder.Default
    private Boolean notificationEnabled = true;
    
    @Column(name = "sound_enabled", nullable = false)
    @Builder.Default
    private Boolean soundEnabled = true;
}