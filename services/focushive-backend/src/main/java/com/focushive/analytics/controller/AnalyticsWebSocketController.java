package com.focushive.analytics.controller;

import com.focushive.analytics.dto.ProductivitySummaryResponse;
import com.focushive.analytics.dto.StreakInfoResponse;
import com.focushive.analytics.dto.DailyGoalResponse;
import com.focushive.analytics.service.EnhancedAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket controller for real-time analytics updates.
 * Handles live productivity metrics, achievement notifications, and goal progress.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AnalyticsWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final EnhancedAnalyticsService analyticsService;

    // ==================== CLIENT MESSAGE HANDLERS ====================

    @MessageMapping("/analytics/subscribe")
    public void subscribeToAnalytics(@Payload Map<String, String> request,
                                   SimpMessageHeaderAccessor headerAccessor,
                                   Principal principal) {
        String userId = principal.getName();
        String subscriptionType = request.get("type");

        log.info("User {} subscribing to analytics updates: {}", userId, subscriptionType);

        // Send initial data based on subscription type
        switch (subscriptionType) {
            case "productivity" -> sendProductivityUpdate(userId);
            case "streak" -> sendStreakUpdate(userId);
            case "goals" -> sendGoalUpdate(userId);
            case "achievements" -> sendAchievementUpdate(userId);
            case "all" -> sendAllUpdates(userId);
            default -> log.warn("Unknown subscription type: {}", subscriptionType);
        }
    }

    @MessageMapping("/analytics/unsubscribe")
    public void unsubscribeFromAnalytics(@Payload Map<String, String> request,
                                       Principal principal) {
        String userId = principal.getName();
        String subscriptionType = request.get("type");

        log.info("User {} unsubscribing from analytics updates: {}", userId, subscriptionType);

        // Handle unsubscription logic if needed
        messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/unsubscribed",
            Map.of("type", subscriptionType, "status", "unsubscribed"));
    }

    // ==================== REAL-TIME UPDATE SENDERS ====================

    /**
     * Send productivity summary update to user
     */
    public void sendProductivityUpdate(String userId) {
        try {
            ProductivitySummaryResponse summary = analyticsService.getUserProductivitySummary(userId);

            AnalyticsUpdate update = AnalyticsUpdate.builder()
                .type("PRODUCTIVITY_UPDATE")
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .data(summary)
                .message("Productivity metrics updated")
                .build();

            messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/productivity", update);

            log.debug("Sent productivity update to user: {}", userId);
        } catch (Exception e) {
            log.error("Error sending productivity update to user {}: ", userId, e);
        }
    }

    /**
     * Send streak information update to user
     */
    public void sendStreakUpdate(String userId) {
        try {
            StreakInfoResponse streakInfo = analyticsService.getStreakInformation(userId);

            AnalyticsUpdate update = AnalyticsUpdate.builder()
                .type("STREAK_UPDATE")
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .data(streakInfo)
                .message("Streak information updated")
                .build();

            messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/streak", update);

            log.debug("Sent streak update to user: {}", userId);
        } catch (Exception e) {
            log.error("Error sending streak update to user {}: ", userId, e);
        }
    }

    /**
     * Send goal progress update to user
     */
    public void sendGoalUpdate(String userId) {
        try {
            DailyGoalResponse goal = analyticsService.getDailyGoal(userId);

            AnalyticsUpdate update = AnalyticsUpdate.builder()
                .type("GOAL_UPDATE")
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .data(goal)
                .message(goal != null ? "Goal progress updated" : "No goal set for today")
                .build();

            messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/goals", update);

            log.debug("Sent goal update to user: {}", userId);
        } catch (Exception e) {
            log.error("Error sending goal update to user {}: ", userId, e);
        }
    }

    /**
     * Send achievement unlock notification to user
     */
    public void sendAchievementUpdate(String userId) {
        try {
            // This would typically be called when an achievement is unlocked
            AchievementNotification notification = AchievementNotification.builder()
                .type("ACHIEVEMENT_UNLOCKED")
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .title("Achievement Unlocked!")
                .message("Congratulations on your progress!")
                .points(0)
                .category("General")
                .build();

            messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/achievements", notification);

            log.debug("Sent achievement update to user: {}", userId);
        } catch (Exception e) {
            log.error("Error sending achievement update to user {}: ", userId, e);
        }
    }

    /**
     * Send all analytics updates to user
     */
    public void sendAllUpdates(String userId) {
        sendProductivityUpdate(userId);
        sendStreakUpdate(userId);
        sendGoalUpdate(userId);
    }

    // ==================== EVENT LISTENERS ====================

    /**
     * Listen for focus session completion events and send real-time updates
     */
    @EventListener
    public void handleFocusSessionCompletion(FocusSessionCompletedEvent event) {
        String userId = event.getUserId();
        log.info("Handling focus session completion for user: {}", userId);

        // Send updated analytics to user
        sendProductivityUpdate(userId);
        sendStreakUpdate(userId);
        sendGoalUpdate(userId);

        // Notify about milestone achievements if any
        if (event.isNewStreakMilestone()) {
            sendStreakMilestoneNotification(userId, event.getStreakDays());
        }

        if (event.isGoalAchieved()) {
            sendGoalAchievedNotification(userId);
        }
    }

    /**
     * Listen for achievement unlock events
     */
    @EventListener
    public void handleAchievementUnlocked(AchievementUnlockedEvent event) {
        String userId = event.getUserId();

        AchievementNotification notification = AchievementNotification.builder()
            .type("ACHIEVEMENT_UNLOCKED")
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .title("🏆 Achievement Unlocked!")
            .message(String.format("Congratulations! You've unlocked '%s'", event.getAchievementName()))
            .achievementName(event.getAchievementName())
            .achievementDescription(event.getAchievementDescription())
            .points(event.getPoints())
            .category(event.getCategory())
            .build();

        messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/achievements", notification);

        log.info("Sent achievement unlock notification to user: {} for achievement: {}",
            userId, event.getAchievementName());
    }

    /**
     * Listen for goal achievement events
     */
    @EventListener
    public void handleGoalAchieved(GoalAchievedEvent event) {
        String userId = event.getUserId();

        GoalNotification notification = GoalNotification.builder()
            .type("GOAL_ACHIEVED")
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .title("🎯 Goal Achieved!")
            .message(String.format("Congratulations! You've completed your %d-minute goal!", event.getTargetMinutes()))
            .targetMinutes(event.getTargetMinutes())
            .completedMinutes(event.getCompletedMinutes())
            .overachieved(event.getCompletedMinutes() > event.getTargetMinutes())
            .build();

        messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/goals", notification);

        log.info("Sent goal achievement notification to user: {}", userId);
    }

    // ==================== SPECIALIZED NOTIFICATIONS ====================

    /**
     * Send streak milestone notification
     */
    private void sendStreakMilestoneNotification(String userId, int streakDays) {
        String milestoneMessage = getMilestoneMessage(streakDays);

        StreakNotification notification = StreakNotification.builder()
            .type("STREAK_MILESTONE")
            .userId(userId)
            .timestamp(System.currentTimeMillis())
            .title("🔥 Streak Milestone!")
            .message(milestoneMessage)
            .streakDays(streakDays)
            .milestone(true)
            .build();

        messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/streak", notification);

        log.info("Sent streak milestone notification to user: {} for {} days", userId, streakDays);
    }

    /**
     * Send goal achieved notification
     */
    private void sendGoalAchievedNotification(String userId) {
        DailyGoalResponse goal = analyticsService.getDailyGoal(userId);

        if (goal != null && goal.getAchieved()) {
            GoalNotification notification = GoalNotification.builder()
                .type("GOAL_ACHIEVED")
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .title("🎯 Daily Goal Achieved!")
                .message("Amazing work! You've reached your daily focus goal!")
                .targetMinutes(goal.getTargetMinutes())
                .completedMinutes(goal.getCompletedMinutes())
                .overachieved(goal.getOverachieved())
                .build();

            messagingTemplate.convertAndSendToUser(userId, "/queue/analytics/goals", notification);
        }
    }

    // ==================== HELPER METHODS ====================

    private String getMilestoneMessage(int streakDays) {
        return switch (streakDays) {
            case 3 -> "3 days in a row! You're building momentum! 🚀";
            case 7 -> "One week streak! You're on fire! 🔥";
            case 14 -> "Two weeks strong! Incredible consistency! 💪";
            case 30 -> "30-day streak! You're a focus champion! 🏆";
            case 50 -> "50 days! Your dedication is inspiring! ⭐";
            case 100 -> "100-day milestone! You're a legend! 👑";
            default -> String.format("Amazing %d-day streak! Keep it up! 🎉", streakDays);
        };
    }

    // ==================== DATA TRANSFER OBJECTS ====================

    @lombok.Builder
    @lombok.Data
    public static class AnalyticsUpdate {
        private String type;
        private String userId;
        private long timestamp;
        private Object data;
        private String message;
    }

    @lombok.Builder
    @lombok.Data
    public static class AchievementNotification {
        private String type;
        private String userId;
        private long timestamp;
        private String title;
        private String message;
        private String achievementName;
        private String achievementDescription;
        private int points;
        private String category;
    }

    @lombok.Builder
    @lombok.Data
    public static class GoalNotification {
        private String type;
        private String userId;
        private long timestamp;
        private String title;
        private String message;
        private int targetMinutes;
        private int completedMinutes;
        private boolean overachieved;
    }

    @lombok.Builder
    @lombok.Data
    public static class StreakNotification {
        private String type;
        private String userId;
        private long timestamp;
        private String title;
        private String message;
        private int streakDays;
        private boolean milestone;
    }

    // ==================== EVENT CLASSES ====================

    @lombok.Builder
    @lombok.Data
    public static class FocusSessionCompletedEvent {
        private String userId;
        private int sessionMinutes;
        private boolean isNewStreakMilestone;
        private int streakDays;
        private boolean isGoalAchieved;
    }

    @lombok.Builder
    @lombok.Data
    public static class AchievementUnlockedEvent {
        private String userId;
        private String achievementName;
        private String achievementDescription;
        private int points;
        private String category;
    }

    @lombok.Builder
    @lombok.Data
    public static class GoalAchievedEvent {
        private String userId;
        private int targetMinutes;
        private int completedMinutes;
    }
}