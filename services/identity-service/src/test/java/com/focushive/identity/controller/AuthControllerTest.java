package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.TestConfig;
import com.focushive.identity.dto.*;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.service.AuthenticationService;
import com.focushive.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class AuthControllerTest {
    
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
    
    @Autowired
    private AuthenticationService authenticationService;
    
    private User testUser;
    private Persona testPersona;
    private String validToken;
    
    @BeforeEach
    void setUp() {
        // Create test user with persona
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmailVerified(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
        
        testPersona = new Persona();
        testPersona.setUser(testUser);
        testPersona.setName("work");
        testPersona.setType(Persona.PersonaType.WORK);
        testPersona.setDisplayName("Professional Me");
        testPersona.setDefault(true);
        testPersona.setActive(true);
        testPersona = personaRepository.save(testPersona);
        
        testUser.getPersonas().add(testPersona);
        testUser = userRepository.save(testUser);
        
        // Generate valid token
        validToken = jwtTokenProvider.generateAccessToken(testUser, testPersona);
    }
    
    @Test
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        // No confirm password field in RegisterRequest
        request.setFirstName("New");
        request.setLastName("User");
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.activePersona.type").value("PERSONAL"))
                .andExpect(jsonPath("$.activePersona.default").value(true));
    }
    
    @Test
    void testRegister_UsernameAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser"); // Already exists
        request.setEmail("another@example.com");
        request.setPassword("password123");
        // No confirm password field in RegisterRequest
        request.setFirstName("Another");
        request.setLastName("User");
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }
    
    @Test
    void testRegister_EmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("anotheruser");
        request.setEmail("test@example.com"); // Already exists
        request.setPassword("password123");
        // No confirm password field in RegisterRequest
        request.setFirstName("Another");
        request.setLastName("User");
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }
    
    // Password mismatch test not applicable - RegisterRequest doesn't have confirmPassword field
    // @Test
    // void testRegister_PasswordMismatch() throws Exception {
    //     RegisterRequest request = new RegisterRequest();
    //     request.setUsername("newuser");
    //     request.setEmail("newuser@example.com");
    //     request.setPassword("password123");
    //     request.setDisplayName("New User");
    //     
    //     mockMvc.perform(post("/api/v1/auth/register")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(objectMapper.writeValueAsString(request)))
    //             .andExpect(status().isBadRequest());
    // }
    
    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.activePersona.type").value("WORK"));
    }
    
    @Test
    void testLogin_WithEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("test@example.com");
        request.setPassword("password123");
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
    
    @Test
    void testLogin_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("wrongpassword");
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
    
    @Test
    void testLogin_UserNotFound() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("nonexistent");
        request.setPassword("password123");
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
    
    @Test
    void testRefreshToken_Success() throws Exception {
        // First login to get tokens
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");
        
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        AuthenticationResponse authResponse = objectMapper.readValue(loginResponse, AuthenticationResponse.class);
        
        // Small delay to ensure new token has different timestamp
        Thread.sleep(1100); // Wait more than 1 second
        
        // Use refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(authResponse.getRefreshToken());
        
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").value(not(authResponse.getAccessToken())));
    }
    
    @Test
    void testRefreshToken_InvalidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");
        
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }
    
    @Test
    void testValidateToken_Success() throws Exception {
        ValidateTokenRequest request = new ValidateTokenRequest();
        request.setToken(validToken);
        
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.personaId").value(testPersona.getId().toString()));
    }
    
    @Test
    void testValidateToken_InvalidToken() throws Exception {
        ValidateTokenRequest request = new ValidateTokenRequest();
        request.setToken("invalid.jwt.token");
        
        mockMvc.perform(post("/api/v1/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
    
    @Test
    void testIntrospectToken_Success() throws Exception {
        IntrospectTokenRequest request = new IntrospectTokenRequest();
        request.setToken(validToken);
        
        mockMvc.perform(post("/api/v1/auth/introspect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sub").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.persona_id").value(testPersona.getId().toString()))
                .andExpect(jsonPath("$.persona_type").value("WORK"));
    }
    
    @Test
    void testLogout_Success() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setAccessToken(validToken);
        request.setRefreshToken("some-refresh-token");
        
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
    
    @Test
    void testLogout_Unauthorized() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setAccessToken("invalid-token");
        
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testPasswordResetRequest_Success() throws Exception {
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("test@example.com");
        
        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a reset link has been sent."));
    }
    
    @Test
    void testPasswordResetRequest_EmailNotFound() throws Exception {
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("nonexistent@example.com");
        
        // Should still return success for security reasons
        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a reset link has been sent."));
    }
    
    @Test
    void testSwitchPersona_Success() throws Exception {
        // Create another persona for the user
        Persona studyPersona = new Persona();
        studyPersona.setUser(testUser);
        studyPersona.setName("study");
        studyPersona.setType(Persona.PersonaType.STUDY);
        studyPersona.setDisplayName("Student Me");
        studyPersona.setDefault(false);
        studyPersona.setActive(false);
        studyPersona = personaRepository.save(studyPersona);
        
        SwitchPersonaRequest request = new SwitchPersonaRequest();
        request.setPersonaId(studyPersona.getId());
        
        mockMvc.perform(post("/api/v1/auth/personas/switch")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.activePersona.id").value(studyPersona.getId().toString()))
                .andExpect(jsonPath("$.activePersona.type").value("STUDY"));
    }
    
    @Test
    void testSwitchPersona_PersonaNotFound() throws Exception {
        SwitchPersonaRequest request = new SwitchPersonaRequest();
        request.setPersonaId(UUID.randomUUID());
        
        mockMvc.perform(post("/api/v1/auth/personas/switch")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Persona not found"));
    }
    
    @Test
    void testSwitchPersona_Unauthorized() throws Exception {
        SwitchPersonaRequest request = new SwitchPersonaRequest();
        request.setPersonaId(UUID.randomUUID());
        
        mockMvc.perform(post("/api/v1/auth/personas/switch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}