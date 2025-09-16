package com.focushive.buddy.exception;

/**
 * Base exception for all buddy service-related errors.
 * This is the parent exception for all domain-specific exceptions in the buddy service.
 */
public class BuddyServiceException extends RuntimeException {

    /**
     * Constructs a new buddy service exception with the specified detail message.
     *
     * @param message the detail message
     */
    public BuddyServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new buddy service exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public BuddyServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}