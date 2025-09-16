package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for creating an OAuth2 session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2SessionRequest {

    /**
     * User ID for the session.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * Client ID requesting the session.
     */
    @NotBlank(message = "Client ID is required")
    private String clientId;

    /**
     * Authentication method used (password, social, mfa, etc.).
     */
    @NotBlank(message = "Authentication method is required")
    private String authMethod;

    /**
     * Authentication level (basic, elevated, mfa).
     */
    @Builder.Default
    private String authLevel = "basic";

    /**
     * IP address of the client.
     */
    private String ipAddress;

    /**
     * User agent string.
     */
    private String userAgent;

    /**
     * Device fingerprint for enhanced security.
     */
    private String deviceFingerprint;

    /**
     * Requested session duration in hours.
     */
    private Long durationHours;

    /**
     * OAuth2 scopes granted in this session.
     */
    private String grantedScopes;

    /**
     * ID token if using OpenID Connect.
     */
    private String idToken;

    /**
     * Additional metadata for the session.
     */
    private String metadata;

    /**
     * The refresh token ID associated with this session.
     */
    private UUID refreshTokenId;

    /**
     * Whether this is a remember me session.
     */
    @Builder.Default
    private boolean rememberMe = false;

    /**
     * Check if this is an elevated authentication session.
     */
    public boolean isElevated() {
        return "elevated".equals(authLevel) || "mfa".equals(authLevel);
    }

    /**
     * Check if this is an MFA authenticated session.
     */
    public boolean isMfa() {
        return "mfa".equals(authLevel);
    }
}