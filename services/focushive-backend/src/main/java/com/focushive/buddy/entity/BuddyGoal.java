package com.focushive.buddy.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "buddy_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyGoal extends BaseEntity {
    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_id", nullable = false)
    private BuddyRelationship relationship;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "target_date")
    private LocalDate targetDate;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Convert(converter = com.focushive.common.converter.JsonMapConverter.class)
    @Column(name = "metrics", columnDefinition = "TEXT")
    private Map<String, Object> metrics;
    
    @Column(name = "progress_percentage")
    private Integer progressPercentage;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    
    public enum GoalStatus {
        ACTIVE, COMPLETED, CANCELLED
    }
    
    @PrePersist
    @PreUpdate
    private void validateProgress() {
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
    }
    
    public boolean isCompleted() {
        return status == GoalStatus.COMPLETED || progress >= 100;
    }
    
    public boolean isActive() {
        return status == GoalStatus.ACTIVE;
    }
    
    public boolean isOverdue() {
        return targetDate != null && LocalDate.now().isAfter(targetDate) && !isCompleted();
    }
}