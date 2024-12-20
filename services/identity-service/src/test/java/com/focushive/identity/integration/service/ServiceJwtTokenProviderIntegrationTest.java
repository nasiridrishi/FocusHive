package com.focushive.identity.integration.service;

import com.focushive.identity.security.RSAJwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify ServiceJwtTokenProvider can be properly injected
 * and works within the Spring context.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiceJwtTokenProviderIntegrationTest {

    @Autowired(required = false)
    private ServiceJwtTokenProvider serviceJwtTokenProvider;

    @Autowired(required = false)
    private RSAJwtTokenProvider rsaJwtTokenProvider;

    @Test
    void shouldInjectServiceJwtTokenProvider() {
        // Then
        assertNotNull(serviceJwtTokenProvider, "ServiceJwtTokenProvider should be available as Spring bean");
    }

    @Test
    void shouldInjectRSAJwtTokenProvider() {
        // Then
        assertNotNull(rsaJwtTokenProvider, "RSAJwtTokenProvider should be available as Spring bean");
    }

    @Test
    void shouldGenerateServiceTokenInSpringContext() {
        // Given
        assertNotNull(serviceJwtTokenProvider, "ServiceJwtTokenProvider should be injected");

        // When
        String token = serviceJwtTokenProvider.generateServiceToken();

        // Then
        assertNotNull(token, "Generated service token should not be null");
        assertFalse(token.isEmpty(), "Generated service token should not be empty");
        
        // Token should have 3 parts (header.payload.signature)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT token should have 3 parts");
    }

    @Test
    void shouldValidateGeneratedTokenInSpringContext() {
        // Given
        assertNotNull(serviceJwtTokenProvider, "ServiceJwtTokenProvider should be injected");
        String token = serviceJwtTokenProvider.generateServiceToken();

        // When
        boolean shouldRefresh = serviceJwtTokenProvider.shouldRefreshToken(token);

        // Then
        assertFalse(shouldRefresh, "Fresh token should not need refresh in Spring context");
    }
}