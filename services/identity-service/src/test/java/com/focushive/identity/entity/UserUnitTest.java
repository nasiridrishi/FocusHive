package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for User entity covering builder patterns,
 * business logic, relationships, UserDetails implementation, and edge cases.
 */
@DisplayName("User Entity Unit Tests")
class UserUnitTest {

    private Map<String, Boolean> testNotificationPreferences;
    private Persona testPersona1;
    private Persona testPersona2;
    private OAuthClient testOAuthClient;

    @BeforeEach
    void setUp() {
        testNotificationPreferences = new HashMap<>();
        testNotificationPreferences.put("email", true);
        testNotificationPreferences.put("sms", false);
        testNotificationPreferences.put("push", true);

        testPersona1 = Persona.builder()
                .id(UUID.randomUUID())
                .name("Work")
                .bio("Professional persona")
                .isDefault(true)
                .isActive(true)
                .build();

        testPersona2 = Persona.builder()
                .id(UUID.randomUUID())
                .name("Personal")
                .bio("Personal activities")
                .isDefault(false)
                .isActive(false)
                .build();

        testOAuthClient = OAuthClient.builder()
                .id(UUID.randomUUID())
                .clientId("test-client")
                .clientName("Test Client")
                .build();
    }

    @Test
    @DisplayName("Should create User using builder with all fields")
    void shouldCreateUserUsingBuilderWithAllFields() {
        // Given & When
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(true)
                .emailVerificationToken("verify-token")
                .passwordResetToken("reset-token")
                .passwordResetTokenExpiry(now.plusSeconds(3600))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .twoFactorEnabled(false)
                .twoFactorSecret("2FA-SECRET")
                .preferredLanguage("en-US")
                .timezone("America/New_York")
                .notificationPreferences(testNotificationPreferences)
                .createdAt(now)
                .updatedAt(now)
                .lastLoginAt(now.minusSeconds(300))
                .lastLoginIp("192.168.1.1")
                .build();

        // Then
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPassword()).isEqualTo("hashedPassword");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationToken()).isEqualTo("verify-token");
        assertThat(user.getPasswordResetToken()).isEqualTo("reset-token");
        assertThat(user.getPasswordResetTokenExpiry()).isEqualTo(now.plusSeconds(3600));
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isTwoFactorEnabled()).isFalse();
        assertThat(user.getTwoFactorSecret()).isEqualTo("2FA-SECRET");
        assertThat(user.getPreferredLanguage()).isEqualTo("en-US");
        assertThat(user.getTimezone()).isEqualTo("America/New_York");
        assertThat(user.getNotificationPreferences()).isEqualTo(testNotificationPreferences);
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
        assertThat(user.getLastLoginAt()).isEqualTo(now.minusSeconds(300));
        assertThat(user.getLastLoginIp()).isEqualTo("192.168.1.1");
        assertThat(user.getPersonas()).isEmpty();
        assertThat(user.getOauthClients()).isEmpty();
    }

    @Test
    @DisplayName("Should create User with default values")
    void shouldCreateUserWithDefaultValues() {
        // Given & When
        User user = User.builder()
                .username("defaultuser")
                .email("default@example.com")
                .password("password")
                .firstName("Default")
                .lastName("User")
                .build();

        // Then
        assertThat(user.isEmailVerified()).isFalse(); // @Builder.Default
        assertThat(user.isEnabled()).isTrue(); // @Builder.Default
        assertThat(user.isAccountNonExpired()).isTrue(); // @Builder.Default
        assertThat(user.isAccountNonLocked()).isTrue(); // @Builder.Default
        assertThat(user.isCredentialsNonExpired()).isTrue(); // @Builder.Default
        assertThat(user.isTwoFactorEnabled()).isFalse(); // @Builder.Default
        assertThat(user.getPreferredLanguage()).isEqualTo("en"); // @Builder.Default
        assertThat(user.getTimezone()).isEqualTo("UTC"); // @Builder.Default
        assertThat(user.getNotificationPreferences()).isEmpty(); // @Builder.Default
        assertThat(user.getPersonas()).isEmpty(); // @Builder.Default
        assertThat(user.getOauthClients()).isEmpty(); // @Builder.Default
    }

    @Test
    @DisplayName("Should create User using no-args constructor")
    void shouldCreateUserUsingNoArgsConstructor() {
        // Given & When
        User user = new User();

        // Then
        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getFirstName()).isNull();
        assertThat(user.getLastName()).isNull();
        assertThat(user.getPersonas()).isNotNull().isEmpty();
        assertThat(user.getOauthClients()).isNotNull().isEmpty();
        assertThat(user.getNotificationPreferences()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should create User using all-args constructor")
    void shouldCreateUserUsingAllArgsConstructor() {
        // Given
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        List<Persona> personas = new ArrayList<>(Arrays.asList(testPersona1, testPersona2));
        Set<OAuthClient> oauthClients = new HashSet<>(Arrays.asList(testOAuthClient));

        // When
        User user = new User(
                userId, "allArgsUser", "allargs@example.com", "password",
                "All", "Args", true, "verify-token", "reset-token", now.plusSeconds(3600),
                true, true, true, true, false, "2fa-secret",
                "fr-FR", "Europe/Paris", testNotificationPreferences, personas, oauthClients,
                now, now, now.minusSeconds(300), "10.0.0.1", null
        );

        // Then
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getUsername()).isEqualTo("allArgsUser");
        assertThat(user.getEmail()).isEqualTo("allargs@example.com");
        assertThat(user.getPersonas()).hasSize(2);
        assertThat(user.getOauthClients()).hasSize(1);
    }

    // UserDetails implementation tests
    @Test
    @DisplayName("Should implement UserDetails getAuthorities correctly")
    void shouldImplementUserDetailsGetAuthoritiesCorrectly() {
        // Given
        User user = createTestUser();

        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Then
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should implement UserDetails isEnabled correctly when user is enabled and not deleted")
    void shouldImplementUserDetailsIsEnabledCorrectlyWhenEnabledAndNotDeleted() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .deletedAt(null)
                .build();

        // Then
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should implement UserDetails isEnabled correctly when user is disabled")
    void shouldImplementUserDetailsIsEnabledCorrectlyWhenDisabled() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .enabled(false)
                .deletedAt(null)
                .build();

        // Then
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should implement UserDetails isEnabled correctly when user is deleted")
    void shouldImplementUserDetailsIsEnabledCorrectlyWhenDeleted() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .deletedAt(Instant.now())
                .build();

        // Then
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should implement other UserDetails methods correctly")
    void shouldImplementOtherUserDetailsMethodsCorrectly() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // Then
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    // Persona management tests
    @Test
    @DisplayName("Should add persona to user correctly")
    void shouldAddPersonaToUserCorrectly() {
        // Given
        User user = createTestUser();
        Persona persona = createTestPersona("Test Persona");

        // When
        user.addPersona(persona);

        // Then
        assertThat(user.getPersonas()).hasSize(1);
        assertThat(user.getPersonas()).contains(persona);
        assertThat(persona.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should remove persona from user correctly")
    void shouldRemovePersonaFromUserCorrectly() {
        // Given
        User user = createTestUser();
        Persona persona = createTestPersona("Test Persona");
        user.addPersona(persona);

        // When
        user.removePersona(persona);

        // Then
        assertThat(user.getPersonas()).isEmpty();
        assertThat(persona.getUser()).isNull();
    }

    @Test
    @DisplayName("Should get active persona correctly when one exists")
    void shouldGetActivePersonaCorrectlyWhenOneExists() {
        // Given
        User user = createTestUser();
        Persona activePersona = createTestPersona("Active Persona");
        activePersona.setActive(true);
        Persona inactivePersona = createTestPersona("Inactive Persona");
        inactivePersona.setActive(false);
        
        user.addPersona(activePersona);
        user.addPersona(inactivePersona);

        // When
        Optional<Persona> result = user.getActivePersona();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(activePersona);
    }

    @Test
    @DisplayName("Should return empty when no active persona exists")
    void shouldReturnEmptyWhenNoActivePersonaExists() {
        // Given
        User user = createTestUser();
        Persona inactivePersona = createTestPersona("Inactive Persona");
        inactivePersona.setActive(false);
        user.addPersona(inactivePersona);

        // When
        Optional<Persona> result = user.getActivePersona();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get default persona correctly when one exists")
    void shouldGetDefaultPersonaCorrectlyWhenOneExists() {
        // Given
        User user = createTestUser();
        Persona defaultPersona = createTestPersona("Default Persona");
        defaultPersona.setDefault(true);
        Persona nonDefaultPersona = createTestPersona("Non-Default Persona");
        nonDefaultPersona.setDefault(false);
        
        user.addPersona(defaultPersona);
        user.addPersona(nonDefaultPersona);

        // When
        Optional<Persona> result = user.getDefaultPersona();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(defaultPersona);
    }

    @Test
    @DisplayName("Should return empty when no default persona exists")
    void shouldReturnEmptyWhenNoDefaultPersonaExists() {
        // Given
        User user = createTestUser();
        Persona nonDefaultPersona = createTestPersona("Non-Default Persona");
        nonDefaultPersona.setDefault(false);
        user.addPersona(nonDefaultPersona);

        // When
        Optional<Persona> result = user.getDefaultPersona();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple personas correctly")
    void shouldHandleMultiplePersonasCorrectly() {
        // Given
        User user = createTestUser();
        List<Persona> personas = Arrays.asList(
                createTestPersona("Work"),
                createTestPersona("Personal"),
                createTestPersona("Study")
        );

        // When
        personas.forEach(user::addPersona);

        // Then
        assertThat(user.getPersonas()).hasSize(3);
        personas.forEach(persona -> {
            assertThat(persona.getUser()).isEqualTo(user);
        });
    }

    // Equality and hashCode tests
    @Test
    @DisplayName("Should test equality with same ID")
    void shouldTestEqualityWithSameId() {
        // Given
        UUID sameId = UUID.randomUUID();
        User user1 = User.builder()
                .id(sameId)
                .username("user1")
                .email("user1@example.com")
                .password("password1")
                .firstName("User")
                .lastName("One")
                .build();

        User user2 = User.builder()
                .id(sameId)
                .username("user2")
                .email("user2@example.com")
                .password("password2")
                .firstName("User")
                .lastName("Two")
                .build();

        // Then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different ID")
    void shouldTestInequalityWithDifferentId() {
        // Given
        User user1 = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .password("password")
                .firstName("User")
                .lastName("One")
                .build();

        User user2 = User.builder()
                .id(UUID.randomUUID())
                .username("user1")
                .email("user1@example.com")
                .password("password")
                .firstName("User")
                .lastName("One")
                .build();

        // Then
        assertThat(user1).isNotEqualTo(user2);
        assertThat(user1.hashCode()).isNotEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("Should exclude personas and oauthClients from equals and hashCode")
    void shouldExcludePersonasAndOauthClientsFromEqualsAndHashCode() {
        // Given
        UUID sameId = UUID.randomUUID();
        User user1 = User.builder()
                .id(sameId)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .build();

        User user2 = User.builder()
                .id(sameId)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .build();

        // Add different personas to each user
        user1.addPersona(createTestPersona("Persona1"));
        user2.addPersona(createTestPersona("Persona2"));

        // Then - should still be equal despite different personas
        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    // toString tests
    @Test
    @DisplayName("Should exclude password, personas, and oauthClients from toString")
    void shouldExcludePasswordPersonasAndOauthClientsFromToString() {
        // Given
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("secretPassword")
                .firstName("Test")
                .lastName("User")
                .build();

        user.addPersona(createTestPersona("Test Persona"));

        // When
        String toString = user.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("username=testuser");
        assertThat(toString).contains("email=test@example.com");
        assertThat(toString).contains("firstName=Test");
        assertThat(toString).contains("lastName=User");
        
        // Should NOT contain excluded fields
        assertThat(toString).doesNotContain("secretPassword");
        assertThat(toString).doesNotContain("personas");
        assertThat(toString).doesNotContain("oauthClients");
    }

    // Notification preferences tests
    @Test
    @DisplayName("Should handle notification preferences correctly")
    void shouldHandleNotificationPreferencesCorrectly() {
        // Given
        Map<String, Boolean> preferences = new HashMap<>();
        preferences.put("email", true);
        preferences.put("sms", false);
        preferences.put("push", true);
        preferences.put("desktop", false);

        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .notificationPreferences(preferences)
                .build();

        // Then
        assertThat(user.getNotificationPreferences()).hasSize(4);
        assertThat(user.getNotificationPreferences().get("email")).isTrue();
        assertThat(user.getNotificationPreferences().get("sms")).isFalse();
        assertThat(user.getNotificationPreferences().get("push")).isTrue();
        assertThat(user.getNotificationPreferences().get("desktop")).isFalse();
    }

    @Test
    @DisplayName("Should handle empty notification preferences")
    void shouldHandleEmptyNotificationPreferences() {
        // Given
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .firstName("Test")
                .lastName("User")
                .build();

        // Then
        assertThat(user.getNotificationPreferences()).isNotNull();
        assertThat(user.getNotificationPreferences()).isEmpty();
    }

    // Edge cases and null handling
    @Test
    @DisplayName("Should handle null persona operations safely")
    void shouldHandleNullPersonaOperationsSafely() {
        // Given
        User user = createTestUser();

        // When & Then - should not throw exceptions
        assertThatCode(() -> user.addPersona(null)).doesNotThrowAnyException();
        assertThatCode(() -> user.removePersona(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle special characters in string fields")
    void shouldHandleSpecialCharactersInStringFields() {
        // Given
        String specialChars = "Special!@#$%^&*()";
        String unicodeChars = "üñïçødé 测试";

        // When
        User user = User.builder()
                .username("user" + specialChars)
                .email("special@example.com")
                .password("pass" + specialChars)
                .firstName(unicodeChars)
                .lastName("LastName" + specialChars)
                .preferredLanguage("zh-CN")
                .timezone("Asia/Shanghai")
                .lastLoginIp("192.168.1.100")
                .build();

        // Then
        assertThat(user.getUsername()).contains(specialChars);
        assertThat(user.getFirstName()).isEqualTo(unicodeChars);
        assertThat(user.getLastName()).contains(specialChars);
        assertThat(user.getPreferredLanguage()).isEqualTo("zh-CN");
        assertThat(user.getTimezone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    @DisplayName("Should handle password reset token expiry correctly")
    void shouldHandlePasswordResetTokenExpiryCorrectly() {
        // Given
        Instant futureExpiry = Instant.now().plusSeconds(3600);
        Instant pastExpiry = Instant.now().minusSeconds(3600);

        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .password("password")
                .firstName("User")
                .lastName("One")
                .passwordResetTokenExpiry(futureExpiry)
                .build();

        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .password("password")
                .firstName("User")
                .lastName("Two")
                .passwordResetTokenExpiry(pastExpiry)
                .build();

        // Then
        assertThat(user1.getPasswordResetTokenExpiry()).isAfter(Instant.now());
        assertThat(user2.getPasswordResetTokenExpiry()).isBefore(Instant.now());
    }

    @Test
    @DisplayName("Should handle two-factor authentication fields correctly")
    void shouldHandleTwoFactorAuthenticationFieldsCorrectly() {
        // Given
        User user = User.builder()
                .username("2fauser")
                .email("2fa@example.com")
                .password("password")
                .firstName("TwoFA")
                .lastName("User")
                .twoFactorEnabled(true)
                .twoFactorSecret("JBSWY3DPEHPK3PXP")
                .build();

        // Then
        assertThat(user.isTwoFactorEnabled()).isTrue();
        assertThat(user.getTwoFactorSecret()).isEqualTo("JBSWY3DPEHPK3PXP");
    }

    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    private Persona createTestPersona(String name) {
        return Persona.builder()
                .id(UUID.randomUUID())
                .name(name)
                .bio(name + " persona")
                .isDefault(false)
                .isActive(false)
                .build();
    }
}