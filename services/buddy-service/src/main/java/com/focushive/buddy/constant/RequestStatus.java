package com.focushive.buddy.constant;

/**
 * Enumeration of buddy request statuses.
 * Represents the current state of a buddy partnership request.
 */
public enum RequestStatus {

    PENDING(false, true),
    ACCEPTED(true, false),
    DECLINED(true, false),
    EXPIRED(true, false),
    WITHDRAWN(true, false);

    private final boolean isFinal;
    private final boolean canRespond;

    RequestStatus(boolean isFinal, boolean canRespond) {
        this.isFinal = isFinal;
        this.canRespond = canRespond;
    }

    /**
     * Checks if this status is a final state (no further changes allowed).
     *
     * @return true if this is a final status, false otherwise
     */
    public boolean isFinal() {
        return isFinal;
    }

    /**
     * Checks if the recipient can still respond to a request in this status.
     *
     * @return true if the request can be responded to, false otherwise
     */
    public boolean canRespond() {
        return canRespond;
    }
}