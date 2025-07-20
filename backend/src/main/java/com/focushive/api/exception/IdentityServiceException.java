package com.focushive.api.exception;

/**
 * Exception thrown when Identity Service communication fails.
 */
public class IdentityServiceException extends RuntimeException {
    
    private final int statusCode;
    
    public IdentityServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public IdentityServiceException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}