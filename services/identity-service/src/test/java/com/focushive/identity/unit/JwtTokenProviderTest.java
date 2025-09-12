package com.focushive.identity.unit;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Role;
import com.focushive.identity.entity.User;
import com.focushive.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtTokenProvider - testing JWT token generation and validation.
 * This is a pure unit test without Spring context to avoid circular dependency issues.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private Persona testPersona;

    @BeforeEach
    void setUp() {
        // Initialize JWT provider with test secret - constructor needs 5 params
        String testSecret = "testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesAbcDefGhiJklMnoPqrStuVwxYz123456789AbcDef";
        long accessTokenExpiration = 3600000; // 1 hour in milliseconds
        long refreshTokenExpiration = 86400000; // 24 hours in milliseconds  
        long rememberMeExpiration = 7776000000L; // 90 days in milliseconds
        String issuer = "test-identity-service";
        
        jwtTokenProvider = new JwtTokenProvider(testSecret, accessTokenExpiration, refreshTokenExpiration, rememberMeExpiration, issuer);

        // Create test user
        testUser = new User();
        testUser.setId(java.util.UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);

        // Create test persona
        testPersona = new Persona();
        testPersona.setId(java.util.UUID.randomUUID());
        testPersona.setName("Default");
        testPersona.setType(Persona.PersonaType.PERSONAL);
        testPersona.setDefault(true);
        testPersona.setUser(testUser);
    }

    @Test
    void testGenerateAccessToken_ValidUser_Success() {
        // Act
        String accessToken = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Assert
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotBlank();
        assertThat(accessToken.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
    }

    @Test
    void testGenerateRefreshToken_ValidUser_Success() {
        // Act
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        // Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotBlank();
        assertThat(refreshToken.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
    }

    @Test
    void testExtractUsername_ValidToken_ReturnsUsername() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Act
        String extractedUsername = jwtTokenProvider.extractUsername(token);

        // Assert
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtTokenProvider.generateAccessToken(testUser, testPersona);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Assert
        assertThat(isValid).isFalse();
    }
}