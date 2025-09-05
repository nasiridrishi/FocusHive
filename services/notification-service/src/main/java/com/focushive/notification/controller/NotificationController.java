package com.focushive.notification.controller;

import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.dto.NotificationResponse;
import com.focushive.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for notification management.
 * Provides endpoints for creating, reading, updating, and deleting notifications.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Notifications", description = "Notification management API")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Create a new notification.
     *
     * @param request the notification creation request
     * @return the created notification
     */
    @PostMapping
    @Operation(summary = "Create a new notification", description = "Creates and delivers a new notification to a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Notification created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<NotificationDto> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        
        log.debug("Creating notification for user {} of type {}", request.getUserId(), request.getType());
        
        try {
            NotificationDto notification = notificationService.createNotification(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create notification: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get paginated notifications for a user.
     *
     * @param userId the user ID
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated notifications
     */
    @GetMapping
    @Operation(summary = "Get user notifications", description = "Retrieves paginated notifications for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<NotificationResponse> getNotifications(
            @Parameter(description = "User ID", required = true)
            @RequestParam @NotBlank String userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        
        log.debug("Getting notifications for user {} - page: {}, size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        NotificationResponse response = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get paginated unread notifications for a user.
     *
     * @param userId the user ID
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated unread notifications
     */
    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications", description = "Retrieves paginated unread notifications for a user")
    public ResponseEntity<NotificationResponse> getUnreadNotifications(
            @Parameter(description = "User ID", required = true)
            @RequestParam @NotBlank String userId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        
        log.debug("Getting unread notifications for user {} - page: {}, size: {}", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        NotificationResponse response = notificationService.getUnreadNotifications(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the count of unread notifications for a user.
     *
     * @param userId the user ID
     * @return unread notification count
     */
    @GetMapping("/unread/count")
    @Operation(summary = "Get unread notification count", description = "Gets the count of unread notifications for a user")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @Parameter(description = "User ID", required = true)
            @RequestParam @NotBlank String userId) {
        
        log.debug("Getting unread count for user {}", userId);
        
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    /**
     * Mark a notification as read.
     *
     * @param id the notification ID
     * @param userId the user ID (for security)
     * @return success response
     */
    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @ApiResponse(responseCode = "400", description = "Notification not found or doesn't belong to user"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Map<String, String>> markAsRead(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable String id,
            @Parameter(description = "User ID", required = true)
            @RequestParam @NotBlank String userId) {
        
        log.debug("Marking notification {} as read for user {}", id, userId);
        
        try {
            notificationService.markAsRead(id, userId);
            return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to mark notification as read: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read for a user.
     *
     * @param userId the user ID
     * @return count of notifications marked as read
     */
    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for a user")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @Parameter(description = "User ID", required = true)
            @RequestParam @NotBlank String userId) {
        
        log.debug("Marking all notifications as read for user {}", userId);
        
        int markedCount = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("markedCount", markedCount));
    }

    /**
     * Delete a notification.
     *
     * @param id the notification ID
     * @param userId the user ID (for security)
     * @return success response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Deletes a specific notification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Notification deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Notification not found or doesn't belong to user"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable String id,
            @Parameter(description = "User ID", required = true)
            @RequestParam @NotBlank String userId) {
        
        log.debug("Deleting notification {} for user {}", id, userId);
        
        try {
            notificationService.deleteNotification(id, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete notification: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Archive a notification.
     *
     * @param id the notification ID
     * @param userId the user ID (for security)
     * @return success response
     */
    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive notification", description = "Archives a specific notification")
    public ResponseEntity<Map<String, String>> archiveNotification(
            @Parameter(description = "Notification ID", required = true)
            @PathVariable String id,
            @Parameter(description = "User ID", required = true)
            @RequestParam @NotBlank String userId) {
        
        log.debug("Archiving notification {} for user {}", id, userId);
        
        try {
            notificationService.archiveNotification(id, userId);
            return ResponseEntity.ok(Map.of("message", "Notification archived"));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to archive notification: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Global exception handler for this controller.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
    }
}