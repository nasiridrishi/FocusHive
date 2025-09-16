package com.focushive.buddy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a partnership operation conflicts with existing state.
 * This could include duplicate partnerships, conflicting requests, or invalid state transitions.
 * This extends BuddyServiceException and is mapped to HTTP 409 responses.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PartnershipConflictException extends BuddyServiceException {

    private final String conflictType;

    /**
     * Constructs a new partnership conflict exception with the specified detail message.
     *
     * @param message the detail message
     */
    public PartnershipConflictException(String message) {
        super(message);
        this.conflictType = null;
    }

    /**
     * Constructs a new partnership conflict exception with the specified detail message and conflict type.
     *
     * @param message      the detail message
     * @param conflictType the type of conflict that occurred
     */
    public PartnershipConflictException(String message, String conflictType) {
        super(message);
        this.conflictType = conflictType;
    }

    /**
     * Returns the type of conflict that caused this exception.
     *
     * @return the conflict type, or null if not specified
     */
    public String getConflictType() {
        return conflictType;
    }
}