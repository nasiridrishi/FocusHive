package com.focushive.buddy.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for buddy service constants and enums.
 *
 * Following TDD approach:
 * 1. RED: These tests will initially FAIL because constant classes don't exist
 * 2. GREEN: Implement constant classes to make tests pass
 * 3. REFACTOR: Improve implementation while keeping tests green
 */
@DisplayName("Buddy Constants and Enums")
class BuddyConstantsTest {

    @Test
    @DisplayName("BuddyConstants should have correct default values")
    void testBuddyConstants() {
        assertThat(BuddyConstants.DEFAULT_PARTNERSHIP_DURATION_DAYS).isEqualTo(30);
        assertThat(BuddyConstants.MIN_PARTNERSHIP_DURATION_DAYS).isEqualTo(7);
        assertThat(BuddyConstants.MAX_PARTNERSHIP_DURATION_DAYS).isEqualTo(365);
        assertThat(BuddyConstants.MAX_PARTNERSHIPS_PER_USER).isEqualTo(3);
        assertThat(BuddyConstants.COMPATIBILITY_THRESHOLD).isEqualTo(0.6);
        assertThat(BuddyConstants.MIN_CHECKIN_RATE_GOOD_STANDING).isEqualTo(0.7);
        assertThat(BuddyConstants.STREAK_BREAK_THRESHOLD_DAYS).isEqualTo(2);
        assertThat(BuddyConstants.DEFAULT_CHECKIN_REMINDER_HOUR).isEqualTo(20); // 8 PM
        assertThat(BuddyConstants.CHECKIN_GRACE_PERIOD_HOURS).isEqualTo(24);
        assertThat(BuddyConstants.MAX_MATCH_SUGGESTIONS).isEqualTo(10);
    }

    @Test
    @DisplayName("BuddyConstants should have correct string constants")
    void testBuddyStringConstants() {
        assertThat(BuddyConstants.DEFAULT_TIMEZONE).isEqualTo("UTC");
        assertThat(BuddyConstants.PARTNERSHIP_REQUEST_EXPIRES_HOURS).isEqualTo(72); // 3 days
    }

    @Test
    @DisplayName("PartnershipStatus enum should have all required values")
    void testPartnershipStatus() {
        // Test all expected enum values exist
        assertThat(PartnershipStatus.PENDING).isNotNull();
        assertThat(PartnershipStatus.ACTIVE).isNotNull();
        assertThat(PartnershipStatus.PAUSED).isNotNull();
        assertThat(PartnershipStatus.ENDED).isNotNull();

        // Test enum properties
        assertThat(PartnershipStatus.PENDING.getDisplayName()).isEqualTo("Pending");
        assertThat(PartnershipStatus.ACTIVE.getDisplayName()).isEqualTo("Active");
        assertThat(PartnershipStatus.PAUSED.getDisplayName()).isEqualTo("Paused");
        assertThat(PartnershipStatus.ENDED.getDisplayName()).isEqualTo("Ended");

        // Test descriptions
        assertThat(PartnershipStatus.PENDING.getDescription()).contains("pending acceptance");
        assertThat(PartnershipStatus.ACTIVE.getDescription()).contains("actively collaborating");
        assertThat(PartnershipStatus.PAUSED.getDescription()).contains("temporarily paused");
        assertThat(PartnershipStatus.ENDED.getDescription()).contains("permanently ended");
    }

    @Test
    @DisplayName("PartnershipStatus should support valid transitions")
    void testPartnershipStatusTransitions() {
        // From PENDING
        assertThat(PartnershipStatus.PENDING.canTransitionTo(PartnershipStatus.ACTIVE)).isTrue();
        assertThat(PartnershipStatus.PENDING.canTransitionTo(PartnershipStatus.ENDED)).isTrue();
        assertThat(PartnershipStatus.PENDING.canTransitionTo(PartnershipStatus.PENDING)).isFalse(); // Same state

        // From ACTIVE
        assertThat(PartnershipStatus.ACTIVE.canTransitionTo(PartnershipStatus.PAUSED)).isTrue();
        assertThat(PartnershipStatus.ACTIVE.canTransitionTo(PartnershipStatus.ENDED)).isTrue();
        assertThat(PartnershipStatus.ACTIVE.canTransitionTo(PartnershipStatus.ACTIVE)).isFalse(); // Same state

        // From PAUSED
        assertThat(PartnershipStatus.PAUSED.canTransitionTo(PartnershipStatus.ACTIVE)).isTrue();
        assertThat(PartnershipStatus.PAUSED.canTransitionTo(PartnershipStatus.ENDED)).isTrue();

        // From ENDED (no transitions allowed)
        assertThat(PartnershipStatus.ENDED.canTransitionTo(PartnershipStatus.ACTIVE)).isFalse();
        assertThat(PartnershipStatus.ENDED.canTransitionTo(PartnershipStatus.PAUSED)).isFalse();
        assertThat(PartnershipStatus.ENDED.canTransitionTo(PartnershipStatus.PENDING)).isFalse();
    }

    @Test
    @DisplayName("CheckInType enum should have all required values")
    void testCheckInType() {
        // Test all expected enum values exist
        assertThat(CheckInType.DAILY).isNotNull();
        assertThat(CheckInType.WEEKLY).isNotNull();
        assertThat(CheckInType.MILESTONE).isNotNull();
        assertThat(CheckInType.GOAL_UPDATE).isNotNull();
        assertThat(CheckInType.ENCOURAGEMENT).isNotNull();

        // Test display names
        assertThat(CheckInType.DAILY.getDisplayName()).isEqualTo("Daily Check-in");
        assertThat(CheckInType.WEEKLY.getDisplayName()).isEqualTo("Weekly Check-in");
        assertThat(CheckInType.MILESTONE.getDisplayName()).isEqualTo("Milestone Update");
        assertThat(CheckInType.GOAL_UPDATE.getDisplayName()).isEqualTo("Goal Progress Update");
        assertThat(CheckInType.ENCOURAGEMENT.getDisplayName()).isEqualTo("Encouragement Message");

        // Test frequencies
        assertThat(CheckInType.DAILY.getExpectedFrequencyHours()).isEqualTo(24);
        assertThat(CheckInType.WEEKLY.getExpectedFrequencyHours()).isEqualTo(168); // 7 * 24
        assertThat(CheckInType.MILESTONE.getExpectedFrequencyHours()).isEqualTo(0); // Variable
        assertThat(CheckInType.GOAL_UPDATE.getExpectedFrequencyHours()).isEqualTo(72); // 3 days
        assertThat(CheckInType.ENCOURAGEMENT.getExpectedFrequencyHours()).isEqualTo(0); // Ad-hoc
    }

    @Test
    @DisplayName("InteractionType enum should have all required values")
    void testInteractionType() {
        // Test all expected enum values exist
        assertThat(InteractionType.MESSAGE).isNotNull();
        assertThat(InteractionType.NUDGE).isNotNull();
        assertThat(InteractionType.CELEBRATION).isNotNull();
        assertThat(InteractionType.CHECK_IN_REMINDER).isNotNull();
        assertThat(InteractionType.GOAL_REMINDER).isNotNull();
        assertThat(InteractionType.MEETING_INVITE).isNotNull();

        // Test categories
        assertThat(InteractionType.MESSAGE.getCategory()).isEqualTo("communication");
        assertThat(InteractionType.NUDGE.getCategory()).isEqualTo("motivation");
        assertThat(InteractionType.CELEBRATION.getCategory()).isEqualTo("recognition");
        assertThat(InteractionType.CHECK_IN_REMINDER.getCategory()).isEqualTo("reminder");
        assertThat(InteractionType.GOAL_REMINDER.getCategory()).isEqualTo("reminder");
        assertThat(InteractionType.MEETING_INVITE.getCategory()).isEqualTo("scheduling");

        // Test notification requirements
        assertThat(InteractionType.MESSAGE.requiresNotification()).isTrue();
        assertThat(InteractionType.NUDGE.requiresNotification()).isTrue();
        assertThat(InteractionType.CELEBRATION.requiresNotification()).isTrue();
        assertThat(InteractionType.CHECK_IN_REMINDER.requiresNotification()).isTrue();
        assertThat(InteractionType.GOAL_REMINDER.requiresNotification()).isTrue();
        assertThat(InteractionType.MEETING_INVITE.requiresNotification()).isTrue();
    }

    @Test
    @DisplayName("MoodType enum should have all required values")
    void testMoodType() {
        // Test all expected enum values exist
        assertThat(MoodType.MOTIVATED).isNotNull();
        assertThat(MoodType.FOCUSED).isNotNull();
        assertThat(MoodType.STRESSED).isNotNull();
        assertThat(MoodType.TIRED).isNotNull();
        assertThat(MoodType.EXCITED).isNotNull();
        assertThat(MoodType.NEUTRAL).isNotNull();
        assertThat(MoodType.FRUSTRATED).isNotNull();
        assertThat(MoodType.ACCOMPLISHED).isNotNull();

        // Test display names
        assertThat(MoodType.MOTIVATED.getDisplayName()).isEqualTo("Motivated");
        assertThat(MoodType.FOCUSED.getDisplayName()).isEqualTo("Focused");
        assertThat(MoodType.STRESSED.getDisplayName()).isEqualTo("Stressed");
        assertThat(MoodType.TIRED.getDisplayName()).isEqualTo("Tired");
        assertThat(MoodType.EXCITED.getDisplayName()).isEqualTo("Excited");
        assertThat(MoodType.NEUTRAL.getDisplayName()).isEqualTo("Neutral");
        assertThat(MoodType.FRUSTRATED.getDisplayName()).isEqualTo("Frustrated");
        assertThat(MoodType.ACCOMPLISHED.getDisplayName()).isEqualTo("Accomplished");

        // Test emotional scores (1-10, where 10 is most positive)
        assertThat(MoodType.MOTIVATED.getEmotionalScore()).isEqualTo(9);
        assertThat(MoodType.FOCUSED.getEmotionalScore()).isEqualTo(8);
        assertThat(MoodType.STRESSED.getEmotionalScore()).isEqualTo(3);
        assertThat(MoodType.TIRED.getEmotionalScore()).isEqualTo(4);
        assertThat(MoodType.EXCITED.getEmotionalScore()).isEqualTo(10);
        assertThat(MoodType.NEUTRAL.getEmotionalScore()).isEqualTo(5);
        assertThat(MoodType.FRUSTRATED.getEmotionalScore()).isEqualTo(2);
        assertThat(MoodType.ACCOMPLISHED.getEmotionalScore()).isEqualTo(9);

        // Test emoji representations
        assertThat(MoodType.MOTIVATED.getEmoji()).isEqualTo("💪");
        assertThat(MoodType.FOCUSED.getEmoji()).isEqualTo("🎯");
        assertThat(MoodType.STRESSED.getEmoji()).isEqualTo("😰");
        assertThat(MoodType.TIRED.getEmoji()).isEqualTo("😴");
        assertThat(MoodType.EXCITED.getEmoji()).isEqualTo("🎉");
        assertThat(MoodType.NEUTRAL.getEmoji()).isEqualTo("😐");
        assertThat(MoodType.FRUSTRATED.getEmoji()).isEqualTo("😤");
        assertThat(MoodType.ACCOMPLISHED.getEmoji()).isEqualTo("🏆");
    }

    @Test
    @DisplayName("MoodType should categorize moods correctly")
    void testMoodTypeCategories() {
        // Test positive moods (score >= 7)
        assertThat(MoodType.MOTIVATED.isPositive()).isTrue();
        assertThat(MoodType.FOCUSED.isPositive()).isTrue();
        assertThat(MoodType.EXCITED.isPositive()).isTrue();
        assertThat(MoodType.ACCOMPLISHED.isPositive()).isTrue();

        // Test negative moods (score <= 4)
        assertThat(MoodType.STRESSED.isNegative()).isTrue();
        assertThat(MoodType.TIRED.isNegative()).isTrue();
        assertThat(MoodType.FRUSTRATED.isNegative()).isTrue();

        // Test neutral mood
        assertThat(MoodType.NEUTRAL.isNeutral()).isTrue();
        assertThat(MoodType.NEUTRAL.isPositive()).isFalse();
        assertThat(MoodType.NEUTRAL.isNegative()).isFalse();
    }

    @Test
    @DisplayName("RequestStatus enum should have all required values")
    void testRequestStatus() {
        // Test all expected enum values exist
        assertThat(RequestStatus.PENDING).isNotNull();
        assertThat(RequestStatus.ACCEPTED).isNotNull();
        assertThat(RequestStatus.DECLINED).isNotNull();
        assertThat(RequestStatus.EXPIRED).isNotNull();
        assertThat(RequestStatus.WITHDRAWN).isNotNull();

        // Test final states
        assertThat(RequestStatus.PENDING.isFinal()).isFalse();
        assertThat(RequestStatus.ACCEPTED.isFinal()).isTrue();
        assertThat(RequestStatus.DECLINED.isFinal()).isTrue();
        assertThat(RequestStatus.EXPIRED.isFinal()).isTrue();
        assertThat(RequestStatus.WITHDRAWN.isFinal()).isTrue();

        // Test can respond
        assertThat(RequestStatus.PENDING.canRespond()).isTrue();
        assertThat(RequestStatus.ACCEPTED.canRespond()).isFalse();
        assertThat(RequestStatus.DECLINED.canRespond()).isFalse();
        assertThat(RequestStatus.EXPIRED.canRespond()).isFalse();
        assertThat(RequestStatus.WITHDRAWN.canRespond()).isFalse();
    }

    @Test
    @DisplayName("GoalStatus enum should have all required values")
    void testGoalStatus() {
        // Test all expected enum values exist
        assertThat(GoalStatus.NOT_STARTED).isNotNull();
        assertThat(GoalStatus.IN_PROGRESS).isNotNull();
        assertThat(GoalStatus.COMPLETED).isNotNull();
        assertThat(GoalStatus.PAUSED).isNotNull();
        assertThat(GoalStatus.CANCELLED).isNotNull();
        assertThat(GoalStatus.OVERDUE).isNotNull();

        // Test display names
        assertThat(GoalStatus.NOT_STARTED.getDisplayName()).isEqualTo("Not Started");
        assertThat(GoalStatus.IN_PROGRESS.getDisplayName()).isEqualTo("In Progress");
        assertThat(GoalStatus.COMPLETED.getDisplayName()).isEqualTo("Completed");
        assertThat(GoalStatus.PAUSED.getDisplayName()).isEqualTo("Paused");
        assertThat(GoalStatus.CANCELLED.getDisplayName()).isEqualTo("Cancelled");
        assertThat(GoalStatus.OVERDUE.getDisplayName()).isEqualTo("Overdue");

        // Test completion states
        assertThat(GoalStatus.NOT_STARTED.isCompleted()).isFalse();
        assertThat(GoalStatus.IN_PROGRESS.isCompleted()).isFalse();
        assertThat(GoalStatus.COMPLETED.isCompleted()).isTrue();
        assertThat(GoalStatus.PAUSED.isCompleted()).isFalse();
        assertThat(GoalStatus.CANCELLED.isCompleted()).isFalse();
        assertThat(GoalStatus.OVERDUE.isCompleted()).isFalse();

        // Test active states
        assertThat(GoalStatus.NOT_STARTED.isActive()).isFalse();
        assertThat(GoalStatus.IN_PROGRESS.isActive()).isTrue();
        assertThat(GoalStatus.COMPLETED.isActive()).isFalse();
        assertThat(GoalStatus.PAUSED.isActive()).isFalse();
        assertThat(GoalStatus.CANCELLED.isActive()).isFalse();
        assertThat(GoalStatus.OVERDUE.isActive()).isTrue();
    }

    @Test
    @DisplayName("Enums should be serializable for JSON")
    void testEnumsJsonSerialization() {
        // Test that enums can be converted to/from strings (for JSON serialization)

        // Test PartnershipStatus
        assertThat(PartnershipStatus.valueOf("ACTIVE")).isEqualTo(PartnershipStatus.ACTIVE);
        assertThat(PartnershipStatus.ACTIVE.name()).isEqualTo("ACTIVE");

        // Test CheckInType
        assertThat(CheckInType.valueOf("DAILY")).isEqualTo(CheckInType.DAILY);
        assertThat(CheckInType.DAILY.name()).isEqualTo("DAILY");

        // Test InteractionType
        assertThat(InteractionType.valueOf("MESSAGE")).isEqualTo(InteractionType.MESSAGE);
        assertThat(InteractionType.MESSAGE.name()).isEqualTo("MESSAGE");

        // Test MoodType
        assertThat(MoodType.valueOf("MOTIVATED")).isEqualTo(MoodType.MOTIVATED);
        assertThat(MoodType.MOTIVATED.name()).isEqualTo("MOTIVATED");

        // Test RequestStatus
        assertThat(RequestStatus.valueOf("PENDING")).isEqualTo(RequestStatus.PENDING);
        assertThat(RequestStatus.PENDING.name()).isEqualTo("PENDING");

        // Test GoalStatus
        assertThat(GoalStatus.valueOf("IN_PROGRESS")).isEqualTo(GoalStatus.IN_PROGRESS);
        assertThat(GoalStatus.IN_PROGRESS.name()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Enum values method should return all values")
    void testEnumValues() {
        // Test that values() method returns correct number of enum constants
        assertThat(PartnershipStatus.values()).hasSize(4);
        assertThat(CheckInType.values()).hasSize(5);
        assertThat(InteractionType.values()).hasSize(6);
        assertThat(MoodType.values()).hasSize(8);
        assertThat(RequestStatus.values()).hasSize(5);
        assertThat(GoalStatus.values()).hasSize(6);
    }
}