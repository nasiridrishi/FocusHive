package com.focushive.api.client;

import com.focushive.api.dto.identity.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Feign client for communicating with the Identity Service.
 * Handles authentication, user management, and persona operations.
 * Configured with circuit breaker, retry, and other resilience patterns.
 */
@FeignClient(
    name = "identity-service",
    url = "${identity.service.url}",
    fallback = IdentityServiceFallback.class,
    configuration = FeignConfiguration.class
)
@Profile("!test") // Don't create this bean in test profile
public interface IdentityServiceClient {
    
    // Authentication endpoints
    
    @PostMapping("/api/v1/auth/validate")
    TokenValidationResponse validateToken(@RequestHeader("Authorization") String token);
    
    @PostMapping("/api/v1/auth/refresh")
    TokenRefreshResponse refreshToken(@RequestBody TokenRefreshRequest request);
    
    @PostMapping("/api/v1/auth/introspect")
    TokenIntrospectionResponse introspectToken(@RequestBody TokenIntrospectionRequest request);
    
    // User management endpoints
    
    @GetMapping("/api/v1/users/{id}")
    UserDto getUser(@PathVariable("id") UUID id);
    
    @GetMapping("/api/v1/users/by-email/{email}")
    UserDto getUserByEmail(@PathVariable("email") String email);
    
    @PutMapping("/api/v1/users/{id}")
    UserDto updateUser(@PathVariable("id") UUID id, @RequestBody UpdateUserRequest request);
    
    // Persona management endpoints
    
    @GetMapping("/api/v1/users/{userId}/personas")
    PersonaListResponse getUserPersonas(@PathVariable("userId") UUID userId);
    
    @GetMapping("/api/v1/users/{userId}/active-persona")
    PersonaDto getActivePersona(@PathVariable("userId") UUID userId);
    
    @PostMapping("/api/v1/users/{userId}/personas/switch")
    PersonaDto switchPersona(@PathVariable("userId") UUID userId, @RequestBody SwitchPersonaRequest request);
    
    // Service health check
    
    @GetMapping("/actuator/health")
    HealthResponse checkHealth();
}