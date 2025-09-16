package com.focushive.buddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested buddy or partnership resource is not found.
 * This extends BuddyServiceException and is mapped to HTTP 404 responses.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BuddyNotFoundException extends BuddyServiceException {

    /**
     * Constructs a new buddy not found exception with the specified detail message.
     *
     * @param message the detail message
     */
    public BuddyNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new buddy not found exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public BuddyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new buddy not found exception with resource details.
     * Creates a formatted message like "User not found with id : 'user123'".
     *
     * @param resourceName the name of the resource that was not found
     * @param fieldName    the field name used for lookup
     * @param fieldValue   the field value used for lookup
     */
    public BuddyNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}