package com.focushive.buddy.exception;

/**
 * Exception thrown when a service is temporarily unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {
    
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceUnavailableException(String service, int retryAfterSeconds) {
        super(String.format("%s is temporarily unavailable. Please retry after %d seconds", service, retryAfterSeconds));
    }
}