package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Request DTO for registering a new OAuth2 client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2ClientRequest {

    /**
     * Unique client identifier.
     */
    @NotBlank(message = "Client ID is required")
    @Size(min = 3, max = 100, message = "Client ID must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Client ID can only contain letters, numbers, hyphens, and underscores")
    private String clientId;

    /**
     * Client secret (will be hashed before storage).
     * If not provided, one will be generated.
     */
    private String clientSecret;

    /**
     * Display name for the client.
     */
    @NotBlank(message = "Client name is required")
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
    @NotNull(message = "At least one redirect URI is required")
    @Size(min = 1, message = "At least one redirect URI is required")
    @Builder.Default
    private Set<String> redirectUris = new LinkedHashSet<>();

    /**
     * Authorized grant types.
     */
    @NotNull(message = "At least one grant type is required")
    @Size(min = 1, message = "At least one grant type is required")
    @Builder.Default
    private Set<String> grantTypes = new LinkedHashSet<>();

    /**
     * Authorized scopes.
     */
    @Builder.Default
    private Set<String> scopes = new LinkedHashSet<>();

    /**
     * Access token validity in seconds.
     */
    @Builder.Default
    private Integer accessTokenValiditySeconds = 3600; // 1 hour

    /**
     * Refresh token validity in seconds.
     */
    @Builder.Default
    private Integer refreshTokenValiditySeconds = 2592000; // 30 days

    /**
     * Whether to auto-approve scopes (skip consent).
     */
    @Builder.Default
    private boolean autoApprove = false;

    /**
     * Whether the client is trusted (system client).
     */
    @Builder.Default
    private boolean trusted = false;

    /**
     * Whether PKCE is required for this client.
     */
    @Builder.Default
    private boolean requirePkce = false;

    /**
     * Whether the client is enabled.
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Client type (public or confidential).
     */
    @NotNull(message = "Client type is required")
    @Builder.Default
    private ClientType clientType = ClientType.CONFIDENTIAL;

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
     * Client type enumeration.
     */
    public enum ClientType {
        PUBLIC,       // JavaScript apps, mobile apps
        CONFIDENTIAL  // Server-side apps
    }

    /**
     * Validate redirect URIs format.
     */
    public boolean validateRedirectUris() {
        if (redirectUris == null || redirectUris.isEmpty()) {
            return false;
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
     * Validate grant types.
     */
    public boolean validateGrantTypes() {
        if (grantTypes == null || grantTypes.isEmpty()) {
            return false;
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