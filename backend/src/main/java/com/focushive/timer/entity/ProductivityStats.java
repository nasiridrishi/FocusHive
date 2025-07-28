package com.focushive.timer.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity representing daily productivity statistics for a user.
 */
@Entity
@Table(name = "productivity_stats", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityStats extends BaseEntity {
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private LocalDate date;
    
    @Column(name = "total_focus_minutes", nullable = false)
    @Builder.Default
    private Integer totalFocusMinutes = 0;
    
    @Column(name = "total_break_minutes", nullable = false)
    @Builder.Default
    private Integer totalBreakMinutes = 0;
    
    @Column(name = "sessions_completed", nullable = false)
    @Builder.Default
    private Integer sessionsCompleted = 0;
    
    @Column(name = "sessions_started", nullable = false)
    @Builder.Default
    private Integer sessionsStarted = 0;
    
    @Column(name = "longest_streak_minutes", nullable = false)
    @Builder.Default
    private Integer longestStreakMinutes = 0;
    
    @Column(name = "daily_goal_minutes", nullable = false)
    @Builder.Default
    private Integer dailyGoalMinutes = 480; // 8 hours default
    
    /**
     * Calculate completion percentage based on daily goal.
     */
    public double getCompletionPercentage() {
        if (dailyGoalMinutes == 0) return 100.0;
        return Math.min(100.0, (totalFocusMinutes * 100.0) / dailyGoalMinutes);
    }
}