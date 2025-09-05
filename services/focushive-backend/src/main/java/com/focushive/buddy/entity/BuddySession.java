package com.focushive.buddy.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "buddy_sessions",
    indexes = {
        @Index(name = "idx_buddy_session_relationship", columnList = "relationship_id"),
        @Index(name = "idx_buddy_session_date", columnList = "session_date DESC"),
        @Index(name = "idx_buddy_session_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddySession extends BaseEntity {
    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_id", nullable = false)
    private BuddyRelationship relationship;
    
    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;
    
    @Column(name = "planned_duration_minutes", nullable = false)
    private Integer plannedDurationMinutes;
    
    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;
    
    @Column(columnDefinition = "TEXT")
    private String agenda;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;
    
    @Column(name = "user1_joined")
    private LocalDateTime user1Joined;
    
    @Column(name = "user1_left")
    private LocalDateTime user1Left;
    
    @Column(name = "user2_joined")
    private LocalDateTime user2Joined;
    
    @Column(name = "user2_left")
    private LocalDateTime user2Left;
    
    @Column(name = "user1_rating")
    private Integer user1Rating;
    
    @Column(name = "user1_feedback", columnDefinition = "TEXT")
    private String user1Feedback;
    
    @Column(name = "user2_rating")
    private Integer user2Rating;
    
    @Column(name = "user2_feedback", columnDefinition = "TEXT")
    private String user2Feedback;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancelled_by")
    private String cancelledBy;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    
    public enum SessionStatus {
        SCHEDULED,    // Session is planned for future
        IN_PROGRESS,  // Session is currently active
        COMPLETED,    // Session finished normally
        CANCELLED,    // Session was cancelled
        NO_SHOW       // One or both participants didn't show up
    }
    
    public boolean isUpcoming() {
        return status == SessionStatus.SCHEDULED && 
               sessionDate.isAfter(LocalDateTime.now());
    }
    
    public boolean isPast() {
        return sessionDate.isBefore(LocalDateTime.now());
    }
    
    public boolean canStart() {
        if (status != SessionStatus.SCHEDULED) {
            return false;
        }
        
        // Allow starting 5 minutes before scheduled time
        LocalDateTime startWindow = sessionDate.minusMinutes(5);
        LocalDateTime endWindow = sessionDate.plusMinutes(30); // 30 minutes late window
        LocalDateTime now = LocalDateTime.now();
        
        return now.isAfter(startWindow) && now.isBefore(endWindow);
    }
    
    public void startSession() {
        if (canStart()) {
            this.status = SessionStatus.IN_PROGRESS;
        }
    }
    
    public void userJoined(String userId) {
        LocalDateTime now = LocalDateTime.now();
        if (relationship.getUser1().getId().equals(userId)) {
            this.user1Joined = now;
        } else if (relationship.getUser2().getId().equals(userId)) {
            this.user2Joined = now;
        }
        
        // Start session if both users have joined
        if (user1Joined != null && user2Joined != null && status == SessionStatus.SCHEDULED) {
            startSession();
        }
    }
    
    public void userLeft(String userId) {
        LocalDateTime now = LocalDateTime.now();
        if (relationship.getUser1().getId().equals(userId)) {
            this.user1Left = now;
        } else if (relationship.getUser2().getId().equals(userId)) {
            this.user2Left = now;
        }
        
        // End session if both users have left
        if (user1Left != null && user2Left != null && status == SessionStatus.IN_PROGRESS) {
            completeSession();
        }
    }
    
    public void completeSession() {
        this.status = SessionStatus.COMPLETED;
        calculateActualDuration();
    }
    
    private void calculateActualDuration() {
        if (user1Joined != null && user2Joined != null) {
            LocalDateTime sessionStart = user1Joined.isAfter(user2Joined) ? user1Joined : user2Joined;
            LocalDateTime sessionEnd = LocalDateTime.now();
            
            if (user1Left != null && user2Left != null) {
                sessionEnd = user1Left.isBefore(user2Left) ? user1Left : user2Left;
            }
            
            Duration duration = Duration.between(sessionStart, sessionEnd);
            this.actualDurationMinutes = (int) duration.toMinutes();
        }
    }
    
    public void cancelSession(String userId, String reason) {
        this.status = SessionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = userId;
        this.cancellationReason = reason;
    }
    
    public void markAsNoShow() {
        this.status = SessionStatus.NO_SHOW;
    }
    
    public boolean canRate(String userId) {
        if (status != SessionStatus.COMPLETED) {
            return false;
        }
        
        if (relationship.getUser1().getId().equals(userId)) {
            return user1Rating == null;
        } else if (relationship.getUser2().getId().equals(userId)) {
            return user2Rating == null;
        }
        
        return false;
    }
    
    public void addRating(String userId, Integer rating, String feedback) {
        if (!canRate(userId)) {
            return;
        }
        
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        
        if (relationship.getUser1().getId().equals(userId)) {
            this.user1Rating = rating;
            this.user1Feedback = feedback;
        } else if (relationship.getUser2().getId().equals(userId)) {
            this.user2Rating = rating;
            this.user2Feedback = feedback;
        }
    }
    
    public Double getAverageRating() {
        if (user1Rating == null && user2Rating == null) {
            return null;
        }
        if (user1Rating == null) {
            return user2Rating.doubleValue();
        }
        if (user2Rating == null) {
            return user1Rating.doubleValue();
        }
        return (user1Rating + user2Rating) / 2.0;
    }
}