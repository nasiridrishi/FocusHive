package com.focushive.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Set;

/**
 * Exception thrown when a token lacks required scopes for an operation.
 * Provides detailed information for constructing proper OAuth2 error responses.
 */
@Getter
public class InsufficientScopeException extends RuntimeException {

    private final String requiredScope;
    private final Set<String> providedScopes;
    private final HttpStatus httpStatus = HttpStatus.FORBIDDEN;
    private final String error = "insufficient_scope";
    private final String wwwAuthenticate;

    /**
     * Constructor for single required scope.
     */
    public InsufficientScopeException(String requiredScope, Set<String> providedScopes) {
        super(String.format("Insufficient scope. Required: %s, Provided: %s",
            requiredScope, providedScopes));
        this.requiredScope = requiredScope;
        this.providedScopes = providedScopes;
        this.wwwAuthenticate = buildWwwAuthenticate(requiredScope);
    }

    /**
     * Constructor for multiple required scopes.
     */
    public InsufficientScopeException(Set<String> requiredScopes, Set<String> providedScopes) {
        super(String.format("Insufficient scopes. Required: %s, Provided: %s",
            requiredScopes, providedScopes));
        this.requiredScope = String.join(" ", requiredScopes);
        this.providedScopes = providedScopes;
        this.wwwAuthenticate = buildWwwAuthenticate(this.requiredScope);
    }

    /**
     * Constructor with custom message.
     */
    public InsufficientScopeException(String message, String requiredScope,
                                     Set<String> providedScopes) {
        super(message);
        this.requiredScope = requiredScope;
        this.providedScopes = providedScopes;
        this.wwwAuthenticate = buildWwwAuthenticate(requiredScope);
    }

    /**
     * Build WWW-Authenticate header value for OAuth2 error response.
     */
    private String buildWwwAuthenticate(String scope) {
        return String.format(
            "Bearer realm=\"focushive\", error=\"insufficient_scope\", " +
            "error_description=\"The access token provided does not have sufficient scope\", " +
            "scope=\"%s\"",
            scope
        );
    }

    /**
     * Get missing scopes (scopes that were required but not provided).
     */
    public Set<String> getMissingScopes(Set<String> requiredScopes) {
        Set<String> missing = new java.util.HashSet<>(requiredScopes);
        missing.removeAll(providedScopes);
        return missing;
    }

    /**
     * Convert to OAuth2 error response body.
     */
    public java.util.Map<String, Object> toOAuth2ErrorResponse() {
        java.util.Map<String, Object> error = new java.util.HashMap<>();
        error.put("error", this.error);
        error.put("error_description", getMessage());
        error.put("required_scope", requiredScope);
        return error;
    }
}