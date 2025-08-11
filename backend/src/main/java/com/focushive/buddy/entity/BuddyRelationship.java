package com.focushive.buddy.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "buddy_relationships", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private BuddyStatus status = BuddyStatus.PENDING;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;
    
    @Column(name = "matched_at")
    private LocalDateTime matchedAt;
    
    @Column(name = "started_at")
    private LocalDateTime startDate;
    
    @Column(name = "ended_at")
    private LocalDateTime endDate;
    
    @Column(name = "termination_reason", columnDefinition = "TEXT")
    private String terminationReason;
    
    @Column(name = "match_score", precision = 3, scale = 2)
    private BigDecimal matchScore;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum BuddyStatus {
        PENDING, ACTIVE, ENDED, BLOCKED
    }
    
    // Helper methods
    public boolean isActive() {
        return status == BuddyStatus.ACTIVE;
    }
    
    public boolean isPending() {
        return status == BuddyStatus.PENDING;
    }
    
    public boolean involvesUser(String userId) {
        return user1.getId().equals(userId) || user2.getId().equals(userId);
    }
    
    public String getPartnerId(String userId) {
        if (user1.getId().equals(userId)) {
            return user2.getId();
        } else if (user2.getId().equals(userId)) {
            return user1.getId();
        }
        return null;
    }
    
    public User getPartner(String userId) {
        if (user1.getId().equals(userId)) {
            return user2;
        } else if (user2.getId().equals(userId)) {
            return user1;
        }
        return null;
    }
    
    public boolean isRecipient(String userId) {
        return involvesUser(userId) && !initiatedBy.getId().equals(userId);
    }
    
    @PrePersist
    @PreUpdate
    private void ensureUserOrder() {
        // Ensure user1_id is always less than user2_id for consistency
        if (user1 != null && user2 != null && user1.getId().compareTo(user2.getId()) > 0) {
            User temp = user1;
            user1 = user2;
            user2 = temp;
        }
    }
}