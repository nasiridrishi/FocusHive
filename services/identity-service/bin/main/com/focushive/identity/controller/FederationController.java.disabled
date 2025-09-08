package com.focushive.identity.controller;

import com.focushive.identity.dto.*;
import com.focushive.identity.service.FederationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Federation and Single Sign-On controller.
 * Provides endpoints for identity federation, SAML SSO, and external identity provider integration.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/federation")
@Tag(name = "Identity Federation & SSO", description = "Cross-service identity federation and single sign-on endpoints")
@RequiredArgsConstructor
public class FederationController {

    private final FederationService federationService;

    @Operation(
            summary = "Get federated identity providers",
            description = "Get list of available external identity providers for federation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Identity providers retrieved successfully")
    })
    @GetMapping("/providers")
    public ResponseEntity<FederationProvidersResponse> getIdentityProviders() {
        log.debug("Federation providers request");
        
        FederationProvidersResponse response = federationService.getAvailableProviders();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Initiate external provider authentication",
            description = "Start authentication flow with an external identity provider"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to external identity provider"),
            @ApiResponse(responseCode = "400", description = "Invalid provider or configuration"),
            @ApiResponse(responseCode = "404", description = "Provider not found")
    })
    @GetMapping("/auth/{providerId}")
    public void initiateExternalAuth(
            @Parameter(description = "External identity provider ID") @PathVariable String providerId,
            @Parameter(description = "Return URL after authentication") @RequestParam(value = "return_url", required = false) String returnUrl,
            @Parameter(description = "State parameter for CSRF protection") @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.info("Initiating external authentication with provider: {}", providerId);
        
        FederationAuthRequest authRequest = FederationAuthRequest.builder()
                .providerId(providerId)
                .returnUrl(returnUrl)
                .state(state)
                .build();
        
        federationService.initiateExternalAuth(authRequest, request, response);
    }

    @Operation(
            summary = "Handle external provider callback",
            description = "Handle the callback from external identity provider after authentication"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirect to application with authentication result"),
            @ApiResponse(responseCode = "400", description = "Invalid callback parameters"),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @GetMapping("/callback/{providerId}")
    public void handleExternalCallback(
            @Parameter(description = "External identity provider ID") @PathVariable String providerId,
            @Parameter(description = "Authorization code") @RequestParam(value = "code", required = false) String code,
            @Parameter(description = "Error code") @RequestParam(value = "error", required = false) String error,
            @Parameter(description = "State parameter") @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.info("Handling external callback from provider: {}", providerId);
        
        FederationCallbackRequest callbackRequest = FederationCallbackRequest.builder()
                .providerId(providerId)
                .code(code)
                .error(error)
                .state(state)
                .build();
        
        federationService.handleExternalCallback(callbackRequest, request, response);
    }

    @Operation(
            summary = "Link external account",
            description = "Link an external identity provider account to the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account linked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid link request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "409", description = "Account already linked")
    })
    @PostMapping("/accounts/link")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<FederationAccountResponse> linkExternalAccount(
            @Valid @RequestBody LinkExternalAccountRequest request,
            Authentication authentication) {
        
        log.info("Link external account request for provider: {}", request.getProviderId());
        
        UUID userId = getUserIdFromAuthentication(authentication);
        FederationAccountResponse response = federationService.linkExternalAccount(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Unlink external account",
            description = "Unlink an external identity provider account from the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account unlinked successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Linked account not found")
    })
    @DeleteMapping("/accounts/{providerId}")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<Void> unlinkExternalAccount(
            @Parameter(description = "External identity provider ID") @PathVariable String providerId,
            Authentication authentication) {
        
        log.info("Unlink external account request for provider: {}", providerId);
        
        UUID userId = getUserIdFromAuthentication(authentication);
        federationService.unlinkExternalAccount(userId, providerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get linked accounts",
            description = "Get list of external accounts linked to the current user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linked accounts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/accounts")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<LinkedAccountsResponse> getLinkedAccounts(Authentication authentication) {
        log.debug("Linked accounts request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        LinkedAccountsResponse response = federationService.getLinkedAccounts(userId);
        return ResponseEntity.ok(response);
    }

    // SAML SSO Endpoints

    @Operation(
            summary = "SAML Metadata",
            description = "Get SAML Identity Provider metadata for SSO configuration"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SAML metadata returned")
    })
    @GetMapping(value = "/saml/metadata", produces = "application/xml")
    public ResponseEntity<String> getSamlMetadata(HttpServletRequest request) {
        log.debug("SAML metadata request");
        
        String metadata = federationService.getSamlMetadata(request);
        return ResponseEntity.ok()
                .header("Content-Type", "application/xml")
                .body(metadata);
    }

    @Operation(
            summary = "SAML SSO endpoint",
            description = "SAML Single Sign-On endpoint for receiving authentication requests"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SAML response generated"),
            @ApiResponse(responseCode = "400", description = "Invalid SAML request")
    })
    @PostMapping("/saml/sso")
    public void handleSamlSso(
            @RequestParam(value = "SAMLRequest", required = false) String samlRequest,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.info("SAML SSO request received");
        
        SamlSsoRequest ssoRequest = SamlSsoRequest.builder()
                .samlRequest(samlRequest)
                .relayState(relayState)
                .build();
        
        federationService.handleSamlSso(ssoRequest, request, response);
    }

    @Operation(
            summary = "SAML SLO endpoint",
            description = "SAML Single Logout endpoint for receiving logout requests"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SAML logout response generated"),
            @ApiResponse(responseCode = "400", description = "Invalid SAML logout request")
    })
    @PostMapping("/saml/slo")
    public void handleSamlSlo(
            @RequestParam(value = "SAMLRequest", required = false) String samlLogoutRequest,
            @RequestParam(value = "SAMLResponse", required = false) String samlLogoutResponse,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        log.info("SAML SLO request received");
        
        SamlSloRequest sloRequest = SamlSloRequest.builder()
                .samlLogoutRequest(samlLogoutRequest)
                .samlLogoutResponse(samlLogoutResponse)
                .relayState(relayState)
                .build();
        
        federationService.handleSamlSlo(sloRequest, request, response);
    }

    // Cross-Service Identity Management

    @Operation(
            summary = "Register service for federation",
            description = "Register a FocusHive service for cross-service identity federation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid service registration"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to register services")
    })
    @PostMapping("/services")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<FederatedServiceResponse> registerService(
            @Valid @RequestBody RegisterFederatedServiceRequest request,
            Authentication authentication) {
        
        log.info("Service registration request for: {}", request.getServiceName());
        
        UUID userId = getUserIdFromAuthentication(authentication);
        FederatedServiceResponse response = federationService.registerService(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get registered services",
            description = "Get list of FocusHive services registered for federation"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Services retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/services")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<FederatedServicesResponse> getRegisteredServices(Authentication authentication) {
        log.debug("Registered services request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        FederatedServicesResponse response = federationService.getRegisteredServices(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Issue service token",
            description = "Issue an identity token for cross-service authentication"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service token issued successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid token request"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "Service not found")
    })
    @PostMapping("/services/{serviceId}/token")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<ServiceTokenResponse> issueServiceToken(
            @Parameter(description = "Federated service ID") @PathVariable String serviceId,
            @Valid @RequestBody ServiceTokenRequest request,
            Authentication authentication) {
        
        log.info("Service token request for service: {}", serviceId);
        
        UUID userId = getUserIdFromAuthentication(authentication);
        ServiceTokenResponse response = federationService.issueServiceToken(userId, serviceId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Validate service token",
            description = "Validate a cross-service identity token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token validation result"),
            @ApiResponse(responseCode = "400", description = "Invalid token validation request")
    })
    @PostMapping("/services/validate-token")
    public ResponseEntity<ServiceTokenValidationResponse> validateServiceToken(
            @Valid @RequestBody ServiceTokenValidationRequest request) {
        
        log.debug("Service token validation request");
        
        ServiceTokenValidationResponse response = federationService.validateServiceToken(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Propagate identity update",
            description = "Propagate identity or persona changes to federated services"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Identity update propagated successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid propagation request")
    })
    @PostMapping("/identity/propagate")
    @SecurityRequirement(name = "JWT")
    public ResponseEntity<IdentityPropagationResponse> propagateIdentityUpdate(
            @Valid @RequestBody IdentityPropagationRequest request,
            Authentication authentication) {
        
        log.info("Identity propagation request");
        
        UUID userId = getUserIdFromAuthentication(authentication);
        IdentityPropagationResponse response = federationService.propagateIdentityUpdate(userId, request);
        return ResponseEntity.ok(response);
    }

    private UUID getUserIdFromAuthentication(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid user authentication", e);
        }
    }
}