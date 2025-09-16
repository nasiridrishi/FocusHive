package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for OAuth2 consent page data.
 * Contains all necessary information to display the consent page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ConsentPageData {

    /**
     * The OAuth2 client requesting consent.
     */
    private String clientId;

    /**
     * The client's display name.
     */
    private String clientName;

    /**
     * The client's description.
     */
    private String clientDescription;

    /**
     * The scopes being requested.
     */
    private Set<String> requestedScopes;

    /**
     * Existing consent if any.
     */
    private OAuth2ConsentResponse existingConsent;

    /**
     * OAuth2 state parameter.
     */
    private String state;

    /**
     * Redirect URI for the authorization flow.
     */
    private String redirectUri;

    /**
     * OAuth2 response type.
     */
    private String responseType;

    /**
     * PKCE code challenge.
     */
    private String codeChallenge;

    /**
     * PKCE code challenge method.
     */
    private String codeChallengeMethod;

    /**
     * Check if user has already consented to all requested scopes.
     */
    public boolean hasFullConsent() {
        if (existingConsent == null || !existingConsent.isGranted()) {
            return false;
        }
        return existingConsent.hasAllScopes(requestedScopes);
    }

    /**
     * Check if this is a new consent request.
     */
    public boolean isNewConsent() {
        return existingConsent == null || !existingConsent.isGranted();
    }

    /**
     * Check if additional scopes are being requested.
     */
    public boolean hasAdditionalScopes() {
        if (existingConsent == null || !existingConsent.isGranted()) {
            return true;
        }
        // Check if any requested scope is not already granted
        for (String scope : requestedScopes) {
            if (!existingConsent.hasScope(scope)) {
                return true;
            }
        }
        return false;
    }
}