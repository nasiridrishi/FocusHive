package com.focushive.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an account is locked due to security policies.
 * Implements OWASP A04:2021 Insecure Design protection.
 * Returns 423 Locked status as per RFC 4918.
 */
@ResponseStatus(value = HttpStatus.LOCKED, reason = "Account locked")
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }

    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}