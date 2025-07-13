package com.focushive.analytics.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_summaries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
public class DailySummary extends BaseEntity {
    
    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;
    
    @NotNull(message = "Total minutes is required")
    @Min(value = 0, message = "Total minutes cannot be negative")
    @Column(name = "total_minutes", nullable = false)
    private Integer totalMinutes = 0;
    
    @NotNull(message = "Focus minutes is required")
    @Min(value = 0, message = "Focus minutes cannot be negative")
    @Column(name = "focus_minutes", nullable = false)
    private Integer focusMinutes = 0;
    
    @NotNull(message = "Break minutes is required")
    @Min(value = 0, message = "Break minutes cannot be negative")
    @Column(name = "break_minutes", nullable = false)
    private Integer breakMinutes = 0;
    
    @NotNull(message = "Sessions count is required")
    @Min(value = 0, message = "Sessions count cannot be negative")
    @Column(name = "sessions_count", nullable = false)
    private Integer sessionsCount = 0;
    
    @NotNull(message = "Completed sessions is required")
    @Min(value = 0, message = "Completed sessions cannot be negative")
    @Column(name = "completed_sessions", nullable = false)
    private Integer completedSessions = 0;
    
    @Min(value = 0, message = "Average session length cannot be negative")
    @Column(name = "average_session_length")
    private Integer averageSessionLength;
    
    @Min(value = 0, message = "Productivity score cannot be negative")
    @Max(value = 100, message = "Productivity score cannot exceed 100")
    @Column(name = "productivity_score")
    private Integer productivityScore;
    
    @Min(value = 0, message = "Most productive hour must be between 0 and 23")
    @Max(value = 23, message = "Most productive hour must be between 0 and 23")
    @Column(name = "most_productive_hour")
    private Integer mostProductiveHour;
    
    @NotNull(message = "Breaks taken is required")
    @Min(value = 0, message = "Breaks taken cannot be negative")
    @Column(name = "breaks_taken", nullable = false)
    private Integer breaksTaken = 0;
    
    @NotNull(message = "Distractions count is required")
    @Min(value = 0, message = "Distractions count cannot be negative")
    @Column(name = "distractions_count", nullable = false)
    private Integer distractionsCount = 0;
    
    @NotNull(message = "Goals achieved is required")
    @Min(value = 0, message = "Goals achieved cannot be negative")
    @Column(name = "goals_achieved", nullable = false)
    private Integer goalsAchieved = 0;
    
    @NotNull(message = "Streak days is required")
    @Min(value = 0, message = "Streak days cannot be negative")
    @Column(name = "streak_days", nullable = false)
    private Integer streakDays = 0;
    
    @Column(name = "hives_visited", columnDefinition = "jsonb")
    private String hivesVisited = "[]";
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public Integer getTotalMinutes() {
        return totalMinutes;
    }
    
    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
    }
    
    public Integer getFocusMinutes() {
        return focusMinutes;
    }
    
    public void setFocusMinutes(Integer focusMinutes) {
        this.focusMinutes = focusMinutes;
    }
    
    public Integer getBreakMinutes() {
        return breakMinutes;
    }
    
    public void setBreakMinutes(Integer breakMinutes) {
        this.breakMinutes = breakMinutes;
    }
    
    public Integer getSessionsCount() {
        return sessionsCount;
    }
    
    public void setSessionsCount(Integer sessionsCount) {
        this.sessionsCount = sessionsCount;
    }
    
    public Integer getCompletedSessions() {
        return completedSessions;
    }
    
    public void setCompletedSessions(Integer completedSessions) {
        this.completedSessions = completedSessions;
    }
    
    public Integer getAverageSessionLength() {
        return averageSessionLength;
    }
    
    public void setAverageSessionLength(Integer averageSessionLength) {
        this.averageSessionLength = averageSessionLength;
    }
    
    public Integer getProductivityScore() {
        return productivityScore;
    }
    
    public void setProductivityScore(Integer productivityScore) {
        this.productivityScore = productivityScore;
    }
    
    public Integer getMostProductiveHour() {
        return mostProductiveHour;
    }
    
    public void setMostProductiveHour(Integer mostProductiveHour) {
        this.mostProductiveHour = mostProductiveHour;
    }
    
    public Integer getBreaksTaken() {
        return breaksTaken;
    }
    
    public void setBreaksTaken(Integer breaksTaken) {
        this.breaksTaken = breaksTaken;
    }
    
    public Integer getDistractionsCount() {
        return distractionsCount;
    }
    
    public void setDistractionsCount(Integer distractionsCount) {
        this.distractionsCount = distractionsCount;
    }
    
    public Integer getGoalsAchieved() {
        return goalsAchieved;
    }
    
    public void setGoalsAchieved(Integer goalsAchieved) {
        this.goalsAchieved = goalsAchieved;
    }
    
    public Integer getStreakDays() {
        return streakDays;
    }
    
    public void setStreakDays(Integer streakDays) {
        this.streakDays = streakDays;
    }
    
    public String getHivesVisited() {
        return hivesVisited;
    }
    
    public void setHivesVisited(String hivesVisited) {
        this.hivesVisited = hivesVisited;
    }
}