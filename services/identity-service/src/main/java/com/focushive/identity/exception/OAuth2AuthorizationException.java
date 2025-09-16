package com.focushive.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when OAuth2 authorization fails.
 * This exception is used for all OAuth2-specific authorization errors including:
 * - Invalid client credentials
 * - Unauthorized grant types
 * - Invalid or expired authorization codes
 * - Invalid or expired tokens
 * - Scope validation failures
 * - Rate limit exceeded
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OAuth2AuthorizationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private String errorUri;
    private String state;

    public OAuth2AuthorizationException(String message) {
        super(message);
        this.errorCode = "unauthorized";
        this.httpStatus = HttpStatus.UNAUTHORIZED;
    }

    public OAuth2AuthorizationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.UNAUTHORIZED;
    }

    public OAuth2AuthorizationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.UNAUTHORIZED;
    }

    public OAuth2AuthorizationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public OAuth2AuthorizationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "unauthorized";
        this.httpStatus = HttpStatus.UNAUTHORIZED;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorUri() {
        return errorUri;
    }

    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    /**
     * OAuth2 error codes as per RFC 6749
     */
    public static class ErrorCodes {
        public static final String INVALID_REQUEST = "invalid_request";
        public static final String INVALID_CLIENT = "invalid_client";
        public static final String INVALID_GRANT = "invalid_grant";
        public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
        public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
        public static final String INVALID_SCOPE = "invalid_scope";
        public static final String ACCESS_DENIED = "access_denied";
        public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
        public static final String SERVER_ERROR = "server_error";
        public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";
    }
}