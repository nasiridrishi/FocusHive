package com.focushive.identity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Comprehensive unit tests for AuthenticationResponse DTO.
 * Tests all aspects of the authentication response including JWT tokens,
 * user information, persona data, and nested DTOs.
 */
@DisplayName("AuthenticationResponse DTO Unit Tests")
class AuthenticationResponseUnitTest {

    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For Java 8 time support
    }

    @Nested
    @DisplayName("Builder Pattern Tests")
    class BuilderPatternTests {

        @Test
        @DisplayName("Should create AuthenticationResponse using builder with all fields")
        void shouldCreateAuthenticationResponseUsingBuilderWithAllFields() {
            // Given
            UUID userId = UUID.randomUUID();
            UUID personaId = UUID.randomUUID();
            Instant now = Instant.now();
            
            AuthenticationResponse.PrivacySettings privacySettings = 
                    AuthenticationResponse.PrivacySettings.builder()
                            .showRealName(true)
                            .showEmail(false)
                            .showActivity(true)
                            .allowDirectMessages(true)
                            .visibilityLevel("FRIENDS")
                            .build();
            
            AuthenticationResponse.PersonaInfo activePersona = 
                    AuthenticationResponse.PersonaInfo.builder()
                            .id(personaId)
                            .name("Work Profile")
                            .type("WORK")
                            .isDefault(true)
                            .avatarUrl("https://example.com/avatar.jpg")
                            .privacySettings(privacySettings)
                            .build();
            
            List<AuthenticationResponse.PersonaInfo> availablePersonas = Arrays.asList(
                    activePersona,
                    AuthenticationResponse.PersonaInfo.builder()
                            .id(UUID.randomUUID())
                            .name("Personal Profile")
                            .type("PERSONAL")
                            .isDefault(false)
                            .build()
            );

            // When
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("access.jwt.token")
                    .refreshToken("refresh.jwt.token")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .userId(userId)
                    .username("testuser")
                    .email("test@example.com")
                    .displayName("Test User")
                    .activePersona(activePersona)
                    .availablePersonas(availablePersonas)
                    .scope("read write")
                    .issuedAt(now)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo("access.jwt.token"),
                    () -> assertThat(response.getRefreshToken()).isEqualTo("refresh.jwt.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(3600L),
                    () -> assertThat(response.getUserId()).isEqualTo(userId),
                    () -> assertThat(response.getUsername()).isEqualTo("testuser"),
                    () -> assertThat(response.getEmail()).isEqualTo("test@example.com"),
                    () -> assertThat(response.getDisplayName()).isEqualTo("Test User"),
                    () -> assertThat(response.getActivePersona()).isEqualTo(activePersona),
                    () -> assertThat(response.getAvailablePersonas()).hasSize(2),
                    () -> assertThat(response.getScope()).isEqualTo("read write"),
                    () -> assertThat(response.getIssuedAt()).isEqualTo(now)
            );
        }

        @Test
        @DisplayName("Should create AuthenticationResponse with minimal required fields")
        void shouldCreateAuthenticationResponseWithMinimalRequiredFields() {
            // When
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("access.token")
                    .userId(UUID.randomUUID())
                    .username("user")
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo("access.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"), // Default value
                    () -> assertThat(response.getRefreshToken()).isNull(),
                    () -> assertThat(response.getExpiresIn()).isNull(),
                    () -> assertThat(response.getActivePersona()).isNull(),
                    () -> assertThat(response.getAvailablePersonas()).isNull()
            );
        }

        @Test
        @DisplayName("Should have Bearer as default token type")
        void shouldHaveBearerAsDefaultTokenType() {
            // When
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("token")
                    .build();

            // Then
            assertThat(response.getTokenType()).isEqualTo("Bearer");
        }
    }

    @Nested
    @DisplayName("PersonaInfo Nested DTO Tests")
    class PersonaInfoTests {

        @Test
        @DisplayName("Should create PersonaInfo with all fields")
        void shouldCreatePersonaInfoWithAllFields() {
            // Given
            UUID personaId = UUID.randomUUID();
            AuthenticationResponse.PrivacySettings privacy = 
                    AuthenticationResponse.PrivacySettings.builder()
                            .showRealName(true)
                            .visibilityLevel("PUBLIC")
                            .build();

            // When
            AuthenticationResponse.PersonaInfo persona = 
                    AuthenticationResponse.PersonaInfo.builder()
                            .id(personaId)
                            .name("Gaming Profile")
                            .type("GAMING")
                            .isDefault(false)
                            .avatarUrl("https://example.com/gaming.png")
                            .privacySettings(privacy)
                            .build();

            // Then
            assertAll(
                    () -> assertThat(persona).isNotNull(),
                    () -> assertThat(persona.getId()).isEqualTo(personaId),
                    () -> assertThat(persona.getName()).isEqualTo("Gaming Profile"),
                    () -> assertThat(persona.getType()).isEqualTo("GAMING"),
                    () -> assertThat(persona.isDefault()).isFalse(),
                    () -> assertThat(persona.getAvatarUrl()).isEqualTo("https://example.com/gaming.png"),
                    () -> assertThat(persona.getPrivacySettings()).isEqualTo(privacy)
            );
        }

        @Test
        @DisplayName("Should create PersonaInfo with minimal fields")
        void shouldCreatePersonaInfoWithMinimalFields() {
            // When
            AuthenticationResponse.PersonaInfo persona = 
                    AuthenticationResponse.PersonaInfo.builder()
                            .id(UUID.randomUUID())
                            .name("Basic Profile")
                            .type("PERSONAL")
                            .build();

            // Then
            assertAll(
                    () -> assertThat(persona).isNotNull(),
                    () -> assertThat(persona.getName()).isEqualTo("Basic Profile"),
                    () -> assertThat(persona.getType()).isEqualTo("PERSONAL"),
                    () -> assertThat(persona.isDefault()).isFalse(),
                    () -> assertThat(persona.getAvatarUrl()).isNull(),
                    () -> assertThat(persona.getPrivacySettings()).isNull()
            );
        }

        @Test
        @DisplayName("Should handle all persona types")
        void shouldHandleAllPersonaTypes() {
            // Given/When/Then
            String[] types = {"WORK", "PERSONAL", "GAMING", "STUDY", "CUSTOM"};
            
            for (String type : types) {
                AuthenticationResponse.PersonaInfo persona = 
                        AuthenticationResponse.PersonaInfo.builder()
                                .id(UUID.randomUUID())
                                .name(type + " Profile")
                                .type(type)
                                .build();
                                
                assertThat(persona.getType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("PrivacySettings Nested DTO Tests")
    class PrivacySettingsTests {

        @Test
        @DisplayName("Should create PrivacySettings with all fields")
        void shouldCreatePrivacySettingsWithAllFields() {
            // When
            AuthenticationResponse.PrivacySettings privacy = 
                    AuthenticationResponse.PrivacySettings.builder()
                            .showRealName(true)
                            .showEmail(false)
                            .showActivity(true)
                            .allowDirectMessages(false)
                            .visibilityLevel("FRIENDS")
                            .build();

            // Then
            assertAll(
                    () -> assertThat(privacy).isNotNull(),
                    () -> assertThat(privacy.isShowRealName()).isTrue(),
                    () -> assertThat(privacy.isShowEmail()).isFalse(),
                    () -> assertThat(privacy.isShowActivity()).isTrue(),
                    () -> assertThat(privacy.isAllowDirectMessages()).isFalse(),
                    () -> assertThat(privacy.getVisibilityLevel()).isEqualTo("FRIENDS")
            );
        }

        @Test
        @DisplayName("Should handle all visibility levels")
        void shouldHandleAllVisibilityLevels() {
            // Given/When/Then
            String[] levels = {"PUBLIC", "FRIENDS", "PRIVATE"};
            
            for (String level : levels) {
                AuthenticationResponse.PrivacySettings privacy = 
                        AuthenticationResponse.PrivacySettings.builder()
                                .visibilityLevel(level)
                                .build();
                                
                assertThat(privacy.getVisibilityLevel()).isEqualTo(level);
            }
        }

        @Test
        @DisplayName("Should create PrivacySettings with default boolean values")
        void shouldCreatePrivacySettingsWithDefaultBooleanValues() {
            // When
            AuthenticationResponse.PrivacySettings privacy = 
                    AuthenticationResponse.PrivacySettings.builder().build();

            // Then
            assertAll(
                    () -> assertThat(privacy).isNotNull(),
                    () -> assertThat(privacy.isShowRealName()).isFalse(),
                    () -> assertThat(privacy.isShowEmail()).isFalse(),
                    () -> assertThat(privacy.isShowActivity()).isFalse(),
                    () -> assertThat(privacy.isAllowDirectMessages()).isFalse(),
                    () -> assertThat(privacy.getVisibilityLevel()).isNull()
            );
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("Should serialize AuthenticationResponse to JSON correctly")
        void shouldSerializeAuthenticationResponseToJsonCorrectly() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("access.token.123")
                    .refreshToken("refresh.token.456")
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .userId(userId)
                    .username("jsonuser")
                    .email("json@test.com")
                    .scope("read write admin")
                    .issuedAt(now)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"accessToken\":\"access.token.123\""),
                    () -> assertThat(json).contains("\"refreshToken\":\"refresh.token.456\""),
                    () -> assertThat(json).contains("\"tokenType\":\"Bearer\""),
                    () -> assertThat(json).contains("\"expiresIn\":3600"),
                    () -> assertThat(json).contains("\"username\":\"jsonuser\""),
                    () -> assertThat(json).contains("\"email\":\"json@test.com\""),
                    () -> assertThat(json).contains("\"scope\":\"read write admin\""),
                    () -> assertThat(json).contains("\"userId\":\"" + userId.toString() + "\"")
            );
        }

        @Test
        @DisplayName("Should deserialize JSON to AuthenticationResponse correctly")
        void shouldDeserializeJsonToAuthenticationResponseCorrectly() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            String json = String.format("""
                {
                    "accessToken": "access.token.789",
                    "refreshToken": "refresh.token.012",
                    "tokenType": "Bearer",
                    "expiresIn": 7200,
                    "userId": "%s",
                    "username": "jsonuser2",
                    "email": "json2@test.com",
                    "displayName": "JSON User 2",
                    "scope": "read write"
                }
                """, userId);

            // When
            AuthenticationResponse response = objectMapper.readValue(json, AuthenticationResponse.class);

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isEqualTo("access.token.789"),
                    () -> assertThat(response.getRefreshToken()).isEqualTo("refresh.token.012"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getExpiresIn()).isEqualTo(7200L),
                    () -> assertThat(response.getUserId()).isEqualTo(userId),
                    () -> assertThat(response.getUsername()).isEqualTo("jsonuser2"),
                    () -> assertThat(response.getEmail()).isEqualTo("json2@test.com"),
                    () -> assertThat(response.getDisplayName()).isEqualTo("JSON User 2"),
                    () -> assertThat(response.getScope()).isEqualTo("read write")
            );
        }

        @Test
        @DisplayName("Should serialize nested PersonaInfo correctly")
        void shouldSerializeNestedPersonaInfoCorrectly() throws Exception {
            // Given
            UUID personaId = UUID.randomUUID();
            AuthenticationResponse.PersonaInfo persona = 
                    AuthenticationResponse.PersonaInfo.builder()
                            .id(personaId)
                            .name("Test Persona")
                            .type("WORK")
                            .isDefault(true)
                            .avatarUrl("https://example.com/avatar.jpg")
                            .build();
                            
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("token")
                    .activePersona(persona)
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).contains("\"activePersona\":{"),
                    () -> assertThat(json).contains("\"name\":\"Test Persona\""),
                    () -> assertThat(json).contains("\"type\":\"WORK\""),
                    () -> assertThat(json).contains("\"default\":true"), // Jackson uses property name not field name
                    () -> assertThat(json).contains("\"avatarUrl\":\"https://example.com/avatar.jpg\"")
            );
        }

        @Test
        @DisplayName("Should exclude null fields from JSON serialization")
        void shouldExcludeNullFieldsFromJsonSerialization() throws Exception {
            // Given
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("token")
                    .userId(UUID.randomUUID())
                    // refreshToken, email, displayName are null
                    .build();

            // When
            String json = objectMapper.writeValueAsString(response);

            // Then
            assertAll(
                    () -> assertThat(json).doesNotContain("refreshToken"),
                    () -> assertThat(json).doesNotContain("email"),
                    () -> assertThat(json).doesNotContain("displayName"),
                    () -> assertThat(json).doesNotContain("activePersona"),
                    () -> assertThat(json).contains("accessToken"),
                    () -> assertThat(json).contains("userId"),
                    () -> assertThat(json).contains("tokenType") // Has default value
            );
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when all fields are the same")
        void shouldBeEqualWhenAllFieldsAreTheSame() {
            // Given
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            
            AuthenticationResponse response1 = AuthenticationResponse.builder()
                    .accessToken("token")
                    .userId(userId)
                    .username("user")
                    .email("test@example.com")
                    .issuedAt(now)
                    .build();
                    
            AuthenticationResponse response2 = AuthenticationResponse.builder()
                    .accessToken("token")
                    .userId(userId)
                    .username("user")
                    .email("test@example.com")
                    .issuedAt(now)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response1).isEqualTo(response2),
                    () -> assertThat(response1.hashCode()).isEqualTo(response2.hashCode())
            );
        }

        @Test
        @DisplayName("Should not be equal when access tokens differ")
        void shouldNotBeEqualWhenAccessTokensDiffer() {
            // Given
            UUID userId = UUID.randomUUID();
            
            AuthenticationResponse response1 = AuthenticationResponse.builder()
                    .accessToken("token1")
                    .userId(userId)
                    .build();
                    
            AuthenticationResponse response2 = AuthenticationResponse.builder()
                    .accessToken("token2")
                    .userId(userId)
                    .build();

            // Then
            assertThat(response1).isNotEqualTo(response2);
        }

        @Test
        @DisplayName("Should handle null values in equals comparison")
        void shouldHandleNullValuesInEqualsComparison() {
            // Given
            AuthenticationResponse response1 = AuthenticationResponse.builder()
                    .accessToken("token")
                    .refreshToken(null)
                    .build();
                    
            AuthenticationResponse response2 = AuthenticationResponse.builder()
                    .accessToken("token")
                    .refreshToken(null)
                    .build();

            // Then
            assertThat(response1).isEqualTo(response2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Conditions")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long token strings")
        void shouldHandleVeryLongTokenStrings() {
            // Given
            String longToken = "a".repeat(2000);
            
            // When
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken(longToken)
                    .refreshToken(longToken)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(response.getAccessToken()).hasSize(2000),
                    () -> assertThat(response.getRefreshToken()).hasSize(2000)
            );
        }

        @Test
        @DisplayName("Should handle empty persona list")
        void shouldHandleEmptyPersonaList() {
            // When
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("token")
                    .availablePersonas(Arrays.asList())
                    .build();

            // Then
            assertThat(response.getAvailablePersonas()).isEmpty();
        }

        @Test
        @DisplayName("Should handle special characters in scope")
        void shouldHandleSpecialCharactersInScope() {
            // Given
            String specialScope = "read:user write:admin delete:* custom:scope-with-dashes";
            
            // When
            AuthenticationResponse response = AuthenticationResponse.builder()
                    .accessToken("token")
                    .scope(specialScope)
                    .build();

            // Then
            assertThat(response.getScope()).isEqualTo(specialScope);
        }

        @Test
        @DisplayName("Should handle zero and negative expires in values")
        void shouldHandleZeroAndNegativeExpiresInValues() {
            // When/Then
            AuthenticationResponse zeroExpiry = AuthenticationResponse.builder()
                    .accessToken("token")
                    .expiresIn(0L)
                    .build();
            assertThat(zeroExpiry.getExpiresIn()).isZero();
            
            AuthenticationResponse negativeExpiry = AuthenticationResponse.builder()
                    .accessToken("token")
                    .expiresIn(-100L)
                    .build();
            assertThat(negativeExpiry.getExpiresIn()).isEqualTo(-100L);
        }

        @Test
        @DisplayName("Should handle past and future issuedAt timestamps")
        void shouldHandlePastAndFutureIssuedAtTimestamps() {
            // Given
            Instant past = Instant.parse("2020-01-01T00:00:00Z");
            Instant future = Instant.parse("2030-12-31T23:59:59Z");
            
            // When
            AuthenticationResponse pastResponse = AuthenticationResponse.builder()
                    .accessToken("token")
                    .issuedAt(past)
                    .build();
                    
            AuthenticationResponse futureResponse = AuthenticationResponse.builder()
                    .accessToken("token")
                    .issuedAt(future)
                    .build();

            // Then
            assertAll(
                    () -> assertThat(pastResponse.getIssuedAt()).isEqualTo(past),
                    () -> assertThat(futureResponse.getIssuedAt()).isEqualTo(future),
                    () -> assertThat(pastResponse.getIssuedAt()).isBefore(Instant.now()),
                    () -> assertThat(futureResponse.getIssuedAt()).isAfter(Instant.now())
            );
        }
    }

    @Nested
    @DisplayName("NoArgsConstructor and AllArgsConstructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance using no-args constructor")
        void shouldCreateInstanceUsingNoArgsConstructor() {
            // When
            AuthenticationResponse response = new AuthenticationResponse();

            // Then
            assertAll(
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getAccessToken()).isNull(),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"), // Default value from @Builder.Default
                    () -> assertThat(response.getUserId()).isNull()
            );
        }

        @Test
        @DisplayName("Should create nested DTOs using no-args constructors")
        void shouldCreateNestedDtosUsingNoArgsConstructors() {
            // When
            AuthenticationResponse.PersonaInfo persona = new AuthenticationResponse.PersonaInfo();
            AuthenticationResponse.PrivacySettings privacy = new AuthenticationResponse.PrivacySettings();

            // Then
            assertAll(
                    () -> assertThat(persona).isNotNull(),
                    () -> assertThat(persona.getId()).isNull(),
                    () -> assertThat(persona.isDefault()).isFalse(),
                    () -> assertThat(privacy).isNotNull(),
                    () -> assertThat(privacy.isShowRealName()).isFalse(),
                    () -> assertThat(privacy.getVisibilityLevel()).isNull()
            );
        }
    }
}