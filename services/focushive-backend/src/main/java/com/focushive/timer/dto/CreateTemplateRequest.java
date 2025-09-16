package com.focushive.timer.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating timer template request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTemplateRequest {
    
    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Template name cannot exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @NotNull(message = "Focus duration is required")
    @Min(value = 1, message = "Focus duration must be at least 1 minute")
    @Max(value = 240, message = "Focus duration cannot exceed 240 minutes")
    private Integer focusDuration;
    
    @NotNull(message = "Short break duration is required")
    @Min(value = 1, message = "Short break must be at least 1 minute")
    @Max(value = 60, message = "Short break cannot exceed 60 minutes")
    private Integer shortBreakDuration;
    
    @NotNull(message = "Long break duration is required")
    @Min(value = 1, message = "Long break must be at least 1 minute")
    @Max(value = 120, message = "Long break cannot exceed 120 minutes")
    private Integer longBreakDuration;
    
    @Min(value = 1, message = "Must have at least 1 session before long break")
    @Max(value = 10, message = "Cannot exceed 10 sessions before long break")
    @Builder.Default
    private Integer sessionsBeforeLongBreak = 4;
    
    @Builder.Default
    private Boolean autoStartBreaks = false;
    
    @Builder.Default
    private Boolean autoStartFocus = false;
    
    @Builder.Default
    private Boolean soundEnabled = true;
    
    @Builder.Default
    private Boolean notificationEnabled = true;
    
    @Builder.Default
    private Boolean isPublic = false;
    
    private String icon;
    private String color;
    private String userId;
}