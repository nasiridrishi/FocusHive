package com.focushive.identity.service;

import com.focushive.identity.controller.OAuth2ClientManagementController.OAuth2ClientStatistics;
import com.focushive.identity.controller.OAuth2ClientManagementController.TokenRevocationResult;
import com.focushive.identity.dto.OAuth2ClientRequest;
import com.focushive.identity.dto.OAuth2ClientResponse;
import com.focushive.identity.dto.OAuth2ClientUpdateRequest;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.exception.OAuth2AuthorizationException;
import com.focushive.identity.metrics.OAuth2MetricsService;
import com.focushive.identity.repository.OAuthAccessTokenRepository;
import com.focushive.identity.repository.OAuthClientRepository;
import com.focushive.identity.repository.OAuthRefreshTokenRepository;
import com.focushive.identity.repository.OAuth2SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing OAuth2 clients.
 * Handles client registration, updates, deletion, and statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ClientManagementService {

    private final OAuthClientRepository clientRepository;
    private final OAuthAccessTokenRepository accessTokenRepository;
    private final OAuthRefreshTokenRepository refreshTokenRepository;
    private final OAuth2SessionRepository sessionRepository;
    private final OAuth2AuditService auditService;
    private final OAuth2MetricsService metricsService;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Register a new OAuth2 client.
     */
    @Transactional
    public OAuth2ClientResponse registerClient(OAuth2ClientRequest request) {
        log.info("Registering new OAuth2 client: {}", request.getClientId());

        // Validate request
        if (!request.validateRedirectUris()) {
            throw new IllegalArgumentException("Invalid redirect URIs");
        }

        if (!request.validateGrantTypes()) {
            throw new IllegalArgumentException("Invalid grant types");
        }

        // Check if client ID already exists
        if (clientRepository.existsByClientId(request.getClientId())) {
            throw new IllegalArgumentException("Client ID already exists: " + request.getClientId());
        }

        // Generate client secret if not provided
        String clientSecret = request.getClientSecret();
        if (clientSecret == null || clientSecret.isEmpty()) {
            clientSecret = generateClientSecret();
        }

        // Create client entity
        OAuthClient client = OAuthClient.builder()
            .clientId(request.getClientId())
            .clientSecret(passwordEncoder.encode(clientSecret))
            .clientName(request.getClientName())
            .description(request.getDescription())
            .redirectUris(new LinkedHashSet<>(request.getRedirectUris()))
            .authorizedGrantTypes(new LinkedHashSet<>(request.getGrantTypes()))
            .authorizedScopes(request.getScopes() != null ? new LinkedHashSet<>(request.getScopes()) : new LinkedHashSet<>())
            .accessTokenValiditySeconds(request.getAccessTokenValiditySeconds())
            .refreshTokenValiditySeconds(request.getRefreshTokenValiditySeconds())
            .autoApprove(request.isAutoApprove())
            .trusted(request.isTrusted())
            .requirePkce(request.isRequirePkce())
            .enabled(request.isEnabled())
            .createdAt(Instant.now())
            .build();

        client = clientRepository.save(client);

        // Audit log
        auditService.logClientRegistration(client.getClientId(), auditService.getClientIpAddress());

        log.info("Successfully registered OAuth2 client: {}", client.getClientId());

        // Return response with generated secret (only time it's returned in plaintext)
        OAuth2ClientResponse response = mapToResponse(client);
        response.setClientSecret(clientSecret); // Include plaintext secret in registration response only
        return response;
    }

    /**
     * Get OAuth2 client by ID.
     */
    @Transactional(readOnly = true)
    public OAuth2ClientResponse getClient(String clientId) {
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        return mapToResponse(client);
    }

    /**
     * List OAuth2 clients with pagination.
     */
    @Transactional(readOnly = true)
    public Page<OAuth2ClientResponse> listClients(Pageable pageable, Boolean enabled, String searchTerm) {
        Page<OAuthClient> clients;

        if (searchTerm != null && !searchTerm.isEmpty()) {
            if (enabled != null) {
                clients = clientRepository.searchByNameAndEnabled(searchTerm, enabled, pageable);
            } else {
                clients = clientRepository.searchByName(searchTerm, pageable);
            }
        } else if (enabled != null) {
            clients = clientRepository.findByEnabled(enabled, pageable);
        } else {
            clients = clientRepository.findAll(pageable);
        }

        return clients.map(this::mapToResponse);
    }

    /**
     * Update OAuth2 client.
     */
    @Transactional
    public OAuth2ClientResponse updateClient(String clientId, OAuth2ClientUpdateRequest request) {
        log.info("Updating OAuth2 client: {}", clientId);

        if (!request.hasUpdates()) {
            throw new IllegalArgumentException("No updates provided");
        }

        // Validate request
        if (!request.validateRedirectUris()) {
            throw new IllegalArgumentException("Invalid redirect URIs");
        }

        if (!request.validateGrantTypes()) {
            throw new IllegalArgumentException("Invalid grant types");
        }

        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        // Update fields if provided
        if (request.getClientName() != null) {
            client.setClientName(request.getClientName());
        }
        if (request.getDescription() != null) {
            client.setDescription(request.getDescription());
        }
        if (request.getRedirectUris() != null) {
            client.setRedirectUris(new LinkedHashSet<>(request.getRedirectUris()));
        }
        if (request.getGrantTypes() != null) {
            client.setAuthorizedGrantTypes(new LinkedHashSet<>(request.getGrantTypes()));
        }
        if (request.getScopes() != null) {
            client.setAuthorizedScopes(new LinkedHashSet<>(request.getScopes()));
        }
        if (request.getAccessTokenValiditySeconds() != null) {
            client.setAccessTokenValiditySeconds(request.getAccessTokenValiditySeconds());
        }
        if (request.getRefreshTokenValiditySeconds() != null) {
            client.setRefreshTokenValiditySeconds(request.getRefreshTokenValiditySeconds());
        }
        if (request.getAutoApprove() != null) {
            client.setAutoApprove(request.getAutoApprove());
        }
        if (request.getTrusted() != null) {
            client.setTrusted(request.getTrusted());
        }
        if (request.getRequirePkce() != null) {
            client.setRequirePkce(request.getRequirePkce());
        }
        if (request.getEnabled() != null) {
            client.setEnabled(request.getEnabled());
        }

        client.setUpdatedAt(Instant.now());
        client = clientRepository.save(client);

        // Audit log
        auditService.logClientUpdate(client.getClientId(), auditService.getClientIpAddress());

        log.info("Successfully updated OAuth2 client: {}", client.getClientId());

        return mapToResponse(client);
    }

    /**
     * Delete OAuth2 client.
     */
    @Transactional
    public void deleteClient(String clientId) {
        log.info("Deleting OAuth2 client: {}", clientId);

        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        // Check for active tokens
        long activeTokens = accessTokenRepository.countActiveTokensByClient(client.getId());
        if (activeTokens > 0) {
            throw new IllegalStateException("Cannot delete client with active tokens. Revoke tokens first.");
        }

        // Delete the client
        clientRepository.delete(client);

        // Audit log
        auditService.logClientDeletion(clientId, auditService.getClientIpAddress());

        log.info("Successfully deleted OAuth2 client: {}", clientId);
    }

    /**
     * Regenerate client secret.
     */
    @Transactional
    public OAuth2ClientResponse regenerateClientSecret(String clientId) {
        log.info("Regenerating secret for OAuth2 client: {}", clientId);

        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        // Generate new secret
        String newSecret = generateClientSecret();
        client.setClientSecret(passwordEncoder.encode(newSecret));
        client.setSecretRotatedAt(Instant.now());
        client.setUpdatedAt(Instant.now());

        client = clientRepository.save(client);

        // Audit log
        auditService.logClientSecretRotation(clientId, auditService.getClientIpAddress());

        log.info("Successfully regenerated secret for OAuth2 client: {}", clientId);

        // Return response with new secret (only time it's returned in plaintext)
        OAuth2ClientResponse response = mapToResponse(client);
        response.setClientSecret(newSecret); // Include plaintext secret in response
        return response;
    }

    /**
     * Update client status (enable/disable).
     */
    @Transactional
    public OAuth2ClientResponse updateClientStatus(String clientId, boolean enabled) {
        log.info("Updating status for OAuth2 client {}: enabled={}", clientId, enabled);

        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        client.setEnabled(enabled);
        client.setUpdatedAt(Instant.now());
        client = clientRepository.save(client);

        // Audit log
        String action = enabled ? "enabled" : "disabled";
        auditService.logClientStatusChange(clientId, action, auditService.getClientIpAddress());

        log.info("Successfully {} OAuth2 client: {}", action, clientId);

        return mapToResponse(client);
    }

    /**
     * Get client statistics.
     */
    @Transactional(readOnly = true)
    public OAuth2ClientStatistics getClientStatistics(String clientId) {
        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        long activeTokens = accessTokenRepository.countActiveTokensByClient(client.getId());
        long activeSessions = sessionRepository.countActiveSessionsByUser(client.getId(), Instant.now());

        // TODO: Implement more detailed statistics from audit logs

        return OAuth2ClientStatistics.builder()
            .clientId(clientId)
            .activeTokens(activeTokens)
            .activeSessions(activeSessions)
            .lastUsed(client.getLastUsedAt() != null ? client.getLastUsedAt().toString() : "Never")
            .createdAt(client.getCreatedAt().toString())
            .build();
    }

    /**
     * Revoke all tokens for a client.
     */
    @Transactional
    public TokenRevocationResult revokeAllClientTokens(String clientId, String reason) {
        log.info("Revoking all tokens for OAuth2 client: {}", clientId);

        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        String revocationReason = reason != null ? reason : "admin_action";

        // Revoke access tokens
        int accessTokensRevoked = accessTokenRepository.revokeAllTokensForClient(
            client.getId(), Instant.now(), revocationReason
        );

        // Revoke refresh tokens
        int refreshTokensRevoked = refreshTokenRepository.revokeAllTokensForClient(
            client.getId(), Instant.now(), revocationReason
        );

        // Terminate sessions
        int sessionsTerminated = sessionRepository.terminateAllSessionsForClient(
            client.getId(), Instant.now(), revocationReason
        );

        // Audit log
        auditService.logClientTokensRevoked(clientId, accessTokensRevoked + refreshTokensRevoked,
            revocationReason, auditService.getClientIpAddress());

        log.info("Revoked {} access tokens, {} refresh tokens, and terminated {} sessions for client {}",
                 accessTokensRevoked, refreshTokensRevoked, sessionsTerminated, clientId);

        return TokenRevocationResult.builder()
            .clientId(clientId)
            .accessTokensRevoked(accessTokensRevoked)
            .refreshTokensRevoked(refreshTokensRevoked)
            .sessionsTerminated(sessionsTerminated)
            .reason(revocationReason)
            .timestamp(Instant.now().toString())
            .build();
    }

    /**
     * Update client scopes.
     */
    @Transactional
    public OAuth2ClientResponse updateClientScopes(String clientId, List<String> scopes) {
        log.info("Updating scopes for OAuth2 client: {}", clientId);

        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        client.setAuthorizedScopes(new LinkedHashSet<>(scopes));
        client.setUpdatedAt(Instant.now());
        client = clientRepository.save(client);

        // Audit log
        auditService.logClientUpdate(clientId, auditService.getClientIpAddress());

        log.info("Successfully updated scopes for OAuth2 client: {}", clientId);

        return mapToResponse(client);
    }

    /**
     * Update client redirect URIs.
     */
    @Transactional
    public OAuth2ClientResponse updateClientRedirectUris(String clientId, List<String> redirectUris) {
        log.info("Updating redirect URIs for OAuth2 client: {}", clientId);

        // Validate redirect URIs
        for (String uri : redirectUris) {
            if (!isValidRedirectUri(uri)) {
                throw new IllegalArgumentException("Invalid redirect URI: " + uri);
            }
        }

        OAuthClient client = clientRepository.findByClientId(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Client not found: " + clientId));

        client.setRedirectUris(new LinkedHashSet<>(redirectUris));
        client.setUpdatedAt(Instant.now());
        client = clientRepository.save(client);

        // Audit log
        auditService.logClientUpdate(clientId, auditService.getClientIpAddress());

        log.info("Successfully updated redirect URIs for OAuth2 client: {}", clientId);

        return mapToResponse(client);
    }

    /**
     * Generate a secure client secret.
     */
    private String generateClientSecret() {
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
    }

    /**
     * Validate redirect URI format.
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
     * Map client entity to response DTO.
     */
    private OAuth2ClientResponse mapToResponse(OAuthClient client) {
        return OAuth2ClientResponse.builder()
            .clientId(client.getClientId())
            // Never include client secret in responses (except registration/regeneration)
            .clientName(client.getClientName())
            .description(client.getDescription())
            .redirectUris(new LinkedHashSet<>(client.getRedirectUris()))
            .grantTypes(new LinkedHashSet<>(client.getAuthorizedGrantTypes()))
            .scopes(new LinkedHashSet<>(client.getAuthorizedScopes()))
            .accessTokenValiditySeconds(client.getAccessTokenValiditySeconds())
            .refreshTokenValiditySeconds(client.getRefreshTokenValiditySeconds())
            .autoApprove(client.isAutoApprove())
            .trusted(client.isTrusted())
            .requirePkce(client.isRequirePkce())
            .enabled(client.isEnabled())
            .createdAt(client.getCreatedAt())
            .updatedAt(client.getUpdatedAt())
            .lastUsedAt(client.getLastUsedAt())
            .build();
    }
}