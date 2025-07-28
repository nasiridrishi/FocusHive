package com.focushive.presence.dto;

public record PresenceUpdate(
    PresenceStatus status,
    String hiveId,
    String activity
) {}