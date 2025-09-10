package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.TestConfig;
import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PersonaController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@DisplayName("PersonaController Integration Tests")
class PersonaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User testUser;
    private Persona workPersona;
    private Persona personalPersona;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmailVerified(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test personas
        workPersona = new Persona();
        workPersona.setUser(testUser);
        workPersona.setName("Work");
        workPersona.setType(Persona.PersonaType.WORK);
        workPersona.setDisplayName("Work Profile");
        workPersona.setBio("Work persona");
        workPersona.setDefault(true);
        workPersona.setActive(true);
        workPersona.setCreatedAt(Instant.now());
        workPersona = personaRepository.save(workPersona);

        personalPersona = new Persona();
        personalPersona.setUser(testUser);
        personalPersona.setName("Personal");
        personalPersona.setType(Persona.PersonaType.PERSONAL);
        personalPersona.setDisplayName("Personal Profile");
        personalPersona.setBio("Personal persona");
        personalPersona.setDefault(false);
        personalPersona.setActive(false);
        personalPersona.setCreatedAt(Instant.now());
        personalPersona = personaRepository.save(personalPersona);

        // Update user with personas
        testUser.getPersonas().add(workPersona);
        testUser.getPersonas().add(personalPersona);
        testUser = userRepository.save(testUser);

        // Generate valid token
        validToken = jwtTokenProvider.generateAccessToken(testUser, workPersona);
    }

    @Test
    @DisplayName("Should create persona successfully")
    void createPersona_ShouldCreatePersonaSuccessfully() throws Exception {
        // Given
        PersonaDto createRequest = PersonaDto.builder()
                .name("Gaming")
                .type(Persona.PersonaType.GAMING)
                .displayName("Gaming Profile")
                .bio("Gaming persona")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/personas")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gaming"))
                .andExpect(jsonPath("$.type").value("GAMING"))
                .andExpect(jsonPath("$.displayName").value("Gaming Profile"))
                .andExpect(jsonPath("$.bio").value("Gaming persona"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Should get user personas successfully")
    void getUserPersonas_ShouldReturnPersonasSuccessfully() throws Exception {
        mockMvc.perform(get("/api/v1/personas")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", hasItems("Work", "Personal")))
                .andExpect(jsonPath("$[?(@.name=='Work')].isActive").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.name=='Personal')].isActive").value(hasItem(false)));
    }

    @Test
    @DisplayName("Should get specific persona successfully")
    void getPersona_ShouldReturnPersonaSuccessfully() throws Exception {
        UUID personaId = workPersona.getId();

        mockMvc.perform(get("/api/v1/personas/{personaId}", personaId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(personaId.toString()))
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.displayName").value("Work Profile"));
    }

    @Test
    @DisplayName("Should return 404 when persona not found")
    void getPersona_ShouldReturn404WhenNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/personas/{personaId}", nonExistentId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update persona successfully")
    void updatePersona_ShouldUpdatePersonaSuccessfully() throws Exception {
        UUID personaId = workPersona.getId();
        PersonaDto updateRequest = PersonaDto.builder()
                .displayName("Updated Work Profile")
                .bio("Updated bio")
                .build();

        mockMvc.perform(put("/api/v1/personas/{personaId}", personaId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Work Profile"))
                .andExpect(jsonPath("$.bio").value("Updated bio"));
    }

    @Test
    @DisplayName("Should delete persona successfully")
    void deletePersona_ShouldDeletePersonaSuccessfully() throws Exception {
        UUID personaId = personalPersona.getId();

        mockMvc.perform(delete("/api/v1/personas/{personaId}", personaId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());

        // Verify persona was deleted
        assert !personaRepository.existsById(personaId);
    }

    @Test
    @DisplayName("Should not delete default persona")
    void deletePersona_ShouldNotDeleteDefaultPersona() throws Exception {
        UUID defaultPersonaId = workPersona.getId(); // This is the default persona

        mockMvc.perform(delete("/api/v1/personas/{personaId}", defaultPersonaId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should switch persona successfully")
    void switchPersona_ShouldSwitchPersonaSuccessfully() throws Exception {
        UUID personaId = personalPersona.getId();

        mockMvc.perform(post("/api/v1/personas/{personaId}/switch", personaId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(personaId.toString()))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("Should get active persona successfully")
    void getActivePersona_ShouldReturnActivePersona() throws Exception {
        mockMvc.perform(get("/api/v1/personas/active")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("Should set default persona successfully")
    void setDefaultPersona_ShouldSetDefaultSuccessfully() throws Exception {
        UUID personaId = personalPersona.getId();

        mockMvc.perform(post("/api/v1/personas/{personaId}/default", personaId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(personaId.toString()))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("Should create persona from template successfully")
    void createPersonaFromTemplate_ShouldCreatePersonaSuccessfully() throws Exception {
        mockMvc.perform(post("/api/v1/personas/templates/{type}", "STUDY")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("STUDY"))
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.displayName").isString());
    }

    @Test
    @DisplayName("Should get persona templates successfully")
    void getPersonaTemplates_ShouldReturnTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/personas/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].type").isArray());
    }

    @Test
    @DisplayName("Should require authentication for all endpoints")
    void endpoints_ShouldRequireAuthentication() throws Exception {
        // Test various endpoints without authentication
        mockMvc.perform(get("/api/v1/personas"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/personas/active"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/personas/" + UUID.randomUUID() + "/switch"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should validate request body for create persona")
    void createPersona_ShouldValidateRequestBody() throws Exception {
        // Test with invalid data
        PersonaDto invalidRequest = PersonaDto.builder()
                .name("") // Empty name should fail validation
                .build();

        mockMvc.perform(post("/api/v1/personas")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle concurrent persona switching")
    void switchPersona_ShouldHandleConcurrentSwitching() throws Exception {
        // Create additional persona
        Persona studyPersona = new Persona();
        studyPersona.setUser(testUser);
        studyPersona.setName("Study");
        studyPersona.setType(Persona.PersonaType.STUDY);
        studyPersona.setDisplayName("Study Profile");
        studyPersona.setDefault(false);
        studyPersona.setActive(false);
        studyPersona = personaRepository.save(studyPersona);

        // Switch to personal persona
        mockMvc.perform(post("/api/v1/personas/{personaId}/switch", personalPersona.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());

        // Switch to study persona
        mockMvc.perform(post("/api/v1/personas/{personaId}/switch", studyPersona.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studyPersona.getId().toString()))
                .andExpect(jsonPath("$.isActive").value(true));

        // Verify only one persona is active
        long activeCount = personaRepository.findByUserAndActive(testUser, true).size();
        assert activeCount == 1 : "Should have exactly one active persona";
    }

    @Test
    @DisplayName("Should handle persona not belonging to user")
    void endpoints_ShouldReturn404ForOtherUsersPersonas() throws Exception {
        // Create another user and persona
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmailVerified(true);
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        Persona otherPersona = new Persona();
        otherPersona.setUser(otherUser);
        otherPersona.setName("Other Work");
        otherPersona.setType(Persona.PersonaType.WORK);
        otherPersona.setDisplayName("Other Work Profile");
        otherPersona.setDefault(true);
        otherPersona.setActive(true);
        otherPersona = personaRepository.save(otherPersona);

        // Try to access other user's persona
        mockMvc.perform(get("/api/v1/personas/{personaId}", otherPersona.getId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound());

        // Try to update other user's persona
        PersonaDto updateRequest = PersonaDto.builder()
                .displayName("Hacked Profile")
                .build();

        mockMvc.perform(put("/api/v1/personas/{personaId}", otherPersona.getId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should handle invalid persona type in template")
    void createPersonaFromTemplate_ShouldHandleInvalidType() throws Exception {
        mockMvc.perform(post("/api/v1/personas/templates/{type}", "INVALID_TYPE")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should validate unique persona names per user")
    void createPersona_ShouldValidateUniqueNames() throws Exception {
        PersonaDto duplicateRequest = PersonaDto.builder()
                .name("Work") // Same as existing persona
                .type(Persona.PersonaType.WORK)
                .displayName("Duplicate Work Profile")
                .build();

        mockMvc.perform(post("/api/v1/personas")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle invalid token")
    void endpoints_ShouldHandleInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/personas")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}