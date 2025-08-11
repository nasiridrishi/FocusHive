package com.focushive.buddy.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private GoalStatus status = GoalStatus.ACTIVE;
    
    @Column(nullable = false)
    private Integer progress = 0;
    
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