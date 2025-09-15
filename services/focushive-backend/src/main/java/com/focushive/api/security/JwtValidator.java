package com.focushive.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Production-ready JWT validator that validates tokens locally using JWKS.
 * Implements caching, error handling, and monitoring.
 */
@Slf4j
public class JwtValidator {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Cache<String, RSAPublicKey> keyCache;
    private final String issuer;
    private String jwksUri;

    @Value("${jwt.cache.duration.hours:1}")
    private long cacheDurationHours;

    @Value("${jwt.validation.leeway.seconds:30}")
    private long leewaySeconds;

    @Value("${jwt.jwks.uri:https://identity.focushive.app/.well-known/jwks.json}")
    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public JwtValidator(RestTemplate restTemplate,
                       ObjectMapper objectMapper,
                       Cache<String, RSAPublicKey> keyCache,
                       @Value("${jwt.issuer.uri:https://identity.focushive.app}") String issuer) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.keyCache = keyCache != null ? keyCache : createDefaultCache();
        this.issuer = issuer;
        // Set default JWKS URI - will be overridden by @Value after construction
        this.jwksUri = "https://identity.focushive.app/.well-known/jwks.json";
    }

    private Cache<String, RSAPublicKey> createDefaultCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(cacheDurationHours, TimeUnit.HOURS)
            .maximumSize(10)
            .recordStats()
            .build();
    }

    /**
     * Initialize by fetching JWKS on startup.
     */
    @PostConstruct
    public void init() {
        try {
            refreshKeys();
            log.info("JWT Validator initialized successfully with JWKS from {}", jwksUri);
        } catch (Exception e) {
            log.error("Failed to initialize JWT Validator with JWKS. Will retry on first request.", e);
        }
    }

    /**
     * Validates a JWT token and returns the result.
     */
    public ValidationResult validateToken(String token) {
        long startTime = System.currentTimeMillis();

        try {
            // Parse the JWT
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Get the key ID from header
            String keyId = signedJWT.getHeader().getKeyID();
            if (keyId == null) {
                return ValidationResult.failure("JWT missing key ID (kid) in header");
            }

            // Get the public key (from cache or fetch)
            RSAPublicKey publicKey = getPublicKey(keyId);
            if (publicKey == null) {
                return ValidationResult.failure("Public key not found for kid: " + keyId);
            }

            // Verify signature
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                return ValidationResult.failure("Invalid JWT signature");
            }

            // Get claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Validate issuer
            if (!issuer.equals(claims.getIssuer())) {
                return ValidationResult.failure("Invalid issuer. Expected: " + issuer + ", Got: " + claims.getIssuer());
            }

            // Validate expiration
            Date now = new Date();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date(now.getTime() - leewaySeconds * 1000))) {
                return ValidationResult.failure("JWT token expired");
            }

            // Validate not before
            Date notBefore = claims.getNotBeforeTime();
            if (notBefore != null && notBefore.after(new Date(now.getTime() + leewaySeconds * 1000))) {
                return ValidationResult.failure("JWT token not yet valid");
            }

            // Convert claims to Map
            Map<String, Object> claimsMap = new HashMap<>();
            claims.getClaims().forEach((key, value) -> claimsMap.put(key, value));

            long duration = System.currentTimeMillis() - startTime;
            log.debug("JWT validation successful for subject {} in {}ms", claims.getSubject(), duration);

            return ValidationResult.success(claimsMap);

        } catch (ParseException e) {
            log.warn("Failed to parse JWT token", e);
            return ValidationResult.failure("Invalid JWT format: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation", e);
            return ValidationResult.failure("JWT validation failed: " + e.getMessage());
        }
    }

    /**
     * Gets public key from cache or fetches from JWKS endpoint.
     */
    private RSAPublicKey getPublicKey(String keyId) {
        // Try cache first
        RSAPublicKey cachedKey = keyCache.getIfPresent(keyId);
        if (cachedKey != null) {
            log.debug("Using cached public key for kid: {}", keyId);
            return cachedKey;
        }

        // Fetch from JWKS
        log.info("Public key not in cache for kid: {}, fetching from JWKS", keyId);
        refreshKeys();

        return keyCache.getIfPresent(keyId);
    }

    /**
     * Refreshes keys from JWKS endpoint.
     * Scheduled to run periodically.
     */
    @Scheduled(fixedDelayString = "${jwt.jwks.refresh.interval:3600000}") // Default 1 hour
    public void refreshKeys() {
        try {
            log.info("Refreshing JWKS keys from {}", jwksUri);

            // Check if jwksUri is configured
            if (jwksUri == null || jwksUri.isEmpty()) {
                throw new RuntimeException("JWKS URI is not configured");
            }

            // Fetch JWKS using RestTemplate for better control
            ResponseEntity<Map> response = restTemplate.getForEntity(jwksUri, Map.class);

            if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to fetch JWKS: " + (response != null ? response.getStatusCode() : "null response"));
            }

            Map<String, Object> jwksMap = response.getBody();
            List<Map<String, Object>> keys = (List<Map<String, Object>>) jwksMap.get("keys");

            if (keys == null || keys.isEmpty()) {
                log.warn("No keys found in JWKS response");
                return;
            }

            int keysLoaded = 0;
            for (Map<String, Object> keyMap : keys) {
                try {
                    String kid = (String) keyMap.get("kid");
                    String kty = (String) keyMap.get("kty");

                    if (!"RSA".equals(kty)) {
                        log.debug("Skipping non-RSA key: {}", kid);
                        continue;
                    }

                    // Parse RSA key
                    JWK jwk = JWK.parse(objectMapper.writeValueAsString(keyMap));
                    if (jwk instanceof RSAKey) {
                        RSAKey rsaKey = (RSAKey) jwk;
                        RSAPublicKey publicKey = rsaKey.toRSAPublicKey();
                        keyCache.put(kid, publicKey);
                        keysLoaded++;
                        log.debug("Cached public key for kid: {}", kid);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse key from JWKS", e);
                }
            }

            log.info("Successfully loaded {} keys from JWKS", keysLoaded);

        } catch (Exception e) {
            log.error("Failed to refresh JWKS keys", e);
            // Don't throw - we may have cached keys that are still valid
            if (keyCache.asMap().isEmpty()) {
                throw new RuntimeException("Failed to fetch JWKS and no cached keys available", e);
            }
        }
    }

    /**
     * Validation result containing success/failure and claims or error.
     */
    @Data
    public static class ValidationResult {
        private final boolean valid;
        private final Map<String, Object> claims;
        private final String error;

        private ValidationResult(boolean valid, Map<String, Object> claims, String error) {
            this.valid = valid;
            this.claims = claims;
            this.error = error;
        }

        public static ValidationResult success(Map<String, Object> claims) {
            return new ValidationResult(true, claims, null);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, null, error);
        }

        public UUID getUserId() {
            if (claims != null) {
                String userIdStr = (String) claims.get("userId");
                if (userIdStr != null) {
                    try {
                        return UUID.fromString(userIdStr);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid UUID in userId claim: {}", userIdStr);
                    }
                }
            }
            return null;
        }

        public String getEmail() {
            return claims != null ? (String) claims.get("email") : null;
        }

        public String getSubject() {
            return claims != null ? (String) claims.get("sub") : null;
        }
    }
}