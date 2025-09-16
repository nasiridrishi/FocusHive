package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * Request DTO for updating an existing OAuth2 client.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ClientUpdateRequest {

    /**
     * Display name for the client.
     */
    @Size(max = 255, message = "Client name must not exceed 255 characters")
    private String clientName;

    /**
     * Description of the client.
     */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Redirect URIs for authorization code flow.
     */
    private Set<String> redirectUris;

    /**
     * Authorized grant types.
     */
    private Set<String> grantTypes;

    /**
     * Authorized scopes.
     */
    private Set<String> scopes;

    /**
     * Access token validity in seconds.
     */
    private Integer accessTokenValiditySeconds;

    /**
     * Refresh token validity in seconds.
     */
    private Integer refreshTokenValiditySeconds;

    /**
     * Whether to auto-approve scopes (skip consent).
     */
    private Boolean autoApprove;

    /**
     * Whether the client is trusted (system client).
     */
    private Boolean trusted;

    /**
     * Whether PKCE is required for this client.
     */
    private Boolean requirePkce;

    /**
     * Whether the client is enabled.
     */
    private Boolean enabled;

    /**
     * Contact email for the client.
     */
    private String contactEmail;

    /**
     * Client website URL.
     */
    private String websiteUrl;

    /**
     * Client logo URL.
     */
    private String logoUrl;

    /**
     * Terms of service URL.
     */
    private String tosUrl;

    /**
     * Privacy policy URL.
     */
    private String privacyPolicyUrl;

    /**
     * Additional metadata.
     */
    private String metadata;

    /**
     * Check if any fields are being updated.
     */
    public boolean hasUpdates() {
        return clientName != null ||
               description != null ||
               redirectUris != null ||
               grantTypes != null ||
               scopes != null ||
               accessTokenValiditySeconds != null ||
               refreshTokenValiditySeconds != null ||
               autoApprove != null ||
               trusted != null ||
               requirePkce != null ||
               enabled != null ||
               contactEmail != null ||
               websiteUrl != null ||
               logoUrl != null ||
               tosUrl != null ||
               privacyPolicyUrl != null ||
               metadata != null;
    }

    /**
     * Validate redirect URIs format if provided.
     */
    public boolean validateRedirectUris() {
        if (redirectUris == null) {
            return true; // Not updating redirect URIs
        }

        if (redirectUris.isEmpty()) {
            return false; // Can't have empty redirect URIs
        }

        for (String uri : redirectUris) {
            if (!isValidRedirectUri(uri)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a redirect URI is valid.
     */
    private boolean isValidRedirectUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return false;
        }

        // Allow localhost for development
        if (uri.startsWith("http://localhost") || uri.startsWith("https://localhost")) {
            return true;
        }

        // Must be HTTPS for production
        if (!uri.startsWith("https://")) {
            return false;
        }

        // No fragments allowed
        if (uri.contains("#")) {
            return false;
        }

        return true;
    }

    /**
     * Validate grant types if provided.
     */
    public boolean validateGrantTypes() {
        if (grantTypes == null) {
            return true; // Not updating grant types
        }

        if (grantTypes.isEmpty()) {
            return false; // Can't have empty grant types
        }

        Set<String> validGrantTypes = Set.of(
            "authorization_code",
            "refresh_token",
            "client_credentials",
            "implicit" // Deprecated but still supported
        );

        return validGrantTypes.containsAll(grantTypes);
    }
}