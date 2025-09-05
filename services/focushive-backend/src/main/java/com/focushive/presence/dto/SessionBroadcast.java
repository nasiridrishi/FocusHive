package com.focushive.presence.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for broadcasting focus session updates.
 */
@Data
@Builder
public class SessionBroadcast {
    private String sessionId;
    private String userId;
    private String hiveId;
    private BroadcastType type;
    private FocusSession session;
    
    public enum BroadcastType {
        SESSION_STARTED,
        SESSION_ENDED,
        SESSION_UPDATED
    }
}