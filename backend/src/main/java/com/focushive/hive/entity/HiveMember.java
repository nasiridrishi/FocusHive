package com.focushive.hive.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hive_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"hive_id", "user_id"}))
public class HiveMember extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id", nullable = false)
    private Hive hive;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.MEMBER;
    
    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    private LocalDateTime lastActiveAt;
    
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
    
    public enum MemberRole {
        OWNER, MODERATOR, MEMBER
    }
}