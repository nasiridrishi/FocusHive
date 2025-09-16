package com.focushive.buddy.exception;

/**
 * Exception thrown when a user is not authorized to perform an action.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(String userId, String resource, String action) {
        super(String.format("User %s is not authorized to %s %s", userId, action, resource));
    }
}