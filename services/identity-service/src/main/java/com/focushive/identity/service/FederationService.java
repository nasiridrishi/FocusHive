package com.focushive.identity.service;

import com.focushive.identity.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Federation and Single Sign-On Service.
 * Manages external identity provider integration, SAML SSO, and cross-service identity federation.
 */
public interface FederationService {

    /**
     * Get list of available external identity providers.
     */
    FederationProvidersResponse getAvailableProviders();

    /**
     * Initiate authentication with external identity provider.
     */
    void initiateExternalAuth(FederationAuthRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException;

    /**
     * Handle callback from external identity provider.
     */
    void handleExternalCallback(FederationCallbackRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException;

    /**
     * Link external account to user.
     */
    FederationAccountResponse linkExternalAccount(UUID userId, LinkExternalAccountRequest request);

    /**
     * Unlink external account from user.
     */
    void unlinkExternalAccount(UUID userId, String providerId);

    /**
     * Get linked external accounts for user.
     */
    LinkedAccountsResponse getLinkedAccounts(UUID userId);

    // SAML SSO Methods

    /**
     * Get SAML metadata for identity provider.
     */
    String getSamlMetadata(HttpServletRequest request);

    /**
     * Handle SAML SSO request.
     */
    void handleSamlSso(SamlSsoRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException;

    /**
     * Handle SAML Single Logout request.
     */
    void handleSamlSlo(SamlSloRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException;

    // Cross-Service Federation Methods

    /**
     * Register a FocusHive service for federation.
     */
    FederatedServiceResponse registerService(UUID userId, RegisterFederatedServiceRequest request);

    /**
     * Get registered federated services.
     */
    FederatedServicesResponse getRegisteredServices(UUID userId);

    /**
     * Issue token for cross-service authentication.
     */
    ServiceTokenResponse issueServiceToken(UUID userId, String serviceId, ServiceTokenRequest request);

    /**
     * Validate cross-service token.
     */
    ServiceTokenValidationResponse validateServiceToken(ServiceTokenValidationRequest request);

    /**
     * Propagate identity updates to federated services.
     */
    IdentityPropagationResponse propagateIdentityUpdate(UUID userId, IdentityPropagationRequest request);
}