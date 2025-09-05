package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for OAuthRefreshToken entity
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class OAuthRefreshTokenTest {

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private OAuthClient testClient;
    private OAuthAccessToken testAccessToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("hashedpassword")
                .displayName("Test User")
                .build();
        entityManager.persistAndFlush(testUser);

        // Create test OAuth client
        testClient = OAuthClient.builder()
                .clientId("test-client-id")
                .clientSecret("secret")
                .clientName("Test Client")
                .user(testUser)
                .build();
        entityManager.persistAndFlush(testClient);

        // Create test access token
        testAccessToken = OAuthAccessToken.builder()
                .tokenHash("access-token-hash")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        entityManager.persistAndFlush(testAccessToken);
    }

    @Test
    void shouldCreateOAuthRefreshToken() {
        // Given
        OAuthRefreshToken refreshToken = OAuthRefreshToken.builder()
                .tokenHash("refresh-token-hash")
                .user(testUser)
                .client(testClient)
                .accessToken(testAccessToken)
                .scopes(Set.of("read", "write"))
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        // When
        OAuthRefreshToken savedToken = entityManager.persistAndFlush(refreshToken);

        // Then
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getTokenHash()).isEqualTo("refresh-token-hash");
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getClient()).isEqualTo(testClient);
        assertThat(savedToken.getAccessToken()).isEqualTo(testAccessToken);
        assertThat(savedToken.getScopes()).containsExactlyInAnyOrder("read", "write");
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        OAuthRefreshToken refreshToken = OAuthRefreshToken.builder()
                .tokenHash("refresh-token-hash")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        // When
        OAuthRefreshToken savedToken = entityManager.persistAndFlush(refreshToken);

        // Then
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getScopes()).isEmpty();
        assertThat(savedToken.getAccessToken()).isNull(); // Optional relationship
    }

    @Test
    void shouldHandleTokenRevocation() {
        // Given
        OAuthRefreshToken refreshToken = OAuthRefreshToken.builder()
                .tokenHash("refresh-token-hash")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        OAuthRefreshToken savedToken = entityManager.persistAndFlush(refreshToken);

        // When
        savedToken.setRevoked(true);
        savedToken.setRevokedAt(Instant.now());
        OAuthRefreshToken updatedToken = entityManager.persistAndFlush(savedToken);

        // Then
        assertThat(updatedToken.isRevoked()).isTrue();
        assertThat(updatedToken.getRevokedAt()).isNotNull();
    }

    @Test
    void shouldValidateTokenExpiry() {
        // Given
        Instant pastTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant futureTime = Instant.now().plus(30, ChronoUnit.DAYS);

        OAuthRefreshToken expiredToken = OAuthRefreshToken.builder()
                .tokenHash("expired-refresh-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(pastTime)
                .build();

        OAuthRefreshToken validToken = OAuthRefreshToken.builder()
                .tokenHash("valid-refresh-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(futureTime)
                .build();

        // When
        entityManager.persistAndFlush(expiredToken);
        entityManager.persistAndFlush(validToken);

        // Then
        assertThat(expiredToken.isExpired()).isTrue();
        assertThat(validToken.isExpired()).isFalse();
    }

    @Test
    void shouldCheckTokenValidity() {
        // Given
        OAuthRefreshToken validToken = OAuthRefreshToken.builder()
                .tokenHash("valid-refresh-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        OAuthRefreshToken revokedToken = OAuthRefreshToken.builder()
                .tokenHash("revoked-refresh-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .revoked(true)
                .build();

        // When
        entityManager.persistAndFlush(validToken);
        entityManager.persistAndFlush(revokedToken);

        // Then
        assertThat(validToken.isValid()).isTrue();
        assertThat(revokedToken.isValid()).isFalse();
    }

    @Test
    void shouldHandleTokenRotation() {
        // Given
        OAuthRefreshToken originalToken = OAuthRefreshToken.builder()
                .tokenHash("original-refresh-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        entityManager.persistAndFlush(originalToken);

        // When creating a new refresh token that replaces the original
        OAuthRefreshToken newToken = OAuthRefreshToken.builder()
                .tokenHash("new-refresh-token")
                .user(testUser)
                .client(testClient)
                .replacedToken(originalToken)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        entityManager.persistAndFlush(newToken);

        // Then
        assertThat(newToken.getReplacedToken()).isEqualTo(originalToken);
        assertThat(newToken.getId()).isNotEqualTo(originalToken.getId());
    }
}