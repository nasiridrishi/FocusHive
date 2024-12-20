package com.focushive.api.client;

import com.focushive.api.dto.identity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback implementation for Identity Service client.
 * Provides graceful degradation when Identity Service is unavailable.
 */
@Slf4j
@Component
@Profile("!test") // Don't load this in test profile
public class IdentityServiceFallback implements IdentityServiceClient {
    
    // Token validation is now done locally using JwtValidator
    // validateToken method removed as endpoint doesn't exist

    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        log.error("Identity Service is unavailable - token refresh failed");
        throw new RuntimeException("Cannot refresh token - Identity Service unavailable");
    }

    // Token introspection endpoint not implemented in Identity Service
    // introspectToken method removed
    
    @Override
    public UserDto getUser(UUID id) {
        log.error("Identity Service is unavailable - cannot fetch user with id: {}", id);
        return null;
    }
    
    @Override
    public UserDto getUserByEmail(String email) {
        log.error("Identity Service is unavailable - cannot fetch user with email: {}", email);
        return null;
    }
    
    @Override
    public UserDto updateUser(UUID id, UpdateUserRequest request) {
        log.error("Identity Service is unavailable - cannot update user with id: {}", id);
        throw new RuntimeException("Cannot update user - Identity Service unavailable");
    }
    
    @Override
    public PersonaListResponse getUserPersonas(UUID userId) {
        log.error("Identity Service is unavailable - cannot fetch personas for user: {}", userId);
        return PersonaListResponse.builder()
                .personas(java.util.Collections.emptyList())
                .totalCount(0)
                .build();
    }
    
    @Override
    public PersonaDto getActivePersona(UUID userId) {
        log.error("Identity Service is unavailable - cannot fetch active persona for user: {}", userId);
        return null;
    }
    
    @Override
    public PersonaDto switchPersona(UUID userId, SwitchPersonaRequest request) {
        log.error("Identity Service is unavailable - cannot switch persona for user: {}", userId);
        throw new RuntimeException("Cannot switch persona - Identity Service unavailable");
    }
    
    @Override
    public ActuatorHealthResponse checkHealth() {
        log.error("Identity Service health check failed");
        return ActuatorHealthResponse.builder()
                .status("DOWN")
                .components(new java.util.HashMap<>())
                .build();
    }
}