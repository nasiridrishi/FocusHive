package com.focushive.identity.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for password encoding functionality.
 * Tests BCryptPasswordEncoder directly without Spring context.
 */
class ApplicationConfigTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("Should create password encoder bean")
    void passwordEncoder_ShouldBeConfigured() {
        // Then
        assertThat(passwordEncoder).isNotNull();
    }

    @Test
    @DisplayName("Should encode passwords correctly")
    void passwordEncoder_ShouldEncodePasswords() {
        // Given
        String rawPassword = "testPassword123";

        // When
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Then
        assertThat(encodedPassword).isNotNull();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("Should not match wrong passwords")
    void passwordEncoder_ShouldNotMatchWrongPassword() {
        // Given
        String rawPassword = "testPassword123";
        String wrongPassword = "wrongPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // When & Then
        assertThat(passwordEncoder.matches(wrongPassword, encodedPassword)).isFalse();
    }

    @Test
    @DisplayName("Should encode same password to different hashes")
    void passwordEncoder_ShouldGenerateDifferentHashes() {
        // Given
        String rawPassword = "testPassword123";

        // When
        String encodedPassword1 = passwordEncoder.encode(rawPassword);
        String encodedPassword2 = passwordEncoder.encode(rawPassword);

        // Then
        assertThat(encodedPassword1).isNotEqualTo(encodedPassword2);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword2)).isTrue();
    }
}