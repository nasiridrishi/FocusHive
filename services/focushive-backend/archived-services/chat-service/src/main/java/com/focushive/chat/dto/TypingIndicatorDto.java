package com.focushive.chat.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

public class TypingIndicatorDto {
    
    private UUID hiveId;
    private UUID userId;
    private String username;
    private boolean isTyping;
    private ZonedDateTime timestamp;
    
    // Constructors
    public TypingIndicatorDto() {}
    
    public TypingIndicatorDto(UUID hiveId, UUID userId, String username, boolean isTyping) {
        this.hiveId = hiveId;
        this.userId = userId;
        this.username = username;
        this.isTyping = isTyping;
        this.timestamp = ZonedDateTime.now();
    }
    
    // Getters and Setters
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
    
    public boolean isTyping() {
        return isTyping;
    }
    
    public void setTyping(boolean typing) {
        isTyping = typing;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }
}