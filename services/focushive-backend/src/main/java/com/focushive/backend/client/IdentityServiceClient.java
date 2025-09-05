package com.focushive.backend.client;

import com.focushive.backend.client.dto.IdentityDto;
import com.focushive.backend.client.dto.PersonaDto;
import com.focushive.backend.client.dto.TokenValidationResponse;
import com.focushive.backend.client.fallback.IdentityServiceFallback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for communication with Identity Service microservice.
 * Implements circuit breaker pattern and automatic retry logic.
 */
@FeignClient(
    name = "identity-service",
    url = "${identity.service.url:http://localhost:8081}",
    fallback = IdentityServiceFallback.class
)
public interface IdentityServiceClient {

    // Authentication endpoints
    
    @PostMapping("/api/v1/auth/validate")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    TokenValidationResponse validateToken(@RequestHeader("Authorization") String token);

    @GetMapping("/api/v1/auth/user")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    IdentityDto getCurrentUser(@RequestHeader("Authorization") String token);

    // Identity management endpoints
    
    @GetMapping("/api/v1/identities/{id}")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    IdentityDto getIdentity(@PathVariable UUID id, @RequestHeader("Authorization") String serviceToken);

    @GetMapping("/api/v1/identities/email/{email}")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    IdentityDto getIdentityByEmail(@PathVariable String email, @RequestHeader("Authorization") String serviceToken);

    // Persona management endpoints
    
    @GetMapping("/api/v1/personas/identity/{identityId}")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    List<PersonaDto> getPersonasByIdentity(@PathVariable UUID identityId, @RequestHeader("Authorization") String serviceToken);

    @GetMapping("/api/v1/personas/{id}")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    PersonaDto getPersona(@PathVariable UUID id, @RequestHeader("Authorization") String serviceToken);

    @GetMapping("/api/v1/personas/identity/{identityId}/active")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    PersonaDto getActivePersona(@PathVariable UUID identityId, @RequestHeader("Authorization") String serviceToken);

    @PostMapping("/api/v1/personas/{id}/activate")
    @CircuitBreaker(name = "identity-service")
    @Retry(name = "identity-service")
    PersonaDto activatePersona(@PathVariable UUID id, @RequestHeader("Authorization") String serviceToken);

    // Health check endpoint
    
    @GetMapping("/actuator/health")
    String healthCheck();
}