package com.focushive.notification.entity;

/**
 * Enumeration of notification types supported by the FocusHive notification system.
 * Each type represents a different category of notification that can be sent to users.
 */
public enum NotificationType {
    
    /**
     * When a user is invited to join a hive
     */
    HIVE_INVITATION,
    
    /**
     * When a user is assigned a task
     */
    TASK_ASSIGNED,
    
    /**
     * When a team member completes a task
     */
    TASK_COMPLETED,
    
    /**
     * When a user unlocks a gamification achievement
     */
    ACHIEVEMENT_UNLOCKED,
    
    /**
     * When someone sends a buddy request
     */
    BUDDY_REQUEST,
    
    /**
     * When someone accepts a buddy request
     */
    BUDDY_REQUEST_ACCEPTED,
    
    /**
     * When someone declines a buddy request
     */
    BUDDY_REQUEST_DECLINED,
    
    /**
     * Reminders for focus sessions
     */
    SESSION_REMINDER,
    
    /**
     * When a buddy starts a focus session
     */
    BUDDY_SESSION_STARTED,
    
    /**
     * When a buddy completes a focus session
     */
    BUDDY_SESSION_COMPLETED,
    
    /**
     * System announcements from administrators
     */
    SYSTEM_ANNOUNCEMENT,
    
    /**
     * General system notifications
     */
    SYSTEM_NOTIFICATION,
    
    /**
     * When someone mentions you in chat
     */
    CHAT_MENTION,
    
    /**
     * When you receive a new forum reply
     */
    FORUM_REPLY,
    
    /**
     * Weekly productivity summary
     */
    WEEKLY_SUMMARY,
    
    /**
     * When someone joins your hive
     */
    HIVE_MEMBER_JOINED,
    
    /**
     * When someone leaves your hive
     */
    HIVE_MEMBER_LEFT,
    
    /**
     * When hive settings are updated
     */
    HIVE_SETTINGS_UPDATED,
    
    /**
     * Welcome notification for new users
     */
    WELCOME,
    
    /**
     * Password reset notification
     */
    PASSWORD_RESET,
    
    /**
     * Email verification notification
     */
    EMAIL_VERIFICATION,
    
    /**
     * When buddy matching is successful
     */
    BUDDY_MATCHED,
    
    /**
     * General hive activity notifications
     */
    HIVE_ACTIVITY,
    
    /**
     * Chat message notifications
     */
    CHAT_MESSAGE,
    
    /**
     * Marketing and promotional notifications
     */
    MARKETING,
    
    /**
     * System update notifications
     */
    SYSTEM_UPDATE
}