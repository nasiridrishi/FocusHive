package com.focushive.buddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when a buddy request contains invalid data or fails validation.
 * This extends BuddyServiceException and is mapped to HTTP 400 responses.
 * Contains additional validation error details.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidBuddyRequestException extends BuddyServiceException {

    private final List<String> validationErrors;

    /**
     * Constructs a new invalid buddy request exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidBuddyRequestException(String message) {
        super(message);
        this.validationErrors = new ArrayList<>();
    }

    /**
     * Constructs a new invalid buddy request exception with the specified detail message and validation errors.
     *
     * @param message          the detail message
     * @param validationErrors list of validation error messages
     */
    public InvalidBuddyRequestException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = new ArrayList<>(validationErrors);
    }

    /**
     * Returns the validation errors associated with this exception.
     *
     * @return list of validation error messages
     */
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
}