package com.focushive.identity.repository;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PersonaRepository using @DataJpaTest.
 * Tests complex repository queries with H2 in-memory database.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "com.focushive.identity.entity")
class PersonaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;
    private Persona workPersona;
    private Persona personalPersona;
    private Persona gamingPersona;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .password("encoded-password")
                .firstName("Other")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        // Save users first
        testUser = entityManager.persistAndFlush(testUser);
        otherUser = entityManager.persistAndFlush(otherUser);

        // Create personas for test user
        workPersona = Persona.builder()
                .user(testUser)
                .name("Work")
                .type(Persona.PersonaType.WORK)
                .displayName("Work Profile")
                .isDefault(true)
                .isActive(true)
                .privacySettings(new Persona.PrivacySettings())
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now().minusSeconds(3))
                .build();

        personalPersona = Persona.builder()
                .user(testUser)
                .name("Personal")
                .type(Persona.PersonaType.PERSONAL)
                .displayName("Personal Profile")
                .isDefault(false)
                .isActive(false)
                .privacySettings(new Persona.PrivacySettings())
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now().minusSeconds(2))
                .build();

        gamingPersona = Persona.builder()
                .user(testUser)
                .name("Gaming")
                .type(Persona.PersonaType.GAMING)
                .displayName("Gaming Profile")
                .isDefault(false)
                .isActive(false)
                .privacySettings(new Persona.PrivacySettings())
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now().minusSeconds(1))
                .build();

        // Save personas
        workPersona = entityManager.persistAndFlush(workPersona);
        personalPersona = entityManager.persistAndFlush(personalPersona);
        gamingPersona = entityManager.persistAndFlush(gamingPersona);
        entityManager.clear();
    }

    @Test
    @DisplayName("Should find personas by user")
    void findByUser_ExistingUser_ShouldReturnPersonas() {
        // When
        List<Persona> result = personaRepository.findByUser(testUser);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Persona::getName)
                .containsExactlyInAnyOrder("Work", "Personal", "Gaming");
    }

    @Test
    @DisplayName("Should find personas by user ID")
    void findByUserId_ExistingUser_ShouldReturnPersonas() {
        // When
        List<Persona> result = personaRepository.findByUserId(testUser.getId());

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Persona::getName)
                .containsExactlyInAnyOrder("Work", "Personal", "Gaming");
    }

    @Test
    @DisplayName("Should find persona by ID and user")
    void findByIdAndUser_ExistingPersona_ShouldReturnPersona() {
        // When
        Optional<Persona> result = personaRepository.findByIdAndUser(workPersona.getId(), testUser);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should not find persona by ID and different user")
    void findByIdAndUser_DifferentUser_ShouldReturnEmpty() {
        // When
        Optional<Persona> result = personaRepository.findByIdAndUser(workPersona.getId(), otherUser);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find default persona by user")
    void findByUserAndIsDefaultTrue_ExistingDefault_ShouldReturnPersona() {
        // When
        Optional<Persona> result = personaRepository.findByUserAndIsDefaultTrue(testUser);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        assertThat(result.get().isDefault()).isTrue();
    }

    @Test
    @DisplayName("Should find active persona by user")
    void findByUserAndIsActiveTrue_ExistingActive_ShouldReturnPersona() {
        // When
        Optional<Persona> result = personaRepository.findByUserAndIsActiveTrue(testUser);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Should find active persona by user ID")
    void findByUserIdAndIsActiveTrue_ExistingActive_ShouldReturnPersona() {
        // When
        Optional<Persona> result = personaRepository.findByUserIdAndIsActiveTrue(testUser.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Should find persona by user ID and name")
    void findByUserIdAndName_ExistingPersona_ShouldReturnPersona() {
        // When
        Optional<Persona> result = personaRepository.findByUserIdAndName(testUser.getId(), "Work");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        assertThat(result.get().getType()).isEqualTo(Persona.PersonaType.WORK);
    }

    @Test
    @DisplayName("Should return empty when persona name not found")
    void findByUserIdAndName_NonExistentName_ShouldReturnEmpty() {
        // When
        Optional<Persona> result = personaRepository.findByUserIdAndName(testUser.getId(), "NonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should count personas by user ID")
    void countByUserId_ExistingUser_ShouldReturnCount() {
        // When
        long count = personaRepository.countByUserId(testUser.getId());

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return zero count for user with no personas")
    void countByUserId_UserWithNoPersonas_ShouldReturnZero() {
        // When
        long count = personaRepository.countByUserId(otherUser.getId());

        // Then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should update active persona atomically")
    void updateActivePersona_ValidPersona_ShouldUpdateActiveStatus() {
        // Given - initially work persona is active
        assertThat(personaRepository.findByUserIdAndIsActiveTrue(testUser.getId()))
                .hasValueSatisfying(p -> assertThat(p.getName()).isEqualTo("Work"));

        // When
        personaRepository.updateActivePersona(testUser.getId(), personalPersona.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Persona> activePersona = personaRepository.findByUserIdAndIsActiveTrue(testUser.getId());
        assertThat(activePersona).isPresent();
        assertThat(activePersona.get().getName()).isEqualTo("Personal");

        // Verify work persona is no longer active
        Persona workFromDb = personaRepository.findById(workPersona.getId()).orElseThrow();
        assertThat(workFromDb.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should clear default persona except specified one")
    void clearDefaultPersonaExcept_ValidPersona_ShouldClearOthers() {
        // Given - initially work persona is default
        assertThat(personaRepository.findByUserAndIsDefaultTrue(testUser))
                .hasValueSatisfying(p -> assertThat(p.getName()).isEqualTo("Work"));

        // When
        personaRepository.clearDefaultPersonaExcept(testUser.getId(), personalPersona.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        Persona workFromDb = personaRepository.findById(workPersona.getId()).orElseThrow();
        assertThat(workFromDb.isDefault()).isFalse();

        // Personal persona should still be available to be set as default
        Persona personalFromDb = personaRepository.findById(personalPersona.getId()).orElseThrow();
        assertThat(personalFromDb.isDefault()).isFalse(); // Method only clears, doesn't set
    }

    @Test
    @DisplayName("Should find personas ordered by priority")
    void findByUserIdOrderByPriority_MultiplePersonas_ShouldReturnOrderedList() {
        // When
        List<Persona> result = personaRepository.findByUserIdOrderByPriority(testUser.getId());

        // Then
        assertThat(result).hasSize(3);
        
        // Active persona should be first
        assertThat(result.get(0).getName()).isEqualTo("Work");
        assertThat(result.get(0).isActive()).isTrue();
        
        // Then default (which is same as active in this case), then by creation time
        // Since Work is both active and default, next should be by creation order
        assertThat(result.get(1).getName()).isIn("Personal", "Gaming");
        assertThat(result.get(2).getName()).isIn("Personal", "Gaming");
    }

    @Test
    @DisplayName("Should find personas with attributes ordered by priority")
    void findByUserIdOrderByPriorityWithAttributes_MultiplePersonas_ShouldReturnOrderedList() {
        // When
        List<Persona> result = personaRepository.findByUserIdOrderByPriorityWithAttributes(testUser.getId());

        // Then
        assertThat(result).hasSize(3);
        
        // Active persona should be first
        assertThat(result.get(0).getName()).isEqualTo("Work");
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(0).getCustomAttributes()).isNotNull();
    }

    @Test
    @DisplayName("Should find active persona with attributes")
    void findByUserIdAndIsActiveTrueWithAttributes_ExistingActive_ShouldReturnPersona() {
        // When
        Optional<Persona> result = personaRepository.findByUserIdAndIsActiveTrueWithAttributes(testUser.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        assertThat(result.get().isActive()).isTrue();
        assertThat(result.get().getCustomAttributes()).isNotNull();
        assertThat(result.get().getPrivacySettings()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty when no active persona with attributes")
    void findByUserIdAndIsActiveTrueWithAttributes_NoActive_ShouldReturnEmpty() {
        // Given - set all personas to inactive
        personaRepository.updateActivePersona(testUser.getId(), null);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Persona> result = personaRepository.findByUserIdAndIsActiveTrueWithAttributes(testUser.getId());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should save persona with all fields")
    void save_CompletePersona_ShouldPersistAllFields() {
        // Given
        Persona.PrivacySettings privacy = new Persona.PrivacySettings();
        privacy.setShowRealName(true);
        privacy.setShowActivity(false);
        privacy.setVisibilityLevel("FRIENDS");

        Persona studyPersona = Persona.builder()
                .user(testUser)
                .name("Study")
                .type(Persona.PersonaType.STUDY)
                .displayName("Study Profile")
                .bio("Academic focus")
                .statusMessage("Studying")
                .avatarUrl("http://example.com/avatar.jpg")
                .isDefault(false)
                .isActive(false)
                .privacySettings(privacy)
                .customAttributes(Map.of("university", "Test University"))
                .themePreference("light")
                .languagePreference("en")
                .createdAt(Instant.now())
                .build();

        // When
        Persona saved = personaRepository.save(studyPersona);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(saved.getId()).isNotNull();
        
        Persona found = personaRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Study");
        assertThat(found.getType()).isEqualTo(Persona.PersonaType.STUDY);
        assertThat(found.getDisplayName()).isEqualTo("Study Profile");
        assertThat(found.getBio()).isEqualTo("Academic focus");
        assertThat(found.getStatusMessage()).isEqualTo("Studying");
        assertThat(found.getAvatarUrl()).isEqualTo("http://example.com/avatar.jpg");
        assertThat(found.getPrivacySettings().isShowRealName()).isTrue();
        assertThat(found.getPrivacySettings().isShowActivity()).isFalse();
        assertThat(found.getPrivacySettings().getVisibilityLevel()).isEqualTo("FRIENDS");
        assertThat(found.getCustomAttributes()).containsEntry("university", "Test University");
        assertThat(found.getThemePreference()).isEqualTo("light");
        assertThat(found.getLanguagePreference()).isEqualTo("en");
    }

    @Test
    @DisplayName("Should enforce persona name uniqueness per user")
    void save_DuplicatePersonaName_ShouldThrowException() {
        // Given
        Persona duplicatePersona = Persona.builder()
                .user(testUser)
                .name("Work") // Same name as existing persona
                .type(Persona.PersonaType.WORK)
                .displayName("Another Work Profile")
                .isDefault(false)
                .isActive(false)
                .privacySettings(new Persona.PrivacySettings())
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            entityManager.persistAndFlush(duplicatePersona);
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should allow same persona name for different users")
    void save_SamePersonaNameDifferentUser_ShouldSucceed() {
        // Given
        Persona otherUserWorkPersona = Persona.builder()
                .user(otherUser)
                .name("Work") // Same name as testUser's persona but different user
                .type(Persona.PersonaType.WORK)
                .displayName("Other User Work Profile")
                .isDefault(true)
                .isActive(true)
                .privacySettings(new Persona.PrivacySettings())
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        // When
        Persona saved = personaRepository.save(otherUserWorkPersona);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Work");
        assertThat(saved.getUser().getId()).isEqualTo(otherUser.getId());
    }

    @Test
    @DisplayName("Should delete persona and maintain referential integrity")
    void delete_ExistingPersona_ShouldRemovePersona() {
        // Given
        Long initialCount = personaRepository.countByUserId(testUser.getId());
        assertThat(initialCount).isEqualTo(3);

        // When
        personaRepository.delete(gamingPersona);
        entityManager.flush();
        entityManager.clear();

        // Then
        Long finalCount = personaRepository.countByUserId(testUser.getId());
        assertThat(finalCount).isEqualTo(2);
        
        Optional<Persona> deletedPersona = personaRepository.findById(gamingPersona.getId());
        assertThat(deletedPersona).isEmpty();

        // User should still exist
        Optional<User> user = userRepository.findById(testUser.getId());
        assertThat(user).isPresent();
    }
}