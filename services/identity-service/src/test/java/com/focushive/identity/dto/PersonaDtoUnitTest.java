package com.focushive.identity.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Persona.PrivacySettings;
import com.focushive.identity.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for PersonaDto and PrivacySettingsDto.
 * Tests builder patterns, JSON serialization, entity conversion, and validation.
 */
@DisplayName("PersonaDto Unit Tests")
class PersonaDtoUnitTest {
    
    private ObjectMapper objectMapper;
    private UUID testUserId;
    private UUID testPersonaId;
    private Instant testTime;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For Instant serialization
        testUserId = UUID.randomUUID();
        testPersonaId = UUID.randomUUID();
        testTime = Instant.now();
    }
    
    @Nested
    @DisplayName("PersonaDto Builder and Basic Operations")
    class PersonaDtoBasicTests {
        
        @Test
        @DisplayName("Should create PersonaDto with all fields using builder")
        void shouldCreatePersonaDtoWithAllFields() {
            // Arrange
            PersonaDto.PrivacySettingsDto privacySettings = PersonaDto.PrivacySettingsDto.builder()
                    .showRealName(true)
                    .showEmail(false)
                    .showActivity(true)
                    .allowDirectMessages(true)
                    .visibilityLevel("PUBLIC")
                    .searchable(true)
                    .showOnlineStatus(true)
                    .shareFocusSessions(false)
                    .shareAchievements(true)
                    .build();
            
            Map<String, String> customAttributes = Map.of(
                    "favoriteColor", "blue",
                    "workStyle", "focused"
            );
            
            // Act
            PersonaDto personaDto = PersonaDto.builder()
                    .id(testPersonaId)
                    .name("Work Persona")
                    .type(Persona.PersonaType.WORK)
                    .isDefault(true)
                    .isActive(true)
                    .displayName("John Doe (Work)")
                    .avatarUrl("https://example.com/avatar.jpg")
                    .bio("Focused professional mode")
                    .statusMessage("Available for work")
                    .privacySettings(privacySettings)
                    .customAttributes(customAttributes)
                    .themePreference("dark")
                    .languagePreference("en-US")
                    .lastActiveAt(testTime)
                    .createdAt(testTime)
                    .updatedAt(testTime)
                    .build();
            
            // Assert
            assertThat(personaDto.getId()).isEqualTo(testPersonaId);
            assertThat(personaDto.getName()).isEqualTo("Work Persona");
            assertThat(personaDto.getType()).isEqualTo(Persona.PersonaType.WORK);
            assertThat(personaDto.isDefault()).isTrue();
            assertThat(personaDto.isActive()).isTrue();
            assertThat(personaDto.getDisplayName()).isEqualTo("John Doe (Work)");
            assertThat(personaDto.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(personaDto.getBio()).isEqualTo("Focused professional mode");
            assertThat(personaDto.getStatusMessage()).isEqualTo("Available for work");
            assertThat(personaDto.getPrivacySettings()).isEqualTo(privacySettings);
            assertThat(personaDto.getCustomAttributes()).isEqualTo(customAttributes);
            assertThat(personaDto.getThemePreference()).isEqualTo("dark");
            assertThat(personaDto.getLanguagePreference()).isEqualTo("en-US");
            assertThat(personaDto.getLastActiveAt()).isEqualTo(testTime);
            assertThat(personaDto.getCreatedAt()).isEqualTo(testTime);
            assertThat(personaDto.getUpdatedAt()).isEqualTo(testTime);
        }
        
        @Test
        @DisplayName("Should create empty PersonaDto with default constructor")
        void shouldCreateEmptyPersonaDtoWithDefaultConstructor() {
            // Act
            PersonaDto personaDto = new PersonaDto();
            
            // Assert
            assertThat(personaDto.getId()).isNull();
            assertThat(personaDto.getName()).isNull();
            assertThat(personaDto.getType()).isNull();
            assertThat(personaDto.isDefault()).isFalse();
            assertThat(personaDto.isActive()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle all PersonaType values")
        void shouldHandleAllPersonaTypes() {
            // Test all enum values
            for (Persona.PersonaType type : Persona.PersonaType.values()) {
                PersonaDto personaDto = PersonaDto.builder()
                        .type(type)
                        .build();
                
                assertThat(personaDto.getType()).isEqualTo(type);
            }
        }
        
        @Test
        @DisplayName("Should support equals and hashCode from Lombok")
        void shouldSupportEqualsAndHashCode() {
            // Arrange
            PersonaDto persona1 = PersonaDto.builder()
                    .id(testPersonaId)
                    .name("Test Persona")
                    .type(Persona.PersonaType.WORK)
                    .build();
            
            PersonaDto persona2 = PersonaDto.builder()
                    .id(testPersonaId)
                    .name("Test Persona")
                    .type(Persona.PersonaType.WORK)
                    .build();
            
            PersonaDto persona3 = PersonaDto.builder()
                    .id(UUID.randomUUID())
                    .name("Different Persona")
                    .type(Persona.PersonaType.PERSONAL)
                    .build();
            
            // Assert
            assertThat(persona1).isEqualTo(persona2);
            assertThat(persona1.hashCode()).isEqualTo(persona2.hashCode());
            assertThat(persona1).isNotEqualTo(persona3);
        }
    }
    
    @Nested
    @DisplayName("PrivacySettingsDto Builder and Operations")
    class PrivacySettingsDtoTests {
        
        @Test
        @DisplayName("Should create PrivacySettingsDto with all fields")
        void shouldCreatePrivacySettingsDtoWithAllFields() {
            // Act
            PersonaDto.PrivacySettingsDto privacySettings = PersonaDto.PrivacySettingsDto.builder()
                    .showRealName(true)
                    .showEmail(false)
                    .showActivity(true)
                    .allowDirectMessages(false)
                    .visibilityLevel("PRIVATE")
                    .searchable(false)
                    .showOnlineStatus(true)
                    .shareFocusSessions(true)
                    .shareAchievements(false)
                    .build();
            
            // Assert
            assertThat(privacySettings.isShowRealName()).isTrue();
            assertThat(privacySettings.isShowEmail()).isFalse();
            assertThat(privacySettings.isShowActivity()).isTrue();
            assertThat(privacySettings.isAllowDirectMessages()).isFalse();
            assertThat(privacySettings.getVisibilityLevel()).isEqualTo("PRIVATE");
            assertThat(privacySettings.isSearchable()).isFalse();
            assertThat(privacySettings.isShowOnlineStatus()).isTrue();
            assertThat(privacySettings.isShareFocusSessions()).isTrue();
            assertThat(privacySettings.isShareAchievements()).isFalse();
        }
        
        @Test
        @DisplayName("Should create empty PrivacySettingsDto with default constructor")
        void shouldCreateEmptyPrivacySettingsDtoWithDefaultConstructor() {
            // Act
            PersonaDto.PrivacySettingsDto privacySettings = new PersonaDto.PrivacySettingsDto();
            
            // Assert
            assertThat(privacySettings.isShowRealName()).isFalse();
            assertThat(privacySettings.isShowEmail()).isFalse();
            assertThat(privacySettings.isShowActivity()).isFalse();
            assertThat(privacySettings.isAllowDirectMessages()).isFalse();
            assertThat(privacySettings.getVisibilityLevel()).isNull();
            assertThat(privacySettings.isSearchable()).isFalse();
            assertThat(privacySettings.isShowOnlineStatus()).isFalse();
            assertThat(privacySettings.isShareFocusSessions()).isFalse();
            assertThat(privacySettings.isShareAchievements()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle different visibility levels")
        void shouldHandleDifferentVisibilityLevels() {
            // Test different visibility levels
            String[] visibilityLevels = {"PUBLIC", "PRIVATE", "FRIENDS_ONLY", "CUSTOM"};
            
            for (String level : visibilityLevels) {
                PersonaDto.PrivacySettingsDto privacySettings = PersonaDto.PrivacySettingsDto.builder()
                        .visibilityLevel(level)
                        .build();
                
                assertThat(privacySettings.getVisibilityLevel()).isEqualTo(level);
            }
        }
    }
    
    @Nested
    @DisplayName("JSON Serialization and Deserialization")
    class JsonSerializationTests {
        
        @Test
        @DisplayName("Should serialize PersonaDto to JSON correctly")
        void shouldSerializePersonaDtoToJson() throws JsonProcessingException {
            // Arrange
            PersonaDto personaDto = PersonaDto.builder()
                    .id(testPersonaId)
                    .name("Test Persona")
                    .type(Persona.PersonaType.WORK)
                    .isDefault(true)
                    .isActive(false)
                    .displayName("John Doe")
                    .themePreference("dark")
                    .build();
            
            // Act
            String json = objectMapper.writeValueAsString(personaDto);
            
            // Assert
            assertThat(json).isNotNull();
            assertThat(json).contains("\"id\":");
            assertThat(json).contains("\"name\":\"Test Persona\"");
            assertThat(json).contains("\"type\":\"WORK\"");
            assertThat(json).contains("\"default\":true");
            assertThat(json).contains("\"active\":false");
            assertThat(json).contains("\"displayName\":\"John Doe\"");
            assertThat(json).contains("\"themePreference\":\"dark\"");
        }
        
        @Test
        @DisplayName("Should deserialize JSON to PersonaDto correctly")
        void shouldDeserializeJsonToPersonaDto() throws JsonProcessingException {
            // Arrange
            String json = String.format("""
                {
                    "id": "%s",
                    "name": "Test Persona",
                    "type": "PERSONAL",
                    "default": false,
                    "active": true,
                    "displayName": "Jane Smith",
                    "themePreference": "light"
                }
                """, testPersonaId);
            
            // Act
            PersonaDto personaDto = objectMapper.readValue(json, PersonaDto.class);
            
            // Assert
            assertThat(personaDto.getId()).isEqualTo(testPersonaId);
            assertThat(personaDto.getName()).isEqualTo("Test Persona");
            assertThat(personaDto.getType()).isEqualTo(Persona.PersonaType.PERSONAL);
            assertThat(personaDto.isDefault()).isFalse();
            assertThat(personaDto.isActive()).isTrue();
            assertThat(personaDto.getDisplayName()).isEqualTo("Jane Smith");
            assertThat(personaDto.getThemePreference()).isEqualTo("light");
        }
        
        @Test
        @DisplayName("Should handle null fields in JSON serialization (@JsonInclude.NON_NULL)")
        void shouldHandleNullFieldsInJsonSerialization() throws JsonProcessingException {
            // Arrange
            PersonaDto personaDto = PersonaDto.builder()
                    .id(testPersonaId)
                    .name("Test Persona")
                    .type(Persona.PersonaType.WORK)
                    // Intentionally leave other fields null
                    .build();
            
            // Act
            String json = objectMapper.writeValueAsString(personaDto);
            
            // Assert - should not include null fields due to @JsonInclude(JsonInclude.Include.NON_NULL)
            assertThat(json).contains("\"id\":");
            assertThat(json).contains("\"name\":\"Test Persona\"");
            assertThat(json).contains("\"type\":\"WORK\"");
            assertThat(json).doesNotContain("\"avatarUrl\":");
            assertThat(json).doesNotContain("\"bio\":");
            assertThat(json).doesNotContain("\"statusMessage\":");
        }
        
        @Test
        @DisplayName("Should serialize and deserialize PrivacySettingsDto within PersonaDto")
        void shouldSerializeAndDeserializePrivacySettingsDto() throws JsonProcessingException {
            // Arrange
            PersonaDto.PrivacySettingsDto privacySettings = PersonaDto.PrivacySettingsDto.builder()
                    .showRealName(true)
                    .showEmail(false)
                    .visibilityLevel("PUBLIC")
                    .build();
            
            PersonaDto personaDto = PersonaDto.builder()
                    .id(testPersonaId)
                    .name("Test Persona")
                    .privacySettings(privacySettings)
                    .build();
            
            // Act - Serialize and deserialize
            String json = objectMapper.writeValueAsString(personaDto);
            PersonaDto deserializedPersonaDto = objectMapper.readValue(json, PersonaDto.class);
            
            // Assert
            assertThat(deserializedPersonaDto.getPrivacySettings()).isNotNull();
            assertThat(deserializedPersonaDto.getPrivacySettings().isShowRealName()).isTrue();
            assertThat(deserializedPersonaDto.getPrivacySettings().isShowEmail()).isFalse();
            assertThat(deserializedPersonaDto.getPrivacySettings().getVisibilityLevel()).isEqualTo("PUBLIC");
        }
    }
    
    @Nested
    @DisplayName("Entity Conversion Tests")
    class EntityConversionTests {
        
        @Test
        @DisplayName("Should convert Persona entity to PersonaDto using from() method")
        void shouldConvertPersonaEntityToPersonaDto() {
            // Arrange
            User user = User.builder()
                    .id(testUserId)
                    .username("testuser")
                    .email("test@example.com")
                    .build();
            
            PrivacySettings privacySettings = PrivacySettings.builder()
                    .showRealName(true)
                    .showEmail(false)
                    .showActivity(true)
                    .allowDirectMessages(true)
                    .visibilityLevel("PUBLIC")
                    .searchable(true)
                    .showOnlineStatus(false)
                    .shareFocusSessions(true)
                    .shareAchievements(false)
                    .build();
            
            Persona persona = Persona.builder()
                    .id(testPersonaId)
                    .user(user)
                    .name("Work Persona")
                    .type(Persona.PersonaType.WORK)
                    .isDefault(true)
                    .isActive(true)
                    .displayName("John Doe")
                    .avatarUrl("https://example.com/avatar.jpg")
                    .bio("Professional mode")
                    .statusMessage("Available")
                    .privacySettings(privacySettings)
                    .customAttributes(Map.of("key", "value"))
                    .themePreference("dark")
                    .languagePreference("en-US")
                    .lastActiveAt(testTime)
                    .createdAt(testTime)
                    .updatedAt(testTime)
                    .build();
            
            // Act
            PersonaDto personaDto = PersonaDto.from(persona);
            
            // Assert
            assertThat(personaDto).isNotNull();
            assertThat(personaDto.getId()).isEqualTo(testPersonaId);
            assertThat(personaDto.getName()).isEqualTo("Work Persona");
            assertThat(personaDto.getType()).isEqualTo(Persona.PersonaType.WORK);
            assertThat(personaDto.isDefault()).isTrue();
            assertThat(personaDto.isActive()).isTrue();
            assertThat(personaDto.getDisplayName()).isEqualTo("John Doe");
            assertThat(personaDto.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(personaDto.getBio()).isEqualTo("Professional mode");
            assertThat(personaDto.getStatusMessage()).isEqualTo("Available");
            assertThat(personaDto.getCustomAttributes()).isEqualTo(Map.of("key", "value"));
            assertThat(personaDto.getThemePreference()).isEqualTo("dark");
            assertThat(personaDto.getLanguagePreference()).isEqualTo("en-US");
            assertThat(personaDto.getLastActiveAt()).isEqualTo(testTime);
            assertThat(personaDto.getCreatedAt()).isEqualTo(testTime);
            assertThat(personaDto.getUpdatedAt()).isEqualTo(testTime);
            
            // Assert privacy settings conversion
            assertThat(personaDto.getPrivacySettings()).isNotNull();
            assertThat(personaDto.getPrivacySettings().isShowRealName()).isTrue();
            assertThat(personaDto.getPrivacySettings().isShowEmail()).isFalse();
            assertThat(personaDto.getPrivacySettings().isShowActivity()).isTrue();
            assertThat(personaDto.getPrivacySettings().isAllowDirectMessages()).isTrue();
            assertThat(personaDto.getPrivacySettings().getVisibilityLevel()).isEqualTo("PUBLIC");
            assertThat(personaDto.getPrivacySettings().isSearchable()).isTrue();
            assertThat(personaDto.getPrivacySettings().isShowOnlineStatus()).isFalse();
            assertThat(personaDto.getPrivacySettings().isShareFocusSessions()).isTrue();
            assertThat(personaDto.getPrivacySettings().isShareAchievements()).isFalse();
        }
        
        @Test
        @DisplayName("Should return null when converting null Persona entity")
        void shouldReturnNullWhenConvertingNullPersonaEntity() {
            // Act
            PersonaDto personaDto = PersonaDto.from(null);
            
            // Assert
            assertThat(personaDto).isNull();
        }
        
        @Test
        @DisplayName("Should handle Persona entity with null privacy settings")
        void shouldHandlePersonaEntityWithNullPrivacySettings() {
            // Arrange
            User user = User.builder()
                    .id(testUserId)
                    .username("testuser")
                    .email("test@example.com")
                    .build();
            
            Persona persona = Persona.builder()
                    .id(testPersonaId)
                    .user(user)
                    .name("Test Persona")
                    .type(Persona.PersonaType.PERSONAL)
                    .privacySettings(null) // Explicitly null
                    .build();
            
            // Act
            PersonaDto personaDto = PersonaDto.from(persona);
            
            // Assert
            assertThat(personaDto).isNotNull();
            assertThat(personaDto.getPrivacySettings()).isNull();
        }
        
        @Test
        @DisplayName("Should handle all PersonaType enum values in conversion")
        void shouldHandleAllPersonaTypeEnumValuesInConversion() {
            User user = User.builder()
                    .id(testUserId)
                    .username("testuser")
                    .email("test@example.com")
                    .build();
            
            // Test all enum values
            for (Persona.PersonaType type : Persona.PersonaType.values()) {
                // Arrange
                Persona persona = Persona.builder()
                        .id(UUID.randomUUID())
                        .user(user)
                        .name("Test Persona")
                        .type(type)
                        .build();
                
                // Act
                PersonaDto personaDto = PersonaDto.from(persona);
                
                // Assert
                assertThat(personaDto).isNotNull();
                assertThat(personaDto.getType()).isEqualTo(type);
            }
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Validation")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle PersonaDto with empty custom attributes")
        void shouldHandlePersonaDtoWithEmptyCustomAttributes() {
            // Act
            PersonaDto personaDto = PersonaDto.builder()
                    .customAttributes(Map.of()) // Empty map
                    .build();
            
            // Assert
            assertThat(personaDto.getCustomAttributes()).isEmpty();
        }
        
        @Test
        @DisplayName("Should handle PersonaDto with large custom attributes map")
        void shouldHandlePersonaDtoWithLargeCustomAttributesMap() {
            // Arrange
            Map<String, String> largeAttributes = Map.of(
                    "attr1", "value1",
                    "attr2", "value2",
                    "attr3", "value3",
                    "attr4", "value4",
                    "attr5", "value5"
            );
            
            // Act
            PersonaDto personaDto = PersonaDto.builder()
                    .customAttributes(largeAttributes)
                    .build();
            
            // Assert
            assertThat(personaDto.getCustomAttributes()).hasSize(5);
            assertThat(personaDto.getCustomAttributes()).containsAllEntriesOf(largeAttributes);
        }
        
        @Test
        @DisplayName("Should handle PersonaDto toString() method")
        void shouldHandlePersonaDtoToStringMethod() {
            // Arrange
            PersonaDto personaDto = PersonaDto.builder()
                    .id(testPersonaId)
                    .name("Test Persona")
                    .type(Persona.PersonaType.WORK)
                    .build();
            
            // Act
            String toStringResult = personaDto.toString();
            
            // Assert
            assertThat(toStringResult).isNotNull();
            assertThat(toStringResult).contains("PersonaDto");
            assertThat(toStringResult).contains("Test Persona");
            assertThat(toStringResult).contains("WORK");
        }
    }
}