package com.focushive.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a rate limit is exceeded.
 * Contains information about when the client can retry the request.
 */
@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final String clientId;
    private final String endpoint;
    private final Long remainingRequests;
    private final Integer rateLimit;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.clientId = null;
        this.endpoint = null;
        this.remainingRequests = null;
        this.rateLimit = null;
    }

    public RateLimitExceededException(String message, long retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
        this.clientId = null;
        this.endpoint = null;
        this.remainingRequests = null;
        this.rateLimit = null;
    }

    public RateLimitExceededException(String clientId, String endpoint, long retryAfterSeconds,
                                      Long remainingRequests, Integer rateLimit) {
        super(String.format("Rate limit exceeded for client %s on endpoint %s. Retry after %d seconds.",
            clientId != null ? clientId : "anonymous", endpoint, retryAfterSeconds));
        this.clientId = clientId;
        this.endpoint = endpoint;
        this.retryAfterSeconds = retryAfterSeconds;
        this.remainingRequests = remainingRequests;
        this.rateLimit = rateLimit;
    }
}