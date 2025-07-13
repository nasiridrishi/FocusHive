package com.focushive.presence.dto;

public record UserPresence(
    String userId,
    PresenceUpdate.UserStatus status,
    String activity,
    long lastSeen
) {}