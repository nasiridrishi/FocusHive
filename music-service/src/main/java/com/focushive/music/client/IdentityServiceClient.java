package com.focushive.music.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Feign client for communicating with the Identity Service.
 * 
 * Handles user authentication, token validation, and persona management.
 * Includes circuit breaker patterns for resilience and fallback methods.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@FeignClient(
    name = "identity-service",
    url = "${services.identity-service.url}",
    configuration = FeignClientConfig.class,
    fallback = IdentityServiceClientFallback.class
)
public interface IdentityServiceClient {

    /**
     * Validates a JWT token with the identity service.
     * 
     * @param token The JWT token to validate
     * @return Token validation response containing user and persona information
     */
    @PostMapping("/auth/validate")
    ResponseEntity<TokenValidationResponse> validateToken(@RequestBody ValidateTokenRequest token);

    /**
     * Gets user information by user ID.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return User information
     */
    @GetMapping("/users/{userId}")
    ResponseEntity<UserInfoResponse> getUserInfo(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets the active persona for a user.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return Active persona information
     */
    @GetMapping("/users/{userId}/persona/active")
    ResponseEntity<PersonaResponse> getActivePersona(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Gets all personas for a user.
     * 
     * @param userId The user ID
     * @param authorization Authorization header with JWT token
     * @return List of personas
     */
    @GetMapping("/users/{userId}/personas")
    ResponseEntity<PersonaListResponse> getUserPersonas(
        @PathVariable("userId") UUID userId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Introspects a token to get detailed information.
     * 
     * @param token The token to introspect
     * @return Token introspection response
     */
    @PostMapping("/auth/introspect")
    ResponseEntity<TokenIntrospectionResponse> introspectToken(@RequestBody IntrospectTokenRequest token);

    /**
     * Request object for token validation.
     */
    record ValidateTokenRequest(String token) {}

    /**
     * Response object for token validation.
     */
    record TokenValidationResponse(
        boolean valid,
        UUID userId,
        String username,
        String email,
        UUID personaId,
        String personaName,
        String personaType,
        Map<String, Object> claims
    ) {}

    /**
     * Response object for user information.
     */
    record UserInfoResponse(
        UUID id,
        String username,
        String email,
        String displayName,
        boolean emailVerified,
        Map<String, Object> metadata
    ) {}

    /**
     * Response object for persona information.
     */
    record PersonaResponse(
        UUID id,
        String name,
        String type,
        String description,
        Map<String, Object> preferences,
        boolean isActive
    ) {}

    /**
     * Response object for persona list.
     */
    record PersonaListResponse(
        java.util.List<PersonaResponse> personas
    ) {}

    /**
     * Request object for token introspection.
     */
    record IntrospectTokenRequest(String token) {}

    /**
     * Response object for token introspection.
     */
    record TokenIntrospectionResponse(
        boolean active,
        String sub,
        String iss,
        String aud,
        long exp,
        long iat,
        Map<String, Object> claims
    ) {}
}