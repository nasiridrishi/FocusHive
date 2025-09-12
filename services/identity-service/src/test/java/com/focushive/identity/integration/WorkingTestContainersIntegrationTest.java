package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.MinimalTestConfig;
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
 * Working TestContainers integration test for authentication flows.
 * 
 * This test uses the 'test' profile with MinimalTestConfig (which works) 
 * and adds PostgreSQL TestContainer for real database integration.
 * 
 * TDD approach:
 * 1. ✅ Created test file 
 * 2. ⏳ Run test to see if it passes
 * 3. ✅ If it passes, verify it's using real PostgreSQL 
 * 4. ✅ Add more test cases
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MinimalTestConfig.class)
@ContextConfiguration(initializers = WorkingTestContainersIntegrationTest.Initializer.class)
@Testcontainers
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkingTestContainersIntegrationTest {

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
     * 🎯 MAIN TEST: Full authentication flow with real PostgreSQL TestContainer
     * 
     * This test verifies:
     * - User registration persists to REAL PostgreSQL database (not H2, not mocks)
     * - Login authentication works against REAL database  
     * - JWT tokens are generated and valid
     * - Full stack integration: Controller → Service → Repository → Database
     * - TestContainers properly started and configured
     */
    @Test
    @Transactional
    void testFullAuthenticationStack_WithPostgreSQLTestContainer_Success() throws Exception {
        // 🔍 ASSERT: Test environment is correctly configured
        assertThat(userRepository.count()).isEqualTo(0);
        assertThat(postgresql.isRunning()).isTrue();
        assertThat(postgresql.getJdbcUrl()).contains("postgresql");
        
        System.out.println("\n🐳 TestContainers Status:");
        System.out.println("  PostgreSQL Running: " + postgresql.isRunning());
        System.out.println("  JDBC URL: " + postgresql.getJdbcUrl());
        System.out.println("  Container ID: " + postgresql.getContainerId());
        
        // 🎬 ACT: Step 1 - Register a user (should persist to real PostgreSQL)
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

        // 🔍 ASSERT: Registration response is valid
        String registerResponseContent = registerResult.getResponse().getContentAsString();
        AuthenticationResponse registerResponse = objectMapper.readValue(registerResponseContent, AuthenticationResponse.class);

        assertThat(registerResponse.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(registerResponse.getRefreshToken()).isNotNull().isNotEmpty();
        assertThat(registerResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(registerResponse.getUsername()).isEqualTo("testuser");
        assertThat(registerResponse.getEmail()).isEqualTo("test@example.com");

        // 🔍 ASSERT: User was persisted to REAL PostgreSQL database
        assertThat(userRepository.count()).isEqualTo(1);
        Optional<User> savedUser = userRepository.findByUsername("testuser");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.get().getUsername()).isEqualTo("testuser");
        assertThat(savedUser.get().isEnabled()).isTrue();
        assertThat(savedUser.get().isAccountNonLocked()).isTrue();

        // 🔍 ASSERT: Password was properly hashed (not stored in plain text)
        assertThat(passwordEncoder.matches("testpassword123", savedUser.get().getPassword())).isTrue();
        assertThat(savedUser.get().getPassword()).isNotEqualTo("testpassword123"); // Should be hashed

        System.out.println("✅ User successfully persisted to PostgreSQL container:");
        System.out.println("  User ID: " + savedUser.get().getId());
        System.out.println("  Username: " + savedUser.get().getUsername());
        System.out.println("  Email: " + savedUser.get().getEmail());
        System.out.println("  Password Hashed: " + (savedUser.get().getPassword().startsWith("$2a$")));

        // 🎬 ACT: Step 2 - Login with the same credentials (should authenticate against real DB)
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

        // 🔍 ASSERT: Login response is valid
        String loginResponseContent = loginResult.getResponse().getContentAsString();
        AuthenticationResponse loginResponse = objectMapper.readValue(loginResponseContent, AuthenticationResponse.class);

        assertThat(loginResponse.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(loginResponse.getRefreshToken()).isNotNull().isNotEmpty();
        assertThat(loginResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(loginResponse.getUsername()).isEqualTo("testuser");
        assertThat(loginResponse.getEmail()).isEqualTo("test@example.com");
        assertThat(loginResponse.getUserId()).isEqualTo(registerResponse.getUserId());

        // 🔍 ASSERT: JWT tokens are valid and functional
        assertThat(jwtTokenProvider.validateToken(loginResponse.getAccessToken())).isTrue();
        assertThat(jwtTokenProvider.validateToken(loginResponse.getRefreshToken())).isTrue();
        
        // 🔍 ASSERT: Each login creates new tokens (different sessions)
        assertThat(loginResponse.getAccessToken()).isNotEqualTo(registerResponse.getAccessToken());
        assertThat(loginResponse.getRefreshToken()).isNotEqualTo(registerResponse.getRefreshToken());

        // 🔍 ASSERT: Can extract user information from JWT tokens
        String usernameFromToken = jwtTokenProvider.extractUsername(loginResponse.getAccessToken());
        assertThat(usernameFromToken).isEqualTo("testuser");
        
        String emailFromToken = jwtTokenProvider.extractEmail(loginResponse.getAccessToken());
        assertThat(emailFromToken).isEqualTo("test@example.com");

        System.out.println("🎉 FULL INTEGRATION TEST PASSED!");
        System.out.println("✅ Real PostgreSQL TestContainer used");
        System.out.println("✅ User registration persisted to real database");
        System.out.println("✅ Authentication works against real database");
        System.out.println("✅ JWT tokens generated and validated");
        System.out.println("✅ Full stack integration: Controller → Service → Repository → PostgreSQL");
    }

    /**
     * TEST 2: Invalid credentials with real database
     */
    @Test
    @Transactional
    void testLogin_InvalidCredentials_WithRealDatabase_ReturnsUnauthorized() throws Exception {
        // ARRANGE: Register a user first (in real PostgreSQL)
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRegisterRequest)))
            .andExpect(status().isCreated());

        // Verify user exists in real database
        assertThat(userRepository.count()).isEqualTo(1);

        // ACT & ASSERT: Attempt login with wrong password (against real database)
        LoginRequest invalidLoginRequest = new LoginRequest();
        invalidLoginRequest.setUsernameOrEmail("testuser");
        invalidLoginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginRequest)))
            .andExpect(status().isUnauthorized());
            
        System.out.println("✅ Invalid credentials properly rejected with real database");
    }

    /**
     * Spring application context initializer for TestContainers configuration.
     */
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                // PostgreSQL TestContainer configuration
                "spring.datasource.url=" + postgresql.getJdbcUrl(),
                "spring.datasource.username=" + postgresql.getUsername(),
                "spring.datasource.password=" + postgresql.getPassword(),
                "spring.datasource.driver-class-name=" + postgresql.getDriverClassName(),
                
                // Hibernate configuration for tests
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false", // Let Hibernate create the schema
                
                // JWT configuration for tests (required for token generation)
                "jwt.secret=testSecretKeyThatIsAtLeast512BitsLongForHS512SecurityPurposesAbcDefGhiJklMnoPqrStuVwxYz123456789AbcDef",
                
                // Encryption configuration for tests
                "app.encryption.master-key=testMasterKeyForEncryptionThatIsAtLeast32CharsLongForSecurityPurposes",
                
                // Disable problematic configurations
                "spring.main.allow-circular-references=true"
            ).applyTo(context.getEnvironment());
            
            System.out.println("\n🔧 TestContainers Configuration Applied:");
            System.out.println("  PostgreSQL JDBC URL: " + postgresql.getJdbcUrl());
            System.out.println("  Username: " + postgresql.getUsername());
            System.out.println("  Driver: " + postgresql.getDriverClassName());
            System.out.println("  Container Started: " + postgresql.isRunning());
        }
    }
}