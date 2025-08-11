package com.focushive.buddy.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;
    
    @Column(name = "checkin_time", nullable = false)
    private LocalDateTime checkinTime;
    
    @Min(1) @Max(5)
    @Column(name = "mood_rating")
    private Integer moodRating;
    
    @Min(1) @Max(5)
    @Column(name = "progress_rating")
    private Integer progressRating;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "current_focus", length = 500)
    private String currentFocus;
    
    @Column(length = 500)
    private String challenges;
    
    @Column(length = 500)
    private String wins;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Mood mood;
    
    @Column(name = "productivity_score")
    private Integer productivityScore;
    
    @JdbcTypeCode(SqlTypes.JSON)
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