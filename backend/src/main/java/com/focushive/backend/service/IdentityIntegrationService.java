package com.focushive.backend.service;

import com.focushive.backend.client.IdentityServiceClient;
import com.focushive.backend.client.dto.IdentityDto;
import com.focushive.backend.client.dto.PersonaDto;
import com.focushive.backend.client.dto.TokenValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for integrating with Identity Service microservice.
 * Handles authentication delegation, persona management, and caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityIntegrationService {

    private final IdentityServiceClient identityServiceClient;

    /**
     * Validate a JWT token with the Identity Service.
     * Not cached as token validation should always be fresh.
     */
    public TokenValidationResponse validateToken(String token) {
        log.debug("Validating token with Identity Service");
        try {
            TokenValidationResponse response = identityServiceClient.validateToken(token);
            if (response.isValid()) {
                log.debug("Token validated successfully for user: {}", response.getUserId());
            } else {
                log.warn("Token validation failed: {}", response.getErrorMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Error validating token with Identity Service", e);
            return TokenValidationResponse.builder()
                    .valid(false)
                    .errorMessage("Failed to validate token: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get current user from token.
     * Cached for 5 minutes to reduce calls to Identity Service.
     */
    @Cacheable(value = "current-user", key = "#token", unless = "#result == null")
    public IdentityDto getCurrentUser(String token) {
        log.debug("Fetching current user from Identity Service");
        try {
            return identityServiceClient.getCurrentUser(token);
        } catch (Exception e) {
            log.error("Error fetching current user from Identity Service", e);
            return null;
        }
    }

    /**
     * Get identity by ID.
     * Cached for 10 minutes.
     */
    @Cacheable(value = "identity", key = "#id", unless = "#result == null")
    public IdentityDto getIdentity(UUID id, String serviceToken) {
        log.debug("Fetching identity {} from Identity Service", id);
        try {
            return identityServiceClient.getIdentity(id, serviceToken);
        } catch (Exception e) {
            log.error("Error fetching identity {} from Identity Service", id, e);
            return null;
        }
    }

    /**
     * Get identity by email.
     * Cached for 10 minutes.
     */
    @Cacheable(value = "identity-email", key = "#email", unless = "#result == null")
    public IdentityDto getIdentityByEmail(String email, String serviceToken) {
        log.debug("Fetching identity by email {} from Identity Service", email);
        try {
            return identityServiceClient.getIdentityByEmail(email, serviceToken);
        } catch (Exception e) {
            log.error("Error fetching identity by email {} from Identity Service", email, e);
            return null;
        }
    }

    /**
     * Get personas for an identity.
     * Cached for 5 minutes.
     */
    @Cacheable(value = "personas", key = "#identityId", unless = "#result == null || #result.isEmpty()")
    public List<PersonaDto> getPersonasByIdentity(UUID identityId, String serviceToken) {
        log.debug("Fetching personas for identity {} from Identity Service", identityId);
        try {
            return identityServiceClient.getPersonasByIdentity(identityId, serviceToken);
        } catch (Exception e) {
            log.error("Error fetching personas for identity {} from Identity Service", identityId, e);
            return List.of();
        }
    }

    /**
     * Get active persona for an identity.
     * Cached for 2 minutes as this can change frequently.
     */
    @Cacheable(value = "active-persona", key = "#identityId", unless = "#result == null")
    public PersonaDto getActivePersona(UUID identityId, String serviceToken) {
        log.debug("Fetching active persona for identity {} from Identity Service", identityId);
        try {
            return identityServiceClient.getActivePersona(identityId, serviceToken);
        } catch (Exception e) {
            log.error("Error fetching active persona for identity {} from Identity Service", identityId, e);
            return null;
        }
    }

    /**
     * Activate a persona.
     * Clears relevant caches after activation.
     */
    @CacheEvict(value = {"active-persona", "personas"}, key = "#identityId")
    public PersonaDto activatePersona(UUID personaId, UUID identityId, String serviceToken) {
        log.info("Activating persona {} for identity {}", personaId, identityId);
        try {
            PersonaDto activated = identityServiceClient.activatePersona(personaId, serviceToken);
            log.info("Successfully activated persona {} for identity {}", personaId, identityId);
            return activated;
        } catch (Exception e) {
            log.error("Error activating persona {} for identity {}", personaId, identityId, e);
            throw new RuntimeException("Failed to activate persona: " + e.getMessage(), e);
        }
    }

    /**
     * Check health of Identity Service.
     */
    public boolean isIdentityServiceHealthy() {
        try {
            String health = identityServiceClient.healthCheck();
            return health != null && health.contains("\"status\":\"UP\"");
        } catch (Exception e) {
            log.warn("Identity Service health check failed", e);
            return false;
        }
    }

    /**
     * Clear all caches for an identity.
     * Used when significant changes occur.
     */
    @CacheEvict(value = {"identity", "identity-email", "personas", "active-persona", "current-user"}, allEntries = true)
    public void clearIdentityCaches() {
        log.info("Clearing all identity-related caches");
    }
}