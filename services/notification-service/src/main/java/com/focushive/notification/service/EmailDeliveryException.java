package com.focushive.notification.service;

/**
 * Exception thrown when email delivery fails.
 */
public class EmailDeliveryException extends RuntimeException {
    
    public EmailDeliveryException(String message) {
        super(message);
    }
    
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}