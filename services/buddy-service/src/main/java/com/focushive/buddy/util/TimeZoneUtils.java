package com.focushive.buddy.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Utility class for timezone and date/time operations in the buddy service.
 * All methods are static and the class cannot be instantiated.
 */
@UtilityClass
public class TimeZoneUtils {

    /**
     * Converts a UTC LocalDateTime to a ZonedDateTime in the specified user timezone.
     *
     * @param utcTime      the UTC time to convert
     * @param userTimezone the target timezone
     * @return the time converted to the user's timezone
     * @throws IllegalArgumentException if parameters are null or timezone is invalid
     */
    public static ZonedDateTime convertToUserTimezone(LocalDateTime utcTime, String userTimezone) {
        if (utcTime == null) {
            throw new IllegalArgumentException("UTC time cannot be null");
        }
        if (userTimezone == null) {
            throw new IllegalArgumentException("Timezone cannot be null or empty");
        }

        try {
            ZoneId utcZone = ZoneId.of("UTC");
            ZoneId userZone = ZoneId.of(userTimezone);

            // Create a ZonedDateTime in UTC first
            ZonedDateTime utcZoned = utcTime.atZone(utcZone);

            // Convert to user timezone
            return utcZoned.withZoneSameInstant(userZone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + userTimezone, e);
        }
    }

    /**
     * Calculates the overlap in work hours between two users in different timezones.
     *
     * @param user1Start     first user's work start time
     * @param user1End       first user's work end time
     * @param user1Timezone  first user's timezone
     * @param user2Start     second user's work start time
     * @param user2End       second user's work end time
     * @param user2Timezone  second user's timezone
     * @return the number of overlapping hours
     * @throws IllegalArgumentException if any parameter is null or timezone is invalid
     */
    public static double calculateWorkHourOverlap(
            LocalTime user1Start, LocalTime user1End, String user1Timezone,
            LocalTime user2Start, LocalTime user2End, String user2Timezone) {

        validateWorkHourParameters(user1Start, user1End, user1Timezone);
        validateWorkHourParameters(user2Start, user2End, user2Timezone);

        try {
            // Use today's date for calculations
            LocalDate today = LocalDate.now();

            // Convert both work schedules to UTC
            ZonedDateTime user1StartUtc = convertWorkTimeToUtc(today, user1Start, user1Timezone);
            ZonedDateTime user1EndUtc = convertWorkTimeToUtc(today, user1End, user1Timezone);

            ZonedDateTime user2StartUtc = convertWorkTimeToUtc(today, user2Start, user2Timezone);
            ZonedDateTime user2EndUtc = convertWorkTimeToUtc(today, user2End, user2Timezone);

            // Handle overnight shifts
            if (user1End.isBefore(user1Start)) {
                user1EndUtc = user1EndUtc.plusDays(1);
            }
            if (user2End.isBefore(user2Start)) {
                user2EndUtc = user2EndUtc.plusDays(1);
            }

            // Calculate overlap
            ZonedDateTime overlapStart = user1StartUtc.isAfter(user2StartUtc) ? user1StartUtc : user2StartUtc;
            ZonedDateTime overlapEnd = user1EndUtc.isBefore(user2EndUtc) ? user1EndUtc : user2EndUtc;

            if (overlapEnd.isAfter(overlapStart)) {
                Duration overlap = Duration.between(overlapStart, overlapEnd);
                return overlap.toMinutes() / 60.0; // Convert to hours
            }

            return 0.0; // No overlap
        } catch (Exception e) {
            throw new IllegalArgumentException("Error calculating work hour overlap: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the next reminder time in the user's timezone.
     *
     * @param baseTime      the base time to calculate from
     * @param userTimezone  the user's timezone
     * @param reminderTime  the preferred reminder time of day
     * @return the next reminder time in the user's timezone
     * @throws IllegalArgumentException if any parameter is null
     */
    public static ZonedDateTime getNextReminderTime(LocalDateTime baseTime, String userTimezone, LocalTime reminderTime) {
        if (baseTime == null) {
            throw new IllegalArgumentException("Base time cannot be null");
        }
        if (userTimezone == null) {
            throw new IllegalArgumentException("Timezone cannot be null");
        }
        if (reminderTime == null) {
            throw new IllegalArgumentException("Reminder time cannot be null");
        }

        try {
            ZoneId userZone = ZoneId.of(userTimezone);

            // Convert base time to user timezone
            ZonedDateTime baseTimeInUserZone = convertToUserTimezone(baseTime, userTimezone);

            // Create reminder time for today
            LocalDate today = baseTimeInUserZone.toLocalDate();
            ZonedDateTime todayReminder = ZonedDateTime.of(today, reminderTime, userZone);

            // If reminder time has already passed today, schedule for tomorrow
            if (todayReminder.isBefore(baseTimeInUserZone) || todayReminder.isEqual(baseTimeInUserZone)) {
                return todayReminder.plusDays(1);
            }

            return todayReminder;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error calculating next reminder time: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the current streak of consecutive days from a list of check-in dates.
     *
     * @param checkinDates   list of check-in dates sorted in any order
     * @param referenceDate  the reference date to calculate streak from (usually current time)
     * @return the number of consecutive days
     * @throws IllegalArgumentException if parameters are null
     */
    public static int calculateStreak(List<LocalDateTime> checkinDates, LocalDateTime referenceDate) {
        if (checkinDates == null) {
            throw new IllegalArgumentException("Checkin dates cannot be null");
        }
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }

        if (checkinDates.isEmpty()) {
            return 0;
        }

        // Sort dates in descending order (most recent first)
        List<LocalDate> uniqueDates = checkinDates.stream()
            .map(LocalDateTime::toLocalDate)
            .distinct()
            .sorted((d1, d2) -> d2.compareTo(d1)) // Reverse order
            .toList();

        LocalDate referenceLocalDate = referenceDate.toLocalDate();
        int streak = 0;
        LocalDate expectedDate = referenceLocalDate;

        for (LocalDate checkinDate : uniqueDates) {
            // Check if this checkin is for the expected date (today, yesterday, etc.)
            if (checkinDate.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1); // Look for previous day next
            } else if (checkinDate.equals(expectedDate.plusDays(1)) && streak == 0) {
                // Allow for checking yesterday if no checkin today
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else {
                // Gap in streak, stop counting
                break;
            }
        }

        return streak;
    }

    /**
     * Checks if a given time is within work hours.
     *
     * @param checkTime the time to check
     * @param workStart the work start time
     * @param workEnd   the work end time
     * @return true if the time is within work hours
     * @throws IllegalArgumentException if any parameter is null
     */
    public static boolean isWithinWorkHours(LocalTime checkTime, LocalTime workStart, LocalTime workEnd) {
        if (checkTime == null) {
            throw new IllegalArgumentException("Check time cannot be null");
        }
        if (workStart == null) {
            throw new IllegalArgumentException("Work start time cannot be null");
        }
        if (workEnd == null) {
            throw new IllegalArgumentException("Work end time cannot be null");
        }

        // Handle overnight work hours (e.g., 10 PM to 6 AM)
        if (workEnd.isBefore(workStart)) {
            // Overnight shift: time is valid if after start OR before end
            return !checkTime.isBefore(workStart) || !checkTime.isAfter(workEnd);
        } else {
            // Regular shift: time is valid if between start and end (inclusive)
            return !checkTime.isBefore(workStart) && !checkTime.isAfter(workEnd);
        }
    }

    /**
     * Calculates the number of days between two LocalDateTime instances.
     *
     * @param startDateTime the start date/time
     * @param endDateTime   the end date/time
     * @return the number of days between the dates (can be negative)
     * @throws IllegalArgumentException if any parameter is null
     */
    public static long getDaysBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null) {
            throw new IllegalArgumentException("Start date time cannot be null");
        }
        if (endDateTime == null) {
            throw new IllegalArgumentException("End date time cannot be null");
        }

        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();

        return Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays();
    }

    /**
     * Helper method to validate work hour parameters.
     */
    private static void validateWorkHourParameters(LocalTime start, LocalTime end, String timezone) {
        if (start == null) {
            throw new IllegalArgumentException("Work start time cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("Work end time cannot be null");
        }
        if (timezone == null || timezone.trim().isEmpty()) {
            throw new IllegalArgumentException("Timezone cannot be null or empty");
        }

        // Validate timezone
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone, e);
        }
    }

    /**
     * Helper method to convert work time to UTC.
     */
    private static ZonedDateTime convertWorkTimeToUtc(LocalDate date, LocalTime time, String timezone) {
        ZoneId userZone = ZoneId.of(timezone);
        ZoneId utcZone = ZoneId.of("UTC");

        ZonedDateTime workTimeInUserZone = ZonedDateTime.of(date, time, userZone);
        return workTimeInUserZone.withZoneSameInstant(utcZone);
    }
}