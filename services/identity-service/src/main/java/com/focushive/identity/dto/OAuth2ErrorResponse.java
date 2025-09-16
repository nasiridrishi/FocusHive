package com.focushive.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * OAuth2 error response DTO according to RFC 6749.
 * Provides standardized error responses for OAuth2 operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OAuth2ErrorResponse {

    /**
     * Error code (required).
     * Standard values: invalid_request, unauthorized_client, access_denied,
     * unsupported_response_type, invalid_scope, server_error, temporarily_unavailable
     */
    @JsonProperty("error")
    private String error;

    /**
     * Human-readable error description (optional).
     */
    @JsonProperty("error_description")
    private String errorDescription;

    /**
     * URI with error information (optional).
     */
    @JsonProperty("error_uri")
    private String errorUri;

    /**
     * OAuth2 state parameter to maintain state between request and callback (optional).
     */
    @JsonProperty("state")
    private String state;

    /**
     * Required scope for the operation (for insufficient_scope errors).
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * Retry after seconds (for rate limit errors).
     */
    @JsonProperty("retry_after")
    private Long retryAfter;

    /**
     * Timestamp when the error occurred.
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Request path that caused the error.
     */
    @JsonProperty("path")
    private String path;

    /**
     * Unique trace ID for error tracking.
     */
    @JsonProperty("trace_id")
    private String traceId;

    /**
     * Validation errors map (for validation failures).
     */
    @JsonProperty("validation_errors")
    private Map<String, String> validationErrors;

    /**
     * Additional details (for debugging, not included in production).
     */
    @JsonProperty("details")
    private Map<String, Object> details;

    /**
     * Create standard invalid_request error.
     */
    public static OAuth2ErrorResponse invalidRequest(String description) {
        return OAuth2ErrorResponse.builder()
            .error("invalid_request")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard unauthorized_client error.
     */
    public static OAuth2ErrorResponse unauthorizedClient(String description) {
        return OAuth2ErrorResponse.builder()
            .error("unauthorized_client")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard access_denied error.
     */
    public static OAuth2ErrorResponse accessDenied(String description) {
        return OAuth2ErrorResponse.builder()
            .error("access_denied")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard unsupported_response_type error.
     */
    public static OAuth2ErrorResponse unsupportedResponseType(String description) {
        return OAuth2ErrorResponse.builder()
            .error("unsupported_response_type")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard invalid_scope error.
     */
    public static OAuth2ErrorResponse invalidScope(String description, String scope) {
        return OAuth2ErrorResponse.builder()
            .error("invalid_scope")
            .errorDescription(description)
            .scope(scope)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard server_error.
     */
    public static OAuth2ErrorResponse serverError(String description) {
        return OAuth2ErrorResponse.builder()
            .error("server_error")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard temporarily_unavailable error.
     */
    public static OAuth2ErrorResponse temporarilyUnavailable(String description, Long retryAfter) {
        return OAuth2ErrorResponse.builder()
            .error("temporarily_unavailable")
            .errorDescription(description)
            .retryAfter(retryAfter)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard invalid_grant error.
     */
    public static OAuth2ErrorResponse invalidGrant(String description) {
        return OAuth2ErrorResponse.builder()
            .error("invalid_grant")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard invalid_client error.
     */
    public static OAuth2ErrorResponse invalidClient(String description) {
        return OAuth2ErrorResponse.builder()
            .error("invalid_client")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard invalid_token error.
     */
    public static OAuth2ErrorResponse invalidToken(String description) {
        return OAuth2ErrorResponse.builder()
            .error("invalid_token")
            .errorDescription(description)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create standard insufficient_scope error.
     */
    public static OAuth2ErrorResponse insufficientScope(String description, String requiredScope) {
        return OAuth2ErrorResponse.builder()
            .error("insufficient_scope")
            .errorDescription(description)
            .scope(requiredScope)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Check if this is a client error (4xx).
     */
    public boolean isClientError() {
        return error != null && (
            error.equals("invalid_request") ||
            error.equals("unauthorized_client") ||
            error.equals("access_denied") ||
            error.equals("unsupported_response_type") ||
            error.equals("invalid_scope") ||
            error.equals("invalid_grant") ||
            error.equals("invalid_client") ||
            error.equals("invalid_token") ||
            error.equals("insufficient_scope")
        );
    }

    /**
     * Check if this is a server error (5xx).
     */
    public boolean isServerError() {
        return error != null && (
            error.equals("server_error") ||
            error.equals("temporarily_unavailable")
        );
    }
}