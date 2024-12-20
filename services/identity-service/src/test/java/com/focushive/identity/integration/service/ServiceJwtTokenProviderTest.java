package com.focushive.identity.integration.service;

import com.focushive.identity.security.RSAJwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ServiceJwtTokenProvider.
 * Verifies that service-to-service JWT tokens are generated correctly.
 */
@ExtendWith(MockitoExtension.class)
class ServiceJwtTokenProviderTest {

    @Mock
    private ResourceLoader resourceLoader;

    private ServiceJwtTokenProvider serviceJwtTokenProvider;
    private RSAJwtTokenProvider rsaJwtTokenProvider;
    
    // Test configuration values
    private static final String TEST_SERVICE_NAME = "identity-service";
    private static final long TEST_SERVICE_TOKEN_EXPIRATION = 300000L; // 5 minutes
    private static final String TEST_JWT_SECRET = "test-key-that-is-at-least-32-characters-long-for-hmac-validation";
    private static final String TEST_ISSUER = "http://localhost:8081/identity";

    @BeforeEach
    void setUp() {
        // Create RSAJwtTokenProvider with test configuration
        // Use HMAC mode for testing to avoid RSA key complexity
        rsaJwtTokenProvider = new RSAJwtTokenProvider(
            TEST_JWT_SECRET,
            3600000L, // 1 hour
            2592000000L, // 30 days
            7776000000L, // 90 days
            TEST_ISSUER,
            false, // Use HMAC for testing
            null, // No RSA key path
            null, // No RSA key path  
            "test-key-id",
            resourceLoader
        );

        // Create ServiceJwtTokenProvider
        serviceJwtTokenProvider = new ServiceJwtTokenProvider(rsaJwtTokenProvider);
        
        // Set test values using reflection
        ReflectionTestUtils.setField(serviceJwtTokenProvider, "serviceName", TEST_SERVICE_NAME);
        ReflectionTestUtils.setField(serviceJwtTokenProvider, "serviceTokenExpiration", TEST_SERVICE_TOKEN_EXPIRATION);
    }

    @Test
    void shouldGenerateValidServiceToken() {
        // When
        String token = serviceJwtTokenProvider.generateServiceToken();

        // Then
        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isEmpty(), "Generated token should not be empty");
        
        // Token should have 3 parts (header.payload.signature)
        String[] tokenParts = token.split("\\.");
        assertEquals(3, tokenParts.length, "JWT token should have 3 parts");

        // Verify token can be parsed and contains expected claims
        Claims claims = parseTokenClaims(token);
        assertNotNull(claims, "Token claims should be parseable");
        
        // Verify service-specific claims
        assertEquals("service-identity-service", claims.getSubject(), "Subject should be service-specific");
        assertEquals(TEST_SERVICE_NAME, claims.get("service", String.class), "Service claim should match");
        assertEquals("service-account", claims.get("type", String.class), "Type should be service-account");
        
        // Verify roles
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertNotNull(roles, "Roles should not be null");
        assertTrue(roles.contains("SERVICE"), "Roles should contain SERVICE");
        
        // Verify permissions
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) claims.get("permissions");
        assertNotNull(permissions, "Permissions should not be null");
        assertTrue(permissions.contains("notification.send"), "Should have notification.send permission");
        assertTrue(permissions.contains("notification.template.read"), "Should have notification.template.read permission");
        assertTrue(permissions.contains("notification.status.read"), "Should have notification.status.read permission");

        // Verify token expiration
        Date expiration = claims.getExpiration();
        assertNotNull(expiration, "Expiration should not be null");
        
        Instant now = Instant.now();
        Instant expirationInstant = expiration.toInstant();
        assertTrue(expirationInstant.isAfter(now), "Token should not be expired");
        assertTrue(expirationInstant.isBefore(now.plus(TEST_SERVICE_TOKEN_EXPIRATION + 1000, ChronoUnit.MILLIS)), 
                  "Token should expire within expected time");
    }

    @Test
    void shouldGenerateUniqueTokensOnMultipleCalls() {
        // When
        String token1 = serviceJwtTokenProvider.generateServiceToken();
        String token2 = serviceJwtTokenProvider.generateServiceToken();

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2, "Multiple calls should generate unique tokens");
        
        // Verify both tokens have different JTIs (JWT IDs)
        Claims claims1 = parseTokenClaims(token1);
        Claims claims2 = parseTokenClaims(token2);
        
        assertNotNull(claims1.getId());
        assertNotNull(claims2.getId());
        assertNotEquals(claims1.getId(), claims2.getId(), "JWT IDs should be unique");
    }

    @Test
    void shouldValidateTokenCorrectly() {
        // Given
        String token = serviceJwtTokenProvider.generateServiceToken();

        // When
        boolean shouldRefresh = serviceJwtTokenProvider.shouldRefreshToken(token);

        // Then
        assertFalse(shouldRefresh, "Fresh token should not need refresh");
    }

    @Test
    void shouldDetectExpiredTokenNeedsRefresh() {
        // Given - create a token that expires very soon
        ReflectionTestUtils.setField(serviceJwtTokenProvider, "serviceTokenExpiration", 100L); // 100ms
        String token = serviceJwtTokenProvider.generateServiceToken();
        
        // Wait for token to be near expiration
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean shouldRefresh = serviceJwtTokenProvider.shouldRefreshToken(token);

        // Then
        assertTrue(shouldRefresh, "Expired or near-expired token should need refresh");
    }

    @Test
    void shouldDetectInvalidTokenNeedsRefresh() {
        // When
        boolean shouldRefresh = serviceJwtTokenProvider.shouldRefreshToken("invalid-token");

        // Then
        assertTrue(shouldRefresh, "Invalid token should need refresh");
    }

    @Test
    void shouldDetectNullTokenNeedsRefresh() {
        // When
        boolean shouldRefresh = serviceJwtTokenProvider.shouldRefreshToken(null);

        // Then
        assertTrue(shouldRefresh, "Null token should need refresh");
    }

    @Test 
    void shouldHandleRSAJwtTokenProviderException() {
        // Given
        RSAJwtTokenProvider mockProvider = mock(RSAJwtTokenProvider.class);
        when(mockProvider.generateToken(anyString(), anyMap(), anyInt()))
            .thenThrow(new RuntimeException("RSA key not available"));
            
        ServiceJwtTokenProvider providerWithMock = new ServiceJwtTokenProvider(mockProvider);
        ReflectionTestUtils.setField(providerWithMock, "serviceName", TEST_SERVICE_NAME);
        ReflectionTestUtils.setField(providerWithMock, "serviceTokenExpiration", TEST_SERVICE_TOKEN_EXPIRATION);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> providerWithMock.generateServiceToken());
        
        assertTrue(exception.getMessage().contains("Failed to generate service token"));
        assertNotNull(exception.getCause());
    }

    /**
     * Helper method to parse JWT token claims for testing.
     * Uses the same HMAC secret as the provider for verification.
     */
    private Claims parseTokenClaims(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}