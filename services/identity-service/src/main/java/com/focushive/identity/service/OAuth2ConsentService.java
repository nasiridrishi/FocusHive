package com.focushive.identity.service;

import com.focushive.identity.audit.OAuth2AuditEvent;
import com.focushive.identity.dto.OAuth2ConsentRequest;
import com.focushive.identity.dto.OAuth2ConsentResponse;
import com.focushive.identity.entity.OAuth2Consent;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.OAuth2AuthorizationException;
import com.focushive.identity.metrics.OAuth2MetricsService;
import com.focushive.identity.repository.OAuth2ConsentRepository;
import com.focushive.identity.repository.OAuthClientRepository;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing OAuth2 user consent.
 * Handles consent prompts, storage, validation, and revocation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ConsentService {

    private final OAuth2ConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final OAuthClientRepository clientRepository;
    private final OAuth2AuditService auditService;
    private final OAuth2MetricsService metricsService;

    @Value("${oauth2.consent.default-expiry-hours:8760}") // 1 year default
    private long defaultConsentExpiryHours;

    @Value("${oauth2.consent.remember-duration-days:90}")
    private long rememberConsentDays;

    @Value("${oauth2.consent.require-explicit:true}")
    private boolean requireExplicitConsent;

    @Value("${oauth2.consent.allow-partial:true}")
    private boolean allowPartialConsent;

    /**
     * Check if user consent is required for the given client and scopes.
     */
    public boolean isConsentRequired(UUID userId, String clientId, Set<String> requestedScopes) {
        if (!requireExplicitConsent) {
            return false;
        }

        // Find the client
        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Invalid client"));

        // Check if client is trusted (no consent required)
        if (client.isTrusted()) {
            log.debug("Client {} is trusted, no consent required", clientId);
            return false;
        }

        // Check existing consent
        Optional<OAuth2Consent> existingConsent = consentRepository.findValidConsent(
            userId, client.getId(), Instant.now()
        );

        if (existingConsent.isEmpty()) {
            log.debug("No existing consent found for user {} and client {}", userId, clientId);
            return true;
        }

        OAuth2Consent consent = existingConsent.get();

        // Check if all requested scopes are already granted
        boolean allScopesGranted = consent.hasAllScopes(requestedScopes);
        if (!allScopesGranted) {
            log.debug("Additional scopes requested that are not in existing consent");
            return true;
        }

        // Check if consent should be remembered
        if (!consent.isRememberConsent()) {
            log.debug("Consent exists but remember flag is false, prompting again");
            return true;
        }

        return false;
    }

    /**
     * Process user consent decision.
     */
    @Transactional
    public OAuth2ConsentResponse processConsent(OAuth2ConsentRequest request) {
        log.info("Processing consent for user {} and client {}", request.getUserId(), request.getClientId());

        // Validate user and client
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new OAuth2AuthorizationException("User not found"));

        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(request.getClientId())
            .orElseThrow(() -> new OAuth2AuthorizationException("Invalid client"));

        // Check if user approved or denied
        if (!request.isApproved()) {
            handleConsentDenial(user, client, request);
            return OAuth2ConsentResponse.denied(request.getClientId(), "User denied consent");
        }

        // Find or create consent record
        OAuth2Consent consent = consentRepository.findByUserIdAndClientId(user.getId(), client.getId())
            .orElseGet(() -> createNewConsent(user, client, request));

        // Update consent with approved scopes
        updateConsentScopes(consent, request.getApprovedScopes(), request.getDeniedScopes());

        // Set remember preference
        consent.setRememberConsent(request.isRememberConsent());

        // Set expiration
        if (request.isRememberConsent()) {
            consent.setExpiresAt(Instant.now().plus(rememberConsentDays, ChronoUnit.DAYS));
        } else {
            consent.setExpiresAt(Instant.now().plus(defaultConsentExpiryHours, ChronoUnit.HOURS));
        }

        // Update metadata
        consent.setGrantedIp(request.getIpAddress());
        consent.setUserAgent(request.getUserAgent());
        consent.setSessionId(request.getSessionId());

        // Save consent
        consent = consentRepository.save(consent);

        // Audit log
        auditService.logConsentGranted(
            user.getId(),
            client.getClientId(),
            consent.getGrantedScopes(),
            request.getIpAddress()
        );

        // Metrics
        metricsService.recordConsentGranted(client.getClientId());

        log.info("Consent granted for user {} to client {} with scopes: {}",
            user.getUsername(), client.getClientId(), consent.getGrantedScopes());

        return OAuth2ConsentResponse.granted(
            request.getClientId(),
            consent.getGrantedScopes(),
            consent.getExpiresAt()
        );
    }

    /**
     * Handle consent denial.
     */
    private void handleConsentDenial(User user, OAuthClient client, OAuth2ConsentRequest request) {
        log.info("User {} denied consent to client {}", user.getUsername(), client.getClientId());

        // Find existing consent if any
        Optional<OAuth2Consent> existingConsent = consentRepository.findByUserIdAndClientId(
            user.getId(), client.getId()
        );

        if (existingConsent.isPresent()) {
            // Revoke existing consent
            OAuth2Consent consent = existingConsent.get();
            consent.revoke("user_denied");
            consentRepository.save(consent);
        }

        // Audit log
        auditService.logConsentDenied(
            user.getId(),
            client.getClientId(),
            request.getRequestedScopes(),
            request.getIpAddress()
        );

        // Metrics
        metricsService.recordConsentDenied(client.getClientId());
    }

    /**
     * Create new consent record.
     */
    private OAuth2Consent createNewConsent(User user, OAuthClient client, OAuth2ConsentRequest request) {
        return OAuth2Consent.builder()
            .user(user)
            .client(client)
            .grantedScopes(new LinkedHashSet<>())
            .deniedScopes(new LinkedHashSet<>())
            .grantedIp(request.getIpAddress())
            .userAgent(request.getUserAgent())
            .sessionId(request.getSessionId())
            .rememberConsent(request.isRememberConsent())
            .build();
    }

    /**
     * Update consent scopes.
     */
    private void updateConsentScopes(OAuth2Consent consent, Set<String> approvedScopes, Set<String> deniedScopes) {
        // Clear and update granted scopes
        consent.getGrantedScopes().clear();
        if (approvedScopes != null) {
            consent.getGrantedScopes().addAll(approvedScopes);
        }

        // Clear and update denied scopes
        consent.getDeniedScopes().clear();
        if (deniedScopes != null) {
            consent.getDeniedScopes().addAll(deniedScopes);
        }

        consent.setUpdatedAt(Instant.now());
    }

    /**
     * Revoke consent for a user and client.
     */
    @Transactional
    public boolean revokeConsent(UUID userId, String clientId, String reason) {
        log.info("Revoking consent for user {} and client {}", userId, clientId);

        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Invalid client"));

        int revoked = consentRepository.revokeConsent(userId, client.getId(), Instant.now(), reason);

        if (revoked > 0) {
            // Audit log
            auditService.logConsentRevoked(userId, clientId, reason, auditService.getClientIpAddress());

            // Metrics
            metricsService.recordConsentRevoked(clientId);

            log.info("Successfully revoked consent for user {} and client {}", userId, clientId);
            return true;
        }
        return false;
    }

    /**
     * Revoke all consents for a user.
     */
    @Transactional
    public void revokeAllConsentsForUser(UUID userId, String reason) {
        log.info("Revoking all consents for user {}", userId);

        int revoked = consentRepository.revokeAllConsentsForUser(userId, Instant.now(), reason);

        if (revoked > 0) {
            // Audit log
            auditService.logAllConsentsRevoked(userId, reason, auditService.getClientIpAddress());

            log.info("Successfully revoked {} consents for user {}", revoked, userId);
        }
    }

    /**
     * Get all valid consents for a user.
     */
    @Transactional(readOnly = true)
    public List<OAuth2ConsentResponse> getUserConsents(UUID userId) {
        List<OAuth2Consent> consents = consentRepository.findValidConsentsByUser(userId, Instant.now());

        return consents.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get consent details for a specific user and client.
     */
    @Transactional(readOnly = true)
    public Optional<OAuth2ConsentResponse> getConsent(UUID userId, String clientId) {
        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(clientId)
            .orElse(null);

        if (client == null) {
            return Optional.empty();
        }

        return consentRepository.findValidConsent(userId, client.getId(), Instant.now())
            .map(this::mapToResponse);
    }

    /**
     * Validate that user has granted required scopes.
     */
    @Transactional(readOnly = true)
    public boolean validateConsent(UUID userId, String clientId, Set<String> requiredScopes) {
        if (!requireExplicitConsent) {
            return true;
        }

        OAuthClient client = clientRepository.findByClientIdAndEnabledTrue(clientId)
            .orElseThrow(() -> new OAuth2AuthorizationException("Invalid client"));

        // Trusted clients don't need consent
        if (client.isTrusted()) {
            return true;
        }

        Optional<OAuth2Consent> consent = consentRepository.findValidConsent(
            userId, client.getId(), Instant.now()
        );

        if (consent.isEmpty()) {
            return false;
        }

        return consent.get().hasAllScopes(requiredScopes);
    }

    /**
     * Clean up expired consents.
     */
    @Transactional
    public int cleanupExpiredConsents() {
        int deleted = consentRepository.deleteExpiredConsents(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired consents", deleted);
        }
        return deleted;
    }

    /**
     * Get consent statistics for monitoring.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConsentStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConsents", consentRepository.count());
        stats.put("expiringConsents", consentRepository.findConsentsNeedingRenewal(
            Instant.now(),
            Instant.now().plus(7, ChronoUnit.DAYS)
        ).size());
        return stats;
    }

    /**
     * Map consent entity to response DTO.
     */
    private OAuth2ConsentResponse mapToResponse(OAuth2Consent consent) {
        return OAuth2ConsentResponse.builder()
            .clientId(consent.getClient().getClientId())
            .clientName(consent.getClient().getClientName())
            .grantedScopes(new LinkedHashSet<>(consent.getGrantedScopes()))
            .deniedScopes(new LinkedHashSet<>(consent.getDeniedScopes()))
            .grantedAt(consent.getGrantedAt())
            .expiresAt(consent.getExpiresAt())
            .rememberConsent(consent.isRememberConsent())
            .build();
    }
}