package com.focushive.events;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseEvent {
    private final String eventId;
    private final LocalDateTime timestamp;
    private final String aggregateId;
    private final String eventType;
    
    protected BaseEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.eventType = this.getClass().getSimpleName();
    }
    
    // Getters
    public String getEventId() {
        return eventId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public String getEventType() {
        return eventType;
    }
}