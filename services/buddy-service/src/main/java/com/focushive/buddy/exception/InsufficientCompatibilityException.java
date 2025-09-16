package com.focushive.buddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when two users have insufficient compatibility for a partnership.
 * This extends BuddyServiceException and is mapped to HTTP 422 responses.
 * Contains compatibility score information.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InsufficientCompatibilityException extends BuddyServiceException {

    private final Double actualScore;
    private final Double requiredScore;

    /**
     * Constructs a new insufficient compatibility exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InsufficientCompatibilityException(String message) {
        super(message);
        this.actualScore = null;
        this.requiredScore = null;
    }

    /**
     * Constructs a new insufficient compatibility exception with compatibility scores.
     *
     * @param message       the detail message
     * @param actualScore   the actual compatibility score calculated
     * @param requiredScore the minimum required compatibility score
     */
    public InsufficientCompatibilityException(String message, Double actualScore, Double requiredScore) {
        super(message);
        this.actualScore = actualScore;
        this.requiredScore = requiredScore;
    }

    /**
     * Returns the actual compatibility score that was calculated.
     *
     * @return the actual compatibility score, or null if not specified
     */
    public Double getActualScore() {
        return actualScore;
    }

    /**
     * Returns the minimum required compatibility score.
     *
     * @return the required compatibility score, or null if not specified
     */
    public Double getRequiredScore() {
        return requiredScore;
    }
}