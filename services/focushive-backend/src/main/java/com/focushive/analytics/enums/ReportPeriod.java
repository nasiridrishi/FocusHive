package com.focushive.analytics.enums;

/**
 * Enumeration of different time periods for analytics reports.
 */
public enum ReportPeriod {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
    QUARTERLY("Quarterly", 90),
    YEARLY("Yearly", 365);

    private final String displayName;
    private final int days;

    ReportPeriod(String displayName, int days) {
        this.displayName = displayName;
        this.days = days;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDays() {
        return days;
    }
}