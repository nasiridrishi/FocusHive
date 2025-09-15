package com.focushive.identity.config;

import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.security.RSAJwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

/**
 * JWT Configuration for standardizing JWT signing across the application.
 * 
 * This configuration ensures consistent JWT token generation and validation
 * by providing appropriate JWT providers based on the environment and requirements.
 * 
 * Features:
 * - RSA signing for production (default)
 * - HMAC fallback for development/testing
 * - Environment-specific configuration
 * - Proper bean priorities and profiles
 */
@Configuration
@Slf4j
public class JwtConfiguration {

    @Value("${jwt.use-rsa:true}")
    private boolean useRSA;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.issuer:http://localhost:8081/identity}")
    private String issuer;

    /**
     * Primary JWT token provider bean.
     * Uses RSA for production, HMAC for development/testing.
     */
    @Bean
    @Primary
    @Profile("!performance")
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long tokenExpiration,
            @Value("${jwt.refresh-token-expiration-ms:2592000000}") long refreshTokenExpiration,
            @Value("${jwt.remember-me-expiration-ms:7776000000}") long rememberMeTokenExpiration,
            @Value("${jwt.issuer:http://localhost:8081/identity}") String issuer,
            @Value("${jwt.use-rsa:true}") boolean useRSA,
            @Value("${jwt.rsa.private-key-path:classpath:keys/jwt-private.pem}") String privateKeyPath,
            @Value("${jwt.rsa.public-key-path:classpath:keys/jwt-public.pem}") String publicKeyPath,
            @Value("${jwt.rsa.key-id:focushive-2025-01}") String keyId,
            ResourceLoader resourceLoader) {

        if (useRSA) {
            log.info("Configuring RSA JWT Token Provider for production use");
            return new RSAJwtTokenProvider(
                secret, tokenExpiration, refreshTokenExpiration, 
                rememberMeTokenExpiration, issuer, useRSA, 
                privateKeyPath, publicKeyPath, keyId, resourceLoader
            );
        } else {
            log.info("Configuring HMAC JWT Token Provider for development/testing");
            return new JwtTokenProvider(
                secret, tokenExpiration, refreshTokenExpiration, 
                rememberMeTokenExpiration, issuer
            );
        }
    }

    /**
     * Performance-optimized JWT token provider for high-load scenarios.
     * Uses HMAC for maximum performance when RSA overhead is not acceptable.
     */
    @Bean
    @Primary
    @Profile("performance")
    public JwtTokenProvider performanceJwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long tokenExpiration,
            @Value("${jwt.refresh-token-expiration-ms:2592000000}") long refreshTokenExpiration,
            @Value("${jwt.remember-me-expiration-ms:7776000000}") long rememberMeTokenExpiration,
            @Value("${jwt.issuer:http://localhost:8081/identity}") String issuer) {

        log.warn("Using HMAC JWT Token Provider for performance profile - not recommended for production security");
        return new JwtTokenProvider(
            secret, tokenExpiration, refreshTokenExpiration, 
            rememberMeTokenExpiration, issuer
        );
    }

    /**
     * Dedicated RSA JWT token provider bean.
     * Always uses RSA signing regardless of configuration.
     * Useful for specific use cases that require RSA.
     */
    @Bean("rsaJwtTokenProvider")
    public RSAJwtTokenProvider rsaJwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long tokenExpiration,
            @Value("${jwt.refresh-token-expiration-ms:2592000000}") long refreshTokenExpiration,
            @Value("${jwt.remember-me-expiration-ms:7776000000}") long rememberMeTokenExpiration,
            @Value("${jwt.issuer:http://localhost:8081/identity}") String issuer,
            @Value("${jwt.rsa.private-key-path:classpath:keys/jwt-private.pem}") String privateKeyPath,
            @Value("${jwt.rsa.public-key-path:classpath:keys/jwt-public.pem}") String publicKeyPath,
            @Value("${jwt.rsa.key-id:focushive-2025-01}") String keyId,
            ResourceLoader resourceLoader) {

        return new RSAJwtTokenProvider(
            secret, tokenExpiration, refreshTokenExpiration,
            rememberMeTokenExpiration, issuer, true, // Force RSA
            privateKeyPath, publicKeyPath, keyId, resourceLoader
        );
    }

    /**
     * Dedicated HMAC JWT token provider bean.
     * Always uses HMAC signing regardless of configuration.
     * Useful for specific use cases that require HMAC or for performance.
     */
    @Bean("hmacJwtTokenProvider")
    public JwtTokenProvider hmacJwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms:3600000}") long tokenExpiration,
            @Value("${jwt.refresh-token-expiration-ms:2592000000}") long refreshTokenExpiration,
            @Value("${jwt.remember-me-expiration-ms:7776000000}") long rememberMeTokenExpiration,
            @Value("${jwt.issuer:http://localhost:8081/identity}") String issuer) {

        return new JwtTokenProvider(
            secret, tokenExpiration, refreshTokenExpiration,
            rememberMeTokenExpiration, issuer
        );
    }

    /**
     * JWT configuration properties bean for monitoring and diagnostics.
     */
    @Bean
    public JwtConfigProperties jwtConfigProperties() {
        return JwtConfigProperties.builder()
            .useRSA(useRSA)
            .issuer(issuer)
            .secretConfigured(!jwtSecret.isEmpty())
            .build();
    }

    /**
     * Configuration properties for JWT settings.
     * Used for monitoring and diagnostics.
     */
    @lombok.Builder
    @lombok.Data
    public static class JwtConfigProperties {
        private boolean useRSA;
        private String issuer;
        private boolean secretConfigured;
    }
}