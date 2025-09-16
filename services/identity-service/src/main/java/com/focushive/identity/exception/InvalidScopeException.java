package com.focushive.identity.exception;

/**
 * Exception thrown when requested OAuth2 scopes are invalid or unknown.
 */
public class InvalidScopeException extends OAuth2AuthorizationException {

    private String requestedScope;

    public InvalidScopeException(String message) {
        super("invalid_scope", message);
    }

    public InvalidScopeException(String message, String requestedScope) {
        super("invalid_scope", message);
        this.requestedScope = requestedScope;
    }

    public InvalidScopeException(String message, String requestedScope, Throwable cause) {
        super("invalid_scope", message, cause);
        this.requestedScope = requestedScope;
    }

    public String getRequestedScope() {
        return requestedScope;
    }
}