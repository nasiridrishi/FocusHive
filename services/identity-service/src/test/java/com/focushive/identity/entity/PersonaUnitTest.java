package com.focushive.identity.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for Persona entity.
 * Tests persona management, privacy settings, and custom attributes.
 */
@DisplayName("Persona Entity Unit Tests")
class PersonaUnitTest {

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create Persona using builder with all fields")
        void shouldCreatePersonaUsingBuilderWithAllFields() {
            // Given
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .build();
                    
            Map<String, String> customAttributes = new HashMap<>();
            customAttributes.put("theme", "dark");
            customAttributes.put("layout", "compact");
            
            Map<String, Boolean> notificationPrefs = new HashMap<>();
            notificationPrefs.put("email", true);
            notificationPrefs.put("push", false);
            
            Persona.PrivacySettings privacySettings = Persona.PrivacySettings.builder()
                    .showRealName(true)
                    .showEmail(false)
                    .allowDirectMessages(true)
                    .visibilityLevel("FRIENDS")
                    .build();

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Work Profile")
                    .type(Persona.PersonaType.WORK)
                    .isDefault(true)
                    .isActive(true)
                    .displayName("Work User")
                    .avatarUrl("https://example.com/avatar.jpg")
                    .bio("Professional work persona")
                    .statusMessage("Working hard!")
                    .privacySettings(privacySettings)
                    .customAttributes(customAttributes)
                    .notificationPreferences(notificationPrefs)
                    .themePreference("dark")
                    .languagePreference("en")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(persona).isNotNull(),
                    () -> assertThat(persona.getUser()).isEqualTo(user),
                    () -> assertThat(persona.getName()).isEqualTo("Work Profile"),
                    () -> assertThat(persona.getType()).isEqualTo(Persona.PersonaType.WORK),
                    () -> assertThat(persona.isDefault()).isTrue(),
                    () -> assertThat(persona.isActive()).isTrue(),
                    () -> assertThat(persona.getDisplayName()).isEqualTo("Work User"),
                    () -> assertThat(persona.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg"),
                    () -> assertThat(persona.getBio()).isEqualTo("Professional work persona"),
                    () -> assertThat(persona.getStatusMessage()).isEqualTo("Working hard!"),
                    () -> assertThat(persona.getPrivacySettings()).isEqualTo(privacySettings),
                    () -> assertThat(persona.getCustomAttributes()).hasSize(2),
                    () -> assertThat(persona.getNotificationPreferences()).hasSize(2),
                    () -> assertThat(persona.getThemePreference()).isEqualTo("dark"),
                    () -> assertThat(persona.getLanguagePreference()).isEqualTo("en")
            );
        }

        @Test
        @DisplayName("Should create Persona with default values")
        void shouldCreatePersonaWithDefaultValues() {
            // Given
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .build();

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test Profile")
                    .type(Persona.PersonaType.PERSONAL)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(persona).isNotNull(),
                    () -> assertThat(persona.isDefault()).isFalse(), // @Builder.Default
                    () -> assertThat(persona.isActive()).isFalse(), // @Builder.Default
                    () -> assertThat(persona.getPrivacySettings()).isNotNull(), // @Builder.Default creates new instance
                    () -> assertThat(persona.getCustomAttributes()).isNotNull(), // @Builder.Default creates HashMap
                    () -> assertThat(persona.getNotificationPreferences()).isNotNull(), // @Builder.Default creates HashMap
                    () -> assertThat(persona.getThemePreference()).isEqualTo("system") // @Builder.Default
            );
        }
    }

    @Nested
    @DisplayName("PersonaType Enum Tests")
    class PersonaTypeTests {

        @Test
        @DisplayName("Should have correct display names for all persona types")
        void shouldHaveCorrectDisplayNamesForAllPersonaTypes() {
            // Then
            assertAll(
                    () -> assertThat(Persona.PersonaType.WORK.getDisplayName()).isEqualTo("Work Profile"),
                    () -> assertThat(Persona.PersonaType.PERSONAL.getDisplayName()).isEqualTo("Personal Profile"),
                    () -> assertThat(Persona.PersonaType.GAMING.getDisplayName()).isEqualTo("Gaming Profile"),
                    () -> assertThat(Persona.PersonaType.STUDY.getDisplayName()).isEqualTo("Study Profile"),
                    () -> assertThat(Persona.PersonaType.CUSTOM.getDisplayName()).isEqualTo("Custom Profile")
            );
        }

        @Test
        @DisplayName("Should handle all persona types in entity")
        void shouldHandleAllPersonaTypesInEntity() {
            // Given
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .build();
                    
            Persona.PersonaType[] types = Persona.PersonaType.values();

            // When/Then
            for (Persona.PersonaType type : types) {
                Persona persona = Persona.builder()
                        .user(user)
                        .name(type.getDisplayName())
                        .type(type)
                        .build();
                        
                assertThat(persona.getType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("PrivacySettings Tests")
    class PrivacySettingsTests {

        @Test
        @DisplayName("Should create PrivacySettings with all fields")
        void shouldCreatePrivacySettingsWithAllFields() {
            // When
            Persona.PrivacySettings privacy = Persona.PrivacySettings.builder()
                    .showRealName(true)
                    .showEmail(false)
                    .showActivity(true)
                    .allowDirectMessages(false)
                    .visibilityLevel("PRIVATE")
                    .searchable(false)
                    .showOnlineStatus(true)
                    .shareFocusSessions(false)
                    .shareAchievements(true)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(privacy).isNotNull(),
                    () -> assertThat(privacy.isShowRealName()).isTrue(),
                    () -> assertThat(privacy.isShowEmail()).isFalse(),
                    () -> assertThat(privacy.isShowActivity()).isTrue(),
                    () -> assertThat(privacy.isAllowDirectMessages()).isFalse(),
                    () -> assertThat(privacy.getVisibilityLevel()).isEqualTo("PRIVATE"),
                    () -> assertThat(privacy.isSearchable()).isFalse(),
                    () -> assertThat(privacy.isShowOnlineStatus()).isTrue(),
                    () -> assertThat(privacy.isShareFocusSessions()).isFalse(),
                    () -> assertThat(privacy.isShareAchievements()).isTrue()
            );
        }

        @Test
        @DisplayName("Should create PrivacySettings with default values")
        void shouldCreatePrivacySettingsWithDefaultValues() {
            // When
            Persona.PrivacySettings privacy = Persona.PrivacySettings.builder().build();

            // Then
            assertAll(
                    () -> assertThat(privacy).isNotNull(),
                    () -> assertThat(privacy.isShowRealName()).isFalse(), // @Builder.Default
                    () -> assertThat(privacy.isShowEmail()).isFalse(), // @Builder.Default
                    () -> assertThat(privacy.isShowActivity()).isTrue(), // @Builder.Default
                    () -> assertThat(privacy.isAllowDirectMessages()).isTrue(), // @Builder.Default
                    () -> assertThat(privacy.getVisibilityLevel()).isEqualTo("FRIENDS"), // @Builder.Default
                    () -> assertThat(privacy.isSearchable()).isTrue(), // @Builder.Default
                    () -> assertThat(privacy.isShowOnlineStatus()).isTrue(), // @Builder.Default
                    () -> assertThat(privacy.isShareFocusSessions()).isTrue(), // @Builder.Default
                    () -> assertThat(privacy.isShareAchievements()).isTrue() // @Builder.Default
            );
        }

        @Test
        @DisplayName("Should handle all visibility levels")
        void shouldHandleAllVisibilityLevels() {
            // Given
            String[] visibilityLevels = {"PUBLIC", "FRIENDS", "PRIVATE"};

            // When/Then
            for (String level : visibilityLevels) {
                Persona.PrivacySettings privacy = Persona.PrivacySettings.builder()
                        .visibilityLevel(level)
                        .build();
                        
                assertThat(privacy.getVisibilityLevel()).isEqualTo(level);
            }
        }
    }

    @Nested
    @DisplayName("Custom Attributes Tests")
    class CustomAttributesTests {

        @Test
        @DisplayName("Should handle custom attributes map operations")
        void shouldHandleCustomAttributesMapOperations() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
            Map<String, String> attributes = new HashMap<>();
            attributes.put("theme", "dark");
            attributes.put("sidebar", "collapsed");

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.PERSONAL)
                    .customAttributes(attributes)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(persona.getCustomAttributes()).hasSize(2),
                    () -> assertThat(persona.getCustomAttributes().get("theme")).isEqualTo("dark"),
                    () -> assertThat(persona.getCustomAttributes().get("sidebar")).isEqualTo("collapsed")
            );
        }

        @Test
        @DisplayName("Should handle empty custom attributes")
        void shouldHandleEmptyCustomAttributes() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.PERSONAL)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(persona.getCustomAttributes()).isNotNull(),
                    () -> assertThat(persona.getCustomAttributes()).isEmpty()
            );
        }
    }

    @Nested
    @DisplayName("Notification Preferences Tests")
    class NotificationPreferencesTests {

        @Test
        @DisplayName("Should handle notification preferences map operations")
        void shouldHandleNotificationPreferencesMapOperations() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
            Map<String, Boolean> notificationPrefs = new HashMap<>();
            notificationPrefs.put("email", true);
            notificationPrefs.put("push", false);
            notificationPrefs.put("sms", true);

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.WORK)
                    .notificationPreferences(notificationPrefs)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(persona.getNotificationPreferences()).hasSize(3),
                    () -> assertThat(persona.getNotificationPreferences().get("email")).isTrue(),
                    () -> assertThat(persona.getNotificationPreferences().get("push")).isFalse(),
                    () -> assertThat(persona.getNotificationPreferences().get("sms")).isTrue()
            );
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when IDs are the same")
        void shouldBeEqualWhenIdsAreTheSame() {
            // Given
            UUID personaId = UUID.randomUUID();
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
            
            Persona persona1 = Persona.builder()
                    .id(personaId)
                    .user(user)
                    .name("Test1")
                    .type(Persona.PersonaType.WORK)
                    .build();
                    
            Persona persona2 = Persona.builder()
                    .id(personaId)
                    .user(user)
                    .name("Test2") // Different name but same ID
                    .type(Persona.PersonaType.PERSONAL)
                    .build();

            // Then - Note: @EqualsAndHashCode(exclude = {"user"}) means user is excluded
            assertThat(persona1).isEqualTo(persona2);
        }

        @Test
        @DisplayName("Should not be equal when IDs are different")
        void shouldNotBeEqualWhenIdsAreDifferent() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
            
            Persona persona1 = Persona.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.WORK)
                    .build();
                    
            Persona persona2 = Persona.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.WORK)
                    .build();

            // Then
            assertThat(persona1).isNotEqualTo(persona2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should exclude user in toString representation")
        void shouldExcludeUserInToStringRepresentation() {
            // Given
            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("testuser")
                    .build();
                    
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test Profile")
                    .type(Persona.PersonaType.GAMING)
                    .build();

            // When
            String toString = persona.toString();

            // Then - @ToString(exclude = {"user"}) means user should not appear
            assertAll(
                    () -> assertThat(toString).contains("Test Profile"),
                    () -> assertThat(toString).contains("GAMING"),
                    () -> assertThat(toString).doesNotContain("testuser") // User excluded from toString
            );
        }
    }

    @Nested
    @DisplayName("Audit Fields Tests")
    class AuditFieldsTests {

        @Test
        @DisplayName("Should handle instant timestamps")
        void shouldHandleInstantTimestamps() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
            Instant now = Instant.now();
            Instant later = now.plusSeconds(3600);

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.STUDY)
                    .createdAt(now)
                    .updatedAt(later)
                    .lastActiveAt(later)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(persona.getCreatedAt()).isEqualTo(now),
                    () -> assertThat(persona.getUpdatedAt()).isEqualTo(later),
                    () -> assertThat(persona.getLastActiveAt()).isEqualTo(later),
                    () -> assertThat(persona.getUpdatedAt()).isAfter(persona.getCreatedAt())
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null custom attributes gracefully")
        void shouldHandleNullCustomAttributesGracefully() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.PERSONAL)
                    .customAttributes(null)
                    .build();

            // Then
            assertThat(persona.getCustomAttributes()).isNull();
        }

        @Test
        @DisplayName("Should handle long bio text")
        void shouldHandleLongBioText() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
            String longBio = "a".repeat(500); // Max length as defined in entity

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Test")
                    .type(Persona.PersonaType.CUSTOM)
                    .bio(longBio)
                    .build();

            // Then
            assertThat(persona.getBio()).hasSize(500);
        }

        @Test
        @DisplayName("Should handle special characters in attributes")
        void shouldHandleSpecialCharactersInAttributes() {
            // Given
            User user = User.builder().id(UUID.randomUUID()).username("testuser").build();
            Map<String, String> specialAttributes = new HashMap<>();
            specialAttributes.put("emoji", "🎮💻📚");
            specialAttributes.put("unicode", "αβγδε");
            specialAttributes.put("symbols", "!@#$%^&*()");

            // When
            Persona persona = Persona.builder()
                    .user(user)
                    .name("Special Test")
                    .type(Persona.PersonaType.GAMING)
                    .customAttributes(specialAttributes)
                    .statusMessage("Working 💪 hard!")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(persona.getCustomAttributes().get("emoji")).isEqualTo("🎮💻📚"),
                    () -> assertThat(persona.getCustomAttributes().get("unicode")).isEqualTo("αβγδε"),
                    () -> assertThat(persona.getCustomAttributes().get("symbols")).isEqualTo("!@#$%^&*()"),
                    () -> assertThat(persona.getStatusMessage()).contains("💪")
            );
        }
    }

    @Nested
    @DisplayName("NoArgsConstructor Tests")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            Persona persona = new Persona();

            // Then
            assertAll(
                    () -> assertThat(persona).isNotNull(),
                    () -> assertThat(persona.getId()).isNull(),
                    () -> assertThat(persona.getUser()).isNull(),
                    () -> assertThat(persona.getName()).isNull(),
                    () -> assertThat(persona.getType()).isNull()
            );
        }
    }
}