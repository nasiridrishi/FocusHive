package com.focushive.analytics.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.hive.entity.Hive;
import com.focushive.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "focus_sessions")
public class FocusSession extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id")
    private Hive hive;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    @Column(nullable = false)
    private Integer targetDurationMinutes;
    
    private Integer actualDurationMinutes;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType type = SessionType.FOCUS;
    
    @Column(nullable = false)
    private Boolean completed = false;
    
    private Integer breaksTaken = 0;
    
    private Integer distractionsLogged = 0;
    
    private String notes;
    
    // Getters and setters
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Hive getHive() {
        return hive;
    }
    
    public void setHive(Hive hive) {
        this.hive = hive;
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
    
    public SessionType getType() {
        return type;
    }
    
    public void setType(SessionType type) {
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
    
    public enum SessionType {
        FOCUS, BREAK, MEDITATION, PLANNING
    }
}