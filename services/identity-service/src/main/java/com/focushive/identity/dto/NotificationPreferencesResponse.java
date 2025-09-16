package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponse {
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean smsEnabled;
    private Map<String, Boolean> notificationTypes;
    private String quietHoursStart;
    private String quietHoursEnd;
    private Map<String, String> channelSettings;
}
