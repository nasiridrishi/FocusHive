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
 * Test cases for OAuthAuthorizationCode entity
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class OAuthAuthorizationCodeTest {

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
    void shouldCreateOAuthAuthorizationCode() {
        // Given
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
                .code("authorization-code-123")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .scopes(Set.of("read", "write"))
                .state("random-state-value")
                .codeChallenge("challenge")
                .codeChallengeMethod("S256")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        // When
        OAuthAuthorizationCode savedCode = entityManager.persistAndFlush(authCode);

        // Then
        assertThat(savedCode.getId()).isNotNull();
        assertThat(savedCode.getCode()).isEqualTo("authorization-code-123");
        assertThat(savedCode.getUser()).isEqualTo(testUser);
        assertThat(savedCode.getClient()).isEqualTo(testClient);
        assertThat(savedCode.getRedirectUri()).isEqualTo("https://example.com/callback");
        assertThat(savedCode.getScopes()).containsExactlyInAnyOrder("read", "write");
        assertThat(savedCode.getState()).isEqualTo("random-state-value");
        assertThat(savedCode.getCodeChallenge()).isEqualTo("challenge");
        assertThat(savedCode.getCodeChallengeMethod()).isEqualTo("S256");
        assertThat(savedCode.getCreatedAt()).isNotNull();
        assertThat(savedCode.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedCode.isUsed()).isFalse();
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
                .code("authorization-code-123")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        // When
        OAuthAuthorizationCode savedCode = entityManager.persistAndFlush(authCode);

        // Then
        assertThat(savedCode.isUsed()).isFalse();
        assertThat(savedCode.getCreatedAt()).isNotNull();
        assertThat(savedCode.getScopes()).isEmpty();
        assertThat(savedCode.getState()).isNull();
        assertThat(savedCode.getCodeChallenge()).isNull();
        assertThat(savedCode.getCodeChallengeMethod()).isNull();
    }

    @Test
    void shouldHandleCodeUsage() {
        // Given
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
                .code("authorization-code-123")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
        OAuthAuthorizationCode savedCode = entityManager.persistAndFlush(authCode);

        // When
        savedCode.setUsed(true);
        savedCode.setUsedAt(Instant.now());
        OAuthAuthorizationCode updatedCode = entityManager.persistAndFlush(savedCode);

        // Then
        assertThat(updatedCode.isUsed()).isTrue();
        assertThat(updatedCode.getUsedAt()).isNotNull();
    }

    @Test
    void shouldValidateCodeExpiry() {
        // Given
        Instant pastTime = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant futureTime = Instant.now().plus(10, ChronoUnit.MINUTES);

        OAuthAuthorizationCode expiredCode = OAuthAuthorizationCode.builder()
                .code("expired-code")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .expiresAt(pastTime)
                .build();

        OAuthAuthorizationCode validCode = OAuthAuthorizationCode.builder()
                .code("valid-code")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .expiresAt(futureTime)
                .build();

        // When
        entityManager.persistAndFlush(expiredCode);
        entityManager.persistAndFlush(validCode);

        // Then
        assertThat(expiredCode.isExpired()).isTrue();
        assertThat(validCode.isExpired()).isFalse();
    }

    @Test
    void shouldCheckCodeValidity() {
        // Given
        OAuthAuthorizationCode validCode = OAuthAuthorizationCode.builder()
                .code("valid-code")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        OAuthAuthorizationCode usedCode = OAuthAuthorizationCode.builder()
                .code("used-code")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(true)
                .build();

        // When
        entityManager.persistAndFlush(validCode);
        entityManager.persistAndFlush(usedCode);

        // Then
        assertThat(validCode.isValid()).isTrue();
        assertThat(usedCode.isValid()).isFalse();
    }

    @Test
    void shouldSupportPKCE() {
        // Given - PKCE (Proof Key for Code Exchange) authorization code
        OAuthAuthorizationCode pkceCode = OAuthAuthorizationCode.builder()
                .code("pkce-authorization-code")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .scopes(Set.of("read"))
                .codeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
                .codeChallengeMethod("S256")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        // When
        OAuthAuthorizationCode savedCode = entityManager.persistAndFlush(pkceCode);

        // Then
        assertThat(savedCode.getCodeChallenge()).isEqualTo("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");
        assertThat(savedCode.getCodeChallengeMethod()).isEqualTo("S256");
        assertThat(savedCode.isUsingPKCE()).isTrue();
    }

    @Test
    void shouldDetectNonPKCEFlow() {
        // Given - Regular authorization code without PKCE
        OAuthAuthorizationCode regularCode = OAuthAuthorizationCode.builder()
                .code("regular-authorization-code")
                .user(testUser)
                .client(testClient)
                .redirectUri("https://example.com/callback")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();

        // When
        OAuthAuthorizationCode savedCode = entityManager.persistAndFlush(regularCode);

        // Then
        assertThat(savedCode.getCodeChallenge()).isNull();
        assertThat(savedCode.getCodeChallengeMethod()).isNull();
        assertThat(savedCode.isUsingPKCE()).isFalse();
    }
}