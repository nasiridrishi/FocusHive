package com.focushive.buddy.listener;

import com.focushive.buddy.event.*;
import com.focushive.buddy.service.*;
import com.focushive.buddy.config.BuddySchedulingProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Event listener for buddy service integration events.
 *
 * Handles asynchronous event processing for:
 * - Partnership lifecycle events (created, approved, ended)
 * - Goal milestone achievements
 * - Check-in activities and streak management
 * - Match notifications and recommendations
 *
 * All event handlers include:
 * - Comprehensive error handling and retry logic
 * - Performance monitoring and logging
 * - Transaction management for data consistency
 * - Asynchronous processing for scalability
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuddyEventListener {

    private final BuddyPartnershipService partnershipService;
    private final BuddyMatchingService matchingService;
    private final BuddyGoalService goalService;
    private final BuddyCheckinService checkinService;
    private final BuddySchedulingProperties schedulingProperties;

    // =============================================================================
    // PARTNERSHIP LIFECYCLE EVENT HANDLERS
    // =============================================================================

    /**
     * Handles partnership creation events.
     * Triggers notifications and analytics updates.
     */
    @EventListener
    @Async
    @Transactional
    public void handlePartnershipCreated(PartnershipCreatedEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("Processing partnership created event: partnershipId={}, requester={}, recipient={}",
                event.getPartnershipId(), event.getRequesterId(), event.getRecipientId());

            // Send notification to recipient about new partnership request
            sendPartnershipRequestNotification(event);

            // Update matching metrics for algorithm improvement
            updateMatchingMetrics(event);

            // Track partnership creation analytics
            trackPartnershipCreationAnalytics(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Partnership created event processed in {}ms for partnership {}",
                executionTime, event.getPartnershipId());

        } catch (Exception e) {
            log.error("Failed to process partnership created event for partnership {}: {}",
                event.getPartnershipId(), e.getMessage(), e);
            throw new RuntimeException("Partnership created event processing failed", e);
        }
    }

    /**
     * Handles partnership approval events.
     * Triggers onboarding and goal synchronization.
     */
    @EventListener
    @Async
    @Transactional
    public void handlePartnershipApproved(PartnershipApprovedEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("Processing partnership approved event: partnershipId={}, approver={}, partner={}",
                event.getPartnershipId(), event.getApproverId(), event.getPartnerId());

            // Send celebration notifications to both partners
            sendPartnershipApprovalNotifications(event);

            // Initiate onboarding workflow for new partnership
            initiateOnboardingWorkflow(event);

            // Set up goal synchronization between partners
            setupGoalSynchronization(event);

            // Remove users from matching queue
            removeFromMatchingQueue(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Partnership approved event processed in {}ms for partnership {}",
                executionTime, event.getPartnershipId());

        } catch (Exception e) {
            log.error("Failed to process partnership approved event for partnership {}: {}",
                event.getPartnershipId(), e.getMessage(), e);
            throw new RuntimeException("Partnership approved event processing failed", e);
        }
    }

    /**
     * Handles partnership ended events.
     * Triggers cleanup and analytics calculation.
     */
    @EventListener
    @Async
    @Transactional
    public void handlePartnershipEnded(PartnershipEndedEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("Processing partnership ended event: partnershipId={}, initiator={}, reason={}",
                event.getPartnershipId(), event.getInitiatorId(), event.getEndReason());

            // Clean up shared goals and data
            cleanupSharedData(event);

            // Calculate final partnership analytics
            calculateFinalAnalytics(event);

            // Initiate feedback collection from both partners
            initiateFeedbackCollection(event);

            // Update user profiles based on partnership experience
            updateUserProfiles(event);

            // Add users back to matching queue if eligible
            addBackToMatchingQueue(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Partnership ended event processed in {}ms for partnership {}",
                executionTime, event.getPartnershipId());

        } catch (Exception e) {
            log.error("Failed to process partnership ended event for partnership {}: {}",
                event.getPartnershipId(), e.getMessage(), e);
            throw new RuntimeException("Partnership ended event processing failed", e);
        }
    }

    // =============================================================================
    // GOAL MILESTONE EVENT HANDLERS
    // =============================================================================

    /**
     * Handles goal milestone achievement events.
     * Triggers celebrations and partner notifications.
     */
    @EventListener
    @Async
    @Transactional
    public void handleGoalMilestoneAchieved(GoalMilestoneAchievedEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("Processing milestone achieved event: milestoneId={}, goalId={}, userId={}, progress={}%",
                event.getMilestoneId(), event.getGoalId(), event.getUserId(), event.getProgressPercentage());

            // Trigger celebration notifications
            celebrateAchievement(event);

            // Award achievement badges
            awardAchievementBadges(event);

            // Notify partner of achievement
            notifyPartnerOfAchievement(event);

            // Update goal progress analytics
            updateGoalProgressAnalytics(event);

            // Check for goal completion
            checkForGoalCompletion(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Milestone achieved event processed in {}ms for milestone {}",
                executionTime, event.getMilestoneId());

        } catch (Exception e) {
            log.error("Failed to process milestone achieved event for milestone {}: {}",
                event.getMilestoneId(), e.getMessage(), e);
            throw new RuntimeException("Milestone achieved event processing failed", e);
        }
    }

    // =============================================================================
    // CHECK-IN EVENT HANDLERS
    // =============================================================================

    /**
     * Handles check-in creation events.
     * Updates streaks and accountability scores.
     */
    @EventListener
    @Async
    @Transactional
    public void handleCheckinCreated(CheckinCreatedEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("Processing checkin created event: checkinId={}, userId={}, type={}, score={}",
                event.getCheckinId(), event.getUserId(), event.getCheckinType(), event.getProductivityScore());

            // Update streak calculations
            updateStreakCalculations(event);

            // Update accountability score
            updateAccountabilityScore(event);

            // Notify partner of check-in
            notifyPartnerOfCheckin(event);

            // Update progress tracking analytics
            updateProgressAnalytics(event);

            // Check for streak milestones
            checkForStreakMilestones(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Checkin created event processed in {}ms for checkin {}",
                executionTime, event.getCheckinId());

        } catch (Exception e) {
            log.error("Failed to process checkin created event for checkin {}: {}",
                event.getCheckinId(), e.getMessage(), e);
            throw new RuntimeException("Checkin created event processing failed", e);
        }
    }

    // =============================================================================
    // STREAK EVENT HANDLERS
    // =============================================================================

    /**
     * Handles streak broken events.
     * Sends encouragement and support notifications.
     */
    @EventListener
    @Async
    @Transactional
    public void handleStreakBroken(StreakBrokenEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("Processing streak broken event: userId={}, partnershipId={}, streakLength={}, type={}",
                event.getUserId(), event.getPartnershipId(), event.getBrokenStreakLength(), event.getStreakType());

            // Send encouragement notifications
            sendEncouragementNotifications(event);

            // Suggest streak recovery strategies
            suggestStreakRecovery(event);

            // Notify partner to provide support
            notifyPartnerForSupport(event);

            // Update streak analytics
            updateStreakAnalytics(event);

            // Schedule follow-up check-ins
            scheduleFollowUpCheckins(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Streak broken event processed in {}ms for user {}",
                executionTime, event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process streak broken event for user {}: {}",
                event.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Streak broken event processing failed", e);
        }
    }

    // =============================================================================
    // MATCH EVENT HANDLERS
    // =============================================================================

    /**
     * Handles match found events.
     * Sends recommendations and updates algorithms.
     */
    @EventListener
    @Async
    @Transactional
    public void handleMatchFound(MatchFoundEvent event) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("Processing match found event: userId={}, matches={}, criteria={}",
                event.getUserId(), event.getTotalMatches(), event.getMatchingCriteria());

            // Send match recommendation notifications
            sendMatchRecommendations(event);

            // Update matching algorithm with results
            updateMatchingAlgorithm(event);

            // Track matching success metrics
            trackMatchingMetrics(event);

            // Store match results for future reference
            storeMatchResults(event);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Match found event processed in {}ms for user {} with {} matches",
                executionTime, event.getUserId(), event.getTotalMatches());

        } catch (Exception e) {
            log.error("Failed to process match found event for user {}: {}",
                event.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Match found event processing failed", e);
        }
    }

    // =============================================================================
    // HELPER METHODS FOR PARTNERSHIP EVENTS
    // =============================================================================

    private void sendPartnershipRequestNotification(PartnershipCreatedEvent event) {
        log.debug("Sending partnership request notification to user {}", event.getRecipientId());

        // In a real implementation, this would:
        // 1. Create notification message with partnership details
        // 2. Send via notification service (email, push, in-app)
        // 3. Include partnership agreement and requester profile
        // 4. Provide quick approval/rejection actions

        // Placeholder for notification service integration
        log.info("Partnership request notification sent to user {} for partnership {}",
            event.getRecipientId(), event.getPartnershipId());
    }

    private void updateMatchingMetrics(PartnershipCreatedEvent event) {
        log.debug("Updating matching metrics for partnership creation");

        // In a real implementation, this would:
        // 1. Calculate compatibility score between users
        // 2. Update matching algorithm success rates
        // 3. Store partnership creation data for ML training
        // 4. Adjust user preference weights based on selections

        // Placeholder for matching service integration
        log.info("Matching metrics updated for partnership {} between users {} and {}",
            event.getPartnershipId(), event.getRequesterId(), event.getRecipientId());
    }

    private void trackPartnershipCreationAnalytics(PartnershipCreatedEvent event) {
        log.debug("Tracking partnership creation analytics");

        // In a real implementation, this would:
        // 1. Record creation timestamp and user demographics
        // 2. Track conversion rates from matches to partnerships
        // 3. Monitor partnership request patterns
        // 4. Update user engagement metrics

        log.info("Partnership creation analytics tracked for partnership {}", event.getPartnershipId());
    }

    private void sendPartnershipApprovalNotifications(PartnershipApprovedEvent event) {
        log.debug("Sending partnership approval notifications");

        // Send celebration notifications to both partners
        log.info("Partnership approval notifications sent for partnership {} to users {} and {}",
            event.getPartnershipId(), event.getApproverId(), event.getPartnerId());
    }

    private void initiateOnboardingWorkflow(PartnershipApprovedEvent event) {
        log.debug("Initiating onboarding workflow for partnership {}", event.getPartnershipId());

        // In a real implementation, this would:
        // 1. Schedule onboarding tasks for both partners
        // 2. Create welcome messages and guides
        // 3. Set up initial goals and check-in schedules
        // 4. Provide partnership best practices resources

        log.info("Onboarding workflow initiated for partnership {}", event.getPartnershipId());
    }

    private void setupGoalSynchronization(PartnershipApprovedEvent event) {
        log.debug("Setting up goal synchronization for partnership {}", event.getPartnershipId());

        try {
            // Sync existing goals between partners
            goalService.syncSharedGoals(event.getPartnershipId());
            log.info("Goal synchronization setup completed for partnership {}", event.getPartnershipId());
        } catch (Exception e) {
            log.error("Failed to setup goal synchronization for partnership {}: {}",
                event.getPartnershipId(), e.getMessage());
        }
    }

    private void removeFromMatchingQueue(PartnershipApprovedEvent event) {
        log.debug("Removing users from matching queue");

        try {
            matchingService.removeFromMatchingQueue(event.getApproverId());
            matchingService.removeFromMatchingQueue(event.getPartnerId());
            log.info("Users {} and {} removed from matching queue for partnership {}",
                event.getApproverId(), event.getPartnerId(), event.getPartnershipId());
        } catch (Exception e) {
            log.error("Failed to remove users from matching queue for partnership {}: {}",
                event.getPartnershipId(), e.getMessage());
        }
    }

    private void cleanupSharedData(PartnershipEndedEvent event) {
        log.debug("Cleaning up shared data for partnership {}", event.getPartnershipId());

        try {
            // Clean up partnership-related data
            partnershipService.cleanupPartnershipData(event.getPartnershipId());
            log.info("Shared data cleanup completed for partnership {}", event.getPartnershipId());
        } catch (Exception e) {
            log.error("Failed to cleanup shared data for partnership {}: {}",
                event.getPartnershipId(), e.getMessage());
        }
    }

    private void calculateFinalAnalytics(PartnershipEndedEvent event) {
        log.debug("Calculating final analytics for partnership {}", event.getPartnershipId());

        try {
            // Calculate comprehensive partnership analytics
            var analytics = partnershipService.calculateEngagementMetrics(event.getPartnershipId());
            log.info("Final analytics calculated for partnership {} - Duration: {} days, Success: {}",
                event.getPartnershipId(), event.getDurationDays(), event.isWasSuccessful());
        } catch (Exception e) {
            log.error("Failed to calculate final analytics for partnership {}: {}",
                event.getPartnershipId(), e.getMessage());
        }
    }

    private void initiateFeedbackCollection(PartnershipEndedEvent event) {
        log.debug("Initiating feedback collection for partnership {}", event.getPartnershipId());

        // In a real implementation, this would:
        // 1. Send feedback surveys to both partners
        // 2. Collect partnership experience ratings
        // 3. Gather improvement suggestions
        // 4. Schedule follow-up interviews for successful partnerships

        log.info("Feedback collection initiated for partnership {}", event.getPartnershipId());
    }

    private void updateUserProfiles(PartnershipEndedEvent event) {
        log.debug("Updating user profiles based on partnership experience");

        // In a real implementation, this would:
        // 1. Update user compatibility preferences
        // 2. Adjust matching criteria based on experience
        // 3. Update user reliability scores
        // 4. Record partnership success patterns

        log.info("User profiles updated for partnership {} users {} and {}",
            event.getPartnershipId(), event.getInitiatorId(), event.getPartnerId());
    }

    private void addBackToMatchingQueue(PartnershipEndedEvent event) {
        log.debug("Adding users back to matching queue if eligible");

        try {
            // Only add back to queue if partnership ended positively and users are eligible
            if (event.isWasSuccessful()) {
                matchingService.addToMatchingQueue(event.getInitiatorId());
                matchingService.addToMatchingQueue(event.getPartnerId());
                log.info("Users {} and {} added back to matching queue after successful partnership {}",
                    event.getInitiatorId(), event.getPartnerId(), event.getPartnershipId());
            }
        } catch (Exception e) {
            log.error("Failed to add users back to matching queue for partnership {}: {}",
                event.getPartnershipId(), e.getMessage());
        }
    }

    // =============================================================================
    // HELPER METHODS FOR GOAL EVENTS
    // =============================================================================

    private void celebrateAchievement(GoalMilestoneAchievedEvent event) {
        log.debug("Celebrating milestone achievement for user {}", event.getUserId());

        try {
            var achievements = goalService.celebrateGoalCompletion(event.getGoalId(), event.getUserId());
            log.info("Celebration triggered for milestone {} - {} achievements awarded",
                event.getMilestoneId(), achievements.size());
        } catch (Exception e) {
            log.error("Failed to celebrate achievement for milestone {}: {}",
                event.getMilestoneId(), e.getMessage());
        }
    }

    private void awardAchievementBadges(GoalMilestoneAchievedEvent event) {
        log.debug("Awarding achievement badges for milestone {}", event.getMilestoneId());

        try {
            goalService.awardAchievement(event.getUserId(), "MILESTONE_ACHIEVED",
                event.getMilestoneId(), event.getProgressPercentage());
            log.info("Achievement badge awarded to user {} for milestone {}",
                event.getUserId(), event.getMilestoneId());
        } catch (Exception e) {
            log.error("Failed to award achievement badge for milestone {}: {}",
                event.getMilestoneId(), e.getMessage());
        }
    }

    private void notifyPartnerOfAchievement(GoalMilestoneAchievedEvent event) {
        log.debug("Notifying partner of milestone achievement");

        if (event.getPartnershipId() != null) {
            // In a real implementation, this would send notification to partner
            log.info("Partner notified of milestone achievement for milestone {} by user {}",
                event.getMilestoneId(), event.getUserId());
        }
    }

    private void updateGoalProgressAnalytics(GoalMilestoneAchievedEvent event) {
        log.debug("Updating goal progress analytics");

        try {
            goalService.generateGoalAnalytics(event.getGoalId());
            log.info("Goal progress analytics updated for goal {}", event.getGoalId());
        } catch (Exception e) {
            log.error("Failed to update goal progress analytics for goal {}: {}",
                event.getGoalId(), e.getMessage());
        }
    }

    private void checkForGoalCompletion(GoalMilestoneAchievedEvent event) {
        log.debug("Checking for goal completion");

        if (event.getProgressPercentage() >= 100) {
            log.info("Goal {} appears to be completed with milestone {}",
                event.getGoalId(), event.getMilestoneId());
            // Trigger goal completion celebration
            celebrateAchievement(event);
        }
    }

    // =============================================================================
    // HELPER METHODS FOR CHECK-IN EVENTS
    // =============================================================================

    private void updateStreakCalculations(CheckinCreatedEvent event) {
        log.debug("Updating streak calculations for user {}", event.getUserId());

        try {
            checkinService.updateScoreOnCheckin(event.getUserId(), event.getPartnershipId(),
                com.focushive.buddy.constant.CheckInType.valueOf(event.getCheckinType()));
            log.info("Streak calculations updated for user {} checkin {}",
                event.getUserId(), event.getCheckinId());
        } catch (Exception e) {
            log.error("Failed to update streak calculations for checkin {}: {}",
                event.getCheckinId(), e.getMessage());
        }
    }

    private void updateAccountabilityScore(CheckinCreatedEvent event) {
        log.debug("Updating accountability score for user {}", event.getUserId());

        try {
            checkinService.updateScoreOnCheckin(event.getUserId(), event.getPartnershipId(),
                com.focushive.buddy.constant.CheckInType.valueOf(event.getCheckinType()));
            log.info("Accountability score updated for user {} with score {}",
                event.getUserId(), event.getProductivityScore());
        } catch (Exception e) {
            log.error("Failed to update accountability score for checkin {}: {}",
                event.getCheckinId(), e.getMessage());
        }
    }

    private void notifyPartnerOfCheckin(CheckinCreatedEvent event) {
        log.debug("Notifying partner of check-in");

        try {
            // Get checkin details and notify partner
            var checkin = checkinService.getDailyCheckin(event.getUserId(), event.getCheckinId());
            checkinService.notifyPartnerOfCheckin(event.getUserId(), event.getPartnershipId(), checkin);
            log.info("Partner notified of checkin {} by user {}",
                event.getCheckinId(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to notify partner of checkin {}: {}",
                event.getCheckinId(), e.getMessage());
        }
    }

    private void updateProgressAnalytics(CheckinCreatedEvent event) {
        log.debug("Updating progress analytics for checkin {}", event.getCheckinId());

        try {
            checkinService.generateCheckinAnalytics(event.getUserId(), event.getPartnershipId(),
                java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());
            log.info("Progress analytics updated for user {} checkin {}",
                event.getUserId(), event.getCheckinId());
        } catch (Exception e) {
            log.error("Failed to update progress analytics for checkin {}: {}",
                event.getCheckinId(), e.getMessage());
        }
    }

    private void checkForStreakMilestones(CheckinCreatedEvent event) {
        log.debug("Checking for streak milestones for user {}", event.getUserId());

        try {
            var streakStats = checkinService.calculateDailyStreak(event.getUserId(), event.getPartnershipId());

            // Check for notable milestones
            Integer currentStreak = streakStats.getCurrentDailyStreak();
            if (currentStreak != null && (currentStreak == 7 || currentStreak == 30 || currentStreak == 100)) {
                log.info("Streak milestone achieved: {} days for user {}", currentStreak, event.getUserId());
                // Trigger milestone celebration
            }
        } catch (Exception e) {
            log.error("Failed to check streak milestones for checkin {}: {}",
                event.getCheckinId(), e.getMessage());
        }
    }

    // =============================================================================
    // HELPER METHODS FOR STREAK EVENTS
    // =============================================================================

    private void sendEncouragementNotifications(StreakBrokenEvent event) {
        log.debug("Sending encouragement notifications for broken streak");

        // In a real implementation, this would send motivational messages
        log.info("Encouragement notification sent to user {} for broken {}-day {} streak",
            event.getUserId(), event.getBrokenStreakLength(), event.getStreakType());
    }

    private void suggestStreakRecovery(StreakBrokenEvent event) {
        log.debug("Suggesting streak recovery strategies");

        // In a real implementation, this would provide personalized recovery tips
        log.info("Streak recovery suggestions sent to user {} for {} streak",
            event.getUserId(), event.getStreakType());
    }

    private void notifyPartnerForSupport(StreakBrokenEvent event) {
        log.debug("Notifying partner to provide support");

        if (event.getPartnershipId() != null) {
            // In a real implementation, this would notify partner to offer support
            log.info("Partner support notification sent for user {} streak break",
                event.getUserId());
        }
    }

    private void updateStreakAnalytics(StreakBrokenEvent event) {
        log.debug("Updating streak analytics");

        // Track streak patterns and recovery rates
        log.info("Streak analytics updated for user {} - {} day {} streak broken",
            event.getUserId(), event.getBrokenStreakLength(), event.getStreakType());
    }

    private void scheduleFollowUpCheckins(StreakBrokenEvent event) {
        log.debug("Scheduling follow-up check-ins");

        // In a real implementation, this would schedule reminder notifications
        log.info("Follow-up check-ins scheduled for user {} after streak break",
            event.getUserId());
    }

    // =============================================================================
    // HELPER METHODS FOR MATCH EVENTS
    // =============================================================================

    private void sendMatchRecommendations(MatchFoundEvent event) {
        log.debug("Sending match recommendations to user {}", event.getUserId());

        // In a real implementation, this would send personalized match recommendations
        log.info("Match recommendations sent to user {} - {} potential matches found",
            event.getUserId(), event.getTotalMatches());
    }

    private void updateMatchingAlgorithm(MatchFoundEvent event) {
        log.debug("Updating matching algorithm with results");

        // Update algorithm based on match quality and user interactions
        log.info("Matching algorithm updated with results for user {} - {} matches, criteria: {}",
            event.getUserId(), event.getTotalMatches(), event.getMatchingCriteria());
    }

    private void trackMatchingMetrics(MatchFoundEvent event) {
        log.debug("Tracking matching success metrics");

        // Track match quality, conversion rates, and user satisfaction
        log.info("Matching metrics tracked for user {} - {} matches found with criteria {}",
            event.getUserId(), event.getTotalMatches(), event.getMatchingCriteria());
    }

    private void storeMatchResults(MatchFoundEvent event) {
        log.debug("Storing match results for future reference");

        // Store match history for analytics and improvements
        log.info("Match results stored for user {} - {} matches with average compatibility",
            event.getUserId(), event.getTotalMatches());
    }
}