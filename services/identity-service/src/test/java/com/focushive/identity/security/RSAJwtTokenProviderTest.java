package com.focushive.identity.security;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Persona.PersonaType;
import com.focushive.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.InvalidKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RSAJwtTokenProvider focusing on the RSA token parsing regression.
 * 
 * This test demonstrates that RSA-signed tokens fail to validate because
 * the claim extraction methods still use the HMAC secret from the parent class.
 */
@DisplayName("RSA JWT Token Provider Tests")
class RSAJwtTokenProviderTest {

    private RSAJwtTokenProvider rsaJwtTokenProvider;
    private JwtTokenProvider hmacJwtTokenProvider;
    
    private User testUser;
    private Persona testPersona;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setEmailVerified(true);

        testPersona = new Persona();
        testPersona.setId(UUID.randomUUID());
        testPersona.setName("Test Persona");
        testPersona.setType(PersonaType.PERSONAL);
        
        // Set up providers
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        
        // Create HMAC provider for comparison
        hmacJwtTokenProvider = new JwtTokenProvider(
            "testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesTestOnly",
            3600000L,
            86400000L, 
            7776000000L,
            "http://localhost:8081/identity");
        
        // Create RSA provider that shows the bug
        rsaJwtTokenProvider = new RSAJwtTokenProvider(
            "", // empty secret since we're using RSA
            3600000L,
            86400000L,
            7776000000L,
            "http://localhost:8081/identity",
            true, // use RSA
            null, // no private key path - will be auto generated
            null, // no public key path - will be auto generated
            "test-key-2025",
            resourceLoader);
    }

    @Test
    @DisplayName("Should generate RSA-signed access token")
    void testGenerateAccessToken_UsesRSASigning() {
        // When: Generate access token using RSA provider
        String token = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Then: Token should be generated
        assertNotNull(token, "Access token should not be null");
        assertFalse(token.isEmpty(), "Access token should not be empty");

        // Token should be properly formatted JWT (3 parts separated by dots)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have exactly 3 parts");

        // Decode and check header for RSA algorithm
        String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
        assertTrue(headerJson.contains("RS256"), 
            "Token header should indicate RS256 algorithm, but was: " + headerJson);
    }

    @Test
    @DisplayName("Should validate RSA-signed token successfully - FIXED")
    void testValidateToken_RSAToken_Fixed() {
        // Given: An RSA-signed access token
        String token = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When: Validate the RSA token (should now use RSA public key)
        boolean isValid = rsaJwtTokenProvider.validateToken(token);

        // Then: Token should be valid
        assertTrue(isValid, "RSA-signed token should be valid when using proper RSA validation");
    }

    @Test
    @DisplayName("Should extract claims from RSA token successfully - FIXED")
    void testExtractClaims_RSAToken_Fixed() {
        // Given: An RSA-signed access token
        String token = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When: Extract claims using RSA validation
        Claims claims = rsaJwtTokenProvider.extractAllClaims(token);

        // Then: Claims should be extracted successfully
        assertNotNull(claims, "Claims should be extracted from RSA token");
        assertEquals(testUser.getId().toString(), claims.get("userId"), "User ID should match");
        assertEquals(testUser.getEmail(), claims.get("email"), "Email should match");
        assertEquals(testPersona.getId().toString(), claims.get("personaId"), "Persona ID should match");
        assertEquals(testPersona.getName(), claims.get("personaName"), "Persona name should match");
        assertEquals("access", claims.get("type"), "Token type should be access");
    }

    @Test
    @DisplayName("Should extract user ID from RSA token successfully - FIXED")
    void testExtractUserId_RSAToken_Fixed() {
        // Given: An RSA-signed access token
        String token = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When: Extract user ID using RSA validation
        UUID extractedUserId = rsaJwtTokenProvider.extractUserId(token);

        // Then: User ID should be extracted successfully
        assertEquals(testUser.getId(), extractedUserId, "Extracted user ID should match original");
    }

    @Test
    @DisplayName("Should extract persona ID from RSA token successfully - FIXED")
    void testExtractPersonaId_RSAToken_Fixed() {
        // Given: An RSA-signed access token
        String token = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When: Extract persona ID using RSA validation
        UUID extractedPersonaId = rsaJwtTokenProvider.extractPersonaId(token);

        // Then: Persona ID should be extracted successfully
        assertEquals(testPersona.getId(), extractedPersonaId, "Extracted persona ID should match original");
    }

    @Test
    @DisplayName("Should extract email from RSA token successfully - FIXED")
    void testExtractEmail_RSAToken_Fixed() {
        // Given: An RSA-signed access token
        String token = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When: Extract email using RSA validation
        String extractedEmail = rsaJwtTokenProvider.extractEmail(token);

        // Then: Email should be extracted successfully
        assertEquals(testUser.getEmail(), extractedEmail, "Extracted email should match original");
    }

    @Test
    @DisplayName("Should extract expiration from RSA token successfully - FIXED")
    void testExtractExpiration_RSAToken_Fixed() {
        // Given: An RSA-signed access token
        String token = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When: Extract expiration using RSA validation
        Date extractedExpiration = rsaJwtTokenProvider.extractExpiration(token);

        // Then: Expiration should be extracted successfully
        assertNotNull(extractedExpiration, "Expiration should be extracted from RSA token");
        assertTrue(extractedExpiration.after(new Date()), "Token should not be expired immediately after generation");
    }

    @Test
    @DisplayName("Should maintain backward compatibility with HMAC tokens")
    void testBackwardCompatibilityWithHMAC() {
        // Given: An HMAC-signed access token using the standard JwtTokenProvider
        String hmacToken = hmacJwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When: Process the HMAC token with the HMAC provider
        boolean isValid = hmacJwtTokenProvider.validateToken(hmacToken);
        Claims claims = hmacJwtTokenProvider.extractAllClaims(hmacToken);
        UUID extractedUserId = hmacJwtTokenProvider.extractUserId(hmacToken);
        String extractedEmail = hmacJwtTokenProvider.extractEmail(hmacToken);
        
        // Then: HMAC tokens should work perfectly with the HMAC provider
        assertTrue(isValid, "HMAC token should be valid with HMAC provider");
        assertNotNull(claims, "Claims should be extracted from HMAC token");
        assertEquals(testUser.getId(), extractedUserId, "User ID should match from HMAC token");
        assertEquals(testUser.getEmail(), extractedEmail, "Email should match from HMAC token");
    }
    
    @Test
    @DisplayName("Should handle HMAC tokens with RSA provider when useRSA=false")
    void testRSAProviderWithHMACMode() {
        // Given: An RSA provider configured to use HMAC (useRSA=false)
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        RSAJwtTokenProvider hmacModeProvider = new RSAJwtTokenProvider(
            "testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesTestOnly",
            3600000L, 86400000L, 7776000000L,
            "http://localhost:8081/identity",
            false, // use HMAC instead of RSA
            null, null, "test-key-2025", resourceLoader);
            
        // When: Generate and validate HMAC token using RSA provider in HMAC mode
        String token = hmacModeProvider.generateAccessToken(testUser, testPersona);
        boolean isValid = hmacModeProvider.validateToken(token);
        UUID extractedUserId = hmacModeProvider.extractUserId(token);
        
        // Then: Should work like regular HMAC provider
        assertTrue(isValid, "Token should be valid when RSA provider is in HMAC mode");
        assertEquals(testUser.getId(), extractedUserId, "User ID should be extracted correctly");
        
        // And: Token should be HMAC-signed, not RSA-signed
        String[] parts = token.split("\\.");
        String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
        assertTrue(headerJson.contains("HS512"), 
            "Token should use HS512 algorithm when RSA provider is in HMAC mode, but was: " + headerJson);
    }
    
    @Test
    @DisplayName("Should reject RSA tokens when using HMAC provider with clear error message")
    void testHMACProviderRejectsRSATokens() {
        // Given: An RSA-signed token
        String rsaToken = rsaJwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When: Try to process RSA token with HMAC provider
        boolean validationResult = hmacJwtTokenProvider.validateToken(rsaToken);
        
        // Then: Validation should fail
        assertFalse(validationResult, "HMAC provider should reject RSA token");
        
        // And: Claim extraction should throw clear error
        Exception exception = assertThrows(io.jsonwebtoken.security.InvalidKeyException.class, () -> {
            hmacJwtTokenProvider.extractAllClaims(rsaToken);
        }, "Expected clear InvalidKeyException when HMAC provider processes RSA token");
        
        // And: Error message should be helpful
        assertTrue(exception.getMessage().contains("RSA-signed token"), 
            "Error message should mention RSA-signed token: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("HMAC"), 
            "Error message should mention HMAC: " + exception.getMessage());
    }
    
    // Tests now demonstrate that the RSA JWT parsing fix is working correctly!
    // All claim extraction methods now properly use RSA public key validation
    // instead of attempting to use the HMAC secret on RSA-signed tokens.
}