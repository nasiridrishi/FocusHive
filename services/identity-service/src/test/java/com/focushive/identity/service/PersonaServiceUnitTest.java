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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for PersonaService without Spring context dependencies.
 * Tests all critical functionality including CRUD operations, validation, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonaService Unit Tests")
class PersonaServiceUnitTest {

    @Mock
    private PersonaRepository personaRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private PersonaService personaService;
    
    private UUID userId;
    private UUID personaId;
    private User testUser;
    private Persona testPersona;
    private PersonaDto testPersonaDto;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        personaId = UUID.randomUUID();
        
        // Create test user
        testUser = User.builder()
                .id(userId)
                .username("testuser")
                .email("test@example.com")
                .build();
        
        // Create test persona
        testPersona = Persona.builder()
                .id(personaId)
                .user(testUser)
                .name("Work")
                .type(Persona.PersonaType.WORK)
                .displayName("Work Profile")
                .bio("Professional work profile")
                .isDefault(false)
                .isActive(false)
                .themePreference("light")
                .customAttributes(new HashMap<>())
                .privacySettings(new Persona.PrivacySettings())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Create test DTO
        testPersonaDto = PersonaDto.builder()
                .name("Work")
                .type(Persona.PersonaType.WORK)
                .displayName("Work Profile")
                .bio("Professional work profile")
                .themePreference("light")
                .customAttributes(new HashMap<>())
                .privacySettings(PersonaDto.PrivacySettingsDto.builder()
                        .showRealName(true)
                        .allowDirectMessages(true)
                        .visibilityLevel("PUBLIC")
                        .build())
                .build();
    }
    
    @Test
    @DisplayName("Create persona - Success case")
    void createPersona_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(userId, "Work")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(userId)).thenReturn(0L);
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        
        // When
        PersonaDto result = personaService.createPersona(userId, testPersonaDto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Work");
        assertThat(result.getDisplayName()).isEqualTo("Work Profile");
        
        // Verify interactions
        verify(userRepository).findById(userId);
        verify(personaRepository).findByUserIdAndName(userId, "Work");
        verify(personaRepository).countByUserId(userId);
        
        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        
        Persona savedPersona = personaCaptor.getValue();
        assertThat(savedPersona.getName()).isEqualTo("Work");
        assertThat(savedPersona.isDefault()).isTrue(); // First persona should be default
        assertThat(savedPersona.isActive()).isTrue(); // First persona should be active
    }
    
    @Test
    @DisplayName("Create persona - User not found")
    void createPersona_UserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> personaService.createPersona(userId, testPersonaDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id");
        
        verify(userRepository).findById(userId);
        verifyNoInteractions(personaRepository);
    }
    
    @Test
    @DisplayName("Create persona - Duplicate name")
    void createPersona_DuplicateName() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(userId, "Work")).thenReturn(Optional.of(testPersona));
        
        // When & Then
        assertThatThrownBy(() -> personaService.createPersona(userId, testPersonaDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Persona with name 'Work' already exists");
        
        verify(userRepository).findById(userId);
        verify(personaRepository).findByUserIdAndName(userId, "Work");
        verify(personaRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Create persona - Not first persona")
    void createPersona_NotFirstPersona() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(userId, "Work")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(userId)).thenReturn(2L); // User already has personas
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        
        // When
        PersonaDto result = personaService.createPersona(userId, testPersonaDto);
        
        // Then
        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        
        Persona savedPersona = personaCaptor.getValue();
        assertThat(savedPersona.isDefault()).isFalse(); // Not first persona, should not be default
        assertThat(savedPersona.isActive()).isFalse(); // Not first persona, should not be active
    }
    
    @Test
    @DisplayName("Get user personas - Success")
    void getUserPersonas_Success() {
        // Given
        List<Persona> personas = Arrays.asList(testPersona);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdOrderByPriority(userId)).thenReturn(personas);
        
        // When
        List<PersonaDto> result = personaService.getUserPersonas(userId);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Work");
        
        verify(userRepository).findById(userId);
        verify(personaRepository).findByUserIdOrderByPriority(userId);
    }
    
    @Test
    @DisplayName("Get persona - Success")
    void getPersona_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        
        // When
        PersonaDto result = personaService.getPersona(userId, personaId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Work");
        
        verify(userRepository).findById(userId);
        verify(personaRepository).findByIdAndUser(personaId, testUser);
    }
    
    @Test
    @DisplayName("Get persona - Persona not found")
    void getPersona_PersonaNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> personaService.getPersona(userId, personaId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Persona not found with id");
        
        verify(userRepository).findById(userId);
        verify(personaRepository).findByIdAndUser(personaId, testUser);
    }
    
    @Test
    @DisplayName("Update persona - Success")
    void updatePersona_Success() {
        // Given
        PersonaDto updateDto = PersonaDto.builder()
                .displayName("Updated Work Profile")
                .bio("Updated bio")
                .themePreference("dark")
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        
        // When
        PersonaDto result = personaService.updatePersona(userId, personaId, updateDto);
        
        // Then
        verify(userRepository).findById(userId);
        verify(personaRepository).findByIdAndUser(personaId, testUser);
        verify(personaRepository).save(testPersona);
        
        // Verify persona was updated
        assertThat(testPersona.getDisplayName()).isEqualTo("Updated Work Profile");
        assertThat(testPersona.getBio()).isEqualTo("Updated bio");
        assertThat(testPersona.getThemePreference()).isEqualTo("dark");
    }
    
    @Test
    @DisplayName("Delete persona - Success")
    void deletePersona_Success() {
        // Given
        testPersona.setDefault(false); // Not default persona
        testPersona.setActive(false);  // Not active persona
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        
        // When
        personaService.deletePersona(userId, personaId);
        
        // Then
        verify(userRepository).findById(userId);
        verify(personaRepository).findByIdAndUser(personaId, testUser);
        verify(personaRepository).delete(testPersona);
    }
    
    @Test
    @DisplayName("Delete persona - Cannot delete default persona")
    void deletePersona_CannotDeleteDefault() {
        // Given
        testPersona.setDefault(true); // This is default persona
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        
        // When & Then
        assertThatThrownBy(() -> personaService.deletePersona(userId, personaId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete default persona");
        
        verify(personaRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("Delete persona - Switch from active persona")
    void deletePersona_SwitchFromActive() {
        // Given
        UUID defaultPersonaId = UUID.randomUUID();
        Persona defaultPersona = Persona.builder()
                .id(defaultPersonaId)
                .user(testUser)
                .name("Default")
                .isDefault(true)
                .build();
        
        testPersona.setDefault(false);
        testPersona.setActive(true); // This is active persona
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        when(personaRepository.findByUserAndIsDefaultTrue(testUser)).thenReturn(Optional.of(defaultPersona));
        when(personaRepository.findByIdAndUser(defaultPersonaId, testUser)).thenReturn(Optional.of(defaultPersona));
        when(personaRepository.save(any())).thenReturn(defaultPersona);
        
        // When
        personaService.deletePersona(userId, personaId);
        
        // Then
        verify(personaRepository).updateActivePersona(userId, defaultPersonaId);
        verify(personaRepository).delete(testPersona);
    }
    
    @Test
    @DisplayName("Switch persona - Success")
    void switchPersona_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(testPersona)).thenReturn(testPersona);
        
        // When
        PersonaDto result = personaService.switchPersona(userId, personaId);
        
        // Then
        assertThat(result).isNotNull();
        verify(personaRepository).updateActivePersona(userId, personaId);
        verify(personaRepository).save(testPersona);
        
        assertThat(testPersona.isActive()).isTrue();
        assertThat(testPersona.getLastActiveAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Get active persona - Success")
    void getActivePersona_Success() {
        // Given
        testPersona.setActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.of(testPersona));
        
        // When
        Optional<PersonaDto> result = personaService.getActivePersona(userId);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Work");
        
        verify(userRepository).findById(userId);
        verify(personaRepository).findByUserIdAndIsActiveTrue(userId);
    }
    
    @Test
    @DisplayName("Get active persona - No active persona")
    void getActivePersona_NoActivePersona() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());
        
        // When
        Optional<PersonaDto> result = personaService.getActivePersona(userId);
        
        // Then
        assertThat(result).isEmpty();
        
        verify(userRepository).findById(userId);
        verify(personaRepository).findByUserIdAndIsActiveTrue(userId);
    }
    
    @Test
    @DisplayName("Set default persona - Success")
    void setDefaultPersona_Success() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(testPersona)).thenReturn(testPersona);
        
        // When
        PersonaDto result = personaService.setDefaultPersona(userId, personaId);
        
        // Then
        assertThat(result).isNotNull();
        verify(personaRepository).clearDefaultPersonaExcept(userId, personaId);
        verify(personaRepository).save(testPersona);
        
        assertThat(testPersona.isDefault()).isTrue();
    }
    
    @Test
    @DisplayName("Create persona from template - Work template")
    void createPersonaFromTemplate_WorkTemplate() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(eq(userId), any())).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(userId)).thenReturn(0L);
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        
        // When
        PersonaDto result = personaService.createPersonaFromTemplate(userId, Persona.PersonaType.WORK);
        
        // Then
        assertThat(result).isNotNull();
        
        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        
        Persona savedPersona = personaCaptor.getValue();
        assertThat(savedPersona.getName()).isEqualTo("Work");
        assertThat(savedPersona.getType()).isEqualTo(Persona.PersonaType.WORK);
        assertThat(savedPersona.getDisplayName()).isEqualTo("Work Profile");
    }
    
    @Test
    @DisplayName("Get persona templates - Returns all templates")
    void getPersonaTemplates_ReturnsAllTemplates() {
        // When
        List<PersonaDto> templates = personaService.getPersonaTemplates();
        
        // Then
        assertThat(templates).hasSize(4);
        assertThat(templates).extracting(PersonaDto::getType)
                .containsExactly(
                        Persona.PersonaType.WORK,
                        Persona.PersonaType.PERSONAL,
                        Persona.PersonaType.GAMING,
                        Persona.PersonaType.STUDY
                );
        
        // Verify templates have correct properties
        PersonaDto workTemplate = templates.stream()
                .filter(t -> t.getType() == Persona.PersonaType.WORK)
                .findFirst().orElseThrow();
        
        assertThat(workTemplate.getName()).isEqualTo("Work");
        assertThat(workTemplate.getDisplayName()).isEqualTo("Work Profile");
        assertThat(workTemplate.getPrivacySettings().getVisibilityLevel()).isEqualTo("PUBLIC");
        assertThat(workTemplate.getPrivacySettings().isShowRealName()).isTrue();
    }
    
    @Test
    @DisplayName("Update persona with privacy settings - Success")
    void updatePersona_WithPrivacySettings_Success() {
        // Given
        PersonaDto.PrivacySettingsDto privacyDto = PersonaDto.PrivacySettingsDto.builder()
                .showRealName(false)
                .allowDirectMessages(true)
                .visibilityLevel("FRIENDS")
                .searchable(true)
                .build();
        
        PersonaDto updateDto = PersonaDto.builder()
                .privacySettings(privacyDto)
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        
        // When
        personaService.updatePersona(userId, personaId, updateDto);
        
        // Then
        verify(personaRepository).save(testPersona);
        
        Persona.PrivacySettings privacy = testPersona.getPrivacySettings();
        assertThat(privacy.isShowRealName()).isFalse();
        assertThat(privacy.isAllowDirectMessages()).isTrue();
        assertThat(privacy.getVisibilityLevel()).isEqualTo("FRIENDS");
        assertThat(privacy.isSearchable()).isTrue();
    }
    
    @Test
    @DisplayName("Create persona with null privacy settings - Uses defaults")
    void createPersona_WithNullPrivacySettings_UsesDefaults() {
        // Given
        PersonaDto dtoWithoutPrivacy = PersonaDto.builder()
                .name("Test")
                .type(Persona.PersonaType.PERSONAL)
                .displayName("Test Profile")
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByUserIdAndName(userId, "Test")).thenReturn(Optional.empty());
        when(personaRepository.countByUserId(userId)).thenReturn(0L);
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        
        // When
        personaService.createPersona(userId, dtoWithoutPrivacy);
        
        // Then
        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(personaCaptor.capture());
        
        Persona savedPersona = personaCaptor.getValue();
        assertThat(savedPersona.getPrivacySettings()).isNotNull();
    }
    
    @Test
    @DisplayName("Update persona with null existing privacy settings - Creates new")
    void updatePersona_WithNullExistingPrivacySettings_CreatesNew() {
        // Given
        testPersona.setPrivacySettings(null);
        
        PersonaDto.PrivacySettingsDto privacyDto = PersonaDto.PrivacySettingsDto.builder()
                .showRealName(true)
                .build();
        
        PersonaDto updateDto = PersonaDto.builder()
                .privacySettings(privacyDto)
                .build();
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(personaRepository.findByIdAndUser(personaId, testUser)).thenReturn(Optional.of(testPersona));
        when(personaRepository.save(any(Persona.class))).thenReturn(testPersona);
        
        // When
        personaService.updatePersona(userId, personaId, updateDto);
        
        // Then
        assertThat(testPersona.getPrivacySettings()).isNotNull();
        assertThat(testPersona.getPrivacySettings().isShowRealName()).isTrue();
    }
}