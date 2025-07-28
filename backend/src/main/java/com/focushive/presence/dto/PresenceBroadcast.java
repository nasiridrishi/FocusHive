package com.focushive.presence.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for broadcasting presence updates.
 */
@Data
@Builder
public class PresenceBroadcast {
    private String userId;
    private PresenceStatus status;
    private String activity;
    private String hiveId;
    private BroadcastType type;
    
    public enum BroadcastType {
        USER_JOINED,
        USER_LEFT,
        STATUS_CHANGED,
        ACTIVITY_CHANGED
    }
}