package com.focushive.chat.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "typing_indicators")
public class TypingIndicator {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "hive_id", nullable = false)
    private UUID hiveId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private String username;
    
    @Column(name = "started_at", nullable = false)
    private ZonedDateTime startedAt;
    
    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;
    
    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = ZonedDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = startedAt.plusSeconds(10); // Default 10 second expiry
        }
    }
    
    // Constructors
    public TypingIndicator() {}
    
    public TypingIndicator(UUID hiveId, UUID userId, String username) {
        this.hiveId = hiveId;
        this.userId = userId;
        this.username = username;
        this.startedAt = ZonedDateTime.now();
        this.expiresAt = startedAt.plusSeconds(10);
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getHiveId() {
        return hiveId;
    }
    
    public void setHiveId(UUID hiveId) {
        this.hiveId = hiveId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public ZonedDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(ZonedDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isExpired() {
        return ZonedDateTime.now().isAfter(expiresAt);
    }
}