package com.focushive.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtValidator component.
 * Tests local JWT validation using JWKS endpoint.
 */
class JwtValidatorTest {

    private JwtValidator jwtValidator;

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Cache<String, RSAPublicKey> keyCache;
    private KeyPair keyPair;
    private String kid = "test-key-id";
    private String issuer = "https://identity.focushive.app";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Generate RSA key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        // Initialize cache
        keyCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10)
            .build();

        // Initialize validator
        jwtValidator = new JwtValidator(restTemplate, objectMapper, keyCache, issuer);
        // Manually set JWKS URI since @Value won't work in tests
        jwtValidator.setJwksUri("https://identity.focushive.app/.well-known/jwks.json");
    }

    @Test
    @DisplayName("Should successfully validate a valid JWT token")
    void testValidateToken_Success() throws Exception {
        // Given - Create a valid JWT
        String token = createTestJWT(keyPair, kid, issuer, "test-user", 3600);

        // Mock JWKS response
        Map<String, Object> jwksResponse = createJWKSResponse(keyPair, kid);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(jwksResponse));

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertTrue(result.isValid());
        assertNotNull(result.getClaims());
        assertEquals("test-user", result.getClaims().get("sub"));
        assertEquals(issuer, result.getClaims().get("iss"));
        assertNull(result.getError());
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testValidateToken_Expired() throws Exception {
        // Given - Create an expired JWT
        String token = createTestJWT(keyPair, kid, issuer, "test-user", -3600); // Expired 1 hour ago

        // Mock JWKS response
        Map<String, Object> jwksResponse = createJWKSResponse(keyPair, kid);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(jwksResponse));

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("expired"));
    }

    @Test
    @DisplayName("Should reject JWT with invalid signature")
    void testValidateToken_InvalidSignature() throws Exception {
        // Given - Create JWT with different key
        KeyPair wrongKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String token = createTestJWT(wrongKeyPair, kid, issuer, "test-user", 3600);

        // Mock JWKS response with correct key
        Map<String, Object> jwksResponse = createJWKSResponse(keyPair, kid);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(jwksResponse));

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("signature"));
    }

    @Test
    @DisplayName("Should cache public keys from JWKS")
    void testKeyCache() throws Exception {
        // Given
        String token = createTestJWT(keyPair, kid, issuer, "test-user", 3600);
        Map<String, Object> jwksResponse = createJWKSResponse(keyPair, kid);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(jwksResponse));

        // When - Validate twice
        jwtValidator.validateToken(token);
        jwtValidator.validateToken(token);

        // Then - JWKS should only be fetched once due to caching
        verify(restTemplate, times(1)).getForEntity(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Should handle JWKS endpoint failure gracefully")
    void testJWKSEndpointFailure() throws Exception {
        // Given - Create a proper JWT that will require JWKS
        String token = createTestJWT(keyPair, kid, issuer, "test-user", 3600);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getError());
        // The error could be about public key not found or JWT validation failed
        assertTrue(result.getError().contains("Public key not found") ||
                  result.getError().contains("JWT validation failed") ||
                  result.getError().contains("Failed to fetch JWKS"));
    }

    @Test
    @DisplayName("Should reject token with wrong issuer")
    void testValidateToken_WrongIssuer() throws Exception {
        // Given - Create JWT with wrong issuer
        String wrongIssuer = "https://wrong.issuer.com";
        String token = createTestJWT(keyPair, kid, wrongIssuer, "test-user", 3600);

        // Mock JWKS response
        Map<String, Object> jwksResponse = createJWKSResponse(keyPair, kid);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(jwksResponse));

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(token);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Invalid issuer"));
    }

    @Test
    @DisplayName("Should handle malformed JWT gracefully")
    void testValidateToken_MalformedToken() {
        // Given
        String malformedToken = "not.a.jwt";

        // When
        JwtValidator.ValidationResult result = jwtValidator.validateToken(malformedToken);

        // Then
        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Invalid JWT format"));
    }

    @Test
    @DisplayName("Should refresh keys periodically")
    void testRefreshKeys() throws Exception {
        // Given
        Map<String, Object> jwksResponse = createJWKSResponse(keyPair, kid);
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(jwksResponse));

        // When
        jwtValidator.refreshKeys();

        // Then
        verify(restTemplate, times(1)).getForEntity(contains("/.well-known/jwks.json"), eq(Map.class));
        assertNotNull(keyCache.getIfPresent(kid));
    }

    // Helper methods

    private String createTestJWT(KeyPair keyPair, String kid, String issuer, String subject, int expirySeconds) throws Exception {
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + (expirySeconds * 1000L));

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(subject)
            .issuer(issuer)
            .issueTime(now)
            .expirationTime(expiry)
            .claim("email", subject + "@test.com")
            .claim("userId", UUID.randomUUID().toString())
            .claim("personaId", UUID.randomUUID().toString())
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(kid)
            .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        JWSSigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    private Map<String, Object> createJWKSResponse(KeyPair keyPair, String kid) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        Map<String, Object> key = new HashMap<>();
        key.put("kty", "RSA");
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("kid", kid);
        key.put("n", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getModulus().toByteArray()));
        key.put("e", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getPublicExponent().toByteArray()));

        Map<String, Object> jwks = new HashMap<>();
        jwks.put("keys", Arrays.asList(key));

        return jwks;
    }
}