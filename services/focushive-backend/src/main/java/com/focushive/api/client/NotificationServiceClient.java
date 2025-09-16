package com.focushive.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(
    name = "notification-service",
    url = "${services.notification.url:http://localhost:8083}",
    path = "/api"
)
public interface NotificationServiceClient {

    @PostMapping("/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);

    @PostMapping("/notifications/batch")
    void sendBatchNotifications(@RequestBody List<NotificationRequest> requests);

    @GetMapping("/notifications/user/{userId}")
    List<NotificationDto> getUserNotifications(@PathVariable Long userId);

    @PutMapping("/notifications/{id}/read")
    void markAsRead(@PathVariable Long id);

    @DeleteMapping("/notifications/{id}")
    void deleteNotification(@PathVariable Long id);

    @GetMapping("/preferences/user/{userId}")
    List<NotificationPreferenceDto> getUserPreferences(@PathVariable Long userId);

    @PutMapping("/preferences/user/{userId}")
    void updatePreferences(@PathVariable Long userId, @RequestBody Map<String, Object> preferences);

    // DTOs
    record NotificationRequest(
        Long userId,
        String type,
        String title,
        String message,
        Map<String, Object> metadata,
        String channel
    ) {}

    record NotificationDto(
        Long id,
        Long userId,
        String type,
        String title,
        String message,
        boolean read,
        String createdAt
    ) {}

    record NotificationPreferenceDto(
        String type,
        boolean emailEnabled,
        boolean pushEnabled,
        boolean inAppEnabled
    ) {}
}