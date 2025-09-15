package com.focushive.notification.controller;

import com.focushive.notification.dto.CreateNotificationRequest;
import com.focushive.notification.dto.NotificationDto;
import com.focushive.notification.entity.Notification;
import com.focushive.notification.entity.NotificationType;
import com.focushive.notification.service.EmailService;
import com.focushive.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for sending test emails and notifications.
 * Only enabled when notification.test.enabled=true
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Test", description = "Test endpoints for notification testing")
@ConditionalOnProperty(name = "notification.test.enabled", havingValue = "true", matchIfMissing = false)
@Profile({"dev", "docker", "test"})
public class TestController {

    private final EmailService emailService;
    private final NotificationService notificationService;

    @PostMapping("/email")
    @Operation(summary = "Send test email", description = "Send a test email to verify email service")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestParam String to,
            @RequestParam(defaultValue = "Test Email from FocusHive") String subject,
            @RequestParam(defaultValue = "This is a test email to verify the notification service is working correctly.") String body) {

        log.info("Sending test email to: {}", to);

        try {
            // Send email directly using EmailService
            emailService.sendSimpleEmail(to, subject, body);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("recipient", to);
            response.put("subject", subject);
            response.put("message", "Test email sent successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending test email to {}: ", to, e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Failed to send test email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/notification")
    @Operation(summary = "Create test notification", description = "Create a test notification for testing")
    public ResponseEntity<NotificationDto> createTestNotification(
            @RequestParam String userId,
            @RequestParam(defaultValue = "SYSTEM_ALERT") String type) {

        log.info("Creating test notification for user: {}", userId);

        try {
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(NotificationType.valueOf(type))
                    .priority(Notification.NotificationPriority.HIGH)
                    .title("Test Notification from FocusHive")
                    .content("This is a test notification sent via the test endpoint.")
                    .data(Map.of(
                            "test", "true",
                            "source", "TestController"
                    ))
                    .build();

            NotificationDto response = notificationService.createNotification(request);
            log.info("Test notification created with ID: {}", response.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating test notification: ", e);
            throw new RuntimeException("Failed to create test notification: " + e.getMessage());
        }
    }

    @PostMapping("/metadata-routing")
    @Operation(summary = "Test metadata routing fix", description = "Test notification with metadata to verify email routing")
    public ResponseEntity<NotificationDto> testMetadataRouting(
            @RequestParam String userId,
            @RequestParam String userEmail,
            @RequestParam(defaultValue = "PASSWORD_RESET") String type) {

        log.info("Testing metadata routing for user: {} with email: {}", userId, userEmail);

        try {
            // Create metadata map with userEmail for routing test
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("source", "test-controller");
            metadataMap.put("userEmail", userEmail);
            metadataMap.put("notificationType", "test_routing");
            metadataMap.put("timestamp", java.time.LocalDateTime.now().toString());

            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(NotificationType.valueOf(type))
                    .priority(Notification.NotificationPriority.HIGH)
                    .title("🎯 METADATA ROUTING TEST")
                    .content("Testing if userEmail in metadata now routes to email queue!")
                    .metadataMap(metadataMap)  // Use metadataMap field for testing
                    .data(Map.of(
                            "test", "metadata-routing",
                            "source", "TestController"
                    ))
                    .build();

            NotificationDto response = notificationService.createNotification(request);
            log.info("Metadata routing test notification created with ID: {}", response.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating metadata routing test notification: ", e);
            throw new RuntimeException("Failed to create metadata routing test notification: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Test endpoint health", description = "Check if test endpoints are enabled")
    public ResponseEntity<Map<String, String>> testHealth() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "active");
        health.put("message", "Test endpoints are enabled");
        return ResponseEntity.ok(health);
    }
}