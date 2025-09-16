package com.focushive.buddy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for buddy service scheduling operations.
 *
 * Provides configurable settings for:
 * - Task execution timing and frequency
 * - Reminder and notification settings
 * - Performance and retry parameters
 * - Integration timeouts and thresholds
 */
@Data
@Component
@ConfigurationProperties(prefix = "buddy.scheduling")
public class BuddySchedulingProperties {

    /**
     * Health check configuration
     */
    private HealthCheck healthCheck = new HealthCheck();

    /**
     * Streak calculation configuration
     */
    private StreakCalculation streakCalculation = new StreakCalculation();

    /**
     * Accountability score configuration
     */
    private AccountabilityScore accountabilityScore = new AccountabilityScore();

    /**
     * Inactive user notification configuration
     */
    private InactiveNotification inactiveNotification = new InactiveNotification();

    /**
     * Goal reminder configuration
     */
    private GoalReminder goalReminder = new GoalReminder();

    /**
     * Performance and retry configuration
     */
    private Performance performance = new Performance();

    @Data
    public static class HealthCheck {
        /**
         * Cron expression for daily health check (default: midnight)
         */
        private String cronExpression = "0 0 0 * * ?";

        /**
         * Batch size for processing partnerships
         */
        private int batchSize = 100;

        /**
         * Health score threshold for triggering alerts
         */
        private double alertThreshold = 0.3;

        /**
         * Whether to skip weekends for health checks
         */
        private boolean skipWeekends = false;
    }

    @Data
    public static class StreakCalculation {
        /**
         * Cron expression for daily streak updates (default: 1 AM)
         */
        private String cronExpression = "0 0 1 * * ?";

        /**
         * Maximum streak count before special rewards
         */
        private int maxStreakForRewards = 30;

        /**
         * Grace period in hours for late checkins
         */
        private int gracePeriodHours = 2;

        /**
         * Whether to send streak milestone notifications
         */
        private boolean notifyMilestones = true;
    }

    @Data
    public static class AccountabilityScore {
        /**
         * Cron expression for weekly score recalculation (default: Sunday midnight)
         */
        private String cronExpression = "0 0 0 ? * SUN";

        /**
         * Number of weeks to include in rolling average
         */
        private int rollingWeeks = 4;

        /**
         * Weight for goal completion in score calculation
         */
        private double goalCompletionWeight = 0.4;

        /**
         * Weight for checkin consistency in score calculation
         */
        private double checkinConsistencyWeight = 0.3;

        /**
         * Weight for partner interaction in score calculation
         */
        private double partnerInteractionWeight = 0.3;
    }

    @Data
    public static class InactiveNotification {
        /**
         * Cron expression for inactive user notifications (default: Monday 9 AM)
         */
        private String cronExpression = "0 0 9 ? * MON";

        /**
         * Number of days to consider a user inactive
         */
        private int inactiveDays = 7;

        /**
         * Maximum notifications per user per week
         */
        private int maxNotificationsPerWeek = 1;

        /**
         * Whether to escalate to partner notifications
         */
        private boolean escalateToPartner = true;

        /**
         * Days before escalating to partner
         */
        private int escalationDays = 14;
    }

    @Data
    public static class GoalReminder {
        /**
         * Fixed delay between goal reminder checks (default: 24 hours)
         */
        private long fixedDelayMs = 86400000L; // 24 hours

        /**
         * Days before deadline to send first reminder
         */
        private int goalReminderDays = 3;

        /**
         * Days before deadline to send urgent reminder
         */
        private int urgentReminderDays = 1;

        /**
         * Maximum reminders per goal
         */
        private int maxRemindersPerGoal = 3;

        /**
         * Whether to include partner in goal reminders
         */
        private boolean includePartnerInReminders = true;
    }

    @Data
    public static class Performance {
        /**
         * Maximum execution time in milliseconds before logging warning
         */
        private long maxExecutionTimeMs = 300000L; // 5 minutes

        /**
         * Number of retry attempts for failed operations
         */
        private int retryAttempts = 3;

        /**
         * Delay between retry attempts in milliseconds
         */
        private long retryDelayMs = 5000L; // 5 seconds

        /**
         * Whether to skip execution if previous task is still running
         */
        private boolean skipIfRunning = true;

        /**
         * Thread pool size for concurrent processing
         */
        private int threadPoolSize = 5;
    }

    // Convenience methods for accessing commonly used properties

    public int getGoalReminderDays() {
        return goalReminder.getGoalReminderDays();
    }

    public int getInactiveDays() {
        return inactiveNotification.getInactiveDays();
    }

    public int getHealthCheckBatchSize() {
        return healthCheck.getBatchSize();
    }

    public double getHealthAlertThreshold() {
        return healthCheck.getAlertThreshold();
    }

    public boolean isSkipIfRunning() {
        return performance.isSkipIfRunning();
    }

    public int getRetryAttempts() {
        return performance.getRetryAttempts();
    }

    public long getRetryDelayMs() {
        return performance.getRetryDelayMs();
    }

    public long getMaxExecutionTimeMs() {
        return performance.getMaxExecutionTimeMs();
    }
}