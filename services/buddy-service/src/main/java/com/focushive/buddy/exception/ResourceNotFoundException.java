package com.focushive.buddy.exception;

/**
 * Exception thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found with identifier: %s", resourceType, identifier));
    }

    public ResourceNotFoundException(String resourceType, String field, String value) {
        super(String.format("%s not found with %s: %s", resourceType, field, value));
    }
}