package com.focushive.buddy.entity;

import com.focushive.user.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "buddy_checkins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyCheckin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_id", nullable = false)
    private BuddyRelationship relationship;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Mood mood;
    
    @Column(name = "productivity_score")
    private Integer productivityScore;
    
    @Type(JsonType.class)
    @Column(name = "goals_progress", columnDefinition = "jsonb")
    private Map<Long, Integer> goalsProgress;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum Mood {
        EXCELLENT, GOOD, NEUTRAL, STRUGGLING, DIFFICULT
    }
    
    @PrePersist
    @PreUpdate
    private void validateProductivityScore() {
        if (productivityScore != null) {
            if (productivityScore < 1) productivityScore = 1;
            if (productivityScore > 10) productivityScore = 10;
        }
    }
    
    public boolean isPositive() {
        return mood == Mood.EXCELLENT || mood == Mood.GOOD;
    }
    
    public boolean isStruggling() {
        return mood == Mood.STRUGGLING || mood == Mood.DIFFICULT;
    }
}