package com.focushive.timer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for ending a focus session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndSessionRequest {
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
}