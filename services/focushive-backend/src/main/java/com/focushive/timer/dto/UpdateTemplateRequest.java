package com.focushive.timer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for updating timer template request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTemplateRequest {
    
    @Size(max = 100, message = "Template name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Min(value = 1, message = "Focus duration must be at least 1 minute")
    @Max(value = 240, message = "Focus duration cannot exceed 240 minutes")
    private Integer focusDuration;
    
    @Min(value = 1, message = "Short break must be at least 1 minute")
    @Max(value = 60, message = "Short break cannot exceed 60 minutes")
    private Integer shortBreakDuration;
    
    @Min(value = 1, message = "Long break must be at least 1 minute")
    @Max(value = 120, message = "Long break cannot exceed 120 minutes")
    private Integer longBreakDuration;
    
    @Min(value = 1, message = "Must have at least 1 session before long break")
    @Max(value = 10, message = "Cannot exceed 10 sessions before long break")
    private Integer sessionsBeforeLongBreak;
    
    private Boolean autoStartBreaks;
    private Boolean autoStartFocus;
    private Boolean soundEnabled;
    private Boolean notificationEnabled;
    private Boolean isPublic;
    private Boolean isDefault;
    private String icon;
    private String color;
}