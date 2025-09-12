package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.RegisterRequest;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TestContainers-based integration test for authentication flows.
 * 
 * This test uses REAL PostgreSQL and Redis containers via TestContainers,
 * ensuring full stack integration testing from Controller → Service → Repository → Database.
 * 
 * Following TDD approach:
 * 1. Write ONE test
 * 2. Run it to ensure it fails
 * 3. Fix any issues 
 * 4. Verify it passes
 * 5. Move to next test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationTestContainersIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        // Clean database before each test - using real PostgreSQL container
        userRepository.deleteAll();

        // Setup test data
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setPassword("testpassword123");
        validRegisterRequest.setFirstName("Test");
        validRegisterRequest.setLastName("User");
        validRegisterRequest.setPersonaType("PERSONAL");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsernameOrEmail("testuser");
        validLoginRequest.setPassword("testpassword123");
    }

    /**
     * TEST 1: Basic authentication flow with TestContainers
     * 
     * This test verifies:
     * - User registration persists to real PostgreSQL database
     * - Login authentication works against real database
     * - JWT tokens are generated and valid
     * - Full stack integration (no mocks)
     */
    @Test
    @Transactional
    void testAuthenticationFlow_WithRealDatabase_Success() throws Exception {
        // ARRANGE: Verify we start with empty database
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(postgresql.isRunning()).isTrue(); // Verify TestContainer is running
        
        // ACT & ASSERT: Step 1 - Register user
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andReturn();

        // ASSERT: Parse registration response
        String registerResponseContent = registerResult.getResponse().getContentAsString();
        AuthenticationResponse registerResponse = objectMapper.readValue(registerResponseContent, AuthenticationResponse.class);

        assertThat(registerResponse.getAccessToken()).isNotNull();
        assertThat(registerResponse.getRefreshToken()).isNotNull();
        assertThat(registerResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(registerResponse.getUsername()).isEqualTo("testuser");
        assertThat(registerResponse.getEmail()).isEqualTo("test@example.com");

        // ASSERT: Verify user was persisted to REAL PostgreSQL database
        assertThat(userRepository.count()).isEqualTo(1);
        Optional<User> savedUser = userRepository.findByUsername("testuser");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.get().getUsername()).isEqualTo("testuser");
        assertThat(savedUser.get().isEnabled()).isTrue();
        assertThat(savedUser.get().isAccountNonLocked()).isTrue();

        // ASSERT: Verify password was properly hashed in database
        assertThat(passwordEncoder.matches("testpassword123", savedUser.get().getPassword())).isTrue();

        // ACT & ASSERT: Step 2 - Login with same credentials
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andReturn();

        // ASSERT: Parse login response
        String loginResponseContent = loginResult.getResponse().getContentAsString();
        AuthenticationResponse loginResponse = objectMapper.readValue(loginResponseContent, AuthenticationResponse.class);

        assertThat(loginResponse.getAccessToken()).isNotNull();
        assertThat(loginResponse.getRefreshToken()).isNotNull();
        assertThat(loginResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(loginResponse.getUsername()).isEqualTo("testuser");
        assertThat(loginResponse.getEmail()).isEqualTo("test@example.com");
        assertThat(loginResponse.getUserId()).isEqualTo(registerResponse.getUserId());

        // ASSERT: Verify JWT tokens are valid and different from registration
        assertThat(jwtTokenProvider.validateToken(loginResponse.getAccessToken())).isTrue();
        assertThat(jwtTokenProvider.validateToken(loginResponse.getRefreshToken())).isTrue();
        
        // Different session should generate different tokens
        assertThat(loginResponse.getAccessToken()).isNotEqualTo(registerResponse.getAccessToken());
        assertThat(loginResponse.getRefreshToken()).isNotEqualTo(registerResponse.getRefreshToken());

        // ASSERT: Verify we can extract user details from JWT tokens
        String usernameFromAccessToken = jwtTokenProvider.extractUsername(loginResponse.getAccessToken());
        assertThat(usernameFromAccessToken).isEqualTo("testuser");
    }

    /**
     * TEST 2: Login with invalid credentials against real database
     */
    @Test
    @Transactional  
    void testLogin_InvalidCredentials_WithRealDatabase_ReturnsUnauthorized() throws Exception {
        // ARRANGE: Register a user first
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated());

        // Verify user exists in real database
        assertThat(userRepository.count()).isEqualTo(1);

        // ACT & ASSERT: Attempt login with wrong password
        LoginRequest invalidLoginRequest = new LoginRequest();
        invalidLoginRequest.setUsernameOrEmail("testuser");
        invalidLoginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginRequest)))
            .andExpect(status().isUnauthorized());
    }

    /**
     * TEST 3: Verify database constraints work with TestContainers
     */
    @Test
    @Transactional
    void testDuplicateRegistration_WithRealDatabase_ReturnsConflict() throws Exception {
        // ARRANGE & ACT: Register first user
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated());

        // Verify user exists in real PostgreSQL database
        assertThat(userRepository.count()).isEqualTo(1);

        // ACT & ASSERT: Try to register with same email (should trigger DB constraint)
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setEmail("test@example.com"); // Same email
        duplicateRequest.setUsername("differentuser"); // Different username
        duplicateRequest.setPassword("testpassword123");
        duplicateRequest.setFirstName("Different");
        duplicateRequest.setLastName("User");
        duplicateRequest.setPersonaType("PERSONAL");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isConflict());

        // ASSERT: Database should still have only one user
        assertThat(userRepository.count()).isEqualTo(1);
    }
}