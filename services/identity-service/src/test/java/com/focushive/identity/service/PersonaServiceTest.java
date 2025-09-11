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

    // ======================= NEW COMPREHENSIVE TESTS =======================

    @Test
    @DisplayName("Should handle creating persona with empty name (current behavior test)")
    void createPersona_EmptyName_CurrentBehavior() {
        // Given - Test empty name (this tests current behavior, not validation)
        PersonaDto emptyNameRequest = PersonaDto.builder()
                .name("")
                .type(Persona.PersonaType.WORK)
                .displayName("Empty Name Profile")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(1L);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When 
        PersonaDto result = personaService.createPersona(testUserId, emptyNameRequest);

        // Then - Current behavior allows empty name
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("");
        assertThat(result.getDisplayName()).isEqualTo("Empty Name Profile");
        
        verify(personaRepository).save(argThat(persona -> 
            persona.getName().equals("")));
    }

    @Test
    @DisplayName("Should create persona with all valid PersonaType enum values")
    void createPersona_AllPersonaTypes_ShouldCreateSuccessfully() {
        // Test all PersonaType enum values
        Persona.PersonaType[] allTypes = {
            Persona.PersonaType.WORK,
            Persona.PersonaType.PERSONAL, 
            Persona.PersonaType.GAMING,
            Persona.PersonaType.STUDY,
            Persona.PersonaType.CUSTOM
        };

        for (int i = 0; i < allTypes.length; i++) {
            Persona.PersonaType type = allTypes[i];
            
            // Given
            PersonaDto createRequest = PersonaDto.builder()
                    .name(type.name() + "_Test")
                    .type(type)
                    .displayName(type.name() + " Profile Test")
                    .bio("Testing " + type.name() + " persona creation")
                    .build();

            // Reset mocks for each iteration
            reset(userRepository, personaRepository);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(personaRepository.findByUserIdAndName(testUserId, type.name() + "_Test")).thenReturn(Optional.empty());
            when(personaRepository.countByUserId(testUserId)).thenReturn((long) i); // Simulate existing personas
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
            assertThat(result.getName()).isEqualTo(type.name() + "_Test");
            assertThat(result.getType()).isEqualTo(type);
            assertThat(result.getDisplayName()).isEqualTo(type.name() + " Profile Test");
            assertThat(result.getBio()).isEqualTo("Testing " + type.name() + " persona creation");
            
            // Verify first persona is default and active, others are not
            if (i == 0) {
                assertThat(result.isDefault()).isTrue();
                assertThat(result.isActive()).isTrue();
            } else {
                assertThat(result.isDefault()).isFalse();
                assertThat(result.isActive()).isFalse();
            }

            verify(personaRepository).save(argThat(persona -> 
                persona.getType().equals(type) && 
                persona.getName().equals(type.name() + "_Test")));
        }
    }

    @Test
    @DisplayName("Should create persona with default privacy settings when not provided")
    void createPersona_NoPrivacySettings_ShouldUseDefaults() {
        // Given - Create persona without privacy settings
        PersonaDto createRequest = PersonaDto.builder()
                .name("DefaultPrivacy")
                .type(Persona.PersonaType.WORK)
                .displayName("Default Privacy Profile")
                .privacySettings(null) // No privacy settings provided
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "DefaultPrivacy")).thenReturn(Optional.empty());
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
        
        // Verify actual default privacy settings are applied (based on PrivacySettings entity defaults)
        PersonaDto.PrivacySettingsDto privacySettings = result.getPrivacySettings();
        assertThat(privacySettings.isShowRealName()).isFalse(); // Default is false
        assertThat(privacySettings.isShowEmail()).isFalse(); // Default is false  
        assertThat(privacySettings.isShowActivity()).isTrue(); // Default is true
        assertThat(privacySettings.isAllowDirectMessages()).isTrue(); // Default is true
        assertThat(privacySettings.getVisibilityLevel()).isEqualTo("FRIENDS"); // Default is "FRIENDS"
        assertThat(privacySettings.isSearchable()).isTrue(); // Default is true
        assertThat(privacySettings.isShowOnlineStatus()).isTrue(); // Default is true
        assertThat(privacySettings.isShareFocusSessions()).isTrue(); // Default is true
        assertThat(privacySettings.isShareAchievements()).isTrue(); // Default is true

        verify(personaRepository).save(argThat(persona -> 
            persona.getPrivacySettings() != null));
    }

    @Test
    @DisplayName("Should update lastActiveAt timestamp when switching personas")
    void switchPersona_ShouldUpdateLastActiveAtTimestamp() {
        // Given
        Instant beforeSwitching = Instant.now().minusSeconds(60); // 1 minute ago
        personalPersona.setLastActiveAt(beforeSwitching);
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
        assertThat(result.getLastActiveAt()).isNotNull();
        
        // Verify lastActiveAt was updated to a recent time (within last 5 seconds)
        assertThat(result.getLastActiveAt()).isAfter(beforeSwitching);
        assertThat(result.getLastActiveAt()).isAfter(Instant.now().minusSeconds(5));
        
        verify(personaRepository).updateActivePersona(testUserId, targetPersonaId);
        verify(personaRepository).save(personalPersona);
        
        // Verify the entity was updated
        assertThat(personalPersona.isActive()).isTrue();
        assertThat(personalPersona.getLastActiveAt()).isAfter(beforeSwitching);
    }

    @Test
    @DisplayName("Should handle persona update with selective null field updates")
    void updatePersona_NullFieldHandling_ShouldUpdateOnlyNonNullFields() {
        // Given - Original persona with all fields set
        UUID personaId = workPersona.getId();
        workPersona.setDisplayName("Original Display");
        workPersona.setBio("Original Bio");
        workPersona.setStatusMessage("Original Status");
        workPersona.setAvatarUrl("https://original.com/avatar.jpg");
        workPersona.setThemePreference("dark");
        workPersona.setLanguagePreference("en-US");

        // Update request with mixed null and non-null values
        PersonaDto updateRequest = PersonaDto.builder()
                .displayName("Updated Display Name") // Non-null update
                .bio(null) // Null - should not change original
                .statusMessage("Updated Status") // Non-null update
                .avatarUrl(null) // Null - should not change original
                .themePreference(null) // Null - should not change original
                .languagePreference("es-ES") // Non-null update
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PersonaDto result = personaService.updatePersona(testUserId, personaId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        
        // Verify only non-null fields were updated
        assertThat(result.getDisplayName()).isEqualTo("Updated Display Name"); // Updated
        assertThat(result.getBio()).isEqualTo("Original Bio"); // Unchanged (was null in request)
        assertThat(result.getStatusMessage()).isEqualTo("Updated Status"); // Updated
        assertThat(result.getAvatarUrl()).isEqualTo("https://original.com/avatar.jpg"); // Unchanged (was null in request)
        assertThat(result.getThemePreference()).isEqualTo("dark"); // Unchanged (was null in request)
        assertThat(result.getLanguagePreference()).isEqualTo("es-ES"); // Updated

        // Verify entity state
        assertThat(workPersona.getDisplayName()).isEqualTo("Updated Display Name");
        assertThat(workPersona.getBio()).isEqualTo("Original Bio");
        assertThat(workPersona.getStatusMessage()).isEqualTo("Updated Status");
        assertThat(workPersona.getLanguagePreference()).isEqualTo("es-ES");

        verify(personaRepository).save(workPersona);
    }

    @Test
    @DisplayName("Should handle persona deletion cascade with active persona switching")
    void deletePersona_ActivePersonaCascade_ShouldSwitchToDefaultAndDelete() {
        // Given - Setup a scenario where we delete an active persona
        UUID activePersonaId = personalPersona.getId();
        personalPersona.setActive(true); // Make personal persona active
        workPersona.setDefault(true); // Work persona is default
        workPersona.setActive(false); // But not active
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(activePersonaId, testUser)).thenReturn(Optional.of(personalPersona));
        when(personaRepository.findByUserAndIsDefaultTrue(testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.findByIdAndUser(workPersona.getId(), testUser)).thenReturn(Optional.of(workPersona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - Delete the active persona
        personaService.deletePersona(testUserId, activePersonaId);

        // Then - Verify cascade behavior
        verify(personaRepository).findByUserAndIsDefaultTrue(testUser);
        verify(personaRepository).updateActivePersona(testUserId, workPersona.getId());
        verify(personaRepository).save(workPersona); // Default persona should be activated
        verify(personaRepository).delete(personalPersona); // Active persona should be deleted
        
        // Verify the default persona was activated
        assertThat(workPersona.isActive()).isTrue();
        assertThat(workPersona.getLastActiveAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create persona from template with specific type characteristics")
    void createPersonaFromTemplate_TypeSpecificCharacteristics_ShouldMatchTemplate() {
        // Test WORK template specifically (most comprehensive)
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "Work")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(1L);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When - Create WORK persona from template
        PersonaDto result = personaService.createPersonaFromTemplate(testUserId, Persona.PersonaType.WORK);

        // Then - Verify WORK template characteristics
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Work");
        assertThat(result.getType()).isEqualTo(Persona.PersonaType.WORK);
        assertThat(result.getDisplayName()).isEqualTo("Work Profile");
        assertThat(result.getBio()).contains("Professional work profile");
        assertThat(result.getThemePreference()).isEqualTo("light");
        
        // Verify WORK-specific privacy settings (more open for professional use)
        PersonaDto.PrivacySettingsDto privacy = result.getPrivacySettings();
        assertThat(privacy.isShowRealName()).isTrue(); // Work persona shows real name
        assertThat(privacy.isShowActivity()).isTrue();
        assertThat(privacy.isAllowDirectMessages()).isTrue();
        assertThat(privacy.getVisibilityLevel()).isEqualTo("PUBLIC"); // Work is public
        assertThat(privacy.isSearchable()).isTrue();
        assertThat(privacy.isShowOnlineStatus()).isTrue();
        assertThat(privacy.isShareFocusSessions()).isTrue();
        assertThat(privacy.isShareAchievements()).isTrue();

        verify(personaRepository).save(argThat(persona -> 
            persona.getType().equals(Persona.PersonaType.WORK) && 
            persona.getName().equals("Work")));
    }

    @Test 
    @DisplayName("Should create persona from PERSONAL template with privacy-focused settings")
    void createPersonaFromTemplate_PersonalType_ShouldHavePrivacyFocusedSettings() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(testUserId, "Personal")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(testUserId)).thenReturn(1L);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
            Persona persona = invocation.getArgument(0);
            persona.setId(UUID.randomUUID());
            persona.setCreatedAt(Instant.now());
            return persona;
        });

        // When - Create PERSONAL persona from template
        PersonaDto result = personaService.createPersonaFromTemplate(testUserId, Persona.PersonaType.PERSONAL);

        // Then - Verify PERSONAL template characteristics (privacy-focused)
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Personal");
        assertThat(result.getType()).isEqualTo(Persona.PersonaType.PERSONAL);
        assertThat(result.getThemePreference()).isEqualTo("system");
        
        // Verify PERSONAL-specific privacy settings (more restrictive)
        PersonaDto.PrivacySettingsDto privacy = result.getPrivacySettings();
        assertThat(privacy.isShowRealName()).isFalse(); // Personal persona hides real name
        assertThat(privacy.isShowActivity()).isFalse(); // Hide activity
        assertThat(privacy.isAllowDirectMessages()).isFalse(); // No direct messages
        assertThat(privacy.getVisibilityLevel()).isEqualTo("PRIVATE"); // Private visibility
        assertThat(privacy.isSearchable()).isFalse(); // Not searchable
        assertThat(privacy.isShowOnlineStatus()).isFalse(); // Hide online status
        assertThat(privacy.isShareFocusSessions()).isFalse(); // Don't share sessions
        assertThat(privacy.isShareAchievements()).isFalse(); // Don't share achievements
    }

    @Test
    @DisplayName("Should allow creating multiple personas without limit (current behavior)")
    void createPersona_MultiplePersonas_ShouldAllowUnlimitedCreation() {
        // Given - Simulate creating many personas (test current behavior)
        int maxPersonasToTest = 15; // Test reasonable number
        
        for (int i = 1; i <= maxPersonasToTest; i++) {
            String personaName = "Persona" + i;
            PersonaDto createRequest = PersonaDto.builder()
                    .name(personaName)
                    .type(Persona.PersonaType.CUSTOM)
                    .displayName("Test Persona " + i)
                    .build();

            // Reset mocks for each iteration
            reset(userRepository, personaRepository);
            
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(personaRepository.findByUserIdAndName(testUserId, personaName)).thenReturn(Optional.empty());
            when(personaRepository.countByUserId(testUserId)).thenReturn((long) (i - 1)); // Previous persona count
            when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> {
                Persona persona = invocation.getArgument(0);
                persona.setId(UUID.randomUUID());
                persona.setCreatedAt(Instant.now());
                return persona;
            });

            // When - Create persona
            PersonaDto result = personaService.createPersona(testUserId, createRequest);

            // Then - Verify creation successful
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(personaName);
            assertThat(result.getDisplayName()).isEqualTo("Test Persona " + i);
            
            // First persona should be default and active
            if (i == 1) {
                assertThat(result.isDefault()).isTrue();
                assertThat(result.isActive()).isTrue();
            } else {
                assertThat(result.isDefault()).isFalse();
                assertThat(result.isActive()).isFalse();
            }
        }

        // All personas should have been created successfully without limits
        // This test documents the current behavior - no persona limits enforced
    }

    @Test
    @DisplayName("Should maintain data integrity during multiple persona operations")
    void multiplePersonaOperations_DataIntegrity_ShouldMaintainConsistency() {
        // Given - Setup personas with different states
        UUID persona1Id = UUID.randomUUID();
        UUID persona2Id = UUID.randomUUID();
        UUID persona3Id = UUID.randomUUID();
        
        Persona persona1 = Persona.builder()
                .id(persona1Id)
                .user(testUser)
                .name("Persona1")
                .type(Persona.PersonaType.WORK)
                .displayName("Work Persona")
                .isDefault(true)
                .isActive(false)
                .customAttributes(new HashMap<>())
                .privacySettings(new Persona.PrivacySettings())
                .createdAt(Instant.now())
                .build();

        Persona persona2 = Persona.builder()
                .id(persona2Id)
                .user(testUser)
                .name("Persona2")
                .type(Persona.PersonaType.PERSONAL)
                .displayName("Personal Persona")
                .isDefault(false)
                .isActive(true)
                .customAttributes(new HashMap<>())
                .privacySettings(new Persona.PrivacySettings())
                .createdAt(Instant.now())
                .build();

        Persona persona3 = Persona.builder()
                .id(persona3Id)
                .user(testUser)
                .name("Persona3")
                .type(Persona.PersonaType.GAMING)
                .displayName("Gaming Persona")
                .isDefault(false)
                .isActive(false)
                .customAttributes(new HashMap<>())
                .privacySettings(new Persona.PrivacySettings())
                .createdAt(Instant.now())
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Test 1: Switch from persona2 to persona1 (active to default)
        when(personaRepository.findByIdAndUser(persona1Id, testUser)).thenReturn(Optional.of(persona1));
        
        PersonaDto result1 = personaService.switchPersona(testUserId, persona1Id);
        
        // Verify switch integrity
        assertThat(result1.getId()).isEqualTo(persona1Id);
        assertThat(result1.isActive()).isTrue();
        assertThat(result1.isDefault()).isTrue(); // Should remain default
        
        // Test 2: Update persona while it's active
        PersonaDto updateRequest = PersonaDto.builder()
                .displayName("Updated Active Persona")
                .statusMessage("Currently Active")
                .build();
        
        PersonaDto result2 = personaService.updatePersona(testUserId, persona1Id, updateRequest);
        
        // Verify update integrity while active
        assertThat(result2.getDisplayName()).isEqualTo("Updated Active Persona");
        assertThat(result2.getStatusMessage()).isEqualTo("Currently Active");
        assertThat(result2.isActive()).isTrue(); // Should remain active
        assertThat(result2.isDefault()).isTrue(); // Should remain default

        // Test 3: Set different persona as default (persona3)
        when(personaRepository.findByIdAndUser(persona3Id, testUser)).thenReturn(Optional.of(persona3));
        
        PersonaDto result3 = personaService.setDefaultPersona(testUserId, persona3Id);
        
        // Verify default change integrity
        assertThat(result3.getId()).isEqualTo(persona3Id);
        assertThat(result3.isDefault()).isTrue();
        
        // Verify repository interactions maintain integrity
        verify(personaRepository, atLeast(1)).updateActivePersona(testUserId, persona1Id);
        verify(personaRepository, atLeast(1)).clearDefaultPersonaExcept(testUserId, persona3Id);
        verify(personaRepository, atLeastOnce()).save(any(Persona.class));
        
        // Verify entities maintain consistent state
        assertThat(persona1.isActive()).isTrue(); // Last active persona
        assertThat(persona3.isDefault()).isTrue(); // Last default persona
    }
}