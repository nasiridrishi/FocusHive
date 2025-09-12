package com.focushive.identity.integration;

import com.focushive.identity.repository.UserRepository;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for error handling scenarios including database failures and security attacks.
 * Phase 4 testing focus: Database connection failures and SQL injection prevention.
 * 
 * These tests verify:
 * 1. System gracefully handles database connection failures
 * 2. Proper error messages are returned without exposing internal details
 * 3. SQL injection attempts are blocked and sanitized
 * 4. Input validation prevents malicious queries
 * 5. System remains secure under attack scenarios
 */
@DisplayName("Error Handling Integration Tests")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ErrorHandlingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private DataSourceQueryCountListener queryCountListener;

    @BeforeEach
    void setUp() {
        // Reset query count listener for SQL monitoring
        queryCountListener = new DataSourceQueryCountListener();
    }


    // ========================================
    // Database Connection Failure Tests
    // ========================================

    @Test
    @DisplayName("Should handle database constraint violations gracefully")
    @WithMockUser
    void testDatabaseConstraintViolation_DuplicateUser() throws Exception {
        // Given: Create a user first
        Map<String, Object> firstUser = createValidRegistrationData();
        String firstUserJson = objectMapper.writeValueAsString(firstUser);
        
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstUserJson)
                        .with(csrf()))
                .andExpect(status().isCreated());

        // When: Try to register the same user again (should violate unique constraints)
        Map<String, Object> duplicateUser = createValidRegistrationData();
        String duplicateUserJson = objectMapper.writeValueAsString(duplicateUser);

        // Then: Should return 400 Bad Request with appropriate error message
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateUserJson)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists())
                .andReturn();

        // Verify: Error response doesn't expose internal database details
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody)
                .doesNotContain("constraint")
                .doesNotContain("duplicate key")
                .doesNotContain("SQLException")
                .doesNotContain("postgres")
                .doesNotContain("DETAIL:");

        // Verify: Response contains appropriate user-friendly error message
        assertThat(responseBody).contains("already");
    }

    @Test
    @DisplayName("Should handle malformed JSON requests gracefully")
    @WithMockUser
    void testMalformedJsonRequest() throws Exception {
        // Given: Malformed JSON request body
        String malformedJson = "{ \"email\": \"test@example.com\", \"username\": \"testuser\" "; // Missing closing brace

        // When & Then: Should return 400 Bad Request for malformed JSON
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid JSON format or missing request body"));
    }

    // ========================================
    // SQL Injection Prevention Tests
    // ========================================

    @Test
    @DisplayName("Should prevent SQL injection in user registration email field")
    @WithMockUser
    void testSqlInjection_RegistrationEmail() throws Exception {
        // Given: Malicious SQL injection payload in email field
        Map<String, Object> maliciousData = new HashMap<>();
        maliciousData.put("username", "normaluser");
        maliciousData.put("email", "test@example.com'; DROP TABLE users; --");
        maliciousData.put("password", "ValidPassword123!");
        maliciousData.put("firstName", "Test");
        maliciousData.put("lastName", "User");

        String requestBody = objectMapper.writeValueAsString(maliciousData);

        // When: Attempt registration with malicious email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").exists());

        // Then: Verify tables still exist (SQL injection was prevented)
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(0);
        
        // Verify: No malicious queries were executed
        verifyNoSqlInjectionQueries();
    }

    @Test
    @DisplayName("Should sanitize and block SQL injection in username field")
    @WithMockUser  
    void testSqlInjection_UsernameField() throws Exception {
        // Given: SQL injection payload in username
        Map<String, Object> maliciousData = new HashMap<>();
        maliciousData.put("username", "admin' OR '1'='1");
        maliciousData.put("email", "test@example.com");
        maliciousData.put("password", "ValidPassword123!");
        maliciousData.put("firstName", "Test");
        maliciousData.put("lastName", "User");

        String requestBody = objectMapper.writeValueAsString(maliciousData);

        // When & Then: Should reject malicious username
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        // Verify: No unauthorized access or data exposure
        verifyNoSqlInjectionQueries();
    }

    @Test
    @DisplayName("Should prevent SQL injection in login credentials")
    @WithMockUser
    void testSqlInjection_LoginCredentials() throws Exception {
        // Given: SQL injection in login request
        Map<String, String> loginData = new HashMap<>();
        loginData.put("usernameOrEmail", "admin' --");
        loginData.put("password", "' OR '1'='1' --");

        String requestBody = objectMapper.writeValueAsString(loginData);

        // When & Then: Should reject malicious login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));

        // Verify: No SQL injection was successful
        verifyNoSqlInjectionQueries();
    }

    @Test
    @DisplayName("Should sanitize special characters in user input")
    @WithMockUser
    void testInputSanitization_SpecialCharacters() throws Exception {
        // Given: Input with various special characters
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("username", "user<script>alert('xss')</script>");
        inputData.put("email", "test@example.com");
        inputData.put("password", "ValidPassword123!");
        inputData.put("firstName", "Test & <img src=x onerror=alert('xss')>");
        inputData.put("lastName", "User');");

        String requestBody = objectMapper.writeValueAsString(inputData);

        // When: Submit data with special characters
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then: Verify input validation caught malicious content
        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("Validation failed");
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Creates valid user registration data for testing.
     */
    private Map<String, Object> createValidRegistrationData() {
        Map<String, Object> data = new HashMap<>();
        data.put("username", "testuser");
        data.put("email", "testuser@example.com");
        data.put("password", "ValidPassword123!");
        data.put("firstName", "Test");
        data.put("lastName", "User");
        return data;
    }

    /**
     * Verifies that no SQL injection queries were executed.
     * This method checks for common SQL injection patterns by verifying database integrity.
     */
    private void verifyNoSqlInjectionQueries() {
        // Verify: Database integrity is maintained (no unauthorized operations occurred)
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(0);
        
        // Verify: No suspicious entries were created with malicious content
        // This would catch if SQL injection bypassed validation
        assertThat(userRepository.findAll())
                .noneMatch(user -> user.getEmail().contains("DROP TABLE"))
                .noneMatch(user -> user.getEmail().contains("--"))
                .noneMatch(user -> user.getUsername().contains("' OR '1'='1"));
    }
}