package com.focushive.notification.service;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Statistics for user notifications.
 */
@Data
@Builder
public class NotificationStatistics {
    private long totalCount;
    private long unreadCount;
    private long readCount;
    private long archivedCount;
    private Map<String, Long> countByType;
    private Map<String, Long> countByChannel;
    private LocalDateTime oldestUnread;
    private LocalDateTime newestNotification;
    private double averageResponseTime; // in hours
    private long last24HoursCount;
    private long last7DaysCount;
    private long last30DaysCount;
}