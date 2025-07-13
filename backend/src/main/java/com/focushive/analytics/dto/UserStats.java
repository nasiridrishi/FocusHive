package com.focushive.analytics.dto;

import java.time.LocalDate;
import java.util.Map;

public class UserStats {
    private String userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalSessions;
    private Integer completedSessions;
    private Integer totalMinutes;
    private Double averageSessionLength;
    private Double completionRate;
    private Integer currentStreak;
    private Integer longestStreak;
    private Map<String, Integer> sessionsByType;
    private Map<String, Integer> minutesByHive;
    
    // Default constructor
    public UserStats() {}
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Integer getTotalSessions() {
        return totalSessions;
    }
    
    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }
    
    public Integer getCompletedSessions() {
        return completedSessions;
    }
    
    public void setCompletedSessions(Integer completedSessions) {
        this.completedSessions = completedSessions;
    }
    
    public Integer getTotalMinutes() {
        return totalMinutes;
    }
    
    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
    }
    
    public Double getAverageSessionLength() {
        return averageSessionLength;
    }
    
    public void setAverageSessionLength(Double averageSessionLength) {
        this.averageSessionLength = averageSessionLength;
    }
    
    public Double getCompletionRate() {
        return completionRate;
    }
    
    public void setCompletionRate(Double completionRate) {
        this.completionRate = completionRate;
    }
    
    public Integer getCurrentStreak() {
        return currentStreak;
    }
    
    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }
    
    public Integer getLongestStreak() {
        return longestStreak;
    }
    
    public void setLongestStreak(Integer longestStreak) {
        this.longestStreak = longestStreak;
    }
    
    public Map<String, Integer> getSessionsByType() {
        return sessionsByType;
    }
    
    public void setSessionsByType(Map<String, Integer> sessionsByType) {
        this.sessionsByType = sessionsByType;
    }
    
    public Map<String, Integer> getMinutesByHive() {
        return minutesByHive;
    }
    
    public void setMinutesByHive(Map<String, Integer> minutesByHive) {
        this.minutesByHive = minutesByHive;
    }
}