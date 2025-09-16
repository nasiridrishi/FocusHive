package com.focushive.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO for consistent error handling across the application.
 * Provides a uniform structure for all error responses with optional fields for additional context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp when the error occurred.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * Error type/category (e.g., "Validation Failed", "Not Found").
     */
    private String error;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * The path/endpoint where the error occurred.
     */
    private String path;

    /**
     * Unique identifier for request tracking and debugging.
     */
    private String correlationId;

    /**
     * Validation errors map (field name -> error message).
     * Only populated for validation errors.
     */
    private Map<String, String> validationErrors;

    /**
     * Additional details about the error.
     * Used for providing extra context when appropriate.
     */
    private Map<String, Object> additionalDetails;
}