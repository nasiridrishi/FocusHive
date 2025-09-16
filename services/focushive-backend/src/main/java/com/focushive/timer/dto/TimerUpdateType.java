package com.focushive.timer.dto;

/**
 * Enum for timer update types.
 */
public enum TimerUpdateType {
    STARTED,
    PAUSED,
    RESUMED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    METRICS_UPDATED,
    NOTE_ADDED,
    TASK_COMPLETED,
    REMINDER_SENT,
    BREAK_TIME,
    POMODORO_CYCLE,
    SYNC_UPDATE
}