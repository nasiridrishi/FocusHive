package com.focushive.identity.service;

import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.ResourceNotFoundException;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for PersonaService following TDD approach.
 * Tests persona management features including CRUD operations, context switching, and templates.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonaService Tests")
class PersonaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PersonaService personaService;

    private User testUser;
    private Persona workPersona;
    private Persona personalPersona;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .personas(new ArrayList<>())
                .build();

        workPersona = Persona.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Work")
                .type(Persona.PersonaType.WORK)
                .displayName("Work Profile")
                .isDefault(true)
                .isActive(true)
                .privacySettings(new Persona.PrivacySettings())
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        personalPersona = Persona.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Personal")
                .type(Persona.PersonaType.PERSONAL)
                .displayName("Personal Profile")
                .isDefault(false)
                .isActive(false)
                .privacySettings(new Persona.PrivacySettings())
                .customAttributes(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        testUser.getPersonas().addAll(Arrays.asList(workPersona, personalPersona));
    }

    @Test
    @DisplayName("Should create persona successfully")
    void createPersona_ShouldCreatePersonaSuccessfully() {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("Gaming")
                .type(Persona.PersonaType.GAMING)
                .displayName("Gaming Profile")
                .bio("My gaming persona")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "Gaming")).thenReturn(Optional.empty());
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersona(testUserId, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Gaming");
        assertThat(result.getType()).isEqualTo(Persona.PersonaType.GAMING);
        assertThat(result.getDisplayName()).isEqualTo("Gaming Profile");
        assertThat(result.getBio()).isEqualTo("My gaming persona");
        
        verify(userRepository).findById(testUserId);
        verify(personaRepository).findByUserIdAndName(testUserId, "Gaming");
        verify(personaRepository).save(any(Persona.class));
    }

    @Test
    @DisplayName("Should throw exception when creating persona with duplicate name")
    void createPersona_ShouldThrowExceptionForDuplicateName() {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("Work")
                .type(Persona.PersonaType.WORK)
                .displayName("Another Work Profile")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "Work")).thenReturn(Optional.of(workPersona));

        // When & Then
        assertThatThrownBy(() -> personaService.createPersona(testUserId, createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Persona with name 'Work' already exists");
        
        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    @DisplayName("Should get user personas successfully")
    void getUserPersonas_ShouldReturnPersonasSuccessfully() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdOrderByPriority(testUserId))
                .thenReturn(Arrays.asList(workPersona, personalPersona));

        // When
        List<PersonaDto> result = personaService.getUserPersonas(testUserId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Work");
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(1).getName()).isEqualTo("Personal");
        assertThat(result.get(1).isActive()).isFalse();
        
        verify(userRepository).findById(testUserId);
        verify(personaRepository).findByUserIdOrderByPriority(testUserId);
    }

    @Test
    @DisplayName("Should switch persona context successfully")
    void switchPersona_ShouldSwitchContextSuccessfully() {
        // Given
        UUID targetPersonaId = personalPersona.getId();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(targetPersonaId, testUser)).thenReturn(Optional.of(personalPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PersonaDto result = personaService.switchPersona(testUserId, targetPersonaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(targetPersonaId);
        assertThat(result.isActive()).isTrue();
        
        verify(personaRepository).updateActivePersona(testUserId, targetPersonaId);
        verify(personaRepository).save(personalPersona);
    }

    @Test
    @DisplayName("Should throw exception when switching to non-existent persona")
    void switchPersona_ShouldThrowExceptionForNonExistentPersona() {
        // Given
        UUID nonExistentPersonaId = UUID.randomUUID();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(nonExistentPersonaId, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> personaService.switchPersona(testUserId, nonExistentPersonaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found");
        
        verify(personaRepository, never()).updateActivePersona(any(), any());
    }

    @Test
    @DisplayName("Should update persona successfully")
    void updatePersona_ShouldUpdatePersonaSuccessfully() {
        // Given
        UUID personaId = workPersona.getId();
        PersonaDto updateRequest = PersonaDto.builder()
                .displayName("Updated Work Profile")
                .bio("Updated bio")
                .statusMessage("In a meeting")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PersonaDto result = personaService.updatePersona(testUserId, personaId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("Updated Work Profile");
        assertThat(result.getBio()).isEqualTo("Updated bio");
        assertThat(result.getStatusMessage()).isEqualTo("In a meeting");
        
        verify(personaRepository).save(workPersona);
    }

    @Test
    @DisplayName("Should delete persona successfully")
    void deletePersona_ShouldDeletePersonaSuccessfully() {
        // Given
        UUID personaId = personalPersona.getId();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(personalPersona));

        // When
        personaService.deletePersona(testUserId, personaId);

        // Then
        verify(personaRepository).delete(personalPersona);
    }

    @Test
    @DisplayName("Should not delete default persona")
    void deletePersona_ShouldNotDeleteDefaultPersona() {
        // Given
        UUID personaId = workPersona.getId();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));

        // When & Then
        assertThatThrownBy(() -> personaService.deletePersona(testUserId, personaId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete default persona");
        
        verify(personaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should get active persona successfully")
    void getActivePersona_ShouldReturnActivePersona() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndIsActiveTrue(testUserId)).thenReturn(Optional.of(workPersona));

        // When
        Optional<PersonaDto> result = personaService.getActivePersona(testUserId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        assertThat(result.get().isActive()).isTrue();
        
        verify(personaRepository).findByUserIdAndIsActiveTrue(testUserId);
    }

    @Test
    @DisplayName("Should create persona from template successfully")
    void createPersonaFromTemplate_ShouldCreatePersonaSuccessfully() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "Study")).thenReturn(Optional.empty());
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersonaFromTemplate(testUserId, Persona.PersonaType.STUDY);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Study");
        assertThat(result.getType()).isEqualTo(Persona.PersonaType.STUDY);
        assertThat(result.getDisplayName()).isEqualTo("Study Profile");
        
        verify(personaRepository).save(any(Persona.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void createPersona_ShouldThrowExceptionWhenUserNotFound() {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("Test")
                .type(Persona.PersonaType.CUSTOM)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> personaService.createPersona(testUserId, createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
        
        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should validate persona data isolation")
    void getPersona_ShouldEnforceDataIsolation() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        UUID personaId = workPersona.getId();
        
        User otherUser = User.builder()
                .id(otherUserId)
                .username("otheruser")
                .email("other@example.com")
                .build();

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(personaRepository.findByIdAndUser(personaId, otherUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> personaService.getPersona(otherUserId, personaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found");
        
        verify(personaRepository).findByIdAndUser(personaId, otherUser);
    }

    // ======================= COMPREHENSIVE NEW TESTS =======================

    @Test
    @DisplayName("Should make first persona default and active automatically")
    void createPersona_FirstPersona_ShouldBeDefaultAndActive() {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("FirstPersona")
                .type(Persona.PersonaType.WORK)
                .displayName("First Profile")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "FirstPersona")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(0L); // First persona
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersona(testUserId, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isTrue();
        assertThat(result.isActive()).isTrue();
        
        verify(personaRepository).countByUserId(testUserId);
        verify(personaRepository).save(argThat(persona -> 
            persona.isDefault() && persona.isActive()));
    }

    @Test
    @DisplayName("Should not make subsequent personas default or active")
    void createPersona_SubsequentPersona_ShouldNotBeDefaultOrActive() {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("SecondPersona")
                .type(Persona.PersonaType.PERSONAL)
                .displayName("Second Profile")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "SecondPersona")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(2L); // Already has personas
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersona(testUserId, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDefault()).isFalse();
        assertThat(result.isActive()).isFalse();
        
        verify(personaRepository).countByUserId(testUserId);
        verify(personaRepository).save(argThat(persona -> 
            !persona.isDefault() && !persona.isActive()));
    }

    @Test
    @DisplayName("Should create persona with privacy settings")
    void createPersona_WithPrivacySettings_ShouldCreateCorrectly() {
        // Given
        PersonaDto.PrivacySettingsDto privacySettings = PersonaDto.PrivacySettingsDto.builder()
                .showRealName(true)
                .showEmail(false)
                .showActivity(true)
                .allowDirectMessages(true)
                .visibilityLevel("FRIENDS")
                .searchable(true)
                .showOnlineStatus(true)
                .shareFocusSessions(true)
                .shareAchievements(false)
                .build();

        PersonaDto createRequest = PersonaDto.builder()
                .name("PrivacyPersona")
                .type(Persona.PersonaType.WORK)
                .displayName("Privacy Profile")
                .privacySettings(privacySettings)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "PrivacyPersona")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(1L);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersona(testUserId, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrivacySettings()).isNotNull();
        assertThat(result.getPrivacySettings().isShowRealName()).isTrue();
        assertThat(result.getPrivacySettings().isShowEmail()).isFalse();
        assertThat(result.getPrivacySettings().getVisibilityLevel()).isEqualTo("FRIENDS");
    }

    @Test
    @DisplayName("Should create persona with custom attributes")
    void createPersona_WithCustomAttributes_ShouldCreateCorrectly() {
        // Given
        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("department", "Engineering");
        customAttributes.put("level", "Senior");
        customAttributes.put("skills", "Java,Spring,React");

        PersonaDto createRequest = PersonaDto.builder()
                .name("CustomPersona")
                .type(Persona.PersonaType.WORK)
                .displayName("Custom Profile")
                .customAttributes(customAttributes)
                .themePreference("dark")
                .languagePreference("en-US")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "CustomPersona")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(1L);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersona(testUserId, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomAttributes()).isNotNull();
        assertThat(result.getCustomAttributes()).containsEntry("department", "Engineering");
        assertThat(result.getCustomAttributes()).containsEntry("level", "Senior");
        assertThat(result.getThemePreference()).isEqualTo("dark");
        assertThat(result.getLanguagePreference()).isEqualTo("en-US");
    }

    @Test
    @DisplayName("Should set default persona successfully")
    void setDefaultPersona_ShouldSetDefaultCorrectly() {
        // Given
        UUID personaId = personalPersona.getId();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(personalPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PersonaDto result = personaService.setDefaultPersona(testUserId, personaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(personaId);
        assertThat(result.isDefault()).isTrue();

        verify(personaRepository).clearDefaultPersonaExcept(testUserId, personaId);
        verify(personaRepository).save(personalPersona);
        assertThat(personalPersona.isDefault()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when setting default for non-existent persona")
    void setDefaultPersona_PersonaNotFound_ShouldThrowException() {
        // Given
        UUID nonExistentPersonaId = UUID.randomUUID();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(nonExistentPersonaId, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> personaService.setDefaultPersona(testUserId, nonExistentPersonaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found");

        verify(personaRepository, never()).clearDefaultPersonaExcept(any(), any());
        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    @DisplayName("Should get persona templates successfully")
    void getPersonaTemplates_ShouldReturnAllTemplates() {
        // When
        List<PersonaDto> templates = personaService.getPersonaTemplates();

        // Then
        assertThat(templates).hasSize(4);
        assertThat(templates.stream().map(PersonaDto::getType))
                .containsExactlyInAnyOrder(
                    Persona.PersonaType.WORK,
                    Persona.PersonaType.PERSONAL,
                    Persona.PersonaType.GAMING,
                    Persona.PersonaType.STUDY
                );

        PersonaDto workTemplate = templates.stream()
                .filter(t -> t.getType() == Persona.PersonaType.WORK)
                .findFirst()
                .orElseThrow();
        assertThat(workTemplate.getName()).isEqualTo("Work");
        assertThat(workTemplate.getDisplayName()).isEqualTo("Work Profile");
        assertThat(workTemplate.getBio()).contains("Professional work profile");
        assertThat(workTemplate.getThemePreference()).isEqualTo("light");
        assertThat(workTemplate.getPrivacySettings().getVisibilityLevel()).isEqualTo("PUBLIC");

        PersonaDto gamingTemplate = templates.stream()
                .filter(t -> t.getType() == Persona.PersonaType.GAMING)
                .findFirst()
                .orElseThrow();
        assertThat(gamingTemplate.getName()).isEqualTo("Gaming");
        assertThat(gamingTemplate.getThemePreference()).isEqualTo("dark");
        assertThat(gamingTemplate.getPrivacySettings().getVisibilityLevel()).isEqualTo("FRIENDS");
    }

    @Test
    @DisplayName("Should get specific persona successfully")
    void getPersona_ShouldReturnPersonaSuccessfully() {
        // Given
        UUID personaId = workPersona.getId();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));

        // When
        PersonaDto result = personaService.getPersona(testUserId, personaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(personaId);
        assertThat(result.getName()).isEqualTo("Work");
        assertThat(result.getType()).isEqualTo(Persona.PersonaType.WORK);
        assertThat(result.isDefault()).isTrue();
        assertThat(result.isActive()).isTrue();

        verify(personaRepository).findByIdAndUser(personaId, testUser);
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent persona")
    void getPersona_PersonaNotFound_ShouldThrowException() {
        // Given
        UUID nonExistentPersonaId = UUID.randomUUID();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(nonExistentPersonaId, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> personaService.getPersona(testUserId, nonExistentPersonaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found");
    }

    @Test
    @DisplayName("Should update persona privacy settings")
    void updatePersona_WithPrivacySettings_ShouldUpdateCorrectly() {
        // Given
        UUID personaId = workPersona.getId();
        PersonaDto.PrivacySettingsDto updatedPrivacySettings = PersonaDto.PrivacySettingsDto.builder()
                .showRealName(false)
                .showEmail(false)
                .showActivity(false)
                .allowDirectMessages(false)
                .visibilityLevel("PRIVATE")
                .searchable(false)
                .showOnlineStatus(false)
                .shareFocusSessions(false)
                .shareAchievements(false)
                .build();

        PersonaDto updateRequest = PersonaDto.builder()
                .displayName("Updated Work Profile")
                .privacySettings(updatedPrivacySettings)
                .themePreference("dark")
                .languagePreference("es-ES")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PersonaDto result = personaService.updatePersona(testUserId, personaId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("Updated Work Profile");
        assertThat(result.getThemePreference()).isEqualTo("dark");
        assertThat(result.getLanguagePreference()).isEqualTo("es-ES");
        assertThat(result.getPrivacySettings()).isNotNull();
        assertThat(result.getPrivacySettings().getVisibilityLevel()).isEqualTo("PRIVATE");
        assertThat(result.getPrivacySettings().isSearchable()).isFalse();
    }

    @Test
    @DisplayName("Should update persona custom attributes")
    void updatePersona_WithCustomAttributes_ShouldUpdateCorrectly() {
        // Given
        UUID personaId = workPersona.getId();
        Map<String, String> updatedAttributes = new HashMap<>();
        updatedAttributes.put("department", "Product");
        updatedAttributes.put("role", "Manager");

        PersonaDto updateRequest = PersonaDto.builder()
                .customAttributes(updatedAttributes)
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PersonaDto result = personaService.updatePersona(testUserId, personaId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomAttributes()).containsEntry("department", "Product");
        assertThat(result.getCustomAttributes()).containsEntry("role", "Manager");
        assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");
    }

    @Test
    @DisplayName("Should handle partial updates correctly")
    void updatePersona_PartialUpdate_ShouldUpdateOnlyProvidedFields() {
        // Given
        UUID personaId = workPersona.getId();
        PersonaDto updateRequest = PersonaDto.builder()
                .statusMessage("Available")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PersonaDto result = personaService.updatePersona(testUserId, personaId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatusMessage()).isEqualTo("Available");
        // Other fields should remain unchanged from original workPersona
        assertThat(result.getName()).isEqualTo("Work");
        assertThat(result.getType()).isEqualTo(Persona.PersonaType.WORK);
    }

    @Test
    @DisplayName("Should delete active persona and switch to default")
    void deletePersona_ActivePersona_ShouldSwitchToDefault() {
        // Given
        personalPersona.setActive(true); // Make personalPersona active
        UUID personaId = personalPersona.getId();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(personalPersona));
        when(personaRepository.findByUserAndIsDefaultTrue(testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.findByIdAndUser(workPersona.getId(), testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        personaService.deletePersona(testUserId, personaId);

        // Then
        verify(personaRepository).findByUserAndIsDefaultTrue(testUser);
        verify(personaRepository).updateActivePersona(testUserId, workPersona.getId());
        verify(personaRepository).delete(personalPersona);
    }

    @Test
    @DisplayName("Should return empty when no active persona found")
    void getActivePersona_NoActivePersona_ShouldReturnEmpty() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndIsActiveTrue(testUserId)).thenReturn(Optional.empty());

        // When
        Optional<PersonaDto> result = personaService.getActivePersona(testUserId);

        // Then
        assertThat(result).isEmpty();
        verify(personaRepository).findByUserIdAndIsActiveTrue(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent persona")
    void updatePersona_PersonaNotFound_ShouldThrowException() {
        // Given
        UUID nonExistentPersonaId = UUID.randomUUID();
        PersonaDto updateRequest = PersonaDto.builder()
                .displayName("Updated")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(nonExistentPersonaId, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> personaService.updatePersona(testUserId, nonExistentPersonaId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found");

        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent persona")
    void deletePersona_PersonaNotFound_ShouldThrowException() {
        // Given
        UUID nonExistentPersonaId = UUID.randomUUID();
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(nonExistentPersonaId, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> personaService.deletePersona(testUserId, nonExistentPersonaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found");

        verify(personaRepository, never()).delete(any(Persona.class));
    }

    @Test
    @DisplayName("Should create persona with null custom attributes")
    void createPersona_WithNullCustomAttributes_ShouldUseEmptyMap() {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("SimplePersona")
                .type(Persona.PersonaType.CUSTOM)
                .displayName("Simple Profile")
                .customAttributes(null) // Explicitly null
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "SimplePersona")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(1L);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersona(testUserId, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomAttributes()).isNotNull();
        assertThat(result.getCustomAttributes()).isEmpty();

        verify(personaRepository).save(argThat(persona -> 
            persona.getCustomAttributes() != null && persona.getCustomAttributes().isEmpty()));
    }

    @Test
    @DisplayName("Should create persona with null privacy settings")
    void createPersona_WithNullPrivacySettings_ShouldUseDefaults() {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("SimplePersona")
                .type(Persona.PersonaType.CUSTOM)
                .displayName("Simple Profile")
                .privacySettings(null) // Explicitly null
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "SimplePersona")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(1L);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When
        PersonaDto result = personaService.createPersona(testUserId, createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrivacySettings()).isNotNull();
        
        verify(personaRepository).save(argThat(persona -> 
            persona.getPrivacySettings() != null));
    }

    @Test
    @DisplayName("Should update persona with null privacy settings in entity")
    void updatePersona_EntityWithNullPrivacySettings_ShouldCreateNew() {
        // Given
        UUID personaId = workPersona.getId();
        workPersona.setPrivacySettings(null); // Null privacy settings in entity

        PersonaDto.PrivacySettingsDto newPrivacySettings = PersonaDto.PrivacySettingsDto.builder()
                .showRealName(true)
                .visibilityLevel("PUBLIC")
                .build();

        PersonaDto updateRequest = PersonaDto.builder()
                .privacySettings(newPrivacySettings)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PersonaDto result = personaService.updatePersona(testUserId, personaId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrivacySettings()).isNotNull();
        assertThat(result.getPrivacySettings().isShowRealName()).isTrue();
        assertThat(result.getPrivacySettings().getVisibilityLevel()).isEqualTo("PUBLIC");

        // Verify that privacy settings were created for the persona
        assertThat(workPersona.getPrivacySettings()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty privacy settings DTO when persona privacy settings are null")
    void convertToDto_NullPrivacySettings_ShouldReturnEmptyDto() {
        // Given
        workPersona.setPrivacySettings(null);
        UUID personaId = workPersona.getId();
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));

        // When
        PersonaDto result = personaService.getPersona(testUserId, personaId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrivacySettings()).isNotNull();
        // Should return empty DTO with default values (null/false/empty)
    }
}