package com.focushive.buddy.listener;

import com.focushive.buddy.event.*;
import com.focushive.buddy.service.*;
import com.focushive.buddy.config.BuddySchedulingProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test Suite for BuddyEventListener
 *
 * Tests event handling for:
 * - Partnership lifecycle events
 * - Goal milestone achievements
 * - Check-in activities
 * - Streak management
 * - Match notifications
 *
 * Each test follows the TDD pattern:
 * 1. RED: Create failing test (assertThrows RuntimeException)
 * 2. GREEN: Implement listener method
 * 3. REFACTOR: Improve implementation
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class BuddyEventListenerTest {

    @Mock
    private BuddyPartnershipService partnershipService;

    @Mock
    private BuddyMatchingService matchingService;

    @Mock
    private BuddyGoalService goalService;

    @Mock
    private BuddyCheckinService checkinService;

    @Mock
    private BuddySchedulingProperties schedulingProperties;

    private BuddyEventListener eventListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventListener = new BuddyEventListener(
            partnershipService,
            matchingService,
            goalService,
            checkinService,
            schedulingProperties
        );
    }

    // =============================================================================
    // PARTNERSHIP LIFECYCLE EVENT TESTS
    // =============================================================================

    @Test
    void shouldHandlePartnershipCreatedEvent() {
        // Given: A new partnership is created
        UUID partnershipId = UUID.randomUUID();
        String requesterId = "user1";
        String recipientId = "user2";
        PartnershipCreatedEvent event = new PartnershipCreatedEvent(
            this, partnershipId, requesterId, recipientId, 30, "Test agreement"
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipCreated(event);
        });

        // Then: Should fail because method doesn't exist yet
    }

    @Test
    void shouldSendNotificationToRecipient() {
        // Given: Partnership creation event
        UUID partnershipId = UUID.randomUUID();
        PartnershipCreatedEvent event = new PartnershipCreatedEvent(
            this, partnershipId, "user1", "user2", 30, "Agreement"
        );

        // When: Event is handled
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipCreated(event);
        });

        // Then: Should send notification to recipient
    }

    @Test
    void shouldUpdateMatchingMetrics() {
        // Given: Partnership creation event
        UUID partnershipId = UUID.randomUUID();
        PartnershipCreatedEvent event = new PartnershipCreatedEvent(
            this, partnershipId, "user1", "user2", 30, "Agreement"
        );

        // When: Event is handled
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipCreated(event);
        });

        // Then: Should update matching algorithm metrics
    }

    @Test
    void shouldHandlePartnershipApprovedEvent() {
        // Given: Partnership approval event
        UUID partnershipId = UUID.randomUUID();
        PartnershipApprovedEvent event = new PartnershipApprovedEvent(
            this, partnershipId, "user2", "user1"
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipApproved(event);
        });

        // Then: Should fail because method doesn't exist yet
    }

    @Test
    void shouldInitiateOnboardingWorkflow() {
        // Given: Approved partnership
        UUID partnershipId = UUID.randomUUID();
        PartnershipApprovedEvent event = new PartnershipApprovedEvent(
            this, partnershipId, "user2", "user1"
        );

        // When: Approval is handled
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipApproved(event);
        });

        // Then: Should start onboarding workflow for both partners
    }

    @Test
    void shouldSetupGoalSynchronization() {
        // Given: Approved partnership
        UUID partnershipId = UUID.randomUUID();
        PartnershipApprovedEvent event = new PartnershipApprovedEvent(
            this, partnershipId, "user2", "user1"
        );

        // When: Partnership is approved
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipApproved(event);
        });

        // Then: Should set up goal synchronization between partners
    }

    @Test
    void shouldHandlePartnershipEndedEvent() {
        // Given: Partnership ended event
        UUID partnershipId = UUID.randomUUID();
        PartnershipEndedEvent event = new PartnershipEndedEvent(
            this, partnershipId, "user1", "user2", "Completed successfully", 30L, true
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipEnded(event);
        });

        // Then: Should fail because method doesn't exist yet
    }

    @Test
    void shouldCleanupSharedData() {
        // Given: Ended partnership
        UUID partnershipId = UUID.randomUUID();
        PartnershipEndedEvent event = new PartnershipEndedEvent(
            this, partnershipId, "user1", "user2", "Mutual dissolution", 45L, true
        );

        // When: Partnership ends
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipEnded(event);
        });

        // Then: Should clean up shared goals and data
    }

    @Test
    void shouldCalculateFinalAnalytics() {
        // Given: Successfully completed partnership
        UUID partnershipId = UUID.randomUUID();
        PartnershipEndedEvent event = new PartnershipEndedEvent(
            this, partnershipId, "user1", "user2", "Goal achieved", 60L, true
        );

        // When: Partnership ends successfully
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipEnded(event);
        });

        // Then: Should calculate final partnership analytics
    }

    @Test
    void shouldInitiateFeedbackCollection() {
        // Given: Ended partnership
        UUID partnershipId = UUID.randomUUID();
        PartnershipEndedEvent event = new PartnershipEndedEvent(
            this, partnershipId, "user1", "user2", "Time expired", 30L, false
        );

        // When: Partnership ends
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipEnded(event);
        });

        // Then: Should initiate feedback collection from both partners
    }

    // =============================================================================
    // GOAL MILESTONE EVENT TESTS
    // =============================================================================

    @Test
    void shouldHandleGoalMilestoneAchievedEvent() {
        // Given: Milestone achievement event
        UUID milestoneId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID partnershipId = UUID.randomUUID();
        GoalMilestoneAchievedEvent event = new GoalMilestoneAchievedEvent(
            this, milestoneId, goalId, userId, partnershipId, "First milestone", 25
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleGoalMilestoneAchieved(event);
        });

        // Then: Should fail because method doesn't exist yet
    }

    @Test
    void shouldCelebrateAchievement() {
        // Given: Milestone achievement
        UUID milestoneId = UUID.randomUUID();
        GoalMilestoneAchievedEvent event = new GoalMilestoneAchievedEvent(
            this, milestoneId, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "Major milestone", 50
        );

        // When: Milestone is achieved
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleGoalMilestoneAchieved(event);
        });

        // Then: Should trigger celebration notifications
    }

    @Test
    void shouldAwardBadges() {
        // Given: Significant milestone achievement
        UUID milestoneId = UUID.randomUUID();
        GoalMilestoneAchievedEvent event = new GoalMilestoneAchievedEvent(
            this, milestoneId, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "Final milestone", 100
        );

        // When: Milestone is achieved
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleGoalMilestoneAchieved(event);
        });

        // Then: Should award achievement badges
    }

    @Test
    void shouldNotifyPartner() {
        // Given: Milestone achievement in shared goal
        UUID milestoneId = UUID.randomUUID();
        GoalMilestoneAchievedEvent event = new GoalMilestoneAchievedEvent(
            this, milestoneId, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "Shared milestone", 75
        );

        // When: Partner achieves milestone
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleGoalMilestoneAchieved(event);
        });

        // Then: Should notify the partner
    }

    // =============================================================================
    // CHECK-IN EVENT TESTS
    // =============================================================================

    @Test
    void shouldHandleCheckinCreatedEvent() {
        // Given: New check-in event
        UUID checkinId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID partnershipId = UUID.randomUUID();
        CheckinCreatedEvent event = new CheckinCreatedEvent(
            this, checkinId, userId, partnershipId, "DAILY", 8, "GOOD", "Productive day"
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleCheckinCreated(event);
        });

        // Then: Should fail because method doesn't exist yet
    }

    @Test
    void shouldUpdateStreakCalculations() {
        // Given: Daily check-in event
        UUID checkinId = UUID.randomUUID();
        CheckinCreatedEvent event = new CheckinCreatedEvent(
            this, checkinId, UUID.randomUUID(), UUID.randomUUID(),
            "DAILY", 7, "NEUTRAL", "Regular day"
        );

        // When: Check-in is created
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleCheckinCreated(event);
        });

        // Then: Should update streak calculations
    }

    @Test
    void shouldUpdateAccountabilityScore() {
        // Given: Check-in with high productivity
        UUID checkinId = UUID.randomUUID();
        CheckinCreatedEvent event = new CheckinCreatedEvent(
            this, checkinId, UUID.randomUUID(), UUID.randomUUID(),
            "DAILY", 9, "EXCELLENT", "Amazing progress"
        );

        // When: High-quality check-in is created
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleCheckinCreated(event);
        });

        // Then: Should update accountability score positively
    }

    @Test
    void shouldNotifyPartnerOfCheckin() {
        // Given: Check-in in active partnership
        UUID checkinId = UUID.randomUUID();
        CheckinCreatedEvent event = new CheckinCreatedEvent(
            this, checkinId, UUID.randomUUID(), UUID.randomUUID(),
            "WEEKLY", 6, "GOOD", "Weekly review done"
        );

        // When: Partner checks in
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleCheckinCreated(event);
        });

        // Then: Should notify partner of the check-in
    }

    // =============================================================================
    // STREAK EVENT TESTS
    // =============================================================================

    @Test
    void shouldHandleStreakBrokenEvent() {
        // Given: Streak broken event
        UUID userId = UUID.randomUUID();
        UUID partnershipId = UUID.randomUUID();
        StreakBrokenEvent event = new StreakBrokenEvent(
            this, userId, partnershipId, 15, "daily", ZonedDateTime.now().minusDays(2)
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleStreakBroken(event);
        });

        // Then: Should fail because method doesn't exist yet
    }

    @Test
    void shouldSendEncouragementNotifications() {
        // Given: Long streak that was broken
        UUID userId = UUID.randomUUID();
        StreakBrokenEvent event = new StreakBrokenEvent(
            this, userId, UUID.randomUUID(), 21, "daily", ZonedDateTime.now().minusDays(1)
        );

        // When: Streak is broken
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleStreakBroken(event);
        });

        // Then: Should send encouragement notifications
    }

    @Test
    void shouldSuggestStreakRecovery() {
        // Given: Broken streak with recovery potential
        UUID userId = UUID.randomUUID();
        StreakBrokenEvent event = new StreakBrokenEvent(
            this, userId, UUID.randomUUID(), 7, "weekly", ZonedDateTime.now().minusDays(3)
        );

        // When: Streak is broken
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleStreakBroken(event);
        });

        // Then: Should suggest streak recovery strategies
    }

    @Test
    void shouldNotifyPartnerForSupport() {
        // Given: Broken streak in partnership
        UUID userId = UUID.randomUUID();
        UUID partnershipId = UUID.randomUUID();
        StreakBrokenEvent event = new StreakBrokenEvent(
            this, userId, partnershipId, 10, "daily", ZonedDateTime.now().minusDays(1)
        );

        // When: Partner's streak is broken
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleStreakBroken(event);
        });

        // Then: Should notify partner to provide support
    }

    // =============================================================================
    // MATCH EVENT TESTS
    // =============================================================================

    @Test
    void shouldHandleMatchFoundEvent() {
        // Given: Match found event
        String userId = "user1";
        List<String> matches = Arrays.asList("user2", "user3", "user4");
        List<Double> scores = Arrays.asList(0.85, 0.78, 0.72);
        MatchFoundEvent event = new MatchFoundEvent(
            this, userId, matches, scores, "interests+timezone"
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleMatchFound(event);
        });

        // Then: Should fail because method doesn't exist yet
    }

    @Test
    void shouldSendMatchRecommendations() {
        // Given: High-quality matches found
        String userId = "user1";
        List<String> matches = Arrays.asList("user2", "user3");
        List<Double> scores = Arrays.asList(0.92, 0.88);
        MatchFoundEvent event = new MatchFoundEvent(
            this, userId, matches, scores, "high_compatibility"
        );

        // When: Matches are found
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleMatchFound(event);
        });

        // Then: Should send match recommendation notifications
    }

    @Test
    void shouldUpdateMatchingAlgorithm() {
        // Given: Match results with feedback potential
        String userId = "user1";
        List<String> matches = Arrays.asList("user2");
        List<Double> scores = Arrays.asList(0.65);
        MatchFoundEvent event = new MatchFoundEvent(
            this, userId, matches, scores, "learning_algorithm"
        );

        // When: Matches are processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleMatchFound(event);
        });

        // Then: Should update matching algorithm with results
    }

    @Test
    void shouldTrackMatchingMetrics() {
        // Given: Match found event with analytics data
        String userId = "user1";
        List<String> matches = Arrays.asList("user2", "user3", "user4", "user5");
        List<Double> scores = Arrays.asList(0.90, 0.82, 0.75, 0.68);
        MatchFoundEvent event = new MatchFoundEvent(
            this, userId, matches, scores, "comprehensive_search"
        );

        // When: Multiple matches are found
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleMatchFound(event);
        });

        // Then: Should track matching success metrics
    }

    // =============================================================================
    // ERROR HANDLING AND RETRY TESTS
    // =============================================================================

    @Test
    void shouldHandleEventProcessingErrors() {
        // Given: Event that will cause processing error
        UUID partnershipId = UUID.randomUUID();
        PartnershipCreatedEvent event = new PartnershipCreatedEvent(
            this, partnershipId, "invalid_user", "another_invalid", 30, null
        );

        // When: Event processing encounters error
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipCreated(event);
        });

        // Then: Should handle errors gracefully
    }

    @Test
    void shouldRetryFailedEventProcessing() {
        // Given: Event that fails initially but might succeed on retry
        UUID checkinId = UUID.randomUUID();
        CheckinCreatedEvent event = new CheckinCreatedEvent(
            this, checkinId, UUID.randomUUID(), UUID.randomUUID(),
            "DAILY", 5, "NEUTRAL", "Retry test"
        );

        // When: Event processing is retried
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleCheckinCreated(event);
        });

        // Then: Should implement retry logic for transient failures
    }

    @Test
    void shouldLogEventProcessingMetrics() {
        // Given: Various event types for performance monitoring
        UUID partnershipId = UUID.randomUUID();
        PartnershipApprovedEvent event = new PartnershipApprovedEvent(
            this, partnershipId, "user1", "user2"
        );

        // When: Event is processed
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipApproved(event);
        });

        // Then: Should log processing times and success rates
    }

    // =============================================================================
    // INTEGRATION AND WORKFLOW TESTS
    // =============================================================================

    @Test
    void shouldCoordinateMultipleServiceCalls() {
        // Given: Complex event requiring multiple service interactions
        UUID milestoneId = UUID.randomUUID();
        GoalMilestoneAchievedEvent event = new GoalMilestoneAchievedEvent(
            this, milestoneId, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "Complex milestone", 80
        );

        // When: Event requires orchestrated service calls
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleGoalMilestoneAchieved(event);
        });

        // Then: Should coordinate calls across multiple services
    }

    @Test
    void shouldMaintainEventProcessingOrder() {
        // Given: Sequence of related events
        UUID partnershipId = UUID.randomUUID();
        PartnershipCreatedEvent createEvent = new PartnershipCreatedEvent(
            this, partnershipId, "user1", "user2", 30, "Agreement"
        );
        PartnershipApprovedEvent approveEvent = new PartnershipApprovedEvent(
            this, partnershipId, "user2", "user1"
        );

        // When: Events are processed in sequence
        assertThrows(RuntimeException.class, () -> {
            eventListener.handlePartnershipCreated(createEvent);
            eventListener.handlePartnershipApproved(approveEvent);
        });

        // Then: Should maintain proper processing order
    }

    @Test
    void shouldHandleAsynchronousEventProcessing() {
        // Given: Event that can be processed asynchronously
        String userId = "user1";
        List<String> matches = Arrays.asList("user2", "user3");
        List<Double> scores = Arrays.asList(0.85, 0.78);
        MatchFoundEvent event = new MatchFoundEvent(
            this, userId, matches, scores, "async_processing"
        );

        // When: Event is processed asynchronously
        assertThrows(RuntimeException.class, () -> {
            eventListener.handleMatchFound(event);
        });

        // Then: Should handle async processing correctly
    }
}