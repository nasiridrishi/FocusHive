package com.focushive.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * PersonaActivity entity for tracking activity per persona.
 * This provides data isolation between personas and enables context-aware analytics.
 */
@Entity
@Table(name = "persona_activities", indexes = {
    @Index(name = "idx_persona_activity_persona", columnList = "persona_id"),
    @Index(name = "idx_persona_activity_type", columnList = "activity_type"),
    @Index(name = "idx_persona_activity_timestamp", columnList = "activity_timestamp"),
    @Index(name = "idx_persona_activity_session", columnList = "session_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"persona"})
@ToString(exclude = {"persona"})
public class PersonaActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;
    
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;
    
    @Column(name = "activity_name", nullable = false, length = 100)
    private String activityName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "session_id")
    private UUID sessionId;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "start_time")
    private Instant startTime;
    
    @Column(name = "end_time")
    private Instant endTime;
    
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "ACTIVE";
    
    // Metadata for extensibility
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = JsonAttributeConverter.class)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    // Tags for categorization
    @ElementCollection
    @CollectionTable(name = "persona_activity_tags", 
                     joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();
    
    // Productivity metrics
    @Column(name = "productivity_score")
    private Double productivityScore;
    
    @Column(name = "focus_score")
    private Double focusScore;
    
    @Column(name = "interruption_count")
    @Builder.Default
    private Integer interruptionCount = 0;
    
    // Location/context information
    @Column(name = "context_location", length = 100)
    private String contextLocation;
    
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    // Audit fields
    @CreationTimestamp
    @Column(name = "activity_timestamp", nullable = false, updatable = false)
    private Instant activityTimestamp;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;
    
    /**
     * Activity types enum for common activities.
     */
    public static class ActivityType {
        public static final String FOCUS_SESSION = "FOCUS_SESSION";
        public static final String BREAK = "BREAK";
        public static final String COLLABORATION = "COLLABORATION";
        public static final String LEARNING = "LEARNING";
        public static final String MEETING = "MEETING";
        public static final String TASK_COMPLETION = "TASK_COMPLETION";
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String PERSONA_SWITCH = "PERSONA_SWITCH";
        public static final String SETTINGS_CHANGE = "SETTINGS_CHANGE";
    }
    
    /**
     * Activity status enum.
     */
    public static class Status {
        public static final String ACTIVE = "ACTIVE";
        public static final String COMPLETED = "COMPLETED";
        public static final String PAUSED = "PAUSED";
        public static final String CANCELLED = "CANCELLED";
    }
    
    /**
     * Calculate duration if not already set.
     */
    @PrePersist
    @PreUpdate
    public void calculateDuration() {
        if (startTime != null && endTime != null && durationMinutes == null) {
            long duration = java.time.Duration.between(startTime, endTime).toMinutes();
            this.durationMinutes = (int) duration;
        }
    }
    
    /**
     * Check if activity is currently active.
     */
    @Transient
    public boolean isActive() {
        return Status.ACTIVE.equals(status) && (endTime == null || endTime.isAfter(Instant.now()));
    }
    
    /**
     * Add a tag to this activity.
     */
    public void addTag(String tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag.toLowerCase().trim());
    }
    
    /**
     * Add metadata entry.
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
}