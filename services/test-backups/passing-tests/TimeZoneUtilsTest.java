package com.focushive.buddy.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TimeZoneUtils class.
 *
 * Following TDD approach:
 * 1. RED: These tests will initially FAIL because TimeZoneUtils class doesn't exist
 * 2. GREEN: Implement TimeZoneUtils class to make tests pass
 * 3. REFACTOR: Improve implementation while keeping tests green
 */
@DisplayName("TimeZone Utils")
class TimeZoneUtilsTest {

    @Test
    @DisplayName("convertToUserTimezone should convert UTC to user timezone")
    void testConvertToUserTimezone() {
        LocalDateTime utcTime = LocalDateTime.of(2025, 9, 14, 12, 0, 0);
        String userTimezone = "America/New_York";

        ZonedDateTime converted = TimeZoneUtils.convertToUserTimezone(utcTime, userTimezone);

        assertThat(converted).isNotNull();
        assertThat(converted.getZone()).isEqualTo(ZoneId.of(userTimezone));

        // New York is typically 4 or 5 hours behind UTC depending on DST
        // The exact time depends on the date, but it should be earlier
        assertThat(converted.getHour()).isLessThan(utcTime.getHour());
    }

    @Test
    @DisplayName("convertToUserTimezone should handle same timezone conversion")
    void testConvertToUserTimezoneSame() {
        LocalDateTime utcTime = LocalDateTime.of(2025, 9, 14, 12, 0, 0);
        String userTimezone = "UTC";

        ZonedDateTime converted = TimeZoneUtils.convertToUserTimezone(utcTime, userTimezone);

        assertThat(converted).isNotNull();
        assertThat(converted.getZone()).isEqualTo(ZoneId.of(userTimezone));
        assertThat(converted.getHour()).isEqualTo(utcTime.getHour());
        assertThat(converted.getMinute()).isEqualTo(utcTime.getMinute());
    }

    @Test
    @DisplayName("convertToUserTimezone should throw exception for invalid timezone")
    void testConvertToUserTimezoneInvalid() {
        LocalDateTime utcTime = LocalDateTime.of(2025, 9, 14, 12, 0, 0);
        String invalidTimezone = "Invalid/Timezone";

        assertThatThrownBy(() -> TimeZoneUtils.convertToUserTimezone(utcTime, invalidTimezone))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid timezone");
    }

    @Test
    @DisplayName("convertToUserTimezone should throw exception for null parameters")
    void testConvertToUserTimezoneNull() {
        LocalDateTime utcTime = LocalDateTime.of(2025, 9, 14, 12, 0, 0);

        assertThatThrownBy(() -> TimeZoneUtils.convertToUserTimezone(null, "UTC"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("UTC time cannot be null");

        assertThatThrownBy(() -> TimeZoneUtils.convertToUserTimezone(utcTime, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Timezone cannot be null");
    }

    @Test
    @DisplayName("calculateWorkHourOverlap should find overlap between work hours")
    void testCalculateWorkHourOverlapWithOverlap() {
        // User 1: 9 AM - 5 PM EST (14:00 - 22:00 UTC)
        LocalTime user1Start = LocalTime.of(9, 0);
        LocalTime user1End = LocalTime.of(17, 0);
        String user1Timezone = "America/New_York";

        // User 2: 10 AM - 6 PM PST (18:00 - 02:00 UTC next day)
        LocalTime user2Start = LocalTime.of(10, 0);
        LocalTime user2End = LocalTime.of(18, 0);
        String user2Timezone = "America/Los_Angeles";

        double overlapHours = TimeZoneUtils.calculateWorkHourOverlap(
            user1Start, user1End, user1Timezone,
            user2Start, user2End, user2Timezone
        );

        // There should be some overlap between EST 9-5 and PST 10-6
        assertThat(overlapHours).isGreaterThan(0.0);
        assertThat(overlapHours).isLessThanOrEqualTo(8.0);
    }

    @Test
    @DisplayName("calculateWorkHourOverlap should return zero for no overlap")
    void testCalculateWorkHourOverlapNoOverlap() {
        // User 1: 9 AM - 5 PM UTC
        LocalTime user1Start = LocalTime.of(9, 0);
        LocalTime user1End = LocalTime.of(17, 0);
        String user1Timezone = "UTC";

        // User 2: 10 PM - 6 AM UTC (overnight shift)
        LocalTime user2Start = LocalTime.of(22, 0);
        LocalTime user2End = LocalTime.of(6, 0);
        String user2Timezone = "UTC";

        double overlapHours = TimeZoneUtils.calculateWorkHourOverlap(
            user1Start, user1End, user1Timezone,
            user2Start, user2End, user2Timezone
        );

        assertThat(overlapHours).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateWorkHourOverlap should handle same timezone same hours")
    void testCalculateWorkHourOverlapSame() {
        // Both users: 9 AM - 5 PM UTC
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        String timezone = "UTC";

        double overlapHours = TimeZoneUtils.calculateWorkHourOverlap(
            startTime, endTime, timezone,
            startTime, endTime, timezone
        );

        assertThat(overlapHours).isEqualTo(8.0); // Full 8-hour overlap
    }

    @Test
    @DisplayName("calculateWorkHourOverlap should throw exception for invalid parameters")
    void testCalculateWorkHourOverlapInvalid() {
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        String timezone = "UTC";

        assertThatThrownBy(() -> TimeZoneUtils.calculateWorkHourOverlap(
            null, endTime, timezone, startTime, endTime, timezone))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeZoneUtils.calculateWorkHourOverlap(
            startTime, endTime, "Invalid/Zone", startTime, endTime, timezone))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getNextReminderTime should calculate next reminder correctly")
    void testGetNextReminderTime() {
        LocalDateTime baseTime = LocalDateTime.of(2025, 9, 14, 10, 0, 0);
        String userTimezone = "UTC";
        LocalTime reminderTime = LocalTime.of(20, 0); // 8 PM

        ZonedDateTime nextReminder = TimeZoneUtils.getNextReminderTime(
            baseTime, userTimezone, reminderTime
        );

        assertThat(nextReminder).isNotNull();
        assertThat(nextReminder.getZone()).isEqualTo(ZoneId.of(userTimezone));
        assertThat(nextReminder.getHour()).isEqualTo(20);
        assertThat(nextReminder.getMinute()).isEqualTo(0);

        // Should be today if base time is before reminder time
        assertThat(nextReminder.toLocalDate()).isEqualTo(baseTime.toLocalDate());
    }

    @Test
    @DisplayName("getNextReminderTime should return next day if reminder time passed")
    void testGetNextReminderTimeNextDay() {
        LocalDateTime baseTime = LocalDateTime.of(2025, 9, 14, 22, 0, 0); // 10 PM
        String userTimezone = "UTC";
        LocalTime reminderTime = LocalTime.of(20, 0); // 8 PM (already passed)

        ZonedDateTime nextReminder = TimeZoneUtils.getNextReminderTime(
            baseTime, userTimezone, reminderTime
        );

        assertThat(nextReminder).isNotNull();
        assertThat(nextReminder.getHour()).isEqualTo(20);

        // Should be tomorrow since reminder time already passed today
        assertThat(nextReminder.toLocalDate()).isEqualTo(baseTime.toLocalDate().plusDays(1));
    }

    @Test
    @DisplayName("getNextReminderTime should handle timezone conversion")
    void testGetNextReminderTimeWithTimezone() {
        LocalDateTime baseTime = LocalDateTime.of(2025, 9, 14, 15, 0, 0); // 3 PM UTC
        String userTimezone = "America/New_York"; // EST/EDT
        LocalTime reminderTime = LocalTime.of(20, 0); // 8 PM user local time

        ZonedDateTime nextReminder = TimeZoneUtils.getNextReminderTime(
            baseTime, userTimezone, reminderTime
        );

        assertThat(nextReminder).isNotNull();
        assertThat(nextReminder.getZone()).isEqualTo(ZoneId.of(userTimezone));
        assertThat(nextReminder.getHour()).isEqualTo(20); // 8 PM in user timezone
    }

    @Test
    @DisplayName("getNextReminderTime should throw exception for invalid parameters")
    void testGetNextReminderTimeInvalid() {
        LocalDateTime baseTime = LocalDateTime.of(2025, 9, 14, 10, 0, 0);
        String timezone = "UTC";
        LocalTime reminderTime = LocalTime.of(20, 0);

        assertThatThrownBy(() -> TimeZoneUtils.getNextReminderTime(null, timezone, reminderTime))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeZoneUtils.getNextReminderTime(baseTime, null, reminderTime))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeZoneUtils.getNextReminderTime(baseTime, timezone, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("calculateStreak should calculate consecutive days correctly")
    void testCalculateStreak() {
        List<LocalDateTime> checkinDates = List.of(
            LocalDateTime.of(2025, 9, 14, 10, 0, 0), // Today
            LocalDateTime.of(2025, 9, 13, 15, 0, 0), // Yesterday
            LocalDateTime.of(2025, 9, 12, 20, 0, 0), // Day before
            LocalDateTime.of(2025, 9, 10, 9, 0, 0)   // 4 days ago (gap)
        );

        LocalDateTime referenceDate = LocalDateTime.of(2025, 9, 14, 23, 59, 59);

        int streak = TimeZoneUtils.calculateStreak(checkinDates, referenceDate);

        assertThat(streak).isEqualTo(3); // 3 consecutive days from today
    }

    @Test
    @DisplayName("calculateStreak should return zero for no checkins")
    void testCalculateStreakEmpty() {
        List<LocalDateTime> checkinDates = List.of();
        LocalDateTime referenceDate = LocalDateTime.of(2025, 9, 14, 12, 0, 0);

        int streak = TimeZoneUtils.calculateStreak(checkinDates, referenceDate);

        assertThat(streak).isEqualTo(0);
    }

    @Test
    @DisplayName("calculateStreak should return zero if no recent checkins")
    void testCalculateStreakNoRecent() {
        List<LocalDateTime> checkinDates = List.of(
            LocalDateTime.of(2025, 9, 10, 10, 0, 0), // 4 days ago
            LocalDateTime.of(2025, 9, 9, 15, 0, 0),  // 5 days ago
            LocalDateTime.of(2025, 9, 8, 20, 0, 0)   // 6 days ago
        );

        LocalDateTime referenceDate = LocalDateTime.of(2025, 9, 14, 12, 0, 0);

        int streak = TimeZoneUtils.calculateStreak(checkinDates, referenceDate);

        assertThat(streak).isEqualTo(0); // No checkins within last 2 days
    }

    @Test
    @DisplayName("calculateStreak should handle single checkin")
    void testCalculateStreakSingle() {
        List<LocalDateTime> checkinDates = List.of(
            LocalDateTime.of(2025, 9, 14, 10, 0, 0) // Today only
        );

        LocalDateTime referenceDate = LocalDateTime.of(2025, 9, 14, 15, 0, 0);

        int streak = TimeZoneUtils.calculateStreak(checkinDates, referenceDate);

        assertThat(streak).isEqualTo(1); // Single day streak
    }

    @Test
    @DisplayName("calculateStreak should throw exception for null parameters")
    void testCalculateStreakNull() {
        List<LocalDateTime> checkinDates = List.of(LocalDateTime.now());
        LocalDateTime referenceDate = LocalDateTime.now();

        assertThatThrownBy(() -> TimeZoneUtils.calculateStreak(null, referenceDate))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeZoneUtils.calculateStreak(checkinDates, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isWithinWorkHours should check if time is within work hours")
    void testIsWithinWorkHours() {
        LocalTime workStart = LocalTime.of(9, 0);   // 9 AM
        LocalTime workEnd = LocalTime.of(17, 0);    // 5 PM

        // Within work hours
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(10, 0), workStart, workEnd))
            .isTrue();
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(16, 59), workStart, workEnd))
            .isTrue();
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(9, 0), workStart, workEnd))
            .isTrue(); // Boundary
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(17, 0), workStart, workEnd))
            .isTrue(); // Boundary

        // Outside work hours
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(8, 59), workStart, workEnd))
            .isFalse();
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(17, 1), workStart, workEnd))
            .isFalse();
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(23, 0), workStart, workEnd))
            .isFalse();
    }

    @Test
    @DisplayName("isWithinWorkHours should handle overnight work hours")
    void testIsWithinWorkHoursOvernight() {
        LocalTime workStart = LocalTime.of(22, 0);  // 10 PM
        LocalTime workEnd = LocalTime.of(6, 0);     // 6 AM next day

        // Within overnight work hours
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(23, 0), workStart, workEnd))
            .isTrue();
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(3, 0), workStart, workEnd))
            .isTrue();
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(5, 59), workStart, workEnd))
            .isTrue();

        // Outside overnight work hours
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(12, 0), workStart, workEnd))
            .isFalse();
        assertThat(TimeZoneUtils.isWithinWorkHours(LocalTime.of(18, 0), workStart, workEnd))
            .isFalse();
    }

    @Test
    @DisplayName("isWithinWorkHours should throw exception for null parameters")
    void testIsWithinWorkHoursNull() {
        LocalTime workStart = LocalTime.of(9, 0);
        LocalTime workEnd = LocalTime.of(17, 0);
        LocalTime checkTime = LocalTime.of(12, 0);

        assertThatThrownBy(() -> TimeZoneUtils.isWithinWorkHours(null, workStart, workEnd))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeZoneUtils.isWithinWorkHours(checkTime, null, workEnd))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeZoneUtils.isWithinWorkHours(checkTime, workStart, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getDaysBetween should calculate days between dates correctly")
    void testGetDaysBetween() {
        LocalDateTime start = LocalDateTime.of(2025, 9, 10, 12, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 14, 15, 30, 0);

        long days = TimeZoneUtils.getDaysBetween(start, end);

        assertThat(days).isEqualTo(4); // 4 days between Sept 10 and Sept 14
    }

    @Test
    @DisplayName("getDaysBetween should return zero for same day")
    void testGetDaysBetweenSameDay() {
        LocalDateTime start = LocalDateTime.of(2025, 9, 14, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 14, 18, 0, 0);

        long days = TimeZoneUtils.getDaysBetween(start, end);

        assertThat(days).isEqualTo(0);
    }

    @Test
    @DisplayName("getDaysBetween should handle negative duration")
    void testGetDaysBetweenNegative() {
        LocalDateTime start = LocalDateTime.of(2025, 9, 14, 12, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 10, 12, 0, 0);

        long days = TimeZoneUtils.getDaysBetween(start, end);

        assertThat(days).isEqualTo(-4); // Negative when end is before start
    }

    @Test
    @DisplayName("getDaysBetween should throw exception for null parameters")
    void testGetDaysBetweenNull() {
        LocalDateTime dateTime = LocalDateTime.now();

        assertThatThrownBy(() -> TimeZoneUtils.getDaysBetween(null, dateTime))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> TimeZoneUtils.getDaysBetween(dateTime, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}