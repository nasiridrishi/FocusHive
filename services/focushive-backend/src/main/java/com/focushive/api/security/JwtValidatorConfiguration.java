package com.focushive.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for JWT validation components.
 * Sets up caching, REST client, and scheduling for JWKS refresh.
 */
@Slf4j
@Configuration
@EnableScheduling
public class JwtValidatorConfiguration {

    @Value("${jwt.cache.duration.hours:1}")
    private long cacheDurationHours;

    @Value("${jwt.cache.max.size:10}")
    private long cacheMaxSize;

    @Value("${jwt.http.connect.timeout.seconds:3}")
    private long connectTimeoutSeconds;

    @Value("${jwt.http.read.timeout.seconds:3}")
    private long readTimeoutSeconds;

    /**
     * RestTemplate configured for JWKS endpoint calls.
     */
    @Bean(name = "jwksRestTemplate")
    public RestTemplate jwksRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .setReadTimeout(Duration.ofSeconds(readTimeoutSeconds))
            .build();
    }

    /**
     * Cache for storing RSA public keys from JWKS.
     * Keys are cached for 1 hour by default.
     */
    @Bean
    public Cache<String, RSAPublicKey> jwksKeyCache() {
        log.info("Initializing JWKS key cache with duration={}h, maxSize={}",
                cacheDurationHours, cacheMaxSize);

        return Caffeine.newBuilder()
            .expireAfterWrite(cacheDurationHours, TimeUnit.HOURS)
            .maximumSize(cacheMaxSize)
            .recordStats() // Enable stats for monitoring
            .removalListener((key, value, cause) ->
                log.debug("JWKS key removed from cache: kid={}, cause={}", key, cause))
            .build();
    }

    /**
     * JwtValidator bean for validating JWT tokens.
     */
    @Bean
    public JwtValidator jwtValidator(
            @Value("${jwt.issuer.uri:https://identity.focushive.app}") String issuer,
            RestTemplate jwksRestTemplate,
            ObjectMapper objectMapper,
            Cache<String, RSAPublicKey> jwksKeyCache) {

        log.info("Creating JwtValidator with issuer: {}", issuer);
        return new JwtValidator(jwksRestTemplate, objectMapper, jwksKeyCache, issuer);
    }
}