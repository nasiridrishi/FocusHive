package com.focushive.notification.service;

/**
 * Exception thrown when template validation fails.
 */
public class TemplateValidationException extends RuntimeException {
    public TemplateValidationException(String message) {
        super(message);
    }
}