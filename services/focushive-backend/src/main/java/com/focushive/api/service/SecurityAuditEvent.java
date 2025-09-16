package com.focushive.api.service;

import java.time.LocalDateTime;

/**
 * Security audit event for tracking authorization attempts.
 * Part of Phase 2, Task 2.4: Authorization Rules implementation.
 */
public class SecurityAuditEvent {

    private final String userId;
    private final String operation;
    private final String resourceId;
    private final boolean granted;
    private final LocalDateTime timestamp;
    private final String userAgent;
    private final String ipAddress;

    public SecurityAuditEvent(String userId, String operation, String resourceId,
                            boolean granted, LocalDateTime timestamp) {
        this(userId, operation, resourceId, granted, timestamp, null, null);
    }

    public SecurityAuditEvent(String userId, String operation, String resourceId,
                            boolean granted, LocalDateTime timestamp, String userAgent, String ipAddress) {
        this.userId = userId;
        this.operation = operation;
        this.resourceId = resourceId;
        this.granted = granted;
        this.timestamp = timestamp;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    public String getUserId() { return userId; }
    public String getOperation() { return operation; }
    public String getResourceId() { return resourceId; }
    public boolean isGranted() { return granted; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getUserAgent() { return userAgent; }
    public String getIpAddress() { return ipAddress; }

    @Override
    public String toString() {
        return String.format("SecurityAuditEvent{userId='%s', operation='%s', resourceId='%s', " +
                "granted=%s, timestamp=%s}", userId, operation, resourceId, granted, timestamp);
    }
}