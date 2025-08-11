package com.focushive.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceUpdate {
    private Long userId;
    private String username;
    private String avatar;
    private PresenceStatus status;
    private String statusMessage;
    private Long hiveId;
    private String currentActivity;
    private LocalDateTime lastSeen;
    private Integer focusMinutesRemaining;
    
    public enum PresenceStatus {
        ONLINE,
        AWAY,
        BUSY,
        IN_FOCUS_SESSION,
        IN_BUDDY_SESSION,
        DO_NOT_DISTURB,
        OFFLINE
    }
}