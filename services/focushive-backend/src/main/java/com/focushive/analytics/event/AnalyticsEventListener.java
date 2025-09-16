package com.focushive.analytics.event;

import com.focushive.analytics.controller.AnalyticsWebSocketController;
import com.focushive.analytics.enums.AchievementType;
import com.focushive.analytics.service.EnhancedAnalyticsService;
import com.focushive.timer.entity.FocusSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Event listener for analytics-related events.
 * Handles integration between timer service and analytics service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventListener {

    private final EnhancedAnalyticsService analyticsService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalyticsWebSocketController webSocketController;

    /**
     * Handle focus session completion events from timer service
     */
    @EventListener
    @Async
    @Transactional
    public void handleFocusSessionCompleted(FocusSessionCompletedEvent event) {
        FocusSession session = event.getSession();
        String userId = session.getUserId();

        log.info("Processing focus session completion event for user: {}, session duration: {} minutes",
            userId, session.getElapsedMinutes());

        try {
            // Process analytics updates
            analyticsService.processFocusSessionCompletion(session);

            // Check for achievements based on session data
            checkAndProcessAchievements(session);

            // Send real-time updates via WebSocket
            sendRealTimeUpdates(userId, session);

            log.debug("Successfully processed focus session completion for user: {}", userId);

        } catch (Exception e) {
            log.error("Error processing focus session completion event for user {}: ", userId, e);
        }
    }

    /**
     * Handle achievement unlock events
     */
    @EventListener
    @Async
    public void handleAchievementUnlocked(AchievementUnlockedEvent event) {
        log.info("Processing achievement unlock event for user: {}, achievement: {}",
            event.getUserId(), event.getAchievementType());

        try {
            // Send WebSocket notification
            AnalyticsWebSocketController.AchievementUnlockedEvent wsEvent =
                AnalyticsWebSocketController.AchievementUnlockedEvent.builder()
                    .userId(event.getUserId())
                    .achievementName(event.getAchievementType().getName())
                    .achievementDescription(event.getAchievementType().getDescription())
                    .points(event.getAchievementType().getPoints())
                    .category(event.getAchievementType().getCategory())
                    .build();

            webSocketController.handleAchievementUnlocked(wsEvent);

        } catch (Exception e) {
            log.error("Error processing achievement unlock event: ", e);
        }
    }

    /**
     * Handle goal achievement events
     */
    @EventListener
    @Async
    public void handleGoalAchieved(GoalAchievedEvent event) {
        log.info("Processing goal achievement event for user: {}", event.getUserId());

        try {
            // Send WebSocket notification
            AnalyticsWebSocketController.GoalAchievedEvent wsEvent =
                AnalyticsWebSocketController.GoalAchievedEvent.builder()
                    .userId(event.getUserId())
                    .targetMinutes(event.getTargetMinutes())
                    .completedMinutes(event.getCompletedMinutes())
                    .build();

            webSocketController.handleGoalAchieved(wsEvent);

        } catch (Exception e) {
            log.error("Error processing goal achievement event: ", e);
        }
    }

    /**
     * Handle streak milestone events
     */
    @EventListener
    @Async
    public void handleStreakMilestone(StreakMilestoneEvent event) {
        log.info("Processing streak milestone event for user: {}, milestone: {} days",
            event.getUserId(), event.getStreakDays());

        try {
            // Check for streak-related achievements
            checkStreakAchievements(event.getUserId(), event.getStreakDays());

            // Send WebSocket notification
            AnalyticsWebSocketController.FocusSessionCompletedEvent wsEvent =
                AnalyticsWebSocketController.FocusSessionCompletedEvent.builder()
                    .userId(event.getUserId())
                    .sessionMinutes(0) // Not relevant for streak milestone
                    .isNewStreakMilestone(true)
                    .streakDays(event.getStreakDays())
                    .isGoalAchieved(false)
                    .build();

            webSocketController.handleFocusSessionCompletion(wsEvent);

        } catch (Exception e) {
            log.error("Error processing streak milestone event: ", e);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void checkAndProcessAchievements(FocusSession session) {
        String userId = session.getUserId();

        // Check session-based achievements
        checkSessionAchievements(userId, session);

        // Check cumulative achievements
        checkCumulativeAchievements(userId);

        // Check time-based achievements
        checkTimeBasedAchievements(userId, session);
    }

    private void checkSessionAchievements(String userId, FocusSession session) {
        // First focus achievement
        if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.FIRST_FOCUS, 1)) {
            publishAchievementEvent(userId, AchievementType.FIRST_FOCUS);
        }

        // Duration-based achievements
        Integer elapsedMinutes = session.getElapsedMinutes();
        if (elapsedMinutes != null) {
            if (elapsedMinutes >= 180) {
                if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.MARATHON_RUNNER, elapsedMinutes)) {
                    publishAchievementEvent(userId, AchievementType.MARATHON_RUNNER);
                }
            }
            if (elapsedMinutes >= 300) {
                if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.ULTRA_RUNNER, elapsedMinutes)) {
                    publishAchievementEvent(userId, AchievementType.ULTRA_RUNNER);
                }
            }
            if (elapsedMinutes >= 480) {
                if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.ENDURANCE_MASTER, elapsedMinutes)) {
                    publishAchievementEvent(userId, AchievementType.ENDURANCE_MASTER);
                }
            }
        }

        // Distraction-free achievement
        Integer distractions = session.getTabSwitches();
        if (distractions != null && distractions == 0) {
            // This would need to track cumulative distraction-free sessions
            // For now, just update progress
            analyticsService.updateAchievementProgress(userId, AchievementType.DISTRACTION_FREE, 1);
        }
    }

    private void checkTimeBasedAchievements(String userId, FocusSession session) {
        LocalDateTime completedAt = session.getCompletedAt();
        if (completedAt != null) {
            int hour = completedAt.getHour();

            // Early bird achievement (before 7 AM)
            if (hour < 7) {
                if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.EARLY_BIRD, 1)) {
                    publishAchievementEvent(userId, AchievementType.EARLY_BIRD);
                }
            }

            // Night owl achievement (after 10 PM)
            if (hour >= 22) {
                if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.NIGHT_OWL, 1)) {
                    publishAchievementEvent(userId, AchievementType.NIGHT_OWL);
                }
            }
        }
    }

    private void checkCumulativeAchievements(String userId) {
        // These would require querying the database for cumulative counts
        // Implementation would depend on having efficient queries for session counts
        // For now, this is a placeholder for future implementation
        log.debug("Checking cumulative achievements for user: {}", userId);
    }

    private void checkStreakAchievements(String userId, int streakDays) {
        // Check streak milestone achievements
        if (streakDays == 3) {
            if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.THREE_DAY_STREAK, streakDays)) {
                publishAchievementEvent(userId, AchievementType.THREE_DAY_STREAK);
            }
        } else if (streakDays == 7) {
            if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.WEEK_WARRIOR, streakDays)) {
                publishAchievementEvent(userId, AchievementType.WEEK_WARRIOR);
            }
        } else if (streakDays == 30) {
            if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.MONTH_MASTER, streakDays)) {
                publishAchievementEvent(userId, AchievementType.MONTH_MASTER);
            }
        } else if (streakDays == 100) {
            if (analyticsService.checkAndUnlockAchievement(userId, AchievementType.CENTURY_STREAK, streakDays)) {
                publishAchievementEvent(userId, AchievementType.CENTURY_STREAK);
            }
        }
    }

    private void sendRealTimeUpdates(String userId, FocusSession session) {
        try {
            // Send updated analytics via WebSocket
            webSocketController.sendProductivityUpdate(userId);
            webSocketController.sendStreakUpdate(userId);
            webSocketController.sendGoalUpdate(userId);

            log.debug("Sent real-time analytics updates for user: {}", userId);
        } catch (Exception e) {
            log.error("Error sending real-time updates for user {}: ", userId, e);
        }
    }

    private void publishAchievementEvent(String userId, AchievementType achievementType) {
        AchievementUnlockedEvent event = new AchievementUnlockedEvent(this, userId, achievementType);

        eventPublisher.publishEvent(event);
        log.info("Published achievement unlock event for user: {}, achievement: {}",
            userId, achievementType.getName());
    }
}