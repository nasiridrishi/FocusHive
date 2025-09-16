package com.focushive.notification.controller;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for error responses.
 */
@Data
@Builder
public class ErrorResponse {
    private String message;
    private int status;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}