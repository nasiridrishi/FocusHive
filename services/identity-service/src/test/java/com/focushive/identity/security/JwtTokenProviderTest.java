package com.focushive.identity.security;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Persona.PersonaType;
import com.focushive.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and claims extraction for security-critical functionality.
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private Persona testPersona;

    @BeforeEach
    void setUp() {
        // Initialize JwtTokenProvider with test configuration
        // Use a secure test JWT key that passes validation (no common weak patterns)
        String testJwtKey = "MyTestJwtSigningKeyForUnitTestsOnly123456789012345678901234567890";
        long accessTokenExpiration = 3600000L; // 1 hour
        long refreshTokenExpiration = 2592000000L; // 30 days
        long rememberMeExpiration = 7776000000L; // 90 days
        String issuer = "test-issuer";

        jwtTokenProvider = new JwtTokenProvider(
            testJwtKey, accessTokenExpiration, refreshTokenExpiration, rememberMeExpiration, issuer);

        // Create test user
        UUID testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(Instant.now())
                .personas(new ArrayList<>())
                .build();

        // Create test persona
        UUID testPersonaId = UUID.randomUUID();
        testPersona = Persona.builder()
                .id(testPersonaId)
                .name("work-persona")
                .type(PersonaType.WORK)
                .displayName("Work Me")
                .bio("Professional persona")
                .isDefault(true)
                .isActive(true)
                .user(testUser)
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should generate access token with correct claims")
    void generateAccessToken_ValidUserAndPersona_ShouldReturnValidToken() {
        // When
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        // Verify claims
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        assertThat(claims.get("displayName", String.class)).isEqualTo("testuser");
        assertThat(claims.get("emailVerified", Boolean.class)).isTrue();
        assertThat(claims.get("personaId", String.class)).isEqualTo(testPersona.getId().toString());
        assertThat(claims.get("personaName", String.class)).isEqualTo("work-persona");
        assertThat(claims.get("personaType", String.class)).isEqualTo("WORK");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
    }

    @Test
    @DisplayName("Should generate refresh token with correct claims")
    void generateRefreshToken_ValidUser_ShouldReturnValidToken() {
        // When
        String token = jwtTokenProvider.generateRefreshToken(testUser);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        // Verify claims
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.getSubject()).isEqualTo("testuser");
        
        // Refresh token should not have persona or email verification info
        assertThat(claims.get("personaId")).isNull();
        assertThat(claims.get("email")).isNull();
    }

    @Test
    @DisplayName("Should generate long-lived refresh token with remember me flag")
    void generateLongLivedRefreshToken_ValidUser_ShouldReturnValidToken() {
        // When
        String token = jwtTokenProvider.generateLongLivedRefreshToken(testUser);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        // Verify claims
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.get("rememberMe", Boolean.class)).isTrue();
        assertThat(claims.getSubject()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should generate custom token with specified claims and expiration")
    void generateToken_CustomClaimsAndExpiration_ShouldReturnValidToken() {
        // Given
        String subject = "oauth-client";
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("clientId", "test-client");
        customClaims.put("scope", "read write");
        customClaims.put("customClaim", "customValue");
        int expirationSeconds = 1800; // 30 minutes

        // When
        String token = jwtTokenProvider.generateToken(subject, customClaims, expirationSeconds);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        // Verify claims
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.get("clientId", String.class)).isEqualTo("test-client");
        assertThat(claims.get("scope", String.class)).isEqualTo("read write");
        assertThat(claims.get("customClaim", String.class)).isEqualTo("customValue");

        // Verify expiration is approximately correct (within 5 seconds)
        Date expiration = claims.getExpiration();
        Date expectedExpiration = new Date(System.currentTimeMillis() + (expirationSeconds * 1000L));
        assertThat(Math.abs(expiration.getTime() - expectedExpiration.getTime())).isLessThan(5000);
    }

    @Test
    @DisplayName("Should extract username correctly")
    void extractUsername_ValidToken_ShouldReturnUsername() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        String username = jwtTokenProvider.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should extract user ID correctly")
    void extractUserId_ValidToken_ShouldReturnUserId() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        UUID userId = jwtTokenProvider.extractUserId(token);

        // Then
        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should extract persona ID correctly")
    void extractPersonaId_ValidAccessToken_ShouldReturnPersonaId() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        UUID personaId = jwtTokenProvider.extractPersonaId(token);

        // Then
        assertThat(personaId).isEqualTo(testPersona.getId());
    }

    @Test
    @DisplayName("Should return null persona ID for refresh token")
    void extractPersonaId_RefreshToken_ShouldReturnNull() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(testUser);

        // When
        UUID personaId = jwtTokenProvider.extractPersonaId(token);

        // Then
        assertThat(personaId).isNull();
    }

    @Test
    @DisplayName("Should extract email correctly")
    void extractEmail_ValidAccessToken_ShouldReturnEmail() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        String email = jwtTokenProvider.extractEmail(token);

        // Then
        assertThat(email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should extract expiration date correctly")
    void extractExpiration_ValidToken_ShouldReturnExpirationDate() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        Date expiration = jwtTokenProvider.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
        // Should expire in approximately 1 hour (within 1 minute tolerance)
        Date expectedExpiration = new Date(System.currentTimeMillis() + 3600000L);
        assertThat(Math.abs(expiration.getTime() - expectedExpiration.getTime())).isLessThan(60000);
    }

    @Test
    @DisplayName("Should extract issued at date correctly")
    void extractIssuedAt_ValidToken_ShouldReturnIssuedAtDate() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        Date issuedAt = jwtTokenProvider.extractIssuedAt(token);

        // Then
        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt).isBeforeOrEqualTo(new Date());
        // Should be issued within last 5 seconds
        assertThat(System.currentTimeMillis() - issuedAt.getTime()).isLessThan(5000);
    }

    @Test
    @DisplayName("Should validate valid token")
    void validateToken_ValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void validateToken_MalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "this.is.not.a.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject token with wrong signature")
    void validateToken_WrongSignature_ShouldReturnFalse() {
        // Given - Create token with different provider (different secret)
        JwtTokenProvider wrongProvider = new JwtTokenProvider(
            "DifferentJwtSigningKeyForTestingWrongSignatureValidation123456789012345678",
            3600000L, 2592000000L, 7776000000L, "test-issuer");
        String tokenWithWrongSignature = wrongProvider.generateAccessToken(testUser, testPersona);

        // When
        boolean isValid = jwtTokenProvider.validateToken(tokenWithWrongSignature);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty token")
    void validateToken_EmptyToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtTokenProvider.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null token")
    void validateToken_NullToken_ShouldReturnFalse() {
        // When
        boolean isValid = jwtTokenProvider.validateToken(null);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token")
    void isTokenExpired_ExpiredToken_ShouldReturnTrue() {
        // Given - Create provider with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
            "ShortExpirationJwtSigningKeyForTestingExpiredTokens123456789012345678901",
            1L, 1L, 1L, "test-issuer"); // 1ms expiration
        String expiredToken = shortExpirationProvider.generateAccessToken(testUser, testPersona);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should detect valid (non-expired) token")
    void isTokenExpired_ValidToken_ShouldReturnFalse() {
        // Given
        String validToken = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(validToken);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should return true for malformed token when checking expiration")
    void isTokenExpired_MalformedToken_ShouldReturnTrue() {
        // Given
        String malformedToken = "malformed-token";

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(malformedToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should get access token expiration in seconds")
    void getAccessTokenExpirationSeconds_ShouldReturnCorrectValue() {
        // When
        long expirationSeconds = jwtTokenProvider.getAccessTokenExpirationSeconds();

        // Then
        assertThat(expirationSeconds).isEqualTo(3600); // 1 hour
    }

    @Test
    @DisplayName("Should get user ID from token as string")
    void getUserIdFromToken_ValidToken_ShouldReturnUserIdString() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        String userIdString = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertThat(userIdString).isEqualTo(testUser.getId().toString());
    }

    @Test
    @DisplayName("Should get persona ID from token as string")
    void getPersonaIdFromToken_ValidToken_ShouldReturnPersonaIdString() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        String personaIdString = jwtTokenProvider.getPersonaIdFromToken(token);

        // Then
        assertThat(personaIdString).isEqualTo(testPersona.getId().toString());
    }


    @Test
    @DisplayName("Should get claims from token")
    void getClaimsFromToken_ValidToken_ShouldReturnClaims() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // When
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.get("userId", String.class)).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        assertThat(claims.getSubject()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should get JWK set for token verification")
    void getJwkSet_ShouldReturnValidJwkSet() {
        // When
        Map<String, Object> jwkSet = jwtTokenProvider.getJwkSet();

        // Then
        assertThat(jwkSet).isNotNull();
        assertThat(jwkSet).containsKey("keys");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwkSet.get("keys");
        assertThat(keys).hasSize(1);
        
        Map<String, Object> key = keys.get(0);
        assertThat(key.get("kty")).isEqualTo("oct");
        assertThat(key.get("use")).isEqualTo("sig");
        assertThat(key.get("kid")).isEqualTo("1");
        assertThat(key.get("alg")).isEqualTo("HS512");
    }

    @Test
    @DisplayName("Should handle token with different persona types")
    void generateAccessToken_DifferentPersonaTypes_ShouldIncludeCorrectType() {
        // Given
        testPersona.setType(PersonaType.GAMING);

        // When
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Then
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("personaType", String.class)).isEqualTo("GAMING");
    }

    @Test
    @DisplayName("Should include unique JWT ID in each token")
    void generateAccessToken_MultipleCalls_ShouldHaveUniqueJwtIds() {
        // When
        String token1 = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        String token2 = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Then
        Claims claims1 = jwtTokenProvider.extractAllClaims(token1);
        Claims claims2 = jwtTokenProvider.extractAllClaims(token2);
        
        assertThat(claims1.getId()).isNotNull();
        assertThat(claims2.getId()).isNotNull();
        assertThat(claims1.getId()).isNotEqualTo(claims2.getId());
    }

    @Test
    @DisplayName("Should handle user with non-verified email")
    void generateAccessToken_NonVerifiedEmail_ShouldIncludeFalseEmailVerified() {
        // Given
        testUser.setEmailVerified(false);

        // When
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Then
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("emailVerified", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when extracting claims from expired token")
    void extractAllClaims_ExpiredToken_ShouldThrowExpiredJwtException() {
        // Given - Create provider with very short expiration
        JwtTokenProvider shortProvider = new JwtTokenProvider(
            "ExpiredTokenJwtSigningKeyForTestingExpirationExceptions1234567890123456789",
            1L, 1L, 1L, "test-issuer");
        String expiredToken = shortProvider.generateAccessToken(testUser, testPersona);

        // Wait for expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then - Use the same provider that created the token to extract claims
        assertThatThrownBy(() -> shortProvider.extractAllClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should throw exception when extracting claims from malformed token")
    void extractAllClaims_MalformedToken_ShouldThrowMalformedJwtException() {
        // Given
        String malformedToken = "not.a.jwt";

        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.extractAllClaims(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }
}