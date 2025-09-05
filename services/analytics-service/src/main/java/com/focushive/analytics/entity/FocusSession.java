package com.focushive.analytics.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "focus_sessions")
public class FocusSession {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "hive_id")
    private UUID hiveId;
    
    @Column(name = "start_time", nullable = false)
    private ZonedDateTime startTime;
    
    @Column(name = "end_time")
    private ZonedDateTime endTime;
    
    @Column(name = "target_duration_minutes", nullable = false)
    private Integer targetDurationMinutes;
    
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @Enumerated(EnumType.STRING)
    private SessionType type = SessionType.FOCUS;
    
    private Boolean completed = false;
    
    @Column(name = "breaks_taken")
    private Integer breaksTaken = 0;
    
    @Column(name = "distractions_logged")
    private Integer distractionsLogged = 0;
    
    @Column(name = "productivity_score")
    private Integer productivityScore;
    
    private String notes;
    
    @ElementCollection
    @CollectionTable(name = "session_tags", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "tag")
    private List<String> tags;
    
    @ElementCollection
    @CollectionTable(name = "session_metadata", joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
    
    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = ZonedDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }
    
    // Constructors, getters, and setters
    public FocusSession() {}
    
    public enum SessionType {
        FOCUS, BREAK, MEDITATION, PLANNING
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public UUID getHiveId() { return hiveId; }
    public void setHiveId(UUID hiveId) { this.hiveId = hiveId; }
    
    public ZonedDateTime getStartTime() { return startTime; }
    public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }
    
    public ZonedDateTime getEndTime() { return endTime; }
    public void setEndTime(ZonedDateTime endTime) { this.endTime = endTime; }
    
    public Integer getTargetDurationMinutes() { return targetDurationMinutes; }
    public void setTargetDurationMinutes(Integer targetDurationMinutes) { this.targetDurationMinutes = targetDurationMinutes; }
    
    public Integer getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(Integer actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }
    
    public SessionType getType() { return type; }
    public void setType(SessionType type) { this.type = type; }
    
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    
    public Integer getBreaksTaken() { return breaksTaken; }
    public void setBreaksTaken(Integer breaksTaken) { this.breaksTaken = breaksTaken; }
    
    public Integer getDistractionsLogged() { return distractionsLogged; }
    public void setDistractionsLogged(Integer distractionsLogged) { this.distractionsLogged = distractionsLogged; }
    
    public Integer getProductivityScore() { return productivityScore; }
    public void setProductivityScore(Integer productivityScore) { this.productivityScore = productivityScore; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
    
    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}