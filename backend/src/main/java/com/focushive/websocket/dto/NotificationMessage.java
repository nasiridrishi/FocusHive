package com.focushive.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {
    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private String actionUrl;
    private Map<String, Object> data;
    private NotificationPriority priority;
    private Boolean requiresAction;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    public enum NotificationType {
        BUDDY_REQUEST,
        BUDDY_ACCEPTED,
        BUDDY_CHECKIN_REMINDER,
        BUDDY_GOAL_DEADLINE,
        BUDDY_SESSION_STARTING,
        FORUM_REPLY,
        FORUM_MENTION,
        FORUM_VOTE,
        FORUM_ACCEPTED_ANSWER,
        HIVE_INVITATION,
        ACHIEVEMENT_UNLOCKED,
        SYSTEM_ANNOUNCEMENT
    }
    
    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}