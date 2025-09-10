package com.focushive.identity.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for UserDTO covering builder patterns,
 * serialization/deserialization, equality, and edge cases.
 */
@DisplayName("UserDTO Unit Tests")
class UserDTOUnitTest {

    private ObjectMapper objectMapper;
    private LocalDateTime testDateTime;
    private Map<String, Boolean> notificationPreferences;
    private List<PersonaDto> personas;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        testDateTime = LocalDateTime.of(2024, 1, 15, 10, 30);
        
        notificationPreferences = new HashMap<>();
        notificationPreferences.put("email", true);
        notificationPreferences.put("sms", false);
        notificationPreferences.put("push", true);
        
        PersonaDto workPersona = PersonaDto.builder()
                .id(UUID.randomUUID())
                .name("Work")
                .bio("Professional persona")
                .build();
        
        PersonaDto personalPersona = PersonaDto.builder()
                .id(UUID.randomUUID())
                .name("Personal")
                .bio("Personal activities")
                .build();
        
        personas = Arrays.asList(workPersona, personalPersona);
    }

    @Test
    @DisplayName("Should create UserDTO using builder with all fields")
    void shouldCreateUserDTOUsingBuilderWithAllFields() {
        // Given & When
        UserDTO userDTO = UserDTO.builder()
                .id("user-123")
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .emailVerified(true)
                .enabled(true)
                .twoFactorEnabled(false)
                .preferredLanguage("en-US")
                .timezone("UTC")
                .notificationPreferences(notificationPreferences)
                .createdAt(testDateTime)
                .lastLoginAt(testDateTime.plusHours(2))
                .personas(personas)
                .build();

        // Then
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getId()).isEqualTo("user-123");
        assertThat(userDTO.getUsername()).isEqualTo("testuser");
        assertThat(userDTO.getEmail()).isEqualTo("test@example.com");
        assertThat(userDTO.getDisplayName()).isEqualTo("Test User");
        assertThat(userDTO.isEmailVerified()).isTrue();
        assertThat(userDTO.isEnabled()).isTrue();
        assertThat(userDTO.isTwoFactorEnabled()).isFalse();
        assertThat(userDTO.getPreferredLanguage()).isEqualTo("en-US");
        assertThat(userDTO.getTimezone()).isEqualTo("UTC");
        assertThat(userDTO.getNotificationPreferences()).isEqualTo(notificationPreferences);
        assertThat(userDTO.getCreatedAt()).isEqualTo(testDateTime);
        assertThat(userDTO.getLastLoginAt()).isEqualTo(testDateTime.plusHours(2));
        assertThat(userDTO.getPersonas()).hasSize(2);
        assertThat(userDTO.getPersonas()).containsExactlyElementsOf(personas);
    }

    @Test
    @DisplayName("Should create UserDTO with minimal required fields")
    void shouldCreateUserDTOWithMinimalFields() {
        // Given & When
        UserDTO userDTO = UserDTO.builder()
                .id("user-456")
                .username("minimaluser")
                .email("minimal@example.com")
                .build();

        // Then
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getId()).isEqualTo("user-456");
        assertThat(userDTO.getUsername()).isEqualTo("minimaluser");
        assertThat(userDTO.getEmail()).isEqualTo("minimal@example.com");
        assertThat(userDTO.getDisplayName()).isNull();
        assertThat(userDTO.isEmailVerified()).isFalse();
        assertThat(userDTO.isEnabled()).isFalse();
        assertThat(userDTO.isTwoFactorEnabled()).isFalse();
        assertThat(userDTO.getNotificationPreferences()).isNull();
        assertThat(userDTO.getPersonas()).isNull();
    }

    @Test
    @DisplayName("Should create UserDTO using no-args constructor and setters")
    void shouldCreateUserDTOUsingNoArgsConstructorAndSetters() {
        // Given
        UserDTO userDTO = new UserDTO();

        // When
        userDTO.setId("user-789");
        userDTO.setUsername("setteruser");
        userDTO.setEmail("setter@example.com");
        userDTO.setDisplayName("Setter User");
        userDTO.setEmailVerified(true);
        userDTO.setEnabled(true);
        userDTO.setTwoFactorEnabled(true);
        userDTO.setPreferredLanguage("fr-FR");
        userDTO.setTimezone("Europe/Paris");
        userDTO.setNotificationPreferences(notificationPreferences);
        userDTO.setCreatedAt(testDateTime);
        userDTO.setLastLoginAt(testDateTime.minusHours(1));
        userDTO.setPersonas(personas);

        // Then
        assertThat(userDTO.getId()).isEqualTo("user-789");
        assertThat(userDTO.getUsername()).isEqualTo("setteruser");
        assertThat(userDTO.getEmail()).isEqualTo("setter@example.com");
        assertThat(userDTO.getDisplayName()).isEqualTo("Setter User");
        assertThat(userDTO.isEmailVerified()).isTrue();
        assertThat(userDTO.isEnabled()).isTrue();
        assertThat(userDTO.isTwoFactorEnabled()).isTrue();
        assertThat(userDTO.getPreferredLanguage()).isEqualTo("fr-FR");
        assertThat(userDTO.getTimezone()).isEqualTo("Europe/Paris");
        assertThat(userDTO.getNotificationPreferences()).isEqualTo(notificationPreferences);
        assertThat(userDTO.getCreatedAt()).isEqualTo(testDateTime);
        assertThat(userDTO.getLastLoginAt()).isEqualTo(testDateTime.minusHours(1));
        assertThat(userDTO.getPersonas()).hasSize(2);
    }

    @Test
    @DisplayName("Should create UserDTO using all-args constructor")
    void shouldCreateUserDTOUsingAllArgsConstructor() {
        // Given & When
        UserDTO userDTO = new UserDTO(
                "user-999",
                "allArgsUser",
                "allargs@example.com",
                "All Args User",
                true,
                true,
                false,
                "es-ES",
                "Europe/Madrid",
                notificationPreferences,
                testDateTime,
                testDateTime.plusDays(1),
                personas
        );

        // Then
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getId()).isEqualTo("user-999");
        assertThat(userDTO.getUsername()).isEqualTo("allArgsUser");
        assertThat(userDTO.getEmail()).isEqualTo("allargs@example.com");
        assertThat(userDTO.getDisplayName()).isEqualTo("All Args User");
        assertThat(userDTO.isEmailVerified()).isTrue();
        assertThat(userDTO.isEnabled()).isTrue();
        assertThat(userDTO.isTwoFactorEnabled()).isFalse();
        assertThat(userDTO.getPreferredLanguage()).isEqualTo("es-ES");
        assertThat(userDTO.getTimezone()).isEqualTo("Europe/Madrid");
        assertThat(userDTO.getNotificationPreferences()).isEqualTo(notificationPreferences);
        assertThat(userDTO.getCreatedAt()).isEqualTo(testDateTime);
        assertThat(userDTO.getLastLoginAt()).isEqualTo(testDateTime.plusDays(1));
        assertThat(userDTO.getPersonas()).hasSize(2);
    }

    @Test
    @DisplayName("Should serialize UserDTO to JSON correctly")
    void shouldSerializeUserDTOToJsonCorrectly() throws JsonProcessingException {
        // Given
        UserDTO userDTO = UserDTO.builder()
                .id("user-serialize")
                .username("jsonuser")
                .email("json@example.com")
                .displayName("JSON User")
                .emailVerified(true)
                .enabled(true)
                .twoFactorEnabled(false)
                .preferredLanguage("en-US")
                .timezone("UTC")
                .notificationPreferences(notificationPreferences)
                .createdAt(testDateTime)
                .lastLoginAt(testDateTime.plusMinutes(30))
                .personas(personas)
                .build();

        // When
        String json = objectMapper.writeValueAsString(userDTO);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"id\":\"user-serialize\"");
        assertThat(json).contains("\"username\":\"jsonuser\"");
        assertThat(json).contains("\"email\":\"json@example.com\"");
        assertThat(json).contains("\"displayName\":\"JSON User\"");
        assertThat(json).contains("\"emailVerified\":true");
        assertThat(json).contains("\"enabled\":true");
        assertThat(json).contains("\"twoFactorEnabled\":false");
        assertThat(json).contains("\"preferredLanguage\":\"en-US\"");
        assertThat(json).contains("\"timezone\":\"UTC\"");
        assertThat(json).contains("\"notificationPreferences\"");
        assertThat(json).contains("\"personas\"");
    }

    @Test
    @DisplayName("Should deserialize JSON to UserDTO correctly")
    void shouldDeserializeJsonToUserDTOCorrectly() throws JsonProcessingException {
        // Given
        String json = """
                {
                    "id": "user-deserialize",
                    "username": "deserializeuser",
                    "email": "deserialize@example.com",
                    "displayName": "Deserialize User",
                    "emailVerified": true,
                    "enabled": true,
                    "twoFactorEnabled": true,
                    "preferredLanguage": "de-DE",
                    "timezone": "Europe/Berlin",
                    "notificationPreferences": {
                        "email": false,
                        "sms": true,
                        "push": false
                    },
                    "createdAt": "2024-01-15T10:30:00",
                    "lastLoginAt": "2024-01-15T12:00:00",
                    "personas": []
                }
                """;

        // When
        UserDTO userDTO = objectMapper.readValue(json, UserDTO.class);

        // Then
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getId()).isEqualTo("user-deserialize");
        assertThat(userDTO.getUsername()).isEqualTo("deserializeuser");
        assertThat(userDTO.getEmail()).isEqualTo("deserialize@example.com");
        assertThat(userDTO.getDisplayName()).isEqualTo("Deserialize User");
        assertThat(userDTO.isEmailVerified()).isTrue();
        assertThat(userDTO.isEnabled()).isTrue();
        assertThat(userDTO.isTwoFactorEnabled()).isTrue();
        assertThat(userDTO.getPreferredLanguage()).isEqualTo("de-DE");
        assertThat(userDTO.getTimezone()).isEqualTo("Europe/Berlin");
        assertThat(userDTO.getNotificationPreferences()).hasSize(3);
        assertThat(userDTO.getNotificationPreferences().get("email")).isFalse();
        assertThat(userDTO.getNotificationPreferences().get("sms")).isTrue();
        assertThat(userDTO.getNotificationPreferences().get("push")).isFalse();
        assertThat(userDTO.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));
        assertThat(userDTO.getLastLoginAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 12, 0));
        assertThat(userDTO.getPersonas()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null values in serialization")
    void shouldHandleNullValuesInSerialization() throws JsonProcessingException {
        // Given
        UserDTO userDTO = UserDTO.builder()
                .id("user-null")
                .username("nulluser")
                .email("null@example.com")
                .displayName(null)
                .notificationPreferences(null)
                .createdAt(null)
                .lastLoginAt(null)
                .personas(null)
                .build();

        // When
        String json = objectMapper.writeValueAsString(userDTO);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"displayName\":null");
        assertThat(json).contains("\"notificationPreferences\":null");
        assertThat(json).contains("\"createdAt\":null");
        assertThat(json).contains("\"lastLoginAt\":null");
        assertThat(json).contains("\"personas\":null");
    }

    @Test
    @DisplayName("Should test equality and hashCode with same values")
    void shouldTestEqualityAndHashCodeWithSameValues() {
        // Given
        UserDTO userDTO1 = UserDTO.builder()
                .id("user-eq1")
                .username("equaluser")
                .email("equal@example.com")
                .emailVerified(true)
                .enabled(true)
                .notificationPreferences(notificationPreferences)
                .personas(personas)
                .build();

        UserDTO userDTO2 = UserDTO.builder()
                .id("user-eq1")
                .username("equaluser")
                .email("equal@example.com")
                .emailVerified(true)
                .enabled(true)
                .notificationPreferences(notificationPreferences)
                .personas(personas)
                .build();

        // Then
        assertThat(userDTO1).isEqualTo(userDTO2);
        assertThat(userDTO1.hashCode()).isEqualTo(userDTO2.hashCode());
    }

    @Test
    @DisplayName("Should test inequality with different values")
    void shouldTestInequalityWithDifferentValues() {
        // Given
        UserDTO userDTO1 = UserDTO.builder()
                .id("user-neq1")
                .username("user1")
                .email("user1@example.com")
                .build();

        UserDTO userDTO2 = UserDTO.builder()
                .id("user-neq2")
                .username("user2")
                .email("user2@example.com")
                .build();

        // Then
        assertThat(userDTO1).isNotEqualTo(userDTO2);
        assertThat(userDTO1.hashCode()).isNotEqualTo(userDTO2.hashCode());
    }

    @Test
    @DisplayName("Should test toString method")
    void shouldTestToStringMethod() {
        // Given
        UserDTO userDTO = UserDTO.builder()
                .id("user-toString")
                .username("stringuser")
                .email("string@example.com")
                .displayName("String User")
                .emailVerified(true)
                .enabled(true)
                .twoFactorEnabled(false)
                .build();

        // When
        String toString = userDTO.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("UserDTO");
        assertThat(toString).contains("id=user-toString");
        assertThat(toString).contains("username=stringuser");
        assertThat(toString).contains("email=string@example.com");
        assertThat(toString).contains("displayName=String User");
        assertThat(toString).contains("emailVerified=true");
        assertThat(toString).contains("enabled=true");
        assertThat(toString).contains("twoFactorEnabled=false");
    }

    @Test
    @DisplayName("Should handle empty collections gracefully")
    void shouldHandleEmptyCollectionsGracefully() {
        // Given
        Map<String, Boolean> emptyPreferences = new HashMap<>();
        List<PersonaDto> emptyPersonas = new ArrayList<>();

        // When
        UserDTO userDTO = UserDTO.builder()
                .id("user-empty")
                .username("emptyuser")
                .email("empty@example.com")
                .notificationPreferences(emptyPreferences)
                .personas(emptyPersonas)
                .build();

        // Then
        assertThat(userDTO.getNotificationPreferences()).isEmpty();
        assertThat(userDTO.getPersonas()).isEmpty();
    }

    @Test
    @DisplayName("Should handle large collections efficiently")
    void shouldHandleLargeCollectionsEfficiently() {
        // Given
        Map<String, Boolean> largePreferences = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largePreferences.put("preference" + i, i % 2 == 0);
        }

        List<PersonaDto> largePersonaList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largePersonaList.add(PersonaDto.builder()
                    .id(UUID.randomUUID())
                    .name("Persona " + i)
                    .bio("Description " + i)
                    .build());
        }

        // When
        UserDTO userDTO = UserDTO.builder()
                .id("user-large")
                .username("largeuser")
                .email("large@example.com")
                .notificationPreferences(largePreferences)
                .personas(largePersonaList)
                .build();

        // Then
        assertThat(userDTO.getNotificationPreferences()).hasSize(100);
        assertThat(userDTO.getPersonas()).hasSize(50);
    }

    @Test
    @DisplayName("Should handle special characters in string fields")
    void shouldHandleSpecialCharactersInStringFields() {
        // Given
        String specialChars = "Special!@#$%^&*(){}[]|\\:;\"'<>?,./-_=+`~";
        String unicodeChars = "üñïçødé 测试 ñoël テスト";

        // When
        UserDTO userDTO = UserDTO.builder()
                .id("user-special")
                .username("user" + specialChars)
                .email("special@example.com")
                .displayName(unicodeChars)
                .preferredLanguage("zh-CN")
                .timezone("Asia/Shanghai")
                .build();

        // Then
        assertThat(userDTO.getUsername()).contains(specialChars);
        assertThat(userDTO.getDisplayName()).isEqualTo(unicodeChars);
        assertThat(userDTO.getPreferredLanguage()).isEqualTo("zh-CN");
        assertThat(userDTO.getTimezone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    @DisplayName("Should maintain immutability of collections when modified externally")
    void shouldMaintainImmutabilityOfCollectionsWhenModifiedExternally() {
        // Given
        Map<String, Boolean> originalPreferences = new HashMap<>(notificationPreferences);
        List<PersonaDto> originalPersonas = new ArrayList<>(personas);

        UserDTO userDTO = UserDTO.builder()
                .id("user-immutable")
                .username("immutableuser")
                .email("immutable@example.com")
                .notificationPreferences(originalPreferences)
                .personas(originalPersonas)
                .build();

        // When - modify external collections
        originalPreferences.put("newPreference", true);
        originalPersonas.add(PersonaDto.builder()
                .id(UUID.randomUUID())
                .name("New Persona")
                .build());

        // Then - UserDTO should not be affected
        assertThat(userDTO.getNotificationPreferences()).hasSize(3);
        assertThat(userDTO.getPersonas()).hasSize(2);
        assertThat(userDTO.getNotificationPreferences()).doesNotContainKey("newPreference");
    }
}