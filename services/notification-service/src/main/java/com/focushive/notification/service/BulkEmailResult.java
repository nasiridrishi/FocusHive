package com.focushive.notification.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of a bulk email sending operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailResult {
    
    /**
     * Total number of emails attempted
     */
    private int totalCount;
    
    /**
     * Number of successfully sent emails
     */
    private int successCount;
    
    /**
     * Number of failed emails
     */
    private int failureCount;
    
    /**
     * List of message IDs for successfully sent emails
     */
    private List<String> messageIds;
    
    /**
     * List of failures with details
     */
    private List<BulkEmailFailure> failures;
    
    /**
     * When the bulk operation started
     */
    private LocalDateTime startedAt;
    
    /**
     * When the bulk operation completed
     */
    private LocalDateTime completedAt;
    
    /**
     * Total processing time in milliseconds
     */
    private Long processingTimeMs;
    
    /**
     * Details about a single email failure in bulk operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkEmailFailure {
        private String notificationId;
        private String recipient;
        private String reason;
        private LocalDateTime failedAt;
    }
}