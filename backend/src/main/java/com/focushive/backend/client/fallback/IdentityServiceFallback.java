package com.focushive.backend.client.fallback;

import com.focushive.backend.client.IdentityServiceClient;
import com.focushive.backend.client.dto.IdentityDto;
import com.focushive.backend.client.dto.PersonaDto;
import com.focushive.backend.client.dto.TokenValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Fallback implementation for Identity Service client.
 * Provides graceful degradation when Identity Service is unavailable.
 * Uses caching to serve recent data when possible.
 */
@Slf4j
@Component
public class IdentityServiceFallback implements IdentityServiceClient {

    @Override
    public TokenValidationResponse validateToken(String token) {
        log.warn("Identity Service unavailable - falling back to cached validation");
        // Return invalid response to force re-authentication
        return TokenValidationResponse.builder()
                .valid(false)
                .errorMessage("Identity Service temporarily unavailable")
                .build();
    }

    @Override
    @Cacheable(value = "identity-user", key = "#token")
    public IdentityDto getCurrentUser(String token) {
        log.warn("Identity Service unavailable - returning cached user if available");
        // Return null to indicate service unavailable
        // Cache interceptor will return cached value if available
        return null;
    }

    @Override
    @Cacheable(value = "identity", key = "#id")
    public IdentityDto getIdentity(UUID id, String serviceToken) {
        log.warn("Identity Service unavailable - returning cached identity for: {}", id);
        return null;
    }

    @Override
    @Cacheable(value = "identity-email", key = "#email")
    public IdentityDto getIdentityByEmail(String email, String serviceToken) {
        log.warn("Identity Service unavailable - returning cached identity for email: {}", email);
        return null;
    }

    @Override
    @Cacheable(value = "personas", key = "#identityId")
    public List<PersonaDto> getPersonasByIdentity(UUID identityId, String serviceToken) {
        log.warn("Identity Service unavailable - returning cached personas for identity: {}", identityId);
        return Collections.emptyList();
    }

    @Override
    @Cacheable(value = "persona", key = "#id")
    public PersonaDto getPersona(UUID id, String serviceToken) {
        log.warn("Identity Service unavailable - returning cached persona: {}", id);
        return null;
    }

    @Override
    @Cacheable(value = "active-persona", key = "#identityId")
    public PersonaDto getActivePersona(UUID identityId, String serviceToken) {
        log.warn("Identity Service unavailable - returning cached active persona for identity: {}", identityId);
        return null;
    }

    @Override
    public PersonaDto activatePersona(UUID id, String serviceToken) {
        log.error("Cannot activate persona - Identity Service unavailable");
        // Cannot perform write operations in fallback
        throw new RuntimeException("Identity Service temporarily unavailable - cannot activate persona");
    }

    @Override
    public String healthCheck() {
        return "{\"status\":\"DOWN\",\"message\":\"Identity Service unreachable\"}";
    }
}