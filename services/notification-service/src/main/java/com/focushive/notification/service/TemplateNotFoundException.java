package com.focushive.notification.service;

/**
 * Exception thrown when a requested template is not found.
 */
public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException(String message) {
        super(message);
    }
}
