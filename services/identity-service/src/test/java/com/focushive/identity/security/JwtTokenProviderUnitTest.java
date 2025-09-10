package com.focushive.identity.security;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Persona.PersonaType;
import com.focushive.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderUnitTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private Persona testPersona;
    
    // Test configuration values
    private static final String TEST_SECRET = "testSecretKeyThatIsLongEnoughForHS512AlgorithmToWorkProperlyAndMeetsThe256BitRequirement";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 2592000000L; // 30 days
    private static final long REMEMBER_ME_EXPIRATION = 7776000000L; // 90 days
    private static final String TEST_ISSUER = "identity-service-test";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
            TEST_SECRET,
            ACCESS_TOKEN_EXPIRATION,
            REFRESH_TOKEN_EXPIRATION,
            REMEMBER_ME_EXPIRATION,
            TEST_ISSUER
        );
        
        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setEmailVerified(true);
        
        // Create test persona
        testPersona = new Persona();
        testPersona.setId(UUID.randomUUID());
        testPersona.setName("Test Persona");
        testPersona.setType(PersonaType.WORK);
        testPersona.setUser(testUser);
    }

    @Test
    @DisplayName("Should generate valid access token with user and persona information")
    void shouldGenerateValidAccessToken() {
        // Given
        // When
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // Then
        assertThat(token).isNotNull().isNotEmpty();
        
        // Verify token structure (JWT has 3 parts separated by dots)
        String[] tokenParts = token.split("\\.");
        assertThat(tokenParts).hasSize(3);
        
        // Verify token is valid
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        
        // Verify claims
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("email")).isEqualTo(testUser.getEmail());
        assertThat(claims.get("displayName")).isEqualTo(testUser.getUsername());
        assertThat(claims.get("emailVerified")).isEqualTo(testUser.isEmailVerified());
        assertThat(claims.get("personaId")).isEqualTo(testPersona.getId().toString());
        assertThat(claims.get("personaName")).isEqualTo(testPersona.getName());
        assertThat(claims.get("personaType")).isEqualTo(testPersona.getType().name());
        assertThat(claims.get("type")).isEqualTo("access");
        assertThat(claims.getSubject()).isEqualTo(testUser.getUsername());
        assertThat(claims.getIssuer()).isEqualTo(TEST_ISSUER);
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        // Given
        // When
        String token = jwtTokenProvider.generateRefreshToken(testUser);
        
        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("type")).isEqualTo("refresh");
        assertThat(claims.getSubject()).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("Should generate valid long-lived refresh token with remember me flag")
    void shouldGenerateLongLivedRefreshToken() {
        // Given
        // When
        String token = jwtTokenProvider.generateLongLivedRefreshToken(testUser);
        
        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("type")).isEqualTo("refresh");
        assertThat(claims.get("rememberMe")).isEqualTo(true);
        assertThat(claims.getSubject()).isEqualTo(testUser.getUsername());
        
        // Verify expiration is longer than regular refresh token
        Date expiration = claims.getExpiration();
        Date now = new Date();
        long expirationMs = expiration.getTime() - now.getTime();
        assertThat(expirationMs).isGreaterThan(REFRESH_TOKEN_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate OAuth2 token with custom claims and expiration")
    void shouldGenerateOAuth2Token() {
        // Given
        String subject = "oauth2-user";
        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("scope", "read write");
        customClaims.put("client_id", "test-client");
        int expirationSeconds = 1800; // 30 minutes
        
        // When
        String token = jwtTokenProvider.generateToken(subject, customClaims, expirationSeconds);
        
        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        
        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.get("scope")).isEqualTo("read write");
        assertThat(claims.get("client_id")).isEqualTo("test-client");
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        String username = jwtTokenProvider.extractUsername(token);
        
        // Then
        assertThat(username).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("Should extract user ID from token")
    void shouldExtractUserId() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        UUID userId = jwtTokenProvider.extractUserId(token);
        
        // Then
        assertThat(userId).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should extract persona ID from token")
    void shouldExtractPersonaId() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        UUID personaId = jwtTokenProvider.extractPersonaId(token);
        
        // Then
        assertThat(personaId).isEqualTo(testPersona.getId());
    }

    @Test
    @DisplayName("Should return null when extracting persona ID from token without persona")
    void shouldReturnNullWhenExtractingPersonaIdFromTokenWithoutPersona() {
        // Given
        String token = jwtTokenProvider.generateRefreshToken(testUser);
        
        // When
        UUID personaId = jwtTokenProvider.extractPersonaId(token);
        
        // Then
        assertThat(personaId).isNull();
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmail() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        String email = jwtTokenProvider.extractEmail(token);
        
        // Then
        assertThat(email).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void shouldExtractExpiration() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        Date expiration = jwtTokenProvider.extractExpiration(token);
        
        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration.getTime()).isGreaterThan(new Date().getTime());
    }

    @Test
    @DisplayName("Should extract issued at date from token")
    void shouldExtractIssuedAt() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        Date issuedAt = jwtTokenProvider.extractIssuedAt(token);
        
        // Then
        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt.getTime()).isLessThanOrEqualTo(new Date().getTime());
    }

    @Test
    @DisplayName("Should check if token is not expired for valid token")
    void shouldCheckTokenIsNotExpired() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);
        
        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should check if token is expired for invalid token")
    void shouldCheckTokenIsExpiredForInvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        
        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(invalidToken);
        
        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should get access token expiration in seconds")
    void shouldGetAccessTokenExpirationSeconds() {
        // When
        long expirationSeconds = jwtTokenProvider.getAccessTokenExpirationSeconds();
        
        // Then
        assertThat(expirationSeconds).isEqualTo(ACCESS_TOKEN_EXPIRATION / 1000);
    }

    @Test
    @DisplayName("Should get user ID from token as string")
    void shouldGetUserIdFromToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        
        // Then
        assertThat(userId).isEqualTo(testUser.getId().toString());
    }

    @Test
    @DisplayName("Should get persona ID from token as string")
    void shouldGetPersonaIdFromToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        String personaId = jwtTokenProvider.getPersonaIdFromToken(token);
        
        // Then
        assertThat(personaId).isEqualTo(testPersona.getId().toString());
    }

    @Test
    @DisplayName("Should get expiration from token as LocalDateTime")
    void shouldGetExpirationFromTokenAsLocalDateTime() {
        // Given
        LocalDateTime beforeGeneration = LocalDateTime.now(java.time.ZoneOffset.UTC);
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        LocalDateTime expiration = jwtTokenProvider.getExpirationFromToken(token);
        
        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(beforeGeneration.plusMinutes(50)); // Should be ~1 hour from now
    }

    @Test
    @DisplayName("Should get claims from token")
    void shouldGetClaimsFromToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        
        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.get("userId")).isEqualTo(testUser.getId().toString());
        assertThat(claims.get("personaId")).isEqualTo(testPersona.getId().toString());
    }

    @Test
    @DisplayName("Should get JWK set for token verification")
    void shouldGetJwkSet() {
        // When
        Map<String, Object> jwkSet = jwtTokenProvider.getJwkSet();
        
        // Then
        assertThat(jwkSet).isNotNull();
        assertThat(jwkSet).containsKey("keys");
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> keys = (java.util.List<Map<String, Object>>) jwkSet.get("keys");
        assertThat(keys).hasSize(1);
        
        Map<String, Object> key = keys.get(0);
        assertThat(key.get("kty")).isEqualTo("oct");
        assertThat(key.get("use")).isEqualTo("sig");
        assertThat(key.get("kid")).isEqualTo("1");
        assertThat(key.get("alg")).isEqualTo("HS512");
    }

    @Test
    @DisplayName("Should validate valid token")
    void shouldValidateValidToken() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(token);
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should not validate malformed token")
    void shouldNotValidateMalformedToken() {
        // Given
        String malformedToken = "invalid.token.format";
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should not validate token with wrong signature")
    void shouldNotValidateTokenWithWrongSignature() {
        // Given
        JwtTokenProvider otherProvider = new JwtTokenProvider(
            "differentSecretKeyThatWillMakeSignatureInvalidAndMeetsThe256BitRequirementForHS512Algorithm",
            ACCESS_TOKEN_EXPIRATION,
            REFRESH_TOKEN_EXPIRATION,
            REMEMBER_ME_EXPIRATION,
            TEST_ISSUER
        );
        String tokenFromOtherProvider = otherProvider.generateAccessToken(testUser, testPersona);
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(tokenFromOtherProvider);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should not validate empty token")
    void shouldNotValidateEmptyToken() {
        // Given
        String emptyToken = "";
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should not validate null token")
    void shouldNotValidateNullToken() {
        // Given
        String nullToken = null;
        
        // When
        boolean isValid = jwtTokenProvider.validateToken(nullToken);
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when extracting claims from invalid token")
    void shouldThrowExceptionWhenExtractingClaimsFromInvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        
        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.extractAllClaims(invalidToken))
            .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Should extract custom claim using claims resolver")
    void shouldExtractCustomClaimUsingClaimsResolver() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // When
        String personaType = jwtTokenProvider.extractClaim(token, claims -> claims.get("personaType", String.class));
        
        // Then
        assertThat(personaType).isEqualTo(testPersona.getType().name());
    }

    @Test
    @DisplayName("Should generate tokens with unique JWT IDs")
    void shouldGenerateTokensWithUniqueJwtIds() {
        // Given
        // When
        String token1 = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        String token2 = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        
        // Then
        assertThat(token1).isNotEqualTo(token2);
        
        Claims claims1 = jwtTokenProvider.extractAllClaims(token1);
        Claims claims2 = jwtTokenProvider.extractAllClaims(token2);
        
        assertThat(claims1.getId()).isNotEqualTo(claims2.getId());
    }

    @Test
    @DisplayName("Should handle token expiration gracefully")
    void shouldHandleTokenExpirationGracefully() {
        // Given - Create a token provider with very short expiration for testing
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
            TEST_SECRET,
            1L, // 1 millisecond expiration
            REFRESH_TOKEN_EXPIRATION,
            REMEMBER_ME_EXPIRATION,
            TEST_ISSUER
        );
        
        String token = shortExpirationProvider.generateAccessToken(testUser, testPersona);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        boolean isValid = shortExpirationProvider.validateToken(token);
        boolean isExpired = shortExpirationProvider.isTokenExpired(token);
        
        // Then
        assertThat(isValid).isFalse();
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Should maintain consistent token structure across different token types")
    void shouldMaintainConsistentTokenStructure() {
        // Given
        String accessToken = jwtTokenProvider.generateAccessToken(testUser, testPersona);
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);
        String longLivedToken = jwtTokenProvider.generateLongLivedRefreshToken(testUser);
        
        // When
        Claims accessClaims = jwtTokenProvider.extractAllClaims(accessToken);
        Claims refreshClaims = jwtTokenProvider.extractAllClaims(refreshToken);
        Claims longLivedClaims = jwtTokenProvider.extractAllClaims(longLivedToken);
        
        // Then
        // All tokens should have consistent structure
        assertThat(accessClaims.getSubject()).isEqualTo(testUser.getUsername());
        assertThat(refreshClaims.getSubject()).isEqualTo(testUser.getUsername());
        assertThat(longLivedClaims.getSubject()).isEqualTo(testUser.getUsername());
        
        assertThat(accessClaims.getIssuer()).isEqualTo(TEST_ISSUER);
        assertThat(refreshClaims.getIssuer()).isEqualTo(TEST_ISSUER);
        assertThat(longLivedClaims.getIssuer()).isEqualTo(TEST_ISSUER);
        
        // All should have unique IDs
        assertThat(accessClaims.getId()).isNotNull();
        assertThat(refreshClaims.getId()).isNotNull();
        assertThat(longLivedClaims.getId()).isNotNull();
        assertThat(accessClaims.getId()).isNotEqualTo(refreshClaims.getId());
    }
}