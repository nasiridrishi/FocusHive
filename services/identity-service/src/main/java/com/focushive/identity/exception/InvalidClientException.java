package com.focushive.identity.exception;

/**
 * Exception thrown when client authentication fails.
 */
public class InvalidClientException extends OAuth2AuthorizationException {

    public InvalidClientException(String message) {
        super("invalid_client", message);
    }

    public InvalidClientException(String message, Throwable cause) {
        super("invalid_client", message, cause);
    }

    public InvalidClientException(String clientId, String message) {
        super("invalid_client", message);
        // Could store clientId for logging
    }
}