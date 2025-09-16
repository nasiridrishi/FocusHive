package com.focushive.identity.exception;

/**
 * Exception thrown when an OAuth2 grant is invalid.
 * This includes expired authorization codes, invalid refresh tokens, etc.
 */
public class InvalidGrantException extends OAuth2AuthorizationException {

    public InvalidGrantException(String message) {
        super("invalid_grant", message);
    }

    public InvalidGrantException(String message, Throwable cause) {
        super("invalid_grant", message, cause);
    }

    public InvalidGrantException(String message, String state) {
        super("invalid_grant", message);
        super.setState(state);
    }
}