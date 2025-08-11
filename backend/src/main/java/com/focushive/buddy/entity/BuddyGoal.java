package com.focushive.buddy.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "buddy_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyGoal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
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
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
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