package com.focushive.events;

public class SessionStartedEvent extends BaseEvent {
    private final String sessionId;
    private final String userId;
    private final String hiveId;
    private final Integer targetDurationMinutes;
    
    public SessionStartedEvent(String sessionId, String userId, String hiveId, Integer targetDurationMinutes) {
        super(sessionId);
        this.sessionId = sessionId;
        this.userId = userId;
        this.hiveId = hiveId;
        this.targetDurationMinutes = targetDurationMinutes;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getHiveId() {
        return hiveId;
    }
    
    public Integer getTargetDurationMinutes() {
        return targetDurationMinutes;
    }
}