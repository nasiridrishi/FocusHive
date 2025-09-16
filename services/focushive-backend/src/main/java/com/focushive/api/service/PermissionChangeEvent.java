package com.focushive.api.service;

import java.time.LocalDateTime;

/**
 * Permission change tracking event.
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 */
public class PermissionChangeEvent {

    private final String userId;
    private final String permission;
    private final String resourceId;
    private final PermissionChangeType changeType;
    private final LocalDateTime timestamp;
    private final String changedByUserId;
    private final String reason;

    public PermissionChangeEvent(String userId, String permission, String resourceId,
                               PermissionChangeType changeType, LocalDateTime timestamp,
                               String changedByUserId, String reason) {
        this.userId = userId;
        this.permission = permission;
        this.resourceId = resourceId;
        this.changeType = changeType;
        this.timestamp = timestamp;
        this.changedByUserId = changedByUserId;
        this.reason = reason;
    }

    public String getUserId() { return userId; }
    public String getPermission() { return permission; }
    public String getResourceId() { return resourceId; }
    public PermissionChangeType getChangeType() { return changeType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getChangedByUserId() { return changedByUserId; }
    public String getReason() { return reason; }

    public enum PermissionChangeType {
        GRANTED, REVOKED, MODIFIED
    }

    @Override
    public String toString() {
        return String.format("PermissionChangeEvent{userId='%s', permission='%s', resourceId='%s', " +
                "changeType=%s, timestamp=%s, changedBy='%s'}",
                userId, permission, resourceId, changeType, timestamp, changedByUserId);
    }
}