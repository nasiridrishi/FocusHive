package com.focushive.presence.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * DTO representing a user's presence information.
 */
@Data
@Builder
public class UserPresence {
    private String userId;
    private PresenceStatus status;
    private String activity;
    private Instant lastSeen;
    private String currentHiveId;
    private boolean inFocusSession;
}