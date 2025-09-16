package com.focushive.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * JWT configuration for the notification service.
 * Configures the JWT decoder for validating tokens from the identity service.
 */
@Slf4j
@Configuration
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8081}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://localhost:8081/.well-known/jwks.json}")
    private String jwkSetUri;

    /**
     * JWT decoder for validating JWT tokens.
     * Uses the JWKS endpoint from the identity service to get public keys.
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder() {
        log.info("Configuring JWT decoder with issuer: {} and JWK Set URI: {}", issuerUri, jwkSetUri);

        try {
            // Try to create decoder from issuer URI (which should auto-discover JWKS)
            if (issuerUri != null && !issuerUri.isEmpty()) {
                return JwtDecoders.fromIssuerLocation(issuerUri);
            }
        } catch (Exception e) {
            log.warn("Failed to create JWT decoder from issuer URI: {}, falling back to JWK Set URI",
                    issuerUri, e);
        }

        // Fallback to creating decoder from JWK Set URI directly
        if (jwkSetUri != null && !jwkSetUri.isEmpty()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }

        throw new IllegalStateException("Unable to configure JWT decoder. " +
                "Either issuer-uri or jwk-set-uri must be configured.");
    }
}