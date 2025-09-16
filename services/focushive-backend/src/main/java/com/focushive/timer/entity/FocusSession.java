package com.focushive.timer.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Entity representing a focus timer session.
 * Tracks user focus periods with productivity metrics.
 */
@Entity
@Table(name = "focus_sessions", indexes = {
    @Index(name = "idx_focus_sessions_user", columnList = "user_id"),
    @Index(name = "idx_focus_sessions_hive", columnList = "hive_id"),
    @Index(name = "idx_focus_sessions_status", columnList = "status"),
    @Index(name = "idx_focus_sessions_started", columnList = "started_at"),
    @Index(name = "idx_focus_sessions_completed", columnList = "completed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class FocusSession extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "hive_id")
    private String hiveId;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    @Builder.Default
    private SessionType sessionType = SessionType.FOCUS;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "total_paused_duration")
    private Duration totalPausedDuration;

    @Column(name = "actual_duration")
    private Duration actualDuration;

    // Productivity metrics
    @Column(name = "productivity_score")
    private Integer productivityScore;

    @Column(name = "tab_switches")
    @Builder.Default
    private Integer tabSwitches = 0;

    @Column(name = "distraction_minutes")
    @Builder.Default
    private Integer distractionMinutes = 0;

    @Column(name = "focus_breaks")
    @Builder.Default
    private Integer focusBreaks = 0;

    @Column(name = "notes_count")
    @Builder.Default
    private Integer notesCount = 0;

    @Column(name = "tasks_completed")
    @Builder.Default
    private Integer tasksCompleted = 0;

    // Reminder settings
    @Column(name = "reminder_enabled")
    @Builder.Default
    private Boolean reminderEnabled = false;

    @Column(name = "reminder_minutes_before")
    @Builder.Default
    private Integer reminderMinutesBefore = 5;

    @Column(name = "reminder_sent")
    @Builder.Default
    private Boolean reminderSent = false;

    // Template reference
    @Column(name = "template_id")
    private String templateId;

    @Column(name = "template_name")
    private String templateName;

    // Session notes
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "tags")
    private String tags; // Comma-separated tags

    // Device sync
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "sync_token")
    private String syncToken;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    public enum SessionType {
        FOCUS,
        SHORT_BREAK,
        LONG_BREAK,
        CUSTOM
    }

    public enum SessionStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        CANCELLED,
        EXPIRED
    }

    /**
     * Calculate remaining minutes based on current time.
     */
    @Transient
    public Integer getRemainingMinutes() {
        if (status != SessionStatus.ACTIVE) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        Duration elapsed = Duration.between(startedAt, now);

        if (totalPausedDuration != null) {
            elapsed = elapsed.minus(totalPausedDuration);
        }

        int elapsedMinutes = (int) elapsed.toMinutes();
        return Math.max(0, durationMinutes - elapsedMinutes);
    }

    /**
     * Calculate elapsed minutes excluding paused time.
     */
    @Transient
    public Integer getElapsedMinutes() {
        if (startedAt == null) {
            return 0;
        }

        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        Duration elapsed = Duration.between(startedAt, endTime);

        if (totalPausedDuration != null) {
            elapsed = elapsed.minus(totalPausedDuration);
        }

        return (int) elapsed.toMinutes();
    }

    /**
     * Calculate total paused minutes.
     */
    @Transient
    public Integer getTotalPausedMinutes() {
        if (totalPausedDuration == null) {
            return 0;
        }
        return (int) totalPausedDuration.toMinutes();
    }

    /**
     * Check if session has expired.
     */
    @Transient
    public boolean isExpired() {
        if (status == SessionStatus.COMPLETED || status == SessionStatus.CANCELLED) {
            return false;
        }

        LocalDateTime expiryTime = startedAt.plusMinutes(durationMinutes);
        if (totalPausedDuration != null) {
            expiryTime = expiryTime.plus(totalPausedDuration);
        }

        return LocalDateTime.now().isAfter(expiryTime.plusMinutes(5)); // 5 min grace period
    }

    /**
     * Calculate productivity score based on metrics.
     */
    public void calculateProductivityScore() {
        int baseScore = 100;

        // Deduct points for distractions
        baseScore -= (tabSwitches * 2);
        baseScore -= (distractionMinutes * 5);
        baseScore -= (focusBreaks * 3);

        // Add points for engagement
        baseScore += Math.min(notesCount * 2, 20);
        baseScore += Math.min(tasksCompleted * 5, 25);

        // Ensure score is between 0 and 100
        productivityScore = Math.max(0, Math.min(100, baseScore));
    }

    @PrePersist
    public void prePersist() {
        super.prePersist();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SessionStatus.ACTIVE;
        }
        if (sessionType == null) {
            sessionType = SessionType.FOCUS;
        }
        if (totalPausedDuration == null) {
            totalPausedDuration = Duration.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        super.preUpdate();
        if (status == SessionStatus.COMPLETED && productivityScore == null) {
            calculateProductivityScore();
        }
    }
}