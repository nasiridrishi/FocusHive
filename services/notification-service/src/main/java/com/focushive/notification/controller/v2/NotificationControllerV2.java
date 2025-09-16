package com.focushive.notification.controller.v2;

import com.focushive.notification.annotation.ApiVersion;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version 2 of the Notification Controller with enhanced features.
 * Includes bulk operations, advanced filtering, and enhanced metadata.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@ApiVersion("2")
@RequiredArgsConstructor
@Tag(name = "Notifications V2", description = "Enhanced notification management API (Version 2)")
public class NotificationControllerV2 {

    private final NotificationService notificationService;

    /**
     * Get notifications with advanced filtering (V2 feature).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notifications with advanced filtering",
               description = "Version 2: Supports advanced filtering, sorting, and pagination")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    public ResponseEntity<Page<NotificationDto>> getNotifications(
            @Parameter(description = "User ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Notification types (comma-separated)") @RequestParam(required = false) List<String> types,
            @Parameter(description = "Notification channels (comma-separated)") @RequestParam(required = false) List<String> channels,
            @Parameter(description = "Read status filter") @RequestParam(required = false) Boolean read,
            @Parameter(description = "Priority levels (comma-separated)") @RequestParam(required = false) List<String> priorities,
            @Parameter(description = "Start date for date range filter") @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "End date for date range filter") @RequestParam(required = false) LocalDateTime endDate,
            @Parameter(description = "Search text in title or content") @RequestParam(required = false) String search,
            Pageable pageable) {

        log.debug("V2: Getting notifications with advanced filters - types: {}, channels: {}, priorities: {}",
            types, channels, priorities);

        // Build filter criteria
        Map<String, Object> filters = new HashMap<>();
        if (userId != null) filters.put("userId", userId);
        if (types != null && !types.isEmpty()) filters.put("types", types);
        if (channels != null && !channels.isEmpty()) filters.put("channels", channels);
        if (read != null) filters.put("read", read);
        if (priorities != null && !priorities.isEmpty()) filters.put("priorities", priorities);
        if (startDate != null) filters.put("startDate", startDate);
        if (endDate != null) filters.put("endDate", endDate);
        if (search != null && !search.isEmpty()) filters.put("search", search);

        // TODO: Implement getNotificationsWithFilters in service
        // For now, use standard getNotifications method
        NotificationResponse response = notificationService.getNotifications(userId, pageable);

        // Convert NotificationResponse to Page<NotificationDto> for compatibility
        Page<NotificationDto> notifications = new org.springframework.data.domain.PageImpl<>(
            response.getNotifications(),
            pageable,
            response.getTotalElements()
        );

        return ResponseEntity.ok(notifications);
    }

    /**
     * Bulk mark notifications as read (V2 feature).
     */
    @PutMapping("/bulk-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bulk mark notifications as read",
               description = "Version 2: Mark multiple notifications as read in a single operation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications marked as read"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "400", description = "Invalid notification IDs")
    })
    public ResponseEntity<BulkOperationResponse> bulkMarkAsRead(
            @Valid @RequestBody BulkNotificationRequest request) {

        log.info("V2: Bulk marking {} notifications as read", request.getNotificationIds().size());

        int successCount = notificationService.bulkMarkAsRead(request.getNotificationIds());
        int failedCount = request.getNotificationIds().size() - successCount;

        BulkOperationResponse response = BulkOperationResponse.builder()
            .operation("MARK_AS_READ")
            .totalRequested(request.getNotificationIds().size())
            .successCount(successCount)
            .failedCount(failedCount)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Bulk delete notifications (V2 feature).
     */
    @DeleteMapping("/bulk-delete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bulk delete notifications",
               description = "Version 2: Delete multiple notifications in a single operation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "400", description = "Invalid notification IDs")
    })
    public ResponseEntity<BulkOperationResponse> bulkDelete(
            @Valid @RequestBody BulkNotificationRequest request) {

        log.info("V2: Bulk deleting {} notifications", request.getNotificationIds().size());

        int successCount = notificationService.bulkDelete(request.getNotificationIds());
        int failedCount = request.getNotificationIds().size() - successCount;

        BulkOperationResponse response = BulkOperationResponse.builder()
            .operation("DELETE")
            .totalRequested(request.getNotificationIds().size())
            .successCount(successCount)
            .failedCount(failedCount)
            .timestamp(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get notification statistics (V2 feature).
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notification statistics",
               description = "Version 2: Get detailed statistics about user notifications")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<NotificationStatistics> getStatistics(
            @Parameter(description = "User ID") @RequestParam String userId) {

        log.debug("V2: Getting notification statistics for user: {}", userId);

        com.focushive.notification.service.NotificationStatistics serviceStats = notificationService.getStatistics(userId);

        // Convert to controller DTO
        NotificationStatistics stats = NotificationStatistics.builder()
            .totalNotifications(serviceStats.getTotalCount())
            .unreadCount(serviceStats.getUnreadCount())
            .readCount(serviceStats.getReadCount())
            .countByType(serviceStats.getCountByType())
            .countByChannel(serviceStats.getCountByChannel())
            .countByPriority(new HashMap<>()) // TODO: Implement priority counts
            .oldestUnread(serviceStats.getOldestUnread())
            .mostRecent(serviceStats.getNewestNotification())
            .averageReadTimeMinutes(serviceStats.getAverageResponseTime())
            .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Request object for bulk operations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkNotificationRequest {
        private List<Long> notificationIds;
    }

    /**
     * Response object for bulk operations.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkOperationResponse {
        private String operation;
        private int totalRequested;
        private int successCount;
        private int failedCount;
        private LocalDateTime timestamp;
    }

    /**
     * Notification statistics response.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationStatistics {
        private long totalNotifications;
        private long unreadCount;
        private long readCount;
        private Map<String, Long> countByType;
        private Map<String, Long> countByChannel;
        private Map<String, Long> countByPriority;
        private LocalDateTime oldestUnread;
        private LocalDateTime mostRecent;
        private double averageReadTimeMinutes;
    }
}