package com.focushive.identity.service;

import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.Persona.PersonaType;
import com.focushive.identity.entity.User;
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
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for OptimizedPersonaService.
 * Tests performance-optimized persona loading with N+1 query prevention.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OptimizedPersonaService Tests")
class OptimizedPersonaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OptimizedPersonaService optimizedPersonaService;

    private UUID testUserId;
    private UUID testPersonaId;
    private User testUser;
    private Persona testPersona;
    private List<Persona> testPersonas;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPersonaId = UUID.randomUUID();

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .firstName("Test")
                .lastName("User")
                .emailVerified(true)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .createdAt(Instant.now())
                .personas(new ArrayList<>())
                .build();

        testPersona = Persona.builder()
                .id(testPersonaId)
                .name("work-persona")
                .type(PersonaType.WORK)
                .displayName("Work Me")
                .bio("Professional persona for work")
                .statusMessage("At work")
                .avatarUrl("https://example.com/avatar.jpg")
                .isDefault(true)
                .isActive(true)
                .user(testUser)
                .customAttributes(Map.of("department", "engineering", "role", "developer"))
                .themePreference("dark")
                .languagePreference("en")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastActiveAt(Instant.now())
                .build();

        testPersonas = List.of(testPersona);
        testUsers = List.of(testUser);
    }

    @Test
    @DisplayName("Should get user personas with optimized query")
    void getUserPersonasOptimized_ValidUserId_ShouldReturnPersonaDtos() {
        // Given
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(testPersonas);

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).hasSize(1);
        PersonaDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(testPersonaId);
        assertThat(dto.getName()).isEqualTo("work-persona");
        assertThat(dto.getType()).isEqualTo(PersonaType.WORK);
        assertThat(dto.getDisplayName()).isEqualTo("Work Me");
        assertThat(dto.getBio()).isEqualTo("Professional persona for work");
        assertThat(dto.getStatusMessage()).isEqualTo("At work");
        assertThat(dto.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(dto.isDefault()).isTrue();
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getCustomAttributes()).containsEntry("department", "engineering");
        assertThat(dto.getThemePreference()).isEqualTo("dark");
        assertThat(dto.getLanguagePreference()).isEqualTo("en");
        
        verify(userRepository).existsById(testUserId);
        verify(personaRepository).findByUserIdOrderByPriorityWithAttributes(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when user not found in getUserPersonasOptimized")
    void getUserPersonasOptimized_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.existsById(testUserId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> optimizedPersonaService.getUserPersonasOptimized(testUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with id: " + testUserId);

        verify(userRepository).existsById(testUserId);
        verify(personaRepository, never()).findByUserIdOrderByPriorityWithAttributes(any());
    }

    @Test
    @DisplayName("Should return empty list when user has no personas")
    void getUserPersonasOptimized_NoPersonas_ShouldReturnEmptyList() {
        // Given
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(Collections.emptyList());

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).existsById(testUserId);
        verify(personaRepository).findByUserIdOrderByPriorityWithAttributes(testUserId);
    }

    @Test
    @DisplayName("Should handle null custom attributes in getUserPersonasOptimized")
    void getUserPersonasOptimized_NullCustomAttributes_ShouldHandleGracefully() {
        // Given
        testPersona.setCustomAttributes(null);
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(testPersonas);

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomAttributes()).isNull();
    }

    @Test
    @DisplayName("Should get active persona with optimized query")
    void getActivePersonaOptimized_ValidUserId_ShouldReturnActivePersona() {
        // Given
        when(personaRepository.findByUserIdAndIsActiveTrueWithAttributes(testUserId)).thenReturn(Optional.of(testPersona));

        // When
        Optional<PersonaDto> result = optimizedPersonaService.getActivePersonaOptimized(testUserId);

        // Then
        assertThat(result).isPresent();
        PersonaDto dto = result.get();
        assertThat(dto.getId()).isEqualTo(testPersonaId);
        assertThat(dto.getName()).isEqualTo("work-persona");
        assertThat(dto.isActive()).isTrue();
        
        verify(personaRepository).findByUserIdAndIsActiveTrueWithAttributes(testUserId);
    }

    @Test
    @DisplayName("Should return empty when no active persona found")
    void getActivePersonaOptimized_NoActivePersona_ShouldReturnEmpty() {
        // Given
        when(personaRepository.findByUserIdAndIsActiveTrueWithAttributes(testUserId)).thenReturn(Optional.empty());

        // When
        Optional<PersonaDto> result = optimizedPersonaService.getActivePersonaOptimized(testUserId);

        // Then
        assertThat(result).isEmpty();
        verify(personaRepository).findByUserIdAndIsActiveTrueWithAttributes(testUserId);
    }

    @Test
    @DisplayName("Should get all users with personas using EntityGraph")
    void getAllUsersWithPersonas_ShouldReturnUsersWithPersonas() {
        // Given
        when(userRepository.findAll()).thenReturn(testUsers);

        // When
        List<User> result = optimizedPersonaService.getAllUsersWithPersonas();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(testUserId);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void getAllUsersWithPersonas_NoUsers_ShouldReturnEmptyList() {
        // Given
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<User> result = optimizedPersonaService.getAllUsersWithPersonas();

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should get users with personas in batch")
    void getUsersWithPersonasBatch_ValidUserIds_ShouldReturnUsers() {
        // Given
        List<UUID> userIds = Arrays.asList(testUserId, UUID.randomUUID());
        when(userRepository.findUsersWithPersonasAndAttributes(userIds)).thenReturn(testUsers);

        // When
        List<User> result = optimizedPersonaService.getUsersWithPersonasBatch(userIds);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(testUserId);
        
        verify(userRepository).findUsersWithPersonasAndAttributes(userIds);
    }

    @Test
    @DisplayName("Should handle empty user IDs list in batch fetch")
    void getUsersWithPersonasBatch_EmptyUserIds_ShouldReturnEmptyList() {
        // Given
        List<UUID> emptyUserIds = Collections.emptyList();
        when(userRepository.findUsersWithPersonasAndAttributes(emptyUserIds)).thenReturn(Collections.emptyList());

        // When
        List<User> result = optimizedPersonaService.getUsersWithPersonasBatch(emptyUserIds);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findUsersWithPersonasAndAttributes(emptyUserIds);
    }

    @Test
    @DisplayName("Should handle null user IDs list in batch fetch")
    void getUsersWithPersonasBatch_NullUserIds_ShouldHandleGracefully() {
        // When & Then
        assertThatThrownBy(() -> optimizedPersonaService.getUsersWithPersonasBatch(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should find user with personas using EntityGraph")
    void findUserWithPersonas_ValidUserId_ShouldReturnUser() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = optimizedPersonaService.findUserWithPersonas(testUserId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testUserId);
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("Should return empty when user not found in findUserWithPersonas")
    void findUserWithPersonas_UserNotFound_ShouldReturnEmpty() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = optimizedPersonaService.findUserWithPersonas(testUserId);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(testUserId);
    }

    @Test
    @DisplayName("Should find user with personas and attributes using batch method")
    void findUserWithPersonasAndAttributes_ValidUserId_ShouldReturnUser() {
        // Given
        when(userRepository.findUsersWithPersonasAndAttributes(List.of(testUserId))).thenReturn(testUsers);

        // When
        Optional<User> result = optimizedPersonaService.findUserWithPersonasAndAttributes(testUserId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testUserId);
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        
        verify(userRepository).findUsersWithPersonasAndAttributes(List.of(testUserId));
    }

    @Test
    @DisplayName("Should return empty when user not found in findUserWithPersonasAndAttributes")
    void findUserWithPersonasAndAttributes_UserNotFound_ShouldReturnEmpty() {
        // Given
        when(userRepository.findUsersWithPersonasAndAttributes(List.of(testUserId))).thenReturn(Collections.emptyList());

        // When
        Optional<User> result = optimizedPersonaService.findUserWithPersonasAndAttributes(testUserId);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findUsersWithPersonasAndAttributes(List.of(testUserId));
    }

    @Test
    @DisplayName("Should handle multiple personas for user")
    void getUserPersonasOptimized_MultiplePersonas_ShouldReturnAllPersonas() {
        // Given
        UUID persona2Id = UUID.randomUUID();
        Persona persona2 = Persona.builder()
                .id(persona2Id)
                .name("personal-persona")
                .type(PersonaType.PERSONAL)
                .displayName("Personal Me")
                .isDefault(false)
                .isActive(false)
                .user(testUser)
                .customAttributes(Map.of("hobby", "gaming"))
                .themePreference("light")
                .languagePreference("es")
                .createdAt(Instant.now())
                .build();
        
        List<Persona> multiplePersonas = Arrays.asList(testPersona, persona2);
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(multiplePersonas);

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).hasSize(2);
        
        PersonaDto dto1 = result.get(0);
        assertThat(dto1.getId()).isEqualTo(testPersonaId);
        assertThat(dto1.getName()).isEqualTo("work-persona");
        assertThat(dto1.isActive()).isTrue();
        
        PersonaDto dto2 = result.get(1);
        assertThat(dto2.getId()).isEqualTo(persona2Id);
        assertThat(dto2.getName()).isEqualTo("personal-persona");
        assertThat(dto2.isActive()).isFalse();
        assertThat(dto2.getCustomAttributes()).containsEntry("hobby", "gaming");
    }

    @Test
    @DisplayName("Should handle persona with null optional fields")
    void getUserPersonasOptimized_PersonaWithNullFields_ShouldHandleGracefully() {
        // Given
        testPersona.setBio(null);
        testPersona.setStatusMessage(null);
        testPersona.setAvatarUrl(null);
        testPersona.setThemePreference(null);
        testPersona.setLanguagePreference(null);
        testPersona.setLastActiveAt(null);
        
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(testPersonas);

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).hasSize(1);
        PersonaDto dto = result.get(0);
        assertThat(dto.getBio()).isNull();
        assertThat(dto.getStatusMessage()).isNull();
        assertThat(dto.getAvatarUrl()).isNull();
        assertThat(dto.getThemePreference()).isNull();
        assertThat(dto.getLanguagePreference()).isNull();
        assertThat(dto.getLastActiveAt()).isNull();
    }

    @Test
    @DisplayName("Should handle empty custom attributes map")
    void getUserPersonasOptimized_EmptyCustomAttributes_ShouldHandleGracefully() {
        // Given
        testPersona.setCustomAttributes(Collections.emptyMap());
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(testPersonas);

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Should preserve all persona timestamps in DTO conversion")
    void getUserPersonasOptimized_AllTimestamps_ShouldBePreserved() {
        // Given
        Instant now = Instant.now();
        Instant earlier = now.minusSeconds(3600);
        testPersona.setCreatedAt(earlier);
        testPersona.setUpdatedAt(now);
        testPersona.setLastActiveAt(now.minusSeconds(1800));
        
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(testPersonas);

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).hasSize(1);
        PersonaDto dto = result.get(0);
        assertThat(dto.getCreatedAt()).isEqualTo(earlier);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
        assertThat(dto.getLastActiveAt()).isEqualTo(now.minusSeconds(1800));
    }

    @Test
    @DisplayName("Should handle large number of personas efficiently")
    void getUserPersonasOptimized_LargePersonaList_ShouldHandleEfficiently() {
        // Given
        List<Persona> largePersonaList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Persona persona = Persona.builder()
                    .id(UUID.randomUUID())
                    .name("persona-" + i)
                    .type(PersonaType.CUSTOM)
                    .displayName("Persona " + i)
                    .isDefault(i == 0)
                    .isActive(i < 5)
                    .user(testUser)
                    .customAttributes(Map.of("index", String.valueOf(i)))
                    .createdAt(Instant.now())
                    .build();
            largePersonaList.add(persona);
        }
        
        when(userRepository.existsById(testUserId)).thenReturn(true);
        when(personaRepository.findByUserIdOrderByPriorityWithAttributes(testUserId)).thenReturn(largePersonaList);

        // When
        List<PersonaDto> result = optimizedPersonaService.getUserPersonasOptimized(testUserId);

        // Then
        assertThat(result).hasSize(100);
        assertThat(result.get(0).isDefault()).isTrue();
        assertThat(result.get(0).isActive()).isTrue();
        assertThat(result.get(99).isDefault()).isFalse();
        assertThat(result.get(99).isActive()).isFalse();
    }
}