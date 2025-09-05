package com.focushive.notification.entity;

/**
 * Enumeration of notification frequency settings.
 * Defines how often notifications of a specific type should be delivered to users.
 */
public enum NotificationFrequency {
    
    /**
     * Send notifications immediately when they occur
     */
    IMMEDIATE,
    
    /**
     * Batch notifications and send as a daily digest
     */
    DAILY_DIGEST,
    
    /**
     * Batch notifications and send as a weekly digest
     */
    WEEKLY_DIGEST,
    
    /**
     * Disable notifications entirely for this type
     */
    OFF
}