package com.focushive.buddy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Standardized error response for buddy service endpoints.
 * Provides detailed error information including field-level validation errors.
 */
@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * The error message describing what went wrong.
     */
    private String message;

    /**
     * The request path where the error occurred.
     */
    private String path;

    /**
     * The HTTP status code.
     */
    private int status;

    /**
     * List of validation errors, if any.
     */
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();

    /**
     * Timestamp when the error occurred.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}