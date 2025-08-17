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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for OAuthAccessToken entity
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class OAuthAccessTokenTest {

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private OAuthClient testClient;

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
    }

    @Test
    void shouldCreateOAuthAccessToken() {
        // Given
        OAuthAccessToken token = OAuthAccessToken.builder()
                .tokenHash("hashed-token-value")
                .user(testUser)
                .client(testClient)
                .scopes(Set.of("read", "write"))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        // When
        OAuthAccessToken savedToken = entityManager.persistAndFlush(token);

        // Then
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getTokenHash()).isEqualTo("hashed-token-value");
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getClient()).isEqualTo(testClient);
        assertThat(savedToken.getScopes()).containsExactlyInAnyOrder("read", "write");
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        OAuthAccessToken token = OAuthAccessToken.builder()
                .tokenHash("hashed-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        // When
        OAuthAccessToken savedToken = entityManager.persistAndFlush(token);

        // Then
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getScopes()).isEmpty();
    }

    @Test
    void shouldHandleTokenRevocation() {
        // Given
        OAuthAccessToken token = OAuthAccessToken.builder()
                .tokenHash("hashed-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();
        OAuthAccessToken savedToken = entityManager.persistAndFlush(token);

        // When
        savedToken.setRevoked(true);
        savedToken.setRevokedAt(Instant.now());
        OAuthAccessToken updatedToken = entityManager.persistAndFlush(savedToken);

        // Then
        assertThat(updatedToken.isRevoked()).isTrue();
        assertThat(updatedToken.getRevokedAt()).isNotNull();
    }

    @Test
    void shouldValidateTokenExpiry() {
        // Given
        Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant futureTime = Instant.now().plus(1, ChronoUnit.HOURS);

        OAuthAccessToken expiredToken = OAuthAccessToken.builder()
                .tokenHash("expired-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(pastTime)
                .build();

        OAuthAccessToken validToken = OAuthAccessToken.builder()
                .tokenHash("valid-token")
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
        OAuthAccessToken validToken = OAuthAccessToken.builder()
                .tokenHash("valid-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        OAuthAccessToken revokedToken = OAuthAccessToken.builder()
                .tokenHash("revoked-token")
                .user(testUser)
                .client(testClient)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .revoked(true)
                .build();

        // When
        entityManager.persistAndFlush(validToken);
        entityManager.persistAndFlush(revokedToken);

        // Then
        assertThat(validToken.isValid()).isTrue();
        assertThat(revokedToken.isValid()).isFalse();
    }
}