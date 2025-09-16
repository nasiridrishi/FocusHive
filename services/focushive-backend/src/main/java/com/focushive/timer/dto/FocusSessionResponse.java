package com.focushive.timer.dto;

import com.focushive.timer.entity.FocusSession;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for focus session response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FocusSessionResponse {

    private String id;
    private String userId;
    private String hiveId;
    private String title;
    private String description;
    private FocusSession.SessionType sessionType;
    private FocusSession.SessionStatus status;
    private Integer durationMinutes;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime resumedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private Integer totalPausedMinutes;
    private Integer elapsedMinutes;
    private Integer remainingMinutes;
    private Integer productivityScore;
    private Integer tabSwitches;
    private Integer distractionMinutes;
    private Integer focusBreaks;
    private Integer notesCount;
    private Integer tasksCompleted;
    private Boolean reminderEnabled;
    private Integer reminderMinutesBefore;
    private Boolean reminderSent;
    private String templateId;
    private String templateName;
    private String notes;
    private String tags;
    private String deviceId;
    private LocalDateTime lastSyncTime;

    /**
     * Create response from entity.
     */
    public static FocusSessionResponse from(FocusSession session) {
        if (session == null) {
            return null;
        }

        return FocusSessionResponse.builder()
            .id(session.getId())
            .userId(session.getUserId())
            .hiveId(session.getHiveId())
            .title(session.getTitle())
            .description(session.getDescription())
            .sessionType(session.getSessionType())
            .status(session.getStatus())
            .durationMinutes(session.getDurationMinutes())
            .startedAt(session.getStartedAt())
            .pausedAt(session.getPausedAt())
            .resumedAt(session.getResumedAt())
            .completedAt(session.getCompletedAt())
            .cancelledAt(session.getCancelledAt())
            .totalPausedMinutes(session.getTotalPausedMinutes())
            .elapsedMinutes(session.getElapsedMinutes())
            .remainingMinutes(session.getRemainingMinutes())
            .productivityScore(session.getProductivityScore())
            .tabSwitches(session.getTabSwitches())
            .distractionMinutes(session.getDistractionMinutes())
            .focusBreaks(session.getFocusBreaks())
            .notesCount(session.getNotesCount())
            .tasksCompleted(session.getTasksCompleted())
            .reminderEnabled(session.getReminderEnabled())
            .reminderMinutesBefore(session.getReminderMinutesBefore())
            .reminderSent(session.getReminderSent())
            .templateId(session.getTemplateId())
            .templateName(session.getTemplateName())
            .notes(session.getNotes())
            .tags(session.getTags())
            .deviceId(session.getDeviceId())
            .lastSyncTime(session.getLastSyncTime())
            .build();
    }
}