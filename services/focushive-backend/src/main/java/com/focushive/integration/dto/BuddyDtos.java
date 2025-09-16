package com.focushive.integration.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BuddyDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PotentialMatchDto {
        private String userId;
        private String username;
        private String bio;
        private List<String> interests;
        private String studySchedule;
        private String timezone;
        private Double compatibilityScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddyMatchRequest {
        private String requesterId;
        private String targetUserId;
        private String message;
        private Map<String, Object> preferences;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddyMatchResponse {
        private String partnershipId;
        private String requesterId;
        private String targetUserId;
        private String status;
        private Boolean success;
        private String message;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddyPartnershipDto {
        private String partnershipId;
        private String partnerId;
        private String partnerName;
        private String status;
        private LocalDateTime startDate;
        private Integer checkInStreak;
        private List<String> sharedGoals;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckInRequest {
        private String userId;
        private String message;
        private String mood;
        private Integer productivityScore;
        private List<String> completedTasks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckInResponse {
        private String checkInId;
        private String partnershipId;
        private String userId;
        private String message;
        private Boolean success;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddyGoalRequest {
        private String userId;
        private String partnershipId;
        private String title;
        private String description;
        private String targetDate;
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddyGoalResponse {
        private String goalId;
        private String userId;
        private String partnershipId;
        private String title;
        private String status;
        private Double progress;
        private Boolean success;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoalProgressRequest {
        private String userId;
        private Double progressPercentage;
        private String updateMessage;
        private Map<String, Object> milestones;
    }
}