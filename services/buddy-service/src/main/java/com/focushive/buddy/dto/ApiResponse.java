package com.focushive.buddy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic API response wrapper for all buddy service endpoints.
 * Provides consistent response structure across the service.
 *
 * @param <T> the type of data being returned
 */
@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indicates if the operation was successful.
     */
    private boolean success;

    /**
     * The response message.
     */
    private String message;

    /**
     * The response data payload.
     */
    private T data;

    /**
     * Single error message for simple error responses.
     */
    private String error;

    /**
     * List of error messages, if any.
     */
    private List<String> errors;

    /**
     * Map of validation errors with field names as keys.
     */
    private Object validationErrors;

    /**
     * Timestamp when the response was created.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a successful response with data and default success message.
     *
     * @param data the response data
     * @param <T>  the type of data
     * @return successful API response
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Success");
    }

    /**
     * Creates a successful response with data and custom message.
     *
     * @param data    the response data
     * @param message the success message
     * @param <T>     the type of data
     * @return successful API response
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Creates an error response with message.
     *
     * @param message the error message
     * @param <T>     the expected data type
     * @return error API response
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .error(message)
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * Creates an error response with message and validation errors.
     *
     * @param message the error message
     * @param errors  list of validation errors
     * @param <T>     the expected data type
     * @return error API response
     */
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .errors(errors)
            .timestamp(LocalDateTime.now())
            .build();
    }
}