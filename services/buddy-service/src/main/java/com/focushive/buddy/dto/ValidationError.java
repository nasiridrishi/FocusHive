package com.focushive.buddy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Represents a field-level validation error.
 * Used in error responses to provide detailed information about validation failures.
 */
@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError {

    /**
     * The field that failed validation.
     */
    private String field;

    /**
     * The validation error message.
     */
    private String message;

    /**
     * The value that was rejected during validation.
     */
    private Object rejectedValue;

    /**
     * Optional error code for programmatic handling.
     */
    private String code;
}