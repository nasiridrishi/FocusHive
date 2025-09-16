package com.focushive.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationRequest {
        private String userId;
        private String type;
        private String title;
        private String message;
        private Map<String, Object> metadata;
        private String channel; // EMAIL, SMS, PUSH, IN_APP
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationResponse {
        private String id;
        private String userId;
        private String type;
        private String title;
        private String message;
        private String status;
        private String channel;
        private LocalDateTime timestamp;
        private LocalDateTime readAt;
        private Map<String, Object> metadata;
    }
}