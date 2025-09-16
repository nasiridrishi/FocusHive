package com.focushive.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * JWT token claims for OAuth2 tokens.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenClaims {

    /**
     * Subject (user ID or client ID for client credentials)
     */
    private String subject;

    /**
     * User ID (for user tokens)
     */
    private String userId;

    /**
     * Username
     */
    private String username;

    /**
     * Email address
     */
    private String email;

    /**
     * User roles
     */
    private Set<String> roles;

    /**
     * OAuth2 scopes
     */
    private Set<String> scopes;

    /**
     * Client ID that requested the token
     */
    private String clientId;

    /**
     * Audience (resource servers)
     */
    private String audience;

    /**
     * Active persona ID
     */
    private String personaId;

    /**
     * Persona type
     */
    private String personaType;

    /**
     * Custom claims
     */
    private java.util.Map<String, Object> customClaims;
}