package com.focushive.api.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for JwtValidator with Spring context.
 * Tests the JWT validation flow in a more realistic environment.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.issuer.uri=https://identity.focushive.app",
    "jwt.jwks.uri=https://identity.focushive.app/.well-known/jwks.json",
    "jwt.validation.enabled=true",
    "jwt.cache.duration.hours=1",
    "jwt.validation.leeway.seconds=30",
    "spring.profiles.active=test"
})
@DisplayName("JWT Validator Integration Tests")
class JwtValidatorIntegrationTest {

    @Autowired
    private JwtValidator jwtValidator;

    @MockBean
    private RestTemplate restTemplate;

    private KeyPair keyPair;
    private String kid = "integration-test-key";
    private String issuer = "https://identity.focushive.app";

    @BeforeEach
    void setUp() throws Exception {
        // Generate RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        // Setup mock JWKS response
        Map<String, Object> jwksResponse = createJWKSResponse();
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(jwksResponse));
    }

    @Test
    @DisplayName("Should validate valid JWT in Spring context")
    void testValidateToken_InSpringContext() throws Exception {
        // Given - Create a valid JWT
        String token = createTestJWT(3600);

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertTrue(result.isValid());
        assertNotNull(result.getClaims());
        assertEquals("integration-test-user", result.getClaims().get("sub"));
        assertEquals(issuer, result.getClaims().get("iss"));
        assertNull(result.getError());
    }

    @Test
    @DisplayName("Should handle expired tokens in Spring context")
    void testExpiredToken_InSpringContext() throws Exception {
        // Given - Create an expired JWT
        String token = createTestJWT(-3600);

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("expired"));
    }

    @Test
    @DisplayName("Should cache keys effectively in Spring context")
    void testCaching_InSpringContext() throws Exception {
        // Given
        String token = createTestJWT(3600);

        // When - Validate token multiple times
        for (int i = 0; i < 5; i++) {
            JwtValidator.ValidationResult result = jwtValidator.validateToken(token);
            assertTrue(result.isValid());
        }

        // Then - JWKS should only be fetched once due to caching
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Should extract user information from token")
    void testUserInfoExtraction() throws Exception {
        // Given - Create JWT with user information
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String email = "test@focushive.app";
        String personaId = "persona-123";

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 3600000);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject("integration-test-user")
            .issuer(issuer)
            .issueTime(now)
            .expirationTime(expiry)
            .claim("userId", userId)
            .claim("email", email)
            .claim("personaId", personaId)
            .build();

        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(),
            claimsSet
        );
        signedJWT.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        String token = signedJWT.serialize();

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertTrue(result.isValid());
        assertEquals(UUID.fromString(userId), result.getUserId());
        assertEquals(email, result.getEmail());
        assertEquals("integration-test-user", result.getSubject());
    }

    @Test
    @DisplayName("Should handle concurrent validation requests")
    void testConcurrentValidation() throws Exception {
        // Given
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tokens.add(createTestJWT(3600));
        }

        // When - Validate all tokens concurrently
        List<JwtValidator.ValidationResult> results = tokens.parallelStream()
            .map(token -> jwtValidator.validateToken(token))
            .toList();

        // Then
        assertEquals(10, results.size());
        results.forEach(result -> {
            assertTrue(result.isValid());
            assertNull(result.getError());
        });

        // JWKS should still be fetched only once due to caching
        verify(restTemplate, atMost(2)).getForEntity(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Should properly initialize with Spring properties")
    void testSpringPropertiesConfiguration() {
        // Verify that the JwtValidator is properly configured with Spring properties
        assertNotNull(jwtValidator);

        // Create a valid token to verify configuration works
        String token = createTestJWT(3600);
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // If configuration is correct, validation should work
        assertTrue(result.isValid());
    }

    // Helper methods

    private String createTestJWT(int expirySeconds) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + (expirySeconds * 1000L));

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("integration-test-user")
                .issuer(issuer)
                .issueTime(now)
                .expirationTime(expiry)
                .claim("email", "test@focushive.app")
                .claim("userId", UUID.randomUUID().toString())
                .claim("personaId", UUID.randomUUID().toString())
                .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(kid)
                .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) keyPair.getPrivate());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test JWT", e);
        }
    }

    private Map<String, Object> createJWKSResponse() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        Map<String, Object> key = new HashMap<>();
        key.put("kty", "RSA");
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("kid", kid);
        key.put("n", Base64.getUrlEncoder().withoutPadding()
            .encodeToString(publicKey.getModulus().toByteArray()));
        key.put("e", Base64.getUrlEncoder().withoutPadding()
            .encodeToString(publicKey.getPublicExponent().toByteArray()));

        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", Arrays.asList(key));

        return jwks;
    }
}