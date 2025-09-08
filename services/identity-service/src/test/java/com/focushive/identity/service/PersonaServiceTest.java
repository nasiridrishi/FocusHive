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
}