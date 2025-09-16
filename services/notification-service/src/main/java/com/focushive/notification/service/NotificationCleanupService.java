package com.focushive.notification.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing notification data cleanup and archival operations.
 * Provides functionality for automated cleanup of old notifications,
 * data archival, and export capabilities.
 */
public interface NotificationCleanupService {

    /**
     * Performs cleanup of old notifications based on retention policy.
     * Notifications older than the configured retention period are archived.
     *
     * @return CleanupResult containing operation details and statistics
     */
    CleanupResult cleanupOldNotifications();

    /**
     * Performs cleanup of old notifications for a specific user.
     *
     * @param userId the user ID to cleanup notifications for
     * @return CleanupResult containing operation details and statistics
     * @throws IllegalArgumentException if userId is null or empty
     */
    CleanupResult cleanupUserNotifications(String userId);

    /**
     * Runs the cleanup operation asynchronously.
     *
     * @return CompletableFuture with cleanup result
     */
    CompletableFuture<CleanupResult> runAsyncCleanup();

    /**
     * Gets statistics about cleanup operations and eligible data.
     *
     * @return CleanupStatistics with current cleanup information
     */
    CleanupStatistics getCleanupStatistics();

    /**
     * Exports archived notification data to a formatted string.
     *
     * @return ExportResult containing exported data
     */
    ExportResult exportArchivedData();

    /**
     * Result of a cleanup operation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CleanupResult {
        private boolean success;
        private int processedCount;
        private int archivedCount;
        private int deletedCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationMillis;
        private String errorMessage;

        public static CleanupResult success(int processed, int archived, int deleted, 
                                          LocalDateTime start, LocalDateTime end) {
            CleanupResult result = new CleanupResult();
            result.success = true;
            result.processedCount = processed;
            result.archivedCount = archived;
            result.deletedCount = deleted;
            result.startTime = start;
            result.endTime = end;
            result.durationMillis = java.time.Duration.between(start, end).toMillis();
            return result;
        }

        public static CleanupResult failure(String errorMessage) {
            CleanupResult result = new CleanupResult();
            result.success = false;
            result.errorMessage = errorMessage;
            result.startTime = LocalDateTime.now();
            result.endTime = LocalDateTime.now();
            return result;
        }
    }

    /**
     * Statistics about cleanup operations.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class CleanupStatistics {
        private long eligibleForCleanup;
        private long totalArchived;
        private long totalDeleted;
        private LocalDateTime lastCleanupTime;
        private int retentionDays;
        private boolean cleanupEnabled;
        private String status;
    }

    /**
     * Result of data export operation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ExportResult {
        private boolean success;
        private int recordCount;
        private String data;
        private String format;
        private LocalDateTime exportTime;
        private String errorMessage;

        public static ExportResult success(int recordCount, String data, String format) {
            ExportResult result = new ExportResult();
            result.success = true;
            result.recordCount = recordCount;
            result.data = data;
            result.format = format;
            result.exportTime = LocalDateTime.now();
            return result;
        }

        public static ExportResult failure(String errorMessage) {
            ExportResult result = new ExportResult();
            result.success = false;
            result.errorMessage = errorMessage;
            result.exportTime = LocalDateTime.now();
            return result;
        }
    }
}