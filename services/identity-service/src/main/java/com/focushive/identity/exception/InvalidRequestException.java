package com.focushive.identity.exception;

/**
 * Exception thrown when an OAuth2 request is malformed or missing required parameters.
 */
public class InvalidRequestException extends OAuth2AuthorizationException {

    public InvalidRequestException(String message) {
        super("invalid_request", message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super("invalid_request", message, cause);
    }

    public InvalidRequestException(String message, String errorUri) {
        super("invalid_request", message);
        super.setErrorUri(errorUri);
    }
}