package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.RefreshTokenRequest;
import com.focushive.identity.entity.User;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.config.MinimalTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for authentication endpoints.
 * Tests the actual REST endpoints with real database operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MinimalTestConfig.class)
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "jwt.secret=testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesAbcDefGhiJklMnoPqrStuVwxYz123456789AbcDef",
    "redis.rate-limiting.enabled=false"
})
@Transactional
class AuthenticationIntegrationTest {

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

    private User testUser;
    private Persona testPersona;

    @BeforeEach
    void setUp() {
        // Clear repositories
        personaRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("testpassword123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmailVerified(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test persona
        testPersona = new Persona();
        testPersona.setId(UUID.randomUUID());
        testPersona.setUser(testUser);
        testPersona.setName("Default");
        testPersona.setType(Persona.PersonaType.PERSONAL);
        testPersona.setDefault(true);
        testPersona = personaRepository.save(testPersona);
    }

    @Test
    void testLogin_ValidCredentials_ReturnsTokens() throws Exception {
        // Create login request
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("testpassword123");

        // Execute login request
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Parse response and verify content
        AuthenticationResponse authResponse = objectMapper.readValue(response, AuthenticationResponse.class);
        
        assertThat(authResponse.getAccessToken()).isNotEmpty();
        assertThat(authResponse.getRefreshToken()).isNotEmpty();
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(authResponse.getUsername()).isEqualTo("testuser");
        assertThat(authResponse.getEmail()).isEqualTo("test@example.com");
        assertThat(authResponse.getActivePersona()).isNotNull();
        assertThat(authResponse.getActivePersona().getName()).isEqualTo("Default");
    }
}