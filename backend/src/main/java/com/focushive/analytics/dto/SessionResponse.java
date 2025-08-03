package com.focushive.analytics.dto;

import com.focushive.timer.entity.FocusSession;

import java.time.LocalDateTime;

public class SessionResponse {
    private String id;
    private String userId;
    private String hiveId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer targetDurationMinutes;
    private Integer actualDurationMinutes;
    private FocusSession.SessionType type;
    private Boolean completed;
    private Integer breaksTaken;
    private Integer distractionsLogged;
    private String notes;
    
    // Default constructor
    public SessionResponse() {}
    
    // Constructor from entity
    public SessionResponse(FocusSession session) {
        this.id = session.getId();
        this.userId = session.getUserId();
        this.hiveId = session.getHiveId();
        this.startTime = session.getStartTime();
        this.endTime = session.getEndTime();
        this.targetDurationMinutes = session.getDurationMinutes(); // Using durationMinutes as target
        this.actualDurationMinutes = session.getActualDurationMinutes();
        this.type = session.getSessionType();
        this.completed = session.getCompleted();
        this.breaksTaken = 0; // Timer entity doesn't have breaksTaken
        this.distractionsLogged = session.getInterruptions(); // Using interruptions as distractionsLogged
        this.notes = session.getNotes();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getHiveId() {
        return hiveId;
    }
    
    public void setHiveId(String hiveId) {
        this.hiveId = hiveId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Integer getTargetDurationMinutes() {
        return targetDurationMinutes;
    }
    
    public void setTargetDurationMinutes(Integer targetDurationMinutes) {
        this.targetDurationMinutes = targetDurationMinutes;
    }
    
    public Integer getActualDurationMinutes() {
        return actualDurationMinutes;
    }
    
    public void setActualDurationMinutes(Integer actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }
    
    public FocusSession.SessionType getType() {
        return type;
    }
    
    public void setType(FocusSession.SessionType type) {
        this.type = type;
    }
    
    public Boolean getCompleted() {
        return completed;
    }
    
    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
    
    public Integer getBreaksTaken() {
        return breaksTaken;
    }
    
    public void setBreaksTaken(Integer breaksTaken) {
        this.breaksTaken = breaksTaken;
    }
    
    public Integer getDistractionsLogged() {
        return distractionsLogged;
    }
    
    public void setDistractionsLogged(Integer distractionsLogged) {
        this.distractionsLogged = distractionsLogged;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}