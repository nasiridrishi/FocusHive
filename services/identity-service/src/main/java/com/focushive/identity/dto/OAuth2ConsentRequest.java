package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for processing OAuth2 consent decisions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ConsentRequest {

    /**
     * User ID granting or denying consent.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * Client ID requesting consent.
     */
    @NotBlank(message = "Client ID is required")
    private String clientId;

    /**
     * Whether the user approved the consent request.
     */
    @NotNull(message = "Approval decision is required")
    private boolean approved;

    /**
     * Scopes that were requested by the client.
     */
    @NotNull(message = "Requested scopes are required")
    @Builder.Default
    private Set<String> requestedScopes = new LinkedHashSet<>();

    /**
     * Scopes that the user approved (subset of requested scopes).
     */
    @Builder.Default
    private Set<String> approvedScopes = new LinkedHashSet<>();

    /**
     * Scopes that the user explicitly denied.
     */
    @Builder.Default
    private Set<String> deniedScopes = new LinkedHashSet<>();

    /**
     * Whether to remember this consent decision for future requests.
     */
    @Builder.Default
    private boolean rememberConsent = false;

    /**
     * The redirect URI to return to after consent.
     */
    private String redirectUri;

    /**
     * The state parameter from the original authorization request.
     */
    private String state;

    /**
     * The response type from the original authorization request.
     */
    private String responseType;

    /**
     * The code challenge for PKCE (if applicable).
     */
    private String codeChallenge;

    /**
     * The code challenge method for PKCE (if applicable).
     */
    private String codeChallengeMethod;

    /**
     * IP address of the user granting consent.
     */
    private String ipAddress;

    /**
     * User agent of the browser granting consent.
     */
    private String userAgent;

    /**
     * Session ID associated with the consent.
     */
    private String sessionId;

    /**
     * Additional metadata about the consent.
     */
    private String metadata;

    /**
     * Validate that approved scopes are subset of requested scopes.
     */
    public boolean isValid() {
        if (approved && approvedScopes != null) {
            return requestedScopes.containsAll(approvedScopes);
        }
        return true;
    }

    /**
     * Check if any scopes were approved.
     */
    public boolean hasApprovedScopes() {
        return approved && approvedScopes != null && !approvedScopes.isEmpty();
    }

    /**
     * Get the effective scopes (approved scopes if any, otherwise empty).
     */
    public Set<String> getEffectiveScopes() {
        if (approved && approvedScopes != null && !approvedScopes.isEmpty()) {
            return approvedScopes;
        }
        return new LinkedHashSet<>();
    }
}