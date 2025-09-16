package com.focushive.timer.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a timer template (e.g., Pomodoro, Custom).
 * Users can create and save their own timer configurations.
 */
@Entity
@Table(name = "timer_templates", indexes = {
    @Index(name = "idx_timer_templates_user", columnList = "user_id"),
    @Index(name = "idx_timer_templates_default", columnList = "is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class TimerTemplate extends BaseEntity {

    @Column(name = "user_id")
    private String userId; // Null for system templates

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "focus_duration", nullable = false)
    private Integer focusDuration; // in minutes

    @Column(name = "short_break_duration", nullable = false)
    private Integer shortBreakDuration; // in minutes

    @Column(name = "long_break_duration", nullable = false)
    private Integer longBreakDuration; // in minutes

    @Column(name = "sessions_before_long_break", nullable = false)
    @Builder.Default
    private Integer sessionsBeforeLongBreak = 4;

    @Column(name = "auto_start_breaks")
    @Builder.Default
    private Boolean autoStartBreaks = false;

    @Column(name = "auto_start_focus")
    @Builder.Default
    private Boolean autoStartFocus = false;

    @Column(name = "sound_enabled")
    @Builder.Default
    private Boolean soundEnabled = true;

    @Column(name = "notification_enabled")
    @Builder.Default
    private Boolean notificationEnabled = true;

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false; // System-provided templates

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false; // User's default template

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false; // Can be shared with others

    @Column(name = "icon")
    private String icon; // Icon identifier for UI

    @Column(name = "color")
    private String color; // Color theme for UI

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (usageCount == null) {
            usageCount = 0;
        }
    }

    /**
     * Create a standard Pomodoro template.
     */
    public static TimerTemplate createPomodoroTemplate() {
        return TimerTemplate.builder()
            .name("Pomodoro")
            .description("Traditional Pomodoro Technique: 25 min focus, 5 min short break, 15 min long break")
            .focusDuration(25)
            .shortBreakDuration(5)
            .longBreakDuration(15)
            .sessionsBeforeLongBreak(4)
            .isSystem(true)
            .icon("tomato")
            .color("#FF6347")
            .build();
    }

    /**
     * Create a Deep Work template.
     */
    public static TimerTemplate createDeepWorkTemplate() {
        return TimerTemplate.builder()
            .name("Deep Work")
            .description("Extended focus periods for deep concentration: 90 min focus, 20 min break")
            .focusDuration(90)
            .shortBreakDuration(20)
            .longBreakDuration(30)
            .sessionsBeforeLongBreak(2)
            .isSystem(true)
            .icon("brain")
            .color("#4169E1")
            .build();
    }

    /**
     * Create a 52-17 template.
     */
    public static TimerTemplate create5217Template() {
        return TimerTemplate.builder()
            .name("52-17")
            .description("DeskTime productivity pattern: 52 min focus, 17 min break")
            .focusDuration(52)
            .shortBreakDuration(17)
            .longBreakDuration(30)
            .sessionsBeforeLongBreak(3)
            .isSystem(true)
            .icon("clock")
            .color("#32CD32")
            .build();
    }
}