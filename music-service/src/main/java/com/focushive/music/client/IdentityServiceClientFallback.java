package com.focushive.music.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * Fallback implementation for Identity Service client.
 * 
 * Provides resilient fallback responses when the Identity Service is unavailable.
 * Implements circuit breaker pattern for graceful degradation.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class IdentityServiceClientFallback implements IdentityServiceClient {

    private static final String FALLBACK_MESSAGE = "Identity Service is currently unavailable. Please try again later.";

    @Override
    public ResponseEntity<TokenValidationResponse> validateToken(ValidateTokenRequest token) {
        log.warn("Identity Service fallback: validateToken called");
        // Return invalid token response - security first approach
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new TokenValidationResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyMap()
            ));
    }

    @Override
    public ResponseEntity<UserInfoResponse> getUserInfo(UUID userId, String authorization) {
        log.warn("Identity Service fallback: getUserInfo called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<PersonaResponse> getActivePersona(UUID userId, String authorization) {
        log.warn("Identity Service fallback: getActivePersona called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<PersonaListResponse> getUserPersonas(UUID userId, String authorization) {
        log.warn("Identity Service fallback: getUserPersonas called for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new PersonaListResponse(Collections.emptyList()));
    }

    @Override
    public ResponseEntity<TokenIntrospectionResponse> introspectToken(IntrospectTokenRequest token) {
        log.warn("Identity Service fallback: introspectToken called");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new TokenIntrospectionResponse(
                false,
                null,
                null,
                null,
                0,
                0,
                Collections.emptyMap()
            ));
    }
}