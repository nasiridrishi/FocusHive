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
public class WebSocketMessage<T> {
    private String id;
    private MessageType type;
    private String event;
    private T payload;
    private String senderId;
    private String senderUsername;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    public enum MessageType {
        // Buddy System Events
        BUDDY_REQUEST,
        BUDDY_REQUEST_ACCEPTED,
        BUDDY_REQUEST_REJECTED,
        BUDDY_CHECKIN,
        BUDDY_GOAL_UPDATE,
        BUDDY_SESSION_START,
        BUDDY_SESSION_END,
        BUDDY_SESSION_REMINDER,
        
        // Forum Events
        FORUM_NEW_POST,
        FORUM_NEW_REPLY,
        FORUM_POST_VOTED,
        FORUM_REPLY_VOTED,
        FORUM_REPLY_ACCEPTED,
        FORUM_MENTION,
        FORUM_POST_EDITED,
        FORUM_POST_DELETED,
        
        // Presence Events
        USER_ONLINE,
        USER_OFFLINE,
        USER_AWAY,
        USER_TYPING,
        USER_STOPPED_TYPING,
        
        // Hive Events
        HIVE_USER_JOINED,
        HIVE_USER_LEFT,
        HIVE_ANNOUNCEMENT,
        
        // System Events
        NOTIFICATION,
        ERROR,
        SUCCESS,
        WARNING,
        INFO,
        PING,
        PONG
    }
}