package com.focushive.analytics.dto;

import com.focushive.timer.entity.FocusSession;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SessionRequest {
    
    private String hiveId;
    
    @NotNull(message = "Target duration is required")
    @Min(value = 1, message = "Target duration must be at least 1 minute")
    private Integer targetDurationMinutes;
    
    @NotNull(message = "Session type is required")
    private FocusSession.SessionType type;
    
    private String notes;
    
    // Getters and setters
    public String getHiveId() {
        return hiveId;
    }
    
    public void setHiveId(String hiveId) {
        this.hiveId = hiveId;
    }
    
    public Integer getTargetDurationMinutes() {
        return targetDurationMinutes;
    }
    
    public void setTargetDurationMinutes(Integer targetDurationMinutes) {
        this.targetDurationMinutes = targetDurationMinutes;
    }
    
    public FocusSession.SessionType getType() {
        return type;
    }
    
    public void setType(FocusSession.SessionType type) {
        this.type = type;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}