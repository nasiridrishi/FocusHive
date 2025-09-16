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
public class UpdateNotificationPreferencesRequest {
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private Boolean smsEnabled;
    private Map<String, Boolean> notificationTypes;
    private String quietHoursStart;
    private String quietHoursEnd;
    private Map<String, String> channelSettings;
}
