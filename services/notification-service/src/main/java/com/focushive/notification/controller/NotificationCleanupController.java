package com.focushive.notification.controller;

import com.focushive.notification.service.NotificationCleanupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for notification cleanup and archival operations.
 * Provides administrative endpoints for data maintenance.
 */
@RestController
@RequestMapping("/admin/cleanup")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Notification Cleanup", description = "Administrative endpoints for notification data cleanup and archival")
public class NotificationCleanupController {

    private final NotificationCleanupService cleanupService;

    /**
     * Triggers manual cleanup of old notifications.
     *
     * @return cleanup result
     */
    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Run notification cleanup",
        description = "Manually triggers cleanup of old notifications based on retention policy. " +
                     "Only administrators can perform this operation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cleanup completed successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error during cleanup")
    })
    public ResponseEntity<NotificationCleanupService.CleanupResult> runCleanup() {
        log.info("Manual cleanup requested by admin");
        
        NotificationCleanupService.CleanupResult result = cleanupService.cleanupOldNotifications();
        
        if (result.isSuccess()) {
            log.info("Manual cleanup completed successfully. Processed: {}, Archived: {}",
                result.getProcessedCount(), result.getArchivedCount());
            return ResponseEntity.ok(result);
        } else {
            log.error("Manual cleanup failed: {}", result.getErrorMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Triggers asynchronous cleanup of old notifications.
     *
     * @return async cleanup future
     */
    @PostMapping("/run-async")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Run asynchronous notification cleanup",
        description = "Triggers cleanup of old notifications asynchronously. Returns immediately while cleanup runs in background."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Cleanup started asynchronously"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "409", description = "Cleanup already in progress")
    })
    public ResponseEntity<String> runAsyncCleanup() {
        log.info("Async cleanup requested by admin");
        
        CompletableFuture<NotificationCleanupService.CleanupResult> future = cleanupService.runAsyncCleanup();
        
        if (future != null) {
            return ResponseEntity.accepted().body("Cleanup started asynchronously");
        } else {
            return ResponseEntity.status(409).body("Cleanup is already running");
        }
    }

    /**
     * Cleanup notifications for a specific user.
     *
     * @param userId user ID to cleanup notifications for
     * @return cleanup result
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cleanup notifications for specific user",
        description = "Manually triggers cleanup of old notifications for a specific user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User cleanup completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user ID"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Internal server error during cleanup")
    })
    public ResponseEntity<NotificationCleanupService.CleanupResult> cleanupUserNotifications(
            @Parameter(description = "User ID to cleanup notifications for", required = true)
            @PathVariable @NotBlank String userId) {
        
        log.info("User cleanup requested by admin for user: {}", userId);
        
        try {
            NotificationCleanupService.CleanupResult result = cleanupService.cleanupUserNotifications(userId);
            
            if (result.isSuccess()) {
                log.info("User cleanup completed successfully for user: {}. Processed: {}, Archived: {}",
                    userId, result.getProcessedCount(), result.getArchivedCount());
                return ResponseEntity.ok(result);
            } else {
                log.error("User cleanup failed for user: {}: {}", userId, result.getErrorMessage());
                return ResponseEntity.internalServerError().body(result);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID for cleanup: {}", userId, e);
            NotificationCleanupService.CleanupResult errorResult = 
                NotificationCleanupService.CleanupResult.failure("Invalid user ID: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    /**
     * Get cleanup statistics and status.
     *
     * @return cleanup statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get cleanup statistics",
        description = "Returns current cleanup statistics including eligible records, last cleanup time, and configuration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public ResponseEntity<NotificationCleanupService.CleanupStatistics> getCleanupStatistics() {
        log.debug("Cleanup statistics requested by admin");
        
        NotificationCleanupService.CleanupStatistics stats = cleanupService.getCleanupStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Export archived notification data.
     *
     * @return exported data in JSON format
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Export archived notification data",
        description = "Exports archived notification data in JSON format for backup or analysis purposes."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data exported successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
        @ApiResponse(responseCode = "500", description = "Export failed")
    })
    public ResponseEntity<NotificationCleanupService.ExportResult> exportArchivedData() {
        log.info("Archived data export requested by admin");
        
        NotificationCleanupService.ExportResult result = cleanupService.exportArchivedData();
        
        if (result.isSuccess()) {
            log.info("Archived data exported successfully. Records: {}", result.getRecordCount());
            return ResponseEntity.ok(result);
        } else {
            log.error("Archived data export failed: {}", result.getErrorMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Get cleanup configuration.
     *
     * @return cleanup configuration details
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get cleanup configuration",
        description = "Returns current cleanup configuration including retention settings and scheduling."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    public ResponseEntity<CleanupConfig> getCleanupConfiguration() {
        log.debug("Cleanup configuration requested by admin");
        
        NotificationCleanupService.CleanupStatistics stats = cleanupService.getCleanupStatistics();
        
        CleanupConfig config = new CleanupConfig();
        config.setEnabled(stats.isCleanupEnabled());
        config.setRetentionDays(stats.getRetentionDays());
        config.setStatus(stats.getStatus());
        config.setLastCleanupTime(stats.getLastCleanupTime());
        
        return ResponseEntity.ok(config);
    }

    /**
     * Configuration details for cleanup operations.
     */
    public static class CleanupConfig {
        private boolean enabled;
        private int retentionDays;
        private String status;
        private java.time.LocalDateTime lastCleanupTime;

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.LocalDateTime getLastCleanupTime() { return lastCleanupTime; }
        public void setLastCleanupTime(java.time.LocalDateTime lastCleanupTime) { this.lastCleanupTime = lastCleanupTime; }
    }
}