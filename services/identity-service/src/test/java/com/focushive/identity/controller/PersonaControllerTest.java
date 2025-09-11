package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.service.PersonaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for PersonaController.
 * Tests all persona management endpoints with various scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration.class
})
@ExtendWith(MockitoExtension.class)
class PersonaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonaService personaService;

    private PersonaDto testPersonaDto;
    private UUID userId;
    private UUID personaId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        personaId = UUID.randomUUID();

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
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testCreatePersona_Success() throws Exception {
        when(personaService.createPersona(any(UUID.class), any(PersonaDto.class)))
                .thenReturn(testPersonaDto);

        mockMvc.perform(post("/api/v1/personas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPersonaDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(personaId.toString())))
                .andExpect(jsonPath("$.name", is("Work Persona")))
                .andExpect(jsonPath("$.type", is("WORK")))
                .andExpect(jsonPath("$.active", is(true)));

        verify(personaService, times(1)).createPersona(any(UUID.class), any(PersonaDto.class));
    }

    @Test
    @DisplayName("POST /personas - Should fail without authentication")
    void testCreatePersona_Unauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/personas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testPersonaDto)))
                .andExpect(status().isUnauthorized());

        verify(personaService, never()).createPersona(any(), any());
    }

    @Test
    @DisplayName("POST /personas - Should fail with invalid data")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testCreatePersona_InvalidData() throws Exception {
        PersonaDto invalidPersona = new PersonaDto();
        // Missing required fields

        mockMvc.perform(post("/api/v1/personas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPersona)))
                .andExpect(status().isBadRequest());

        verify(personaService, never()).createPersona(any(), any());
    }

    // ===== GET USER PERSONAS TESTS =====

    @Test
    @DisplayName("GET /personas - Should get all user personas")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetUserPersonas_Success() throws Exception {
        List<PersonaDto> personas = Arrays.asList(testPersonaDto, createSecondPersona());
        when(personaService.getUserPersonas(any(UUID.class)))
                .thenReturn(personas);

        mockMvc.perform(get("/api/v1/personas")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Work Persona")))
                .andExpect(jsonPath("$[1].name", is("Personal Persona")));

        verify(personaService, times(1)).getUserPersonas(any(UUID.class));
    }

    @Test
    @DisplayName("GET /personas - Should return empty list when no personas")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetUserPersonas_EmptyList() throws Exception {
        when(personaService.getUserPersonas(any(UUID.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/personas")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(personaService, times(1)).getUserPersonas(any(UUID.class));
    }

    // ===== GET SPECIFIC PERSONA TESTS =====

    @Test
    @DisplayName("GET /personas/{id} - Should get specific persona")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetPersona_Success() throws Exception {
        when(personaService.getPersona(any(UUID.class), eq(personaId)))
                .thenReturn(testPersonaDto);

        mockMvc.perform(get("/api/v1/personas/{personaId}", personaId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(personaId.toString())))
                .andExpect(jsonPath("$.name", is("Work Persona")));

        verify(personaService, times(1)).getPersona(any(UUID.class), eq(personaId));
    }

    @Test
    @DisplayName("GET /personas/{id} - Should fail with invalid UUID")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetPersona_InvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/personas/invalid-uuid")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(personaService, never()).getPersona(any(), any());
    }

    // ===== UPDATE PERSONA TESTS =====

    @Test
    @DisplayName("PUT /personas/{id} - Should update persona successfully")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testUpdatePersona_Success() throws Exception {
        PersonaDto updatedPersona = new PersonaDto();
        updatedPersona.setId(personaId);
        updatedPersona.setName("Updated Work Persona");
        updatedPersona.setType(Persona.PersonaType.WORK);
        updatedPersona.setBio("Updated bio");

        when(personaService.updatePersona(any(UUID.class), eq(personaId), any(PersonaDto.class)))
                .thenReturn(updatedPersona);

        mockMvc.perform(put("/api/v1/personas/{personaId}", personaId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPersona)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Work Persona")))
                .andExpect(jsonPath("$.bio", is("Updated bio")));

        verify(personaService, times(1)).updatePersona(any(UUID.class), eq(personaId), any(PersonaDto.class));
    }

    @Test
    @DisplayName("PUT /personas/{id} - Should fail with invalid data")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testUpdatePersona_InvalidData() throws Exception {
        PersonaDto invalidPersona = new PersonaDto();
        // Invalid or missing data

        mockMvc.perform(put("/api/v1/personas/{personaId}", personaId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPersona)))
                .andExpect(status().isBadRequest());

        verify(personaService, never()).updatePersona(any(), any(), any());
    }

    // ===== DELETE PERSONA TESTS =====

    @Test
    @DisplayName("DELETE /personas/{id} - Should delete persona successfully")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testDeletePersona_Success() throws Exception {
        doNothing().when(personaService).deletePersona(any(UUID.class), eq(personaId));

        mockMvc.perform(delete("/api/v1/personas/{personaId}", personaId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(personaService, times(1)).deletePersona(any(UUID.class), eq(personaId));
    }

    @Test
    @DisplayName("DELETE /personas/{id} - Should fail when deleting default persona")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testDeletePersona_DefaultPersona() throws Exception {
        doThrow(new IllegalStateException("Cannot delete default persona"))
                .when(personaService).deletePersona(any(UUID.class), eq(personaId));

        mockMvc.perform(delete("/api/v1/personas/{personaId}", personaId)
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

        verify(personaService, times(1)).deletePersona(any(UUID.class), eq(personaId));
    }

    // ===== SWITCH PERSONA TESTS =====

    @Test
    @DisplayName("POST /personas/{id}/switch - Should switch persona successfully")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testSwitchPersona_Success() throws Exception {
        testPersonaDto.setActive(true);
        when(personaService.switchPersona(any(UUID.class), eq(personaId)))
                .thenReturn(testPersonaDto);

        mockMvc.perform(post("/api/v1/personas/{personaId}/switch", personaId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(personaId.toString())))
                .andExpect(jsonPath("$.active", is(true)));

        verify(personaService, times(1)).switchPersona(any(UUID.class), eq(personaId));
    }

    // ===== GET ACTIVE PERSONA TESTS =====

    @Test
    @DisplayName("GET /personas/active - Should get active persona")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetActivePersona_Success() throws Exception {
        when(personaService.getActivePersona(any(UUID.class)))
                .thenReturn(Optional.of(testPersonaDto));

        mockMvc.perform(get("/api/v1/personas/active")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(personaId.toString())))
                .andExpect(jsonPath("$.active", is(true)));

        verify(personaService, times(1)).getActivePersona(any(UUID.class));
    }

    @Test
    @DisplayName("GET /personas/active - Should return 204 when no active persona")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetActivePersona_NoContent() throws Exception {
        when(personaService.getActivePersona(any(UUID.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/personas/active")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(personaService, times(1)).getActivePersona(any(UUID.class));
    }

    // ===== SET DEFAULT PERSONA TESTS =====

    @Test
    @DisplayName("POST /personas/{id}/default - Should set default persona")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testSetDefaultPersona_Success() throws Exception {
        testPersonaDto.setDefault(true);
        when(personaService.setDefaultPersona(any(UUID.class), eq(personaId)))
                .thenReturn(testPersonaDto);

        mockMvc.perform(post("/api/v1/personas/{personaId}/default", personaId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(personaId.toString())))
                .andExpect(jsonPath("$.default", is(true)));

        verify(personaService, times(1)).setDefaultPersona(any(UUID.class), eq(personaId));
    }

    // ===== CREATE FROM TEMPLATE TESTS =====

    @Test
    @DisplayName("POST /personas/templates/{type} - Should create from template")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testCreateFromTemplate_Success() throws Exception {
        when(personaService.createPersonaFromTemplate(any(UUID.class), eq(Persona.PersonaType.WORK)))
                .thenReturn(testPersonaDto);

        mockMvc.perform(post("/api/v1/personas/templates/{type}", "WORK")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("WORK")));

        verify(personaService, times(1)).createPersonaFromTemplate(any(UUID.class), eq(Persona.PersonaType.WORK));
    }

    @Test
    @DisplayName("POST /personas/templates/{type} - Should fail with invalid template type")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testCreateFromTemplate_InvalidType() throws Exception {
        mockMvc.perform(post("/api/v1/personas/templates/{type}", "INVALID")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(personaService, never()).createPersonaFromTemplate(any(), any());
    }

    // ===== GET TEMPLATES TESTS =====

    @Test
    @DisplayName("GET /personas/templates - Should get all templates")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testGetTemplates_Success() throws Exception {
        List<PersonaDto> templates = Arrays.asList(
                createTemplate("WORK"),
                createTemplate("PERSONAL"),
                createTemplate("GAMING")
        );
        when(personaService.getPersonaTemplates()).thenReturn(templates);

        mockMvc.perform(get("/api/v1/personas/templates")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].type", is("WORK")))
                .andExpect(jsonPath("$[1].type", is("PERSONAL")))
                .andExpect(jsonPath("$[2].type", is("GAMING")));

        verify(personaService, times(1)).getPersonaTemplates();
    }

    // ===== ERROR HANDLING TESTS =====

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testServiceException_Handling() throws Exception {
        when(personaService.getPersona(any(UUID.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/v1/personas/{personaId}", personaId)
                        .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should reject malformed JSON")
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    void testMalformedJson_Rejection() throws Exception {
        String malformedJson = "{ invalid json }";

        mockMvc.perform(post("/api/v1/personas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(personaService, never()).createPersona(any(), any());
    }

    @Test
    @DisplayName("Should handle invalid authentication principal")
    @WithMockUser(username = "not-a-uuid")
    void testInvalidAuthenticationPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/personas")
                        .with(csrf()))
                .andExpect(status().isInternalServerError());

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