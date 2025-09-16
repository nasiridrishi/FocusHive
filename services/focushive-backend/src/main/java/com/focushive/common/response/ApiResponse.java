package com.focushive.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API response wrapper for consistent response structure across all endpoints.
 * Provides success/error handling with optional metadata and timestamps.
 *
 * @param <T> the type of the response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * Indicates if the request was successful
     */
    private boolean success;

    /**
     * Message providing additional context about the response
     */
    private String message;

    /**
     * The actual response data
     */
    private T data;

    /**
     * Additional metadata that might be useful for the client
     */
    private Map<String, Object> metadata;

    /**
     * Timestamp when the response was created
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Creates a successful response with data
     *
     * @param data the response data
     * @param <T> the type of the response data
     * @return ApiResponse with success=true and the provided data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a successful response with data and message
     *
     * @param data the response data
     * @param message success message
     * @param <T> the type of the response data
     * @return ApiResponse with success=true, data, and message
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
     * Creates a successful response with data, message, and metadata
     *
     * @param data the response data
     * @param message success message
     * @param metadata additional metadata
     * @param <T> the type of the response data
     * @return ApiResponse with success=true, data, message, and metadata
     */
    public static <T> ApiResponse<T> success(T data, String message, Map<String, Object> metadata) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with message
     *
     * @param message error message
     * @param <T> the type of the response data
     * @return ApiResponse with success=false and error message
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with message and metadata
     *
     * @param message error message
     * @param metadata additional error metadata
     * @param <T> the type of the response data
     * @return ApiResponse with success=false, error message, and metadata
     */
    public static <T> ApiResponse<T> error(String message, Map<String, Object> metadata) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }
}