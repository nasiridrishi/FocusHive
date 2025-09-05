package com.focushive.buddy.entity;

import com.focushive.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BuddyPreferences entity business logic (no database required).
 * Tests the entity methods and validation without Spring context.
 */
class BuddyPreferencesUnitTest {

    @Test
    void workHoursShouldCalculateOverlapCorrectly() {
        // Given
        BuddyPreferences.WorkHours morning = new BuddyPreferences.WorkHours(8, 12);
        BuddyPreferences.WorkHours afternoon = new BuddyPreferences.WorkHours(13, 17);
        BuddyPreferences.WorkHours overlap = new BuddyPreferences.WorkHours(10, 15);

        // When & Then
        assertThat(morning.overlaps(afternoon)).isFalse();
        assertThat(morning.overlaps(overlap)).isTrue();
        assertThat(afternoon.overlaps(overlap)).isTrue();
        
        assertThat(morning.overlapHours(afternoon)).isEqualTo(0);
        assertThat(morning.overlapHours(overlap)).isEqualTo(2); // 10-12
        assertThat(afternoon.overlapHours(overlap)).isEqualTo(2); // 13-15
    }

    @Test
    void workHoursShouldHandleNullValues() {
        // Given
        BuddyPreferences.WorkHours workHours = new BuddyPreferences.WorkHours(9, 17);

        // When & Then
        assertThat(workHours.overlaps(null)).isFalse();
        assertThat(workHours.overlapHours(null)).isEqualTo(0);
    }

    @Test
    void workHoursShouldHandleEdgeCases() {
        // Given
        BuddyPreferences.WorkHours touching1 = new BuddyPreferences.WorkHours(8, 12);
        BuddyPreferences.WorkHours touching2 = new BuddyPreferences.WorkHours(12, 16);
        BuddyPreferences.WorkHours identical = new BuddyPreferences.WorkHours(9, 17);
        BuddyPreferences.WorkHours same = new BuddyPreferences.WorkHours(9, 17);

        // When & Then
        assertThat(touching1.overlaps(touching2)).isFalse(); // Touching at exactly 12:00
        assertThat(touching1.overlapHours(touching2)).isEqualTo(0);
        
        assertThat(identical.overlaps(same)).isTrue();
        assertThat(identical.overlapHours(same)).isEqualTo(8); // Full overlap
    }

    @Test
    void focusAreaOverlapShouldWorkCorrectly() {
        // Given
        User testUser = new User();
        testUser.setId("test-user");
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setPassword("password");

        BuddyPreferences preferences = new BuddyPreferences();
        preferences.setUser(testUser);
        preferences.setFocusAreas(new String[]{"coding", "reading", "writing"});

        // When & Then
        assertThat(preferences.hasFocusAreaOverlap(new String[]{"coding", "music"})).isTrue();
        assertThat(preferences.hasFocusAreaOverlap(new String[]{"music", "gaming"})).isFalse();
        assertThat(preferences.countFocusAreaOverlap(new String[]{"coding", "reading", "music"})).isEqualTo(2);
    }

    @Test
    void focusAreaOverlapShouldHandleNullAndEmptyArrays() {
        // Given
        BuddyPreferences preferences = new BuddyPreferences();
        preferences.setFocusAreas(new String[]{"coding", "reading"});

        BuddyPreferences emptyPreferences = new BuddyPreferences();
        emptyPreferences.setFocusAreas(new String[]{});

        BuddyPreferences nullPreferences = new BuddyPreferences();
        nullPreferences.setFocusAreas(null);

        // When & Then
        assertThat(preferences.hasFocusAreaOverlap(null)).isFalse();
        assertThat(preferences.countFocusAreaOverlap(null)).isEqualTo(0);
        
        assertThat(nullPreferences.hasFocusAreaOverlap(new String[]{"coding"})).isFalse();
        assertThat(nullPreferences.countFocusAreaOverlap(new String[]{"coding"})).isEqualTo(0);
        
        assertThat(emptyPreferences.hasFocusAreaOverlap(new String[]{"coding"})).isFalse();
        assertThat(emptyPreferences.countFocusAreaOverlap(new String[]{"coding"})).isEqualTo(0);
    }

    @Test
    void entityShouldSupportBasicFieldsAndRelationships() {
        // Given
        User user = new User();
        user.setId("user-123");
        user.setEmail("user@example.com");
        user.setUsername("user123");
        user.setDisplayName("Test User");
        user.setPassword("password");

        Map<String, BuddyPreferences.WorkHours> workHours = Map.of(
                "MONDAY", new BuddyPreferences.WorkHours(9, 17),
                "TUESDAY", new BuddyPreferences.WorkHours(10, 18)
        );

        // When
        BuddyPreferences preferences = BuddyPreferences.builder()
                .user(user)
                .preferredTimezone("America/New_York")
                .preferredWorkHours(workHours)
                .focusAreas(new String[]{"coding", "reading"})
                .communicationStyle(BuddyPreferences.CommunicationStyle.MODERATE)
                .matchingEnabled(true)
                .build();

        // Then
        assertThat(preferences.getUser()).isEqualTo(user);
        assertThat(preferences.getPreferredTimezone()).isEqualTo("America/New_York");
        assertThat(preferences.getPreferredWorkHours()).hasSize(2);
        assertThat(preferences.getPreferredWorkHours().get("MONDAY")).isEqualTo(new BuddyPreferences.WorkHours(9, 17));
        assertThat(preferences.getFocusAreas()).containsExactly("coding", "reading");
        assertThat(preferences.getCommunicationStyle()).isEqualTo(BuddyPreferences.CommunicationStyle.MODERATE);
        assertThat(preferences.getMatchingEnabled()).isTrue();
    }

    @Test
    void workHoursShouldSupportEqualsAndHashCode() {
        // Given
        BuddyPreferences.WorkHours workHours1 = new BuddyPreferences.WorkHours(9, 17);
        BuddyPreferences.WorkHours workHours2 = new BuddyPreferences.WorkHours(9, 17);
        BuddyPreferences.WorkHours workHours3 = new BuddyPreferences.WorkHours(8, 16);

        // When & Then
        assertThat(workHours1).isEqualTo(workHours2);
        assertThat(workHours1).isNotEqualTo(workHours3);
        assertThat(workHours1.hashCode()).isEqualTo(workHours2.hashCode());
    }
}