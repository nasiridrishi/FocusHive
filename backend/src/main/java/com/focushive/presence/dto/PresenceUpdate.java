package com.focushive.presence.dto;

public record PresenceUpdate(
    UserStatus status,
    String activity,
    String hiveId
) {
    public enum UserStatus {
        ONLINE, AWAY, BUSY, OFFLINE
    }
}