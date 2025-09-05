package com.focushive.hive.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hive_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hive_id", "user_id"}))
public class HiveMember extends BaseEntity {
    
    @NotNull(message = "Hive is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id", nullable = false)
    private Hive hive;
    
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role = MemberRole.MEMBER;
    
    @NotNull(message = "Joined date is required")
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    @Min(value = 0, message = "Total minutes cannot be negative")
    @Column(name = "total_minutes")
    private Integer totalMinutes = 0;
    
    @Min(value = 0, message = "Consecutive days cannot be negative")
    @Column(name = "consecutive_days")
    private Integer consecutiveDays = 0;
    
    @Column(name = "is_muted")
    private Boolean isMuted = false;
    
    @Column(name = "notification_settings", columnDefinition = "TEXT")
    private String notificationSettings = "{}";
    
    // Getters and setters
    public Hive getHive() {
        return hive;
    }
    
    public void setHive(Hive hive) {
        this.hive = hive;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public MemberRole getRole() {
        return role;
    }
    
    public void setRole(MemberRole role) {
        this.role = role;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }
    
    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
    
    public Integer getTotalMinutes() {
        return totalMinutes;
    }
    
    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
    }
    
    public Integer getConsecutiveDays() {
        return consecutiveDays;
    }
    
    public void setConsecutiveDays(Integer consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }
    
    public Boolean getIsMuted() {
        return isMuted;
    }
    
    public void setIsMuted(Boolean isMuted) {
        this.isMuted = isMuted;
    }
    
    public String getNotificationSettings() {
        return notificationSettings;
    }
    
    public void setNotificationSettings(String notificationSettings) {
        this.notificationSettings = notificationSettings;
    }
    
    public enum MemberRole {
        OWNER, MODERATOR, MEMBER
    }
}