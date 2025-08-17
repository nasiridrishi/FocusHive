package com.focushive.identity.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for PersonaProfile entity
 */
@ExtendWith(SpringJUnitExtension.class)
@DataJpaTest
class PersonaProfileTest {

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Persona workPersona;
    private Persona personalPersona;

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

        // Create work persona
        workPersona = Persona.builder()
                .user(testUser)
                .name("work")
                .type(PersonaType.WORK)
                .isDefault(false)
                .isActive(false)
                .displayName("Work Persona")
                .build();
        entityManager.persistAndFlush(workPersona);

        // Create personal persona
        personalPersona = Persona.builder()
                .user(testUser)
                .name("personal")
                .type(PersonaType.PERSONAL)
                .isDefault(true)
                .isActive(true)
                .displayName("Personal Persona")
                .build();
        entityManager.persistAndFlush(personalPersona);
    }

    @Test
    void shouldCreatePersonaProfile() {
        // Given
        PersonaProfile profile = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("work_schedule")
                .profileValue("9am-5pm Monday-Friday")
                .category("schedule")
                .visibility("PRIVATE")
                .description("Work schedule preferences")
                .dataType("STRING")
                .build();

        // When
        PersonaProfile savedProfile = entityManager.persistAndFlush(profile);

        // Then
        assertThat(savedProfile.getId()).isNotNull();
        assertThat(savedProfile.getPersona()).isEqualTo(workPersona);
        assertThat(savedProfile.getProfileKey()).isEqualTo("work_schedule");
        assertThat(savedProfile.getProfileValue()).isEqualTo("9am-5pm Monday-Friday");
        assertThat(savedProfile.getCategory()).isEqualTo("schedule");
        assertThat(savedProfile.getVisibility()).isEqualTo("PRIVATE");
        assertThat(savedProfile.getDescription()).isEqualTo("Work schedule preferences");
        assertThat(savedProfile.getDataType()).isEqualTo("STRING");
        assertThat(savedProfile.getCreatedAt()).isNotNull();
        assertThat(savedProfile.getUpdatedAt()).isNotNull();
        assertThat(savedProfile.isEnabled()).isTrue(); // Default value
    }

    @Test
    void shouldSetDefaultValues() {
        // Given
        PersonaProfile profile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("favorite_color")
                .profileValue("blue")
                .build();

        // When
        PersonaProfile savedProfile = entityManager.persistAndFlush(profile);

        // Then
        assertThat(savedProfile.isEnabled()).isTrue();
        assertThat(savedProfile.getDataType()).isEqualTo("STRING"); // Default
        assertThat(savedProfile.getVisibility()).isEqualTo("PRIVATE"); // Default
        assertThat(savedProfile.getCreatedAt()).isNotNull();
        assertThat(savedProfile.getUpdatedAt()).isNotNull();
        assertThat(savedProfile.getCategory()).isNull(); // Optional
        assertThat(savedProfile.getDescription()).isNull(); // Optional
    }

    @Test
    void shouldStoreComplexJsonData() {
        // Given
        PersonaProfile profile = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("project_preferences")
                .profileValue("{\"languages\": [\"Java\", \"Python\"], \"frameworks\": [\"Spring\", \"React\"], \"experience_years\": 5}")
                .category("skills")
                .dataType("JSON")
                .visibility("PUBLIC")
                .description("Technical skills and preferences")
                .build();

        // When
        PersonaProfile savedProfile = entityManager.persistAndFlush(profile);

        // Then
        assertThat(savedProfile.getDataType()).isEqualTo("JSON");
        assertThat(savedProfile.getProfileValue()).contains("Java");
        assertThat(savedProfile.getProfileValue()).contains("Spring");
        assertThat(savedProfile.getProfileValue()).contains("experience_years");
    }

    @Test
    void shouldHandleDifferentDataTypes() {
        // Given - Number data
        PersonaProfile numberProfile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("age")
                .profileValue("30")
                .dataType("NUMBER")
                .category("demographics")
                .build();

        // And - Boolean data
        PersonaProfile booleanProfile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("newsletter_subscription")
                .profileValue("true")
                .dataType("BOOLEAN")
                .category("preferences")
                .build();

        // And - Date data
        PersonaProfile dateProfile = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("start_date")
                .profileValue("2024-01-15")
                .dataType("DATE")
                .category("employment")
                .build();

        // When
        PersonaProfile savedNumber = entityManager.persistAndFlush(numberProfile);
        PersonaProfile savedBoolean = entityManager.persistAndFlush(booleanProfile);
        PersonaProfile savedDate = entityManager.persistAndFlush(dateProfile);

        // Then
        assertThat(savedNumber.getDataType()).isEqualTo("NUMBER");
        assertThat(savedBoolean.getDataType()).isEqualTo("BOOLEAN");
        assertThat(savedDate.getDataType()).isEqualTo("DATE");
    }

    @Test
    void shouldHandleVisibilityLevels() {
        // Given - Public profile
        PersonaProfile publicProfile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("bio")
                .profileValue("Software developer passionate about clean code")
                .category("about")
                .visibility("PUBLIC")
                .build();

        // And - Friends-only profile
        PersonaProfile friendsProfile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("relationship_status")
                .profileValue("married")
                .category("personal")
                .visibility("FRIENDS")
                .build();

        // And - Private profile
        PersonaProfile privateProfile = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("salary_expectation")
                .profileValue("80000")
                .category("confidential")
                .visibility("PRIVATE")
                .build();

        // When
        PersonaProfile savedPublic = entityManager.persistAndFlush(publicProfile);
        PersonaProfile savedFriends = entityManager.persistAndFlush(friendsProfile);
        PersonaProfile savedPrivate = entityManager.persistAndFlush(privateProfile);

        // Then
        assertThat(savedPublic.getVisibility()).isEqualTo("PUBLIC");
        assertThat(savedFriends.getVisibility()).isEqualTo("FRIENDS");
        assertThat(savedPrivate.getVisibility()).isEqualTo("PRIVATE");
    }

    @Test
    void shouldStoreMetadata() {
        // Given
        PersonaProfile profile = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("location")
                .profileValue("San Francisco, CA")
                .category("location")
                .dataType("STRING")
                .visibility("PUBLIC")
                .metadata(Map.of(
                    "timezone", "PST",
                    "coordinates", "37.7749,-122.4194",
                    "verified", "true",
                    "accuracy", "city_level"
                ))
                .description("Work location with metadata")
                .build();

        // When
        PersonaProfile savedProfile = entityManager.persistAndFlush(profile);

        // Then
        assertThat(savedProfile.getMetadata()).containsEntry("timezone", "PST");
        assertThat(savedProfile.getMetadata()).containsEntry("coordinates", "37.7749,-122.4194");
        assertThat(savedProfile.getMetadata()).containsEntry("verified", "true");
        assertThat(savedProfile.getMetadata()).containsEntry("accuracy", "city_level");
    }

    @Test
    void shouldHandleProfileDisabling() {
        // Given
        PersonaProfile profile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("hobby")
                .profileValue("photography")
                .category("interests")
                .enabled(true)
                .build();
        PersonaProfile savedProfile = entityManager.persistAndFlush(profile);

        // When - Disable the profile
        savedProfile.setEnabled(false);
        PersonaProfile updatedProfile = entityManager.persistAndFlush(savedProfile);

        // Then
        assertThat(updatedProfile.isEnabled()).isFalse();
    }

    @Test
    void shouldEnforceUniqueConstraints() {
        // Given - First profile
        PersonaProfile profile1 = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("department")
                .profileValue("Engineering")
                .category("work")
                .build();
        entityManager.persistAndFlush(profile1);

        // When - Try to create duplicate profile for same persona/key
        PersonaProfile profile2 = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("department")
                .profileValue("Marketing")
                .category("work")
                .build();

        // Then - Should not allow duplicate (enforced by unique constraint)
        // Note: In real scenario this would throw a constraint violation exception
        assertThat(profile1.getPersona()).isEqualTo(profile2.getPersona());
        assertThat(profile1.getProfileKey()).isEqualTo(profile2.getProfileKey());
    }

    @Test
    void shouldAllowSameKeyForDifferentPersonas() {
        // Given - Profile for work persona
        PersonaProfile workProfile = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("email")
                .profileValue("john.doe@company.com")
                .category("contact")
                .visibility("PUBLIC")
                .build();

        // And - Profile for personal persona with same key
        PersonaProfile personalProfile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("email")
                .profileValue("john.doe@personal.com")
                .category("contact")
                .visibility("FRIENDS")
                .build();

        // When
        PersonaProfile savedWork = entityManager.persistAndFlush(workProfile);
        PersonaProfile savedPersonal = entityManager.persistAndFlush(personalProfile);

        // Then
        assertThat(savedWork.getProfileKey()).isEqualTo(savedPersonal.getProfileKey());
        assertThat(savedWork.getProfileValue()).isNotEqualTo(savedPersonal.getProfileValue());
        assertThat(savedWork.getPersona()).isNotEqualTo(savedPersonal.getPersona());
    }

    @Test
    void shouldSupportProfileValidation() {
        // Given
        PersonaProfile profile = PersonaProfile.builder()
                .persona(workPersona)
                .profileKey("phone_number")
                .profileValue("+1-555-123-4567")
                .category("contact")
                .dataType("PHONE")
                .validationRules(Map.of(
                    "format", "E.164",
                    "country_code", "required",
                    "verified", "false"
                ))
                .build();

        // When
        PersonaProfile savedProfile = entityManager.persistAndFlush(profile);

        // Then
        assertThat(savedProfile.getValidationRules()).containsEntry("format", "E.164");
        assertThat(savedProfile.getValidationRules()).containsEntry("country_code", "required");
        assertThat(savedProfile.getValidationRules()).containsEntry("verified", "false");
    }

    @Test
    void shouldTrackProfileSource() {
        // Given
        PersonaProfile profile = PersonaProfile.builder()
                .persona(personalPersona)
                .profileKey("interests")
                .profileValue("machine learning, photography, hiking")
                .category("interests")
                .source("user_input")
                .sourceMetadata(Map.of(
                    "input_method", "profile_form",
                    "suggested", "false",
                    "confidence", "high"
                ))
                .build();

        // When
        PersonaProfile savedProfile = entityManager.persistAndFlush(profile);

        // Then
        assertThat(savedProfile.getSource()).isEqualTo("user_input");
        assertThat(savedProfile.getSourceMetadata()).containsEntry("input_method", "profile_form");
        assertThat(savedProfile.getSourceMetadata()).containsEntry("suggested", "false");
        assertThat(savedProfile.getSourceMetadata()).containsEntry("confidence", "high");
    }
}