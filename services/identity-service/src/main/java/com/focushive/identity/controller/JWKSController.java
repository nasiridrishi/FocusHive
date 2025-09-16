package com.focushive.identity.controller;

import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.security.RSAJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * JWKS Controller for exposing public keys for JWT validation.
 * This is critical for service-to-service authentication in production.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class JWKSController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Expose JWKS for service-to-service authentication.
     * This endpoint is crucial for other services to validate JWT tokens.
     */
    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJWKS() {
        log.info("JWKS endpoint called for service-to-service authentication");

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> keys = new ArrayList<>();

        // Check if we're using RSA
        if (jwtTokenProvider instanceof RSAJwtTokenProvider) {
            RSAJwtTokenProvider rsaProvider = (RSAJwtTokenProvider) jwtTokenProvider;
            keys = rsaProvider.getJWKS();
            log.info("Returning {} RSA public keys from JWKS endpoint", keys.size());

            if (keys.isEmpty()) {
                log.warn("RSA provider returned empty key set - this will prevent service-to-service authentication!");
                // In production, this should trigger an alert
            }
        } else {
            log.error("JWT provider is not RSA-based. Service-to-service authentication will fail!");
            // In production, this is a critical error
            response.put("error", "RSA not configured - service authentication unavailable");
        }

        response.put("keys", keys);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check for JWT configuration.
     * This helps diagnose authentication issues in production.
     */
    @GetMapping(value = "/jwt-health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwtHealthCheck() {
        Map<String, Object> health = new HashMap<>();

        health.put("providerType", jwtTokenProvider.getClass().getSimpleName());
        health.put("isRSA", jwtTokenProvider instanceof RSAJwtTokenProvider);

        if (jwtTokenProvider instanceof RSAJwtTokenProvider) {
            RSAJwtTokenProvider rsaProvider = (RSAJwtTokenProvider) jwtTokenProvider;
            List<Map<String, Object>> keys = rsaProvider.getJWKS();
            health.put("keyCount", keys.size());
            health.put("status", keys.isEmpty() ? "ERROR" : "OK");

            if (!keys.isEmpty()) {
                // Add key IDs for debugging
                List<String> keyIds = new ArrayList<>();
                for (Map<String, Object> key : keys) {
                    if (key.containsKey("kid")) {
                        keyIds.add((String) key.get("kid"));
                    }
                }
                health.put("keyIds", keyIds);
            }
        } else {
            health.put("status", "ERROR");
            health.put("error", "RSA not configured");
        }

        log.info("JWT health check: {}", health);
        return ResponseEntity.ok(health);
    }
}