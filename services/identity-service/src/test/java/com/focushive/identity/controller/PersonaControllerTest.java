package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.service.PersonaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PersonaController.
 * Tests all persona management endpoints with various scenarios.
 */
@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    @Mock
    private PersonaService personaService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PersonaController personaController;

    private PersonaDto testPersonaDto;
    private UUID userId;
    private UUID personaId;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        personaId = UUID.randomUUID();

        // Reset mocks before each test
        reset(authentication, personaService);

        testPersonaDto = new PersonaDto();
        testPersonaDto.setId(personaId);
        testPersonaDto.setName("Work Persona");
        testPersonaDto.setType(Persona.PersonaType.WORK);
        testPersonaDto.setBio("Professional work persona");
        testPersonaDto.setActive(true);
        testPersonaDto.setDefault(false);
        testPersonaDto.setDisplayName("Professional");
        testPersonaDto.setAvatarUrl("https://example.com/avatar.jpg");
        testPersonaDto.setStatusMessage("Working hard");
        testPersonaDto.setThemePreference("dark");
        testPersonaDto.setLanguagePreference("en");
        
        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("workspace", "main");
        customAttributes.put("priority", "high");
        testPersonaDto.setCustomAttributes(customAttributes);
    }

    // ===== CREATE PERSONA TESTS =====

    @Test
    @DisplayName("POST /personas - Should create new persona successfully")
    void testCreatePersona_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        when(personaService.createPersona(eq(userId), any(PersonaDto.class)))
                .thenReturn(testPersonaDto);

        ResponseEntity<PersonaDto> response = personaController.createPersona(testPersonaDto, authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(personaId);
        assertThat(response.getBody().getName()).isEqualTo("Work Persona");
        assertThat(response.getBody().getType()).isEqualTo(Persona.PersonaType.WORK);
        assertThat(response.getBody().isActive()).isTrue();

        verify(personaService, times(1)).createPersona(eq(userId), any(PersonaDto.class));
    }

    @Test
    @DisplayName("POST /personas - Should fail without authentication")
    void testCreatePersona_Unauthenticated() {
        assertThatThrownBy(() -> personaController.createPersona(testPersonaDto, null))
                .isInstanceOf(RuntimeException.class);

        verify(personaService, never()).createPersona(any(), any());
    }

    @Test
    @DisplayName("POST /personas - Should fail with invalid authentication")
    void testCreatePersona_InvalidAuthentication() {
        when(authentication.getName()).thenReturn("invalid-uuid");

        assertThatThrownBy(() -> personaController.createPersona(testPersonaDto, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid user authentication");

        verify(personaService, never()).createPersona(any(), any());
    }

    // ===== GET USER PERSONAS TESTS =====

    @Test
    @DisplayName("GET /personas - Should get all user personas")
    void testGetUserPersonas_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        List<PersonaDto> personas = Arrays.asList(testPersonaDto, createSecondPersona());
        when(personaService.getUserPersonas(eq(userId)))
                .thenReturn(personas);

        ResponseEntity<List<PersonaDto>> response = personaController.getUserPersonas(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Work Persona");
        assertThat(response.getBody().get(1).getName()).isEqualTo("Personal Persona");

        verify(personaService, times(1)).getUserPersonas(eq(userId));
    }

    @Test
    @DisplayName("GET /personas - Should return empty list when no personas")
    void testGetUserPersonas_EmptyList() {
        when(authentication.getName()).thenReturn(userId.toString());
        when(personaService.getUserPersonas(eq(userId)))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<PersonaDto>> response = personaController.getUserPersonas(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();

        verify(personaService, times(1)).getUserPersonas(eq(userId));
    }

    // ===== GET SPECIFIC PERSONA TESTS =====

    @Test
    @DisplayName("GET /personas/{id} - Should get specific persona")
    void testGetPersona_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        when(personaService.getPersona(eq(userId), eq(personaId)))
                .thenReturn(testPersonaDto);

        ResponseEntity<PersonaDto> response = personaController.getPersona(personaId, authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(personaId);
        assertThat(response.getBody().getName()).isEqualTo("Work Persona");

        verify(personaService, times(1)).getPersona(eq(userId), eq(personaId));
    }

    // ===== UPDATE PERSONA TESTS =====

    @Test
    @DisplayName("PUT /personas/{id} - Should update persona successfully")
    void testUpdatePersona_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        PersonaDto updatedPersona = new PersonaDto();
        updatedPersona.setId(personaId);
        updatedPersona.setName("Updated Work Persona");
        updatedPersona.setType(Persona.PersonaType.WORK);
        updatedPersona.setBio("Updated bio");

        when(personaService.updatePersona(eq(userId), eq(personaId), any(PersonaDto.class)))
                .thenReturn(updatedPersona);

        ResponseEntity<PersonaDto> response = personaController.updatePersona(personaId, updatedPersona, authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Updated Work Persona");
        assertThat(response.getBody().getBio()).isEqualTo("Updated bio");

        verify(personaService, times(1)).updatePersona(eq(userId), eq(personaId), any(PersonaDto.class));
    }

    // ===== DELETE PERSONA TESTS =====

    @Test
    @DisplayName("DELETE /personas/{id} - Should delete persona successfully")
    void testDeletePersona_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        doNothing().when(personaService).deletePersona(eq(userId), eq(personaId));

        ResponseEntity<Void> response = personaController.deletePersona(personaId, authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(204);

        verify(personaService, times(1)).deletePersona(eq(userId), eq(personaId));
    }

    @Test
    @DisplayName("DELETE /personas/{id} - Should fail when deleting default persona")
    void testDeletePersona_DefaultPersona() {
        when(authentication.getName()).thenReturn(userId.toString());
        doThrow(new IllegalStateException("Cannot delete default persona"))
                .when(personaService).deletePersona(eq(userId), eq(personaId));

        assertThatThrownBy(() -> personaController.deletePersona(personaId, authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete default persona");

        verify(personaService, times(1)).deletePersona(eq(userId), eq(personaId));
    }

    // ===== SWITCH PERSONA TESTS =====

    @Test
    @DisplayName("POST /personas/{id}/switch - Should switch persona successfully")
    void testSwitchPersona_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        testPersonaDto.setActive(true);
        when(personaService.switchPersona(eq(userId), eq(personaId)))
                .thenReturn(testPersonaDto);

        ResponseEntity<PersonaDto> response = personaController.switchPersona(personaId, authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(personaId);
        assertThat(response.getBody().isActive()).isTrue();

        verify(personaService, times(1)).switchPersona(eq(userId), eq(personaId));
    }

    // ===== GET ACTIVE PERSONA TESTS =====

    @Test
    @DisplayName("GET /personas/active - Should get active persona")
    void testGetActivePersona_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        when(personaService.getActivePersona(eq(userId)))
                .thenReturn(Optional.of(testPersonaDto));

        ResponseEntity<PersonaDto> response = personaController.getActivePersona(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(personaId);
        assertThat(response.getBody().isActive()).isTrue();

        verify(personaService, times(1)).getActivePersona(eq(userId));
    }

    @Test
    @DisplayName("GET /personas/active - Should return 204 when no active persona")
    void testGetActivePersona_NoContent() {
        when(authentication.getName()).thenReturn(userId.toString());
        when(personaService.getActivePersona(eq(userId)))
                .thenReturn(Optional.empty());

        ResponseEntity<PersonaDto> response = personaController.getActivePersona(authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(204);
        assertThat(response.getBody()).isNull();

        verify(personaService, times(1)).getActivePersona(eq(userId));
    }

    // ===== SET DEFAULT PERSONA TESTS =====

    @Test
    @DisplayName("POST /personas/{id}/default - Should set default persona")
    void testSetDefaultPersona_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        testPersonaDto.setDefault(true);
        when(personaService.setDefaultPersona(eq(userId), eq(personaId)))
                .thenReturn(testPersonaDto);

        ResponseEntity<PersonaDto> response = personaController.setDefaultPersona(personaId, authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(personaId);
        assertThat(response.getBody().isDefault()).isTrue();

        verify(personaService, times(1)).setDefaultPersona(eq(userId), eq(personaId));
    }

    // ===== CREATE FROM TEMPLATE TESTS =====

    @Test
    @DisplayName("POST /personas/templates/{type} - Should create from template")
    void testCreateFromTemplate_Success() {
        when(authentication.getName()).thenReturn(userId.toString());
        when(personaService.createPersonaFromTemplate(eq(userId), eq(Persona.PersonaType.WORK)))
                .thenReturn(testPersonaDto);

        ResponseEntity<PersonaDto> response = personaController.createPersonaFromTemplate(Persona.PersonaType.WORK, authentication);

        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(Persona.PersonaType.WORK);

        verify(personaService, times(1)).createPersonaFromTemplate(eq(userId), eq(Persona.PersonaType.WORK));
    }

    // ===== GET TEMPLATES TESTS =====

    @Test
    @DisplayName("GET /personas/templates - Should get all templates")
    void testGetTemplates_Success() {
        List<PersonaDto> templates = Arrays.asList(
                createTemplate("WORK"),
                createTemplate("PERSONAL"),
                createTemplate("GAMING")
        );
        when(personaService.getPersonaTemplates()).thenReturn(templates);

        ResponseEntity<List<PersonaDto>> response = personaController.getPersonaTemplates();

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().get(0).getType()).isEqualTo(Persona.PersonaType.WORK);
        assertThat(response.getBody().get(1).getType()).isEqualTo(Persona.PersonaType.PERSONAL);
        assertThat(response.getBody().get(2).getType()).isEqualTo(Persona.PersonaType.GAMING);

        verify(personaService, times(1)).getPersonaTemplates();
    }

    // ===== AUTHENTICATION ERROR HANDLING TESTS =====

    @Test
    @DisplayName("Should handle invalid authentication principal")
    void testInvalidAuthenticationPrincipal() {
        when(authentication.getName()).thenReturn("not-a-uuid");

        assertThatThrownBy(() -> personaController.getUserPersonas(authentication))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid user authentication");

        verify(personaService, never()).getUserPersonas(any());
    }

    // ===== HELPER METHODS =====

    private PersonaDto createSecondPersona() {
        PersonaDto persona = new PersonaDto();
        persona.setId(UUID.randomUUID());
        persona.setName("Personal Persona");
        persona.setType(Persona.PersonaType.PERSONAL);
        persona.setBio("Personal life persona");
        persona.setActive(false);
        persona.setDefault(false);
        return persona;
    }

    private PersonaDto createTemplate(String type) {
        PersonaDto template = new PersonaDto();
        template.setName(type + " Template");
        template.setType(Persona.PersonaType.valueOf(type));
        template.setBio("Template for " + type);
        return template;
    }
}