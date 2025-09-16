package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Response DTO for OAuth2 consent operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ConsentResponse {

    /**
     * Client ID the consent is for.
     */
    private String clientId;

    /**
     * Client name (for display).
     */
    private String clientName;

    /**
     * Whether consent was granted.
     */
    private boolean granted;

    /**
     * Scopes that were granted.
     */
    @Builder.Default
    private Set<String> grantedScopes = new LinkedHashSet<>();

    /**
     * Scopes that were denied.
     */
    @Builder.Default
    private Set<String> deniedScopes = new LinkedHashSet<>();

    /**
     * When the consent was granted.
     */
    private Instant grantedAt;

    /**
     * When the consent expires.
     */
    private Instant expiresAt;

    /**
     * Whether the consent is remembered.
     */
    private boolean rememberConsent;

    /**
     * Error code if consent was denied.
     */
    private String errorCode;

    /**
     * Error description if consent was denied.
     */
    private String errorDescription;

    /**
     * Additional metadata.
     */
    private String metadata;

    /**
     * Create a granted consent response.
     */
    public static OAuth2ConsentResponse granted(String clientId, Set<String> grantedScopes, Instant expiresAt) {
        return OAuth2ConsentResponse.builder()
            .clientId(clientId)
            .granted(true)
            .grantedScopes(grantedScopes)
            .grantedAt(Instant.now())
            .expiresAt(expiresAt)
            .build();
    }

    /**
     * Create a denied consent response.
     */
    public static OAuth2ConsentResponse denied(String clientId, String errorDescription) {
        return OAuth2ConsentResponse.builder()
            .clientId(clientId)
            .granted(false)
            .errorCode("access_denied")
            .errorDescription(errorDescription)
            .build();
    }

    /**
     * Check if consent is still valid.
     */
    public boolean isValid() {
        if (!granted) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return false;
        }
        return true;
    }

    /**
     * Check if consent includes a specific scope.
     */
    public boolean hasScope(String scope) {
        return granted && grantedScopes != null && grantedScopes.contains(scope);
    }

    /**
     * Check if consent includes all specified scopes.
     */
    public boolean hasAllScopes(Set<String> scopes) {
        return granted && grantedScopes != null && grantedScopes.containsAll(scopes);
    }
}