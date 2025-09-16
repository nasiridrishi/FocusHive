package com.focushive.identity.controller;

import com.focushive.identity.dto.OAuth2ClientRequest;
import com.focushive.identity.dto.OAuth2ClientResponse;
import com.focushive.identity.dto.OAuth2ClientUpdateRequest;
import com.focushive.identity.service.OAuth2ClientManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for OAuth2 client management.
 * Provides endpoints for creating, updating, deleting, and managing OAuth2 clients.
 */
@RestController
@RequestMapping("/api/admin/oauth2/clients")
@Tag(name = "OAuth2 Client Management", description = "OAuth2 client management endpoints (admin only)")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class OAuth2ClientManagementController {

    private final OAuth2ClientManagementService clientManagementService;

    /**
     * Register a new OAuth2 client.
     */
    @PostMapping
    @Operation(summary = "Register new OAuth2 client",
               description = "Creates a new OAuth2 client with the provided configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Client created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "409", description = "Client ID already exists")
    })
    public ResponseEntity<OAuth2ClientResponse> registerClient(
            @Valid @RequestBody OAuth2ClientRequest request) {

        log.info("Registering new OAuth2 client: {}", request.getClientId());
        OAuth2ClientResponse response = clientManagementService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get OAuth2 client by ID.
     */
    @GetMapping("/{clientId}")
    @Operation(summary = "Get OAuth2 client",
               description = "Retrieves an OAuth2 client by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Client retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ClientResponse> getClient(
            @Parameter(description = "Client ID") @PathVariable String clientId) {

        log.debug("Retrieving OAuth2 client: {}", clientId);
        OAuth2ClientResponse response = clientManagementService.getClient(clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * List all OAuth2 clients.
     */
    @GetMapping
    @Operation(summary = "List OAuth2 clients",
               description = "Retrieves a paginated list of all OAuth2 clients")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Clients retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<Page<OAuth2ClientResponse>> listClients(
            @Parameter(description = "Pagination parameters") Pageable pageable,
            @Parameter(description = "Filter by enabled status") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Search by name") @RequestParam(required = false) String searchTerm) {

        log.debug("Listing OAuth2 clients with page: {}, enabled: {}, search: {}",
                 pageable, enabled, searchTerm);
        Page<OAuth2ClientResponse> clients = clientManagementService.listClients(pageable, enabled, searchTerm);
        return ResponseEntity.ok(clients);
    }

    /**
     * Update OAuth2 client.
     */
    @PutMapping("/{clientId}")
    @Operation(summary = "Update OAuth2 client",
               description = "Updates an existing OAuth2 client configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Client updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ClientResponse> updateClient(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @Valid @RequestBody OAuth2ClientUpdateRequest request) {

        log.info("Updating OAuth2 client: {}", clientId);
        OAuth2ClientResponse response = clientManagementService.updateClient(clientId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete OAuth2 client.
     */
    @DeleteMapping("/{clientId}")
    @Operation(summary = "Delete OAuth2 client",
               description = "Permanently deletes an OAuth2 client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Client deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found"),
        @ApiResponse(responseCode = "409", description = "Client has active tokens or sessions")
    })
    public ResponseEntity<Void> deleteClient(
            @Parameter(description = "Client ID") @PathVariable String clientId) {

        log.info("Deleting OAuth2 client: {}", clientId);
        clientManagementService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Regenerate client secret.
     */
    @PostMapping("/{clientId}/secret")
    @Operation(summary = "Regenerate client secret",
               description = "Generates a new secret for the OAuth2 client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Secret regenerated successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ClientResponse> regenerateClientSecret(
            @Parameter(description = "Client ID") @PathVariable String clientId) {

        log.info("Regenerating secret for OAuth2 client: {}", clientId);
        OAuth2ClientResponse response = clientManagementService.regenerateClientSecret(clientId);
        return ResponseEntity.ok(response);
    }

    /**
     * Enable or disable OAuth2 client.
     */
    @PatchMapping("/{clientId}/status")
    @Operation(summary = "Update client status",
               description = "Enables or disables an OAuth2 client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ClientResponse> updateClientStatus(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @Parameter(description = "New status") @RequestParam boolean enabled) {

        log.info("Updating status for OAuth2 client {}: enabled={}", clientId, enabled);
        OAuth2ClientResponse response = clientManagementService.updateClientStatus(clientId, enabled);
        return ResponseEntity.ok(response);
    }

    /**
     * Get client statistics.
     */
    @GetMapping("/{clientId}/statistics")
    @Operation(summary = "Get client statistics",
               description = "Retrieves usage statistics for an OAuth2 client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ClientStatistics> getClientStatistics(
            @Parameter(description = "Client ID") @PathVariable String clientId) {

        log.debug("Retrieving statistics for OAuth2 client: {}", clientId);
        OAuth2ClientStatistics statistics = clientManagementService.getClientStatistics(clientId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Revoke all tokens for a client.
     */
    @PostMapping("/{clientId}/revoke-tokens")
    @Operation(summary = "Revoke all client tokens",
               description = "Revokes all access and refresh tokens issued to this client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tokens revoked successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<TokenRevocationResult> revokeAllClientTokens(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @Parameter(description = "Revocation reason") @RequestParam(required = false) String reason) {

        log.info("Revoking all tokens for OAuth2 client: {}", clientId);
        TokenRevocationResult result = clientManagementService.revokeAllClientTokens(clientId, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * Update client scopes.
     */
    @PutMapping("/{clientId}/scopes")
    @Operation(summary = "Update client scopes",
               description = "Updates the authorized scopes for an OAuth2 client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Scopes updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid scopes"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ClientResponse> updateClientScopes(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @Parameter(description = "New scopes") @RequestBody List<String> scopes) {

        log.info("Updating scopes for OAuth2 client: {}", clientId);
        OAuth2ClientResponse response = clientManagementService.updateClientScopes(clientId, scopes);
        return ResponseEntity.ok(response);
    }

    /**
     * Update client redirect URIs.
     */
    @PutMapping("/{clientId}/redirect-uris")
    @Operation(summary = "Update redirect URIs",
               description = "Updates the authorized redirect URIs for an OAuth2 client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Redirect URIs updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid redirect URIs"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public ResponseEntity<OAuth2ClientResponse> updateClientRedirectUris(
            @Parameter(description = "Client ID") @PathVariable String clientId,
            @Parameter(description = "New redirect URIs") @RequestBody List<String> redirectUris) {

        log.info("Updating redirect URIs for OAuth2 client: {}", clientId);
        OAuth2ClientResponse response = clientManagementService.updateClientRedirectUris(clientId, redirectUris);
        return ResponseEntity.ok(response);
    }

    /**
     * DTO for client statistics.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OAuth2ClientStatistics {
        private String clientId;
        private long activeTokens;
        private long activeSessions;
        private long totalRequests;
        private long successfulAuthentications;
        private long failedAuthentications;
        private long revokedTokens;
        private long activeUsers;
        private String lastUsed;
        private String createdAt;
    }

    /**
     * DTO for token revocation result.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TokenRevocationResult {
        private String clientId;
        private int accessTokensRevoked;
        private int refreshTokensRevoked;
        private int sessionsTerminated;
        private String reason;
        private String timestamp;
    }
}