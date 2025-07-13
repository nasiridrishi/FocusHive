package com.focushive.analytics.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.hive.entity.Hive;
import com.focushive.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "focus_sessions")
public class FocusSession extends BaseEntity {
    
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id")
    private Hive hive;
    
    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @NotNull(message = "Target duration is required")
    @Min(value = 1, message = "Target duration must be at least 1 minute")
    @Column(name = "target_duration_minutes", nullable = false)
    private Integer targetDurationMinutes;
    
    @Min(value = 0, message = "Actual duration cannot be negative")
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @NotNull(message = "Session type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionType type = SessionType.FOCUS;
    
    @Column(nullable = false)
    private Boolean completed = false;
    
    @Min(value = 0, message = "Breaks taken cannot be negative")
    @Column(name = "breaks_taken")
    private Integer breaksTaken = 0;
    
    @Min(value = 0, message = "Distractions logged cannot be negative")
    @Column(name = "distractions_logged")
    private Integer distractionsLogged = 0;
    
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Min(value = 0, message = "Productivity score cannot be negative")
    @Max(value = 100, message = "Productivity score cannot exceed 100")
    @Column(name = "productivity_score")
    private Integer productivityScore;
    
    @Column(columnDefinition = "text[]")
    private String[] tags;
    
    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";
    
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
    
    public Integer getProductivityScore() {
        return productivityScore;
    }
    
    public void setProductivityScore(Integer productivityScore) {
        this.productivityScore = productivityScore;
    }
    
    public String[] getTags() {
        return tags;
    }
    
    public void setTags(String[] tags) {
        this.tags = tags;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public enum SessionType {
        FOCUS, BREAK, MEDITATION, PLANNING
    }
}