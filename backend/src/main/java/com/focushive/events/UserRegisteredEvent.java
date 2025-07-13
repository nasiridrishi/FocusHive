package com.focushive.events;

public class UserRegisteredEvent extends BaseEvent {
    private final String userId;
    private final String email;
    private final String username;
    
    public UserRegisteredEvent(String userId, String email, String username) {
        super(userId);
        this.userId = userId;
        this.email = email;
        this.username = username;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getUsername() {
        return username;
    }
}