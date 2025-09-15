package com.focushive.identity.integration.service;

import com.focushive.identity.security.RSAJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Provides JWT tokens for service-to-service authentication.
 * This is used when the Identity Service needs to call other services.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceJwtTokenProvider {

    private final RSAJwtTokenProvider jwtTokenProvider;

    @Value("${spring.application.name:identity-service}")
    private String serviceName;

    @Value("${jwt.service.expiration:300000}") // 5 minutes default
    private long serviceTokenExpiration;

    /**
     * Generates a JWT token for service-to-service authentication.
     * This token is used when calling other microservices like notification-service.
     *
     * @return JWT token string
     */
    public String generateServiceToken() {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plus(serviceTokenExpiration, ChronoUnit.MILLIS);

            Map<String, Object> claims = new HashMap<>();
            claims.put("service", serviceName);
            claims.put("type", "service-account");
            claims.put("roles", Arrays.asList("SERVICE"));
            claims.put("permissions", Arrays.asList(
                "notification.send",
                "notification.template.read",
                "notification.status.read"
            ));
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", expiresAt.getEpochSecond());

            // Generate a service-specific subject
            String subject = "service-" + serviceName;

            // Use the existing JWT token provider's generateToken method
            // Convert expiration to seconds
            int expirationSeconds = (int) (serviceTokenExpiration / 1000);
            String token = jwtTokenProvider.generateToken(subject, claims, expirationSeconds);

            log.debug("Generated service token for {}, expires at {}", serviceName, expiresAt);
            return token;

        } catch (Exception e) {
            log.error("Failed to generate service token", e);
            throw new RuntimeException("Failed to generate service token", e);
        }
    }

    /**
     * Checks if a service token needs refresh based on expiration.
     *
     * @param token The current token
     * @return true if token should be refreshed
     */
    public boolean shouldRefreshToken(String token) {
        try {
            // Check if token is valid
            if (!jwtTokenProvider.validateToken(token)) {
                return true;
            }

            // Check if token is expired or near expiration
            // If token expires in less than 1 minute, refresh it
            Date expiration = jwtTokenProvider.extractExpiration(token);
            Instant expirationInstant = expiration.toInstant();
            Instant now = Instant.now();

            return expirationInstant.isBefore(now.plus(60, ChronoUnit.SECONDS));
        } catch (Exception e) {
            // If token is invalid or expired, definitely refresh
            return true;
        }
    }
}