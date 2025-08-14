package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 Authorization Server Service.
 * Provides OAuth2 authorization server capabilities for the Identity Service.
 */
public interface OAuth2AuthorizationService {

    /**
     * Handle OAuth2 authorization request.
     * Validates the authorization request and either redirects to login or generates authorization code.
     */
    void authorize(OAuth2AuthorizeRequest request, Authentication authentication,
                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException;

    /**
     * Handle OAuth2 token request.
     * Exchange authorization code for access token or refresh existing token.
     */
    OAuth2TokenResponse token(OAuth2TokenRequest request, HttpServletRequest httpRequest);

    /**
     * Handle OAuth2 token introspection.
     * Validate and return information about a token (RFC 7662).
     */
    OAuth2IntrospectionResponse introspect(OAuth2IntrospectionRequest request);

    /**
     * Handle OAuth2 token revocation.
     * Revoke access or refresh tokens (RFC 7009).
     */
    void revoke(OAuth2RevocationRequest request);

    /**
     * Get user information for valid access token.
     * Returns user profile information based on granted scopes.
     */
    OAuth2UserInfoResponse getUserInfo(String authorizationHeader);

    /**
     * Get OAuth2 authorization server metadata.
     * Returns server capabilities and endpoints (RFC 8414).
     */
    OAuth2ServerMetadata getServerMetadata(HttpServletRequest request);

    /**
     * Get JSON Web Key Set for token verification.
     */
    Map<String, Object> getJwkSet();

    /**
     * Register a new OAuth2 client dynamically.
     */
    OAuth2ClientResponse registerClient(OAuth2ClientRegistrationRequest request, Authentication authentication);

    /**
     * Get OAuth2 clients for the authenticated user.
     */
    OAuth2ClientListResponse getUserClients(Authentication authentication);

    /**
     * Update an existing OAuth2 client.
     */
    OAuth2ClientResponse updateClient(String clientId, OAuth2ClientUpdateRequest request, Authentication authentication);

    /**
     * Delete an OAuth2 client.
     */
    void deleteClient(String clientId, Authentication authentication);

    /**
     * Validate OAuth2 client credentials.
     */
    boolean validateClientCredentials(String clientId, String clientSecret);

    /**
     * Generate authorization code for authenticated user.
     */
    String generateAuthorizationCode(String clientId, String redirectUri, String scope, Authentication authentication);

    /**
     * Validate authorization code and return associated information.
     */
    OAuth2AuthorizationCode validateAuthorizationCode(String code, String clientId, String redirectUri);

    /**
     * Generate access token for user and client.
     */
    OAuth2TokenResponse generateAccessToken(String userId, String clientId, String scope);

    /**
     * Validate access token and return token information.
     */
    OAuth2TokenInfo validateAccessToken(String accessToken);
}