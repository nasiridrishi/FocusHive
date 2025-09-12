package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.TestContainersConfig;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.RegisterRequest;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simple TestContainers-based integration test for authentication flows.
 * 
 * This test uses REAL PostgreSQL container via TestContainers (no Redis for simplicity),
 * ensuring full stack integration testing from Controller → Service → Repository → Database.
 * 
 * Following strict TDD approach:
 * 1. Write ONE test
 * 2. Run it to ensure it fails
 * 3. Fix any issues
 * 4. Verify it passes
 * 5. Move to next test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@ContextConfiguration(initializers = SimpleTestContainersIntegrationTest.Initializer.class)
@Import(TestContainersConfig.class)
@Testcontainers
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SimpleTestContainersIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("identity_service_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;

    @BeforeAll
    static void configureProperties() {
        postgresql.start();
    }

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
     * TEST 1: Basic authentication flow with PostgreSQL TestContainer
     * 
     * This test verifies:
     * - User registration persists to real PostgreSQL database
     * - Login authentication works against real database
     * - JWT tokens are generated and valid
     * - Full stack integration (no mocks for database)
     */
    @Test
    @Transactional
    void testAuthenticationFlow_WithPostgreSQLContainer_Success() throws Exception {
        // ARRANGE: Verify we start with empty database and container is running
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(postgresql.isRunning()).isTrue();
        assertThat(postgresql.getJdbcUrl()).startsWith("jdbc:postgresql://");
        
        System.out.println("PostgreSQL TestContainer URL: " + postgresql.getJdbcUrl());
        
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
        
        // ASSERT: Verify database query was made (shows real database integration)
        System.out.println("User count in database: " + userRepository.count());
        System.out.println("Access token valid: " + jwtTokenProvider.validateToken(loginResponse.getAccessToken()));
        System.out.println("Database URL: " + postgresql.getJdbcUrl());
        System.out.println("✅ TEST PASSED - Full stack integration with PostgreSQL TestContainer verified!");
    }

    /**
     * TEST 2: Login with invalid credentials against real PostgreSQL database
     */
    @Test
    @Transactional  
    void testLogin_InvalidCredentials_WithPostgreSQL_ReturnsUnauthorized() throws Exception {
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
            
        System.out.println("✅ TEST PASSED - Invalid credentials properly rejected");
    }

    /**
     * Spring application context initializer to configure test properties from TestContainers.
     */
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgresql.getJdbcUrl(),
                "spring.datasource.username=" + postgresql.getUsername(),
                "spring.datasource.password=" + postgresql.getPassword(),
                "spring.datasource.driver-class-name=" + postgresql.getDriverClassName(),
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                // Disable Redis-related configurations for this test
                "spring.cache.type=simple",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
                // Disable security headers to prevent duplicate bean issues
                "focushive.security.headers.enabled=false",
                "focushive.security.headers.permissions-policy.enabled=false"
            ).applyTo(context.getEnvironment());
            
            System.out.println("🔧 TestContainers Configuration:");
            System.out.println("  PostgreSQL JDBC URL: " + postgresql.getJdbcUrl());
            System.out.println("  PostgreSQL Username: " + postgresql.getUsername());
            System.out.println("  Container Running: " + postgresql.isRunning());
        }
    }
}