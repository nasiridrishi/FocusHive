package com.focushive.integration.service;

import com.focushive.integration.client.BuddyServiceClient;
import com.focushive.integration.dto.BuddyDtos;
import com.focushive.integration.dto.NotificationDtos;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for integrating with the Buddy microservice.
 * Handles all buddy system operations through Feign client.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.features.buddy.enabled", havingValue = "true")
public class BuddyIntegrationService {

    private final BuddyServiceClient buddyClient;
    private final NotificationIntegrationService notificationService;

    @Autowired
    public BuddyIntegrationService(BuddyServiceClient buddyClient,
                                    NotificationIntegrationService notificationService) {
        this.buddyClient = buddyClient;
        this.notificationService = notificationService;
        log.info("Buddy Integration Service initialized");
    }

    /**
     * Get potential buddy matches for a user.
     */
    public List<BuddyDtos.PotentialMatchDto> getPotentialMatches(String userId) {
        log.debug("Fetching potential matches for user: {}", userId);

        try {
            List<BuddyDtos.PotentialMatchDto> matches = buddyClient.getPotentialMatches(userId);
            log.info("Found {} potential matches for user: {}", matches.size(), userId);
            return matches;
        } catch (Exception e) {
            log.error("Failed to fetch potential matches for user: {}", userId, e);
            // Fallback response is handled by BuddyServiceFallback
            throw e;
        }
    }

    /**
     * Create a buddy match request.
     */
    public BuddyDtos.BuddyMatchResponse createBuddyMatch(String requesterId, String targetUserId,
                                                String message, Map<String, Object> preferences) {
        log.debug("Creating buddy match - Requester: {}, Target: {}", requesterId, targetUserId);

        BuddyDtos.BuddyMatchRequest request = new BuddyDtos.BuddyMatchRequest();
        request.setRequesterId(requesterId);
        request.setTargetUserId(targetUserId);
        request.setMessage(message);
        request.setPreferences(preferences);

        try {
            BuddyDtos.BuddyMatchResponse response = buddyClient.createMatch(request);

            if (response.getSuccess()) {
                log.info("Buddy match created successfully - Partnership ID: {}", response.getPartnershipId());

                // Send notification to target user
                notificationService.sendBuddyNotification(
                    targetUserId,
                    requesterId,
                    "MATCH_REQUEST",
                    "You have a new buddy match request!"
                );
            } else {
                log.warn("Buddy match creation failed: {}", response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to create buddy match", e);
            throw e;
        }
    }

    /**
     * Get user's active buddy partnerships.
     */
    public List<BuddyDtos.BuddyPartnershipDto> getUserPartnerships(String userId) {
        log.debug("Fetching partnerships for user: {}", userId);

        try {
            List<BuddyDtos.BuddyPartnershipDto> partnerships = buddyClient.getUserPartnerships(userId);
            log.info("User {} has {} active partnerships", userId, partnerships.size());
            return partnerships;
        } catch (Exception e) {
            log.error("Failed to fetch partnerships for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Create a check-in for a partnership.
     */
    public BuddyDtos.CheckInResponse createCheckIn(String partnershipId, String userId, String message,
                                          String mood, Integer productivityScore,
                                          List<String> completedTasks) {
        log.debug("Creating check-in for partnership: {} by user: {}", partnershipId, userId);

        BuddyDtos.CheckInRequest request = new BuddyDtos.CheckInRequest();
        request.setUserId(userId);
        request.setMessage(message);
        request.setMood(mood);
        request.setProductivityScore(productivityScore);
        request.setCompletedTasks(completedTasks);

        try {
            BuddyDtos.CheckInResponse response = buddyClient.createCheckIn(partnershipId, request);

            if (response.getSuccess()) {
                log.info("Check-in created successfully - ID: {}", response.getCheckInId());

                // Notify partner about the check-in
                // Note: Partner ID should be retrieved from partnership details
                notificationService.sendBuddyNotification(
                    userId,
                    partnershipId,
                    "CHECK_IN",
                    "Your buddy has checked in!"
                );
            } else {
                log.warn("Check-in creation failed: {}", response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to create check-in", e);
            throw e;
        }
    }

    /**
     * Get check-ins for a partnership.
     */
    public List<BuddyDtos.CheckInResponse> getPartnershipCheckIns(String partnershipId) {
        log.debug("Fetching check-ins for partnership: {}", partnershipId);

        try {
            List<BuddyDtos.CheckInResponse> checkIns = buddyClient.getCheckIns(partnershipId);
            log.info("Retrieved {} check-ins for partnership: {}", checkIns.size(), partnershipId);
            return checkIns;
        } catch (Exception e) {
            log.error("Failed to fetch check-ins for partnership: {}", partnershipId, e);
            throw e;
        }
    }

    /**
     * Create a shared goal.
     */
    public BuddyDtos.BuddyGoalResponse createSharedGoal(String userId, String partnershipId,
                                               String title, String description,
                                               String targetDate, String category) {
        log.debug("Creating shared goal for partnership: {}", partnershipId);

        BuddyDtos.BuddyGoalRequest request = new BuddyDtos.BuddyGoalRequest();
        request.setUserId(userId);
        request.setPartnershipId(partnershipId);
        request.setTitle(title);
        request.setDescription(description);
        request.setTargetDate(targetDate);
        request.setCategory(category);

        try {
            BuddyDtos.BuddyGoalResponse response = buddyClient.createGoal(request);

            if (response.getSuccess()) {
                log.info("Shared goal created successfully - ID: {}", response.getGoalId());

                // Notify partner about the new goal
                notificationService.sendBuddyNotification(
                    userId,
                    partnershipId,
                    "NEW_GOAL",
                    "A new shared goal has been created: " + title
                );
            } else {
                log.warn("Goal creation failed: {}", response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to create shared goal", e);
            throw e;
        }
    }

    /**
     * Get user's shared goals.
     */
    public List<BuddyDtos.BuddyGoalResponse> getUserGoals(String userId) {
        log.debug("Fetching goals for user: {}", userId);

        try {
            List<BuddyDtos.BuddyGoalResponse> goals = buddyClient.getUserGoals(userId);
            log.info("User {} has {} shared goals", userId, goals.size());
            return goals;
        } catch (Exception e) {
            log.error("Failed to fetch goals for user: {}", userId, e);
            throw e;
        }
    }

    /**
     * Update goal progress.
     */
    public void updateGoalProgress(String goalId, String userId, Double progressPercentage,
                                    String updateMessage, Map<String, Object> milestones) {
        log.debug("Updating progress for goal: {} to {}%", goalId, progressPercentage);

        BuddyDtos.GoalProgressRequest request = new BuddyDtos.GoalProgressRequest();
        request.setUserId(userId);
        request.setProgressPercentage(progressPercentage);
        request.setUpdateMessage(updateMessage);
        request.setMilestones(milestones);

        try {
            buddyClient.updateGoalProgress(goalId, request);
            log.info("Goal progress updated successfully - Goal: {}, Progress: {}%", goalId, progressPercentage);

            // Notify partner about progress update
            if (progressPercentage >= 100) {
                notificationService.sendBuddyNotification(
                    userId,
                    goalId,
                    "GOAL_COMPLETED",
                    "Shared goal completed! " + updateMessage
                );
            } else if (progressPercentage >= 75) {
                notificationService.sendBuddyNotification(
                    userId,
                    goalId,
                    "GOAL_PROGRESS",
                    "Goal is 75% complete! " + updateMessage
                );
            }
        } catch (Exception e) {
            log.error("Failed to update goal progress", e);
            throw e;
        }
    }

    /**
     * End a buddy partnership.
     */
    public void endPartnership(String partnershipId, String userId) {
        log.debug("Ending partnership: {} by user: {}", partnershipId, userId);

        try {
            buddyClient.endPartnership(partnershipId);
            log.info("Partnership ended successfully: {}", partnershipId);

            // Notify both partners about the end
            notificationService.sendBuddyNotification(
                userId,
                partnershipId,
                "PARTNERSHIP_ENDED",
                "Your buddy partnership has ended"
            );
        } catch (Exception e) {
            log.error("Failed to end partnership: {}", partnershipId, e);
            throw e;
        }
    }

    /**
     * Check buddy service health.
     */
    public boolean isBuddyServiceHealthy() {
        try {
            String health = buddyClient.healthCheck();
            return health != null && health.contains("UP");
        } catch (Exception e) {
            log.warn("Buddy service health check failed", e);
            return false;
        }
    }

    /**
     * Calculate compatibility score between two users.
     * This is a local calculation based on available data.
     */
    public double calculateCompatibilityScore(String userId1, String userId2) {
        log.debug("Calculating compatibility score between {} and {}", userId1, userId2);

        // This is a simplified compatibility calculation
        // In a real system, this would consider:
        // - Study schedules overlap
        // - Common interests
        // - Time zones
        // - Study goals
        // - Past partnership success rates

        double score = 0.5; // Base score

        // Add logic here based on user profiles
        // For now, returning a mock score

        log.info("Compatibility score between {} and {}: {}", userId1, userId2, score);
        return score;
    }
}