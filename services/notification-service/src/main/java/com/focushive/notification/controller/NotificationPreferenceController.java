package com.focushive.notification.controller;

import com.focushive.notification.entity.NotificationFrequency;
import com.focushive.notification.entity.NotificationPreference;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.service.NotificationDigestService;
import com.focushive.notification.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing notification preferences.
 * Provides endpoints for CRUD operations on user notification settings.
 */
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Preferences", description = "Manage user notification preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final NotificationDigestService digestService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all notification preferences for a user")
    @ApiResponse(responseCode = "200", description = "User preferences retrieved successfully")
    public ResponseEntity<List<NotificationPreference>> getUserPreferences(
            @Parameter(description = "User ID") @PathVariable String userId) {
        log.info("Getting preferences for user: {}", userId);
        List<NotificationPreference> preferences = preferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/user/{userId}/type/{notificationType}")
    @Operation(summary = "Get specific notification preference by type")
    @ApiResponse(responseCode = "200", description = "Preference retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Preference not found")
    public ResponseEntity<NotificationPreference> getUserPreferenceByType(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Notification type") @PathVariable NotificationType notificationType) {
        log.info("Getting preference for user {} and type {}", userId, notificationType);
        Optional<NotificationPreference> preference = preferenceService.getUserPreference(userId, notificationType);
        return preference.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/user/{userId}")
    @Operation(summary = "Create new notification preference")
    @ApiResponse(responseCode = "201", description = "Preference created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Preference already exists")
    public ResponseEntity<NotificationPreference> createPreference(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Valid @RequestBody CreatePreferenceRequest request) {
        log.info("Creating preference for user {} and type {}", userId, request.notificationType());
        
        NotificationPreference created = preferenceService.createPreference(
                userId,
                request.notificationType(),
                request.inAppEnabled(),
                request.emailEnabled(),
                request.pushEnabled(),
                request.frequency(),
                request.quietStartTime(),
                request.quietEndTime()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/user/{userId}")
    @Operation(summary = "Create or update notification preference")
    @ApiResponse(responseCode = "200", description = "Preference updated successfully")
    @ApiResponse(responseCode = "201", description = "Preference created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<NotificationPreference> createOrUpdatePreference(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Valid @RequestBody CreatePreferenceRequest request) {
        log.info("Creating or updating preference for user {} and type {}", userId, request.notificationType());
        
        NotificationPreference result = preferenceService.createOrUpdatePreference(
                userId,
                request.notificationType(),
                request.inAppEnabled(),
                request.emailEnabled(),
                request.pushEnabled(),
                request.frequency(),
                request.quietStartTime(),
                request.quietEndTime()
        );
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{preferenceId}")
    @Operation(summary = "Update existing notification preference")
    @ApiResponse(responseCode = "200", description = "Preference updated successfully")
    @ApiResponse(responseCode = "404", description = "Preference not found")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<NotificationPreference> updatePreference(
            @Parameter(description = "Preference ID") @PathVariable String preferenceId,
            @Valid @RequestBody UpdatePreferenceRequest request) {
        log.info("Updating preference with ID: {}", preferenceId);
        
        NotificationPreference updated = preferenceService.updatePreference(
                preferenceId,
                request.inAppEnabled(),
                request.emailEnabled(),
                request.pushEnabled(),
                request.frequency(),
                request.quietStartTime(),
                request.quietEndTime()
        );
        
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{preferenceId}")
    @Operation(summary = "Delete notification preference")
    @ApiResponse(responseCode = "204", description = "Preference deleted successfully")
    @ApiResponse(responseCode = "404", description = "Preference not found")
    public ResponseEntity<Void> deletePreference(
            @Parameter(description = "Preference ID") @PathVariable String preferenceId) {
        log.info("Deleting preference with ID: {}", preferenceId);
        preferenceService.deletePreference(preferenceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/enabled")
    @Operation(summary = "Get enabled notification preferences for a user")
    @ApiResponse(responseCode = "200", description = "Enabled preferences retrieved successfully")
    public ResponseEntity<List<NotificationPreference>> getEnabledPreferences(
            @Parameter(description = "User ID") @PathVariable String userId) {
        log.info("Getting enabled preferences for user: {}", userId);
        List<NotificationPreference> enabledPrefs = preferenceService.getEnabledPreferencesForUser(userId);
        return ResponseEntity.ok(enabledPrefs);
    }

    @PostMapping("/user/{userId}/defaults")
    @Operation(summary = "Create default notification preferences for a user")
    @ApiResponse(responseCode = "201", description = "Default preferences created successfully")
    @ApiResponse(responseCode = "200", description = "User already has preferences")
    public ResponseEntity<List<NotificationPreference>> createDefaultPreferences(
            @Parameter(description = "User ID") @PathVariable String userId) {
        log.info("Creating default preferences for user: {}", userId);
        List<NotificationPreference> defaults = preferenceService.createDefaultPreferencesForUser(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(defaults);
    }

    @GetMapping("/user/{userId}/type/{notificationType}/enabled")
    @Operation(summary = "Check if notification is enabled for user and type")
    @ApiResponse(responseCode = "200", description = "Enabled status retrieved successfully")
    public ResponseEntity<Map<String, Boolean>> isNotificationEnabled(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Notification type") @PathVariable NotificationType notificationType) {
        log.info("Checking if notifications enabled for user {} and type {}", userId, notificationType);
        boolean enabled = preferenceService.isNotificationEnabled(userId, notificationType);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @GetMapping("/user/{userId}/type/{notificationType}/quiet-hours")
    @Operation(summary = "Check if current time is within quiet hours")
    @ApiResponse(responseCode = "200", description = "Quiet hours status retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid time format")
    public ResponseEntity<Map<String, Boolean>> checkQuietHours(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Notification type") @PathVariable NotificationType notificationType,
            @Parameter(description = "Time to check (HH:mm format)") @RequestParam String time) {
        log.info("Checking quiet hours for user {} and type {} at time {}", userId, notificationType, time);
        
        try {
            LocalTime checkTime = LocalTime.parse(time);
            boolean inQuietHours = preferenceService.isInQuietHours(userId, notificationType, checkTime);
            return ResponseEntity.ok(Map.of("inQuietHours", inQuietHours));
        } catch (Exception e) {
            log.warn("Invalid time format provided: {}", time);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}/digest/summary")
    @Operation(summary = "Get digest summary for user")
    @ApiResponse(responseCode = "200", description = "Digest summary retrieved successfully")
    public ResponseEntity<Map<String, Object>> getDigestSummary(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Digest frequency") @RequestParam NotificationFrequency frequency) {
        log.info("Getting digest summary for user {} with frequency {}", userId, frequency);
        Map<String, Object> summary = digestService.getDigestSummary(userId, frequency);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/user/{userId}/digest/process")
    @Operation(summary = "Process digest notifications for user")
    @ApiResponse(responseCode = "200", description = "Digest processed successfully")
    @ApiResponse(responseCode = "204", description = "No notifications to process")
    public ResponseEntity<Map<String, Object>> processUserDigest(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Digest frequency") @RequestParam NotificationFrequency frequency) {
        log.info("Processing digest for user {} with frequency {}", userId, frequency);
        
        if (!digestService.hasPendingDigestNotifications(userId, frequency)) {
            return ResponseEntity.noContent().build();
        }
        
        digestService.processDigestForUser(userId, frequency);
        
        Map<String, Object> response = Map.of(
                "status", "processed",
                "userId", userId,
                "frequency", frequency.name()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Request DTO for creating notification preferences.
     */
    public record CreatePreferenceRequest(
            @NotNull(message = "Notification type is required")
            @Schema(description = "Type of notification")
            NotificationType notificationType,
            
            @Schema(description = "Enable in-app notifications", defaultValue = "true")
            Boolean inAppEnabled,
            
            @Schema(description = "Enable email notifications", defaultValue = "true")
            Boolean emailEnabled,
            
            @Schema(description = "Enable push notifications", defaultValue = "true")
            Boolean pushEnabled,
            
            @NotNull(message = "Frequency is required")
            @Schema(description = "Notification frequency")
            NotificationFrequency frequency,
            
            @Schema(description = "Start time for quiet hours (HH:mm)")
            LocalTime quietStartTime,
            
            @Schema(description = "End time for quiet hours (HH:mm)")
            LocalTime quietEndTime
    ) {}

    /**
     * Request DTO for updating notification preferences.
     */
    public record UpdatePreferenceRequest(
            @Schema(description = "Enable in-app notifications")
            Boolean inAppEnabled,
            
            @Schema(description = "Enable email notifications")
            Boolean emailEnabled,
            
            @Schema(description = "Enable push notifications")
            Boolean pushEnabled,
            
            @Schema(description = "Notification frequency")
            NotificationFrequency frequency,
            
            @Schema(description = "Start time for quiet hours (HH:mm)")
            LocalTime quietStartTime,
            
            @Schema(description = "End time for quiet hours (HH:mm)")
            LocalTime quietEndTime
    ) {}
}