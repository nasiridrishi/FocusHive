package com.focushive.presence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing presence information for a hive.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HivePresenceInfo {
    private String hiveId;
    private int activeUsers;
    private int focusingSessions;
    private List<UserPresence> onlineMembers;
    private long lastActivity;
}