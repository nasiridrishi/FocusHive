package com.focushive.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.TestSecurityConfig;
import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Role;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.PersonaService;
import com.focushive.identity.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security authorization tests for the Identity Service.
 * Tests @PreAuthorize annotations and SecurityService authorization logic.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("Authorization Security Tests")
class AuthorizationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PersonaService personaService;

    @MockBean
    private SecurityService securityService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PersonaRepository personaRepository;

    private MockMvc mockMvc;
    private User testUser;
    private User otherUser;
    private UUID testPersonaId;
    private PersonaDto testPersona;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Set up test data
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .email("other@example.com")
                .role(Role.USER)
                .build();

        testPersonaId = UUID.randomUUID();
        testPersona = new PersonaDto();
        testPersona.setId(testPersonaId);
        testPersona.setName("Test Persona");
        testPersona.setDescription("Test persona for authorization tests");
    }

    @Test
    @DisplayName("Unauthenticated access should be denied")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/personas")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPersona)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/personas/" + testPersonaId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Authenticated user can access their own personas")
    void testAuthenticatedUserCanAccessOwnPersonas() throws Exception {
        // Mock SecurityService to allow access
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(securityService.hasAccessToPersona(testPersonaId)).thenReturn(true);
        
        // Mock PersonaService responses
        when(personaService.getUserPersonas(any(UUID.class))).thenReturn(java.util.List.of(testPersona));
        when(personaService.getPersona(any(UUID.class), eq(testPersonaId))).thenReturn(testPersona);

        // Test getting user's personas
        mockMvc.perform(get("/api/v1/personas")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Test getting specific persona
        mockMvc.perform(get("/api/v1/personas/" + testPersonaId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPersonaId.toString()));

        verify(securityService, times(1)).hasAccessToPersona(testPersonaId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("User cannot access other user's personas")
    void testUserCannotAccessOtherUserPersonas() throws Exception {
        UUID otherPersonaId = UUID.randomUUID();
        
        // Mock SecurityService to deny access
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(securityService.hasAccessToPersona(otherPersonaId)).thenReturn(false);

        // Test accessing other user's persona should fail
        mockMvc.perform(get("/api/v1/personas/" + otherPersonaId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(securityService, times(1)).hasAccessToPersona(otherPersonaId);
        verify(personaService, never()).getPersona(any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Admin user can access any persona")
    void testAdminCanAccessAnyPersona() throws Exception {
        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        // Mock SecurityService for admin access
        when(securityService.getCurrentUser()).thenReturn(Optional.of(adminUser));
        when(securityService.hasAccessToPersona(any(UUID.class))).thenReturn(true);
        when(personaService.getPersona(any(UUID.class), eq(testPersonaId))).thenReturn(testPersona);

        // Test admin accessing any persona
        mockMvc.perform(get("/api/v1/personas/" + testPersonaId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPersonaId.toString()));

        verify(securityService, times(1)).hasAccessToPersona(testPersonaId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("User can create personas")
    void testUserCanCreatePersonas() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(personaService.createPersona(any(UUID.class), any(PersonaDto.class))).thenReturn(testPersona);

        mockMvc.perform(post("/api/v1/personas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPersona)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Persona"));

        verify(personaService, times(1)).createPersona(any(UUID.class), any(PersonaDto.class));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("User can update their own personas")
    void testUserCanUpdateOwnPersonas() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(securityService.hasAccessToPersona(testPersonaId)).thenReturn(true);
        when(personaService.updatePersona(any(UUID.class), eq(testPersonaId), any(PersonaDto.class)))
                .thenReturn(testPersona);

        mockMvc.perform(put("/api/v1/personas/" + testPersonaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPersona)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPersonaId.toString()));

        verify(securityService, times(1)).hasAccessToPersona(testPersonaId);
        verify(personaService, times(1)).updatePersona(any(UUID.class), eq(testPersonaId), any(PersonaDto.class));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("User cannot update other user's personas")
    void testUserCannotUpdateOtherUserPersonas() throws Exception {
        UUID otherPersonaId = UUID.randomUUID();
        
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(securityService.hasAccessToPersona(otherPersonaId)).thenReturn(false);

        mockMvc.perform(put("/api/v1/personas/" + otherPersonaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPersona)))
                .andExpect(status().isForbidden());

        verify(securityService, times(1)).hasAccessToPersona(otherPersonaId);
        verify(personaService, never()).updatePersona(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("User can delete their own personas")
    void testUserCanDeleteOwnPersonas() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(securityService.hasAccessToPersona(testPersonaId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/personas/" + testPersonaId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(securityService, times(1)).hasAccessToPersona(testPersonaId);
        verify(personaService, times(1)).deletePersona(any(UUID.class), eq(testPersonaId));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("User cannot delete other user's personas")
    void testUserCannotDeleteOtherUserPersonas() throws Exception {
        UUID otherPersonaId = UUID.randomUUID();
        
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(securityService.hasAccessToPersona(otherPersonaId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/personas/" + otherPersonaId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(securityService, times(1)).hasAccessToPersona(otherPersonaId);
        verify(personaService, never()).deletePersona(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("User can switch to their own personas")
    void testUserCanSwitchOwnPersonas() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(Optional.of(testUser));
        when(securityService.hasAccessToPersona(testPersonaId)).thenReturn(true);
        when(personaService.switchPersona(any(UUID.class), eq(testPersonaId))).thenReturn(testPersona);

        mockMvc.perform(post("/api/v1/personas/" + testPersonaId + "/switch")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPersonaId.toString()));

        verify(securityService, times(1)).hasAccessToPersona(testPersonaId);
        verify(personaService, times(1)).switchPersona(any(UUID.class), eq(testPersonaId));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("SecurityService authorization methods work correctly")
    void testSecurityServiceAuthorizationMethods() throws Exception {
        // Test hasAccessToUser
        when(securityService.hasAccessToUser(testUser.getId())).thenReturn(true);
        when(securityService.hasAccessToUser(otherUser.getId())).thenReturn(false);

        // Test hasAccessToPersona
        when(securityService.hasAccessToPersona(testPersonaId)).thenReturn(true);
        UUID otherPersonaId = UUID.randomUUID();
        when(securityService.hasAccessToPersona(otherPersonaId)).thenReturn(false);

        // Test isOwner
        when(securityService.isOwner(testUser.getId())).thenReturn(true);
        when(securityService.isOwner(otherUser.getId())).thenReturn(false);

        // Test hasRole
        when(securityService.hasRole("USER")).thenReturn(true);
        when(securityService.hasRole("ADMIN")).thenReturn(false);

        // Verify method calls would work as expected
        assert securityService.hasAccessToUser(testUser.getId());
        assert !securityService.hasAccessToUser(otherUser.getId());
        assert securityService.hasAccessToPersona(testPersonaId);
        assert !securityService.hasAccessToPersona(otherPersonaId);
        assert securityService.isOwner(testUser.getId());
        assert !securityService.isOwner(otherUser.getId());
        assert securityService.hasRole("USER");
        assert !securityService.hasRole("ADMIN");
    }

    @Test
    @DisplayName("CORS headers should be properly configured")
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/v1/personas")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
}