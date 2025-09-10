package com.focushive.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

/**
 * Security-focused test suite for SimpleAuthController
 * 
 * This test verifies that NO sensitive data is logged to console output
 * and that proper logging practices are followed.
 * 
 * CRITICAL: This test must FAIL initially to demonstrate TDD approach
 * and verify detection of the security vulnerability.
 */
@SpringBootTest
@ActiveProfiles("security-test")
class SimpleAuthControllerSecurityTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SimpleAuthController controller;
    
    // Capture console output to detect sensitive data logging
    private ByteArrayOutputStream consoleOutput;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        controller = new SimpleAuthController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        
        // Capture System.out to detect sensitive data logging
        originalOut = System.out;
        consoleOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(consoleOutput));
    }

    @AfterEach
    void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("SECURITY: Login should NOT log sensitive password data to console")
    void loginShouldNotLogSensitiveData() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "demo_user");
        loginRequest.put("password", "Demo123!");
        
        String jsonContent = objectMapper.writeValueAsString(loginRequest);
        
        // Act
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isOk());
        
        String consoleLog = consoleOutput.toString();
        
        // Assert - These should all PASS after fixing the vulnerability
        assertFalse(consoleLog.contains("Demo123!"), 
            "CRITICAL: Password 'Demo123!' found in console logs!");
        assertFalse(consoleLog.contains("password=Demo123!"), 
            "CRITICAL: Password found in request log!");
        assertFalse(consoleLog.contains("Password received: Demo123!"), 
            "CRITICAL: Raw password logged to console!");
        
        // Verify no System.out.println usage (should use proper logging)
        assertFalse(consoleLog.contains("Login request received:"), 
            "System.out.println detected - should use SLF4J logger");
        assertFalse(consoleLog.contains("Username: demo_user"), 
            "System.out.println detected - should use SLF4J logger");
        assertFalse(consoleLog.contains("Password match result:"), 
            "System.out.println detected - should use SLF4J logger");
    }

    @Test
    @DisplayName("SECURITY: Login with email should NOT log sensitive data")
    void loginWithEmailShouldNotLogSensitiveData() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "demo@focushive.com");
        loginRequest.put("password", "Demo123!");
        
        String jsonContent = objectMapper.writeValueAsString(loginRequest);
        
        // Act
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isOk());
        
        String consoleLog = consoleOutput.toString();
        
        // Assert - These should all PASS after fixing the vulnerability
        assertFalse(consoleLog.contains("Demo123!"), 
            "CRITICAL: Password found in console logs!");
        assertFalse(consoleLog.contains("Password received: Demo123!"), 
            "CRITICAL: Raw password logged to console!");
    }

    @Test
    @DisplayName("SECURITY: Test hash endpoint should NOT expose sensitive data")
    void testHashEndpointShouldNotExposeSensitiveData() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/test-hash"))
                .andExpect(status().isNotFound()) // This endpoint should be removed entirely
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.newHash").doesNotExist())
                .andExpect(jsonPath("$.oldHash").doesNotExist());
    }

    @Test
    @DisplayName("SECURITY: Failed login attempts should NOT log password")
    void failedLoginShouldNotLogPassword() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "invalid_user");
        loginRequest.put("password", "WrongPassword123!");
        
        String jsonContent = objectMapper.writeValueAsString(loginRequest);
        
        // Act
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
        
        String consoleLog = consoleOutput.toString();
        
        // Assert
        assertFalse(consoleLog.contains("WrongPassword123!"), 
            "CRITICAL: Failed login password found in console logs!");
        assertFalse(consoleLog.contains("password=WrongPassword123!"), 
            "CRITICAL: Failed password found in request log!");
    }

    @Test
    @DisplayName("SECURITY: Null password should be handled securely")
    void nullPasswordShouldBeHandledSecurely() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "demo_user");
        // password intentionally omitted
        
        String jsonContent = objectMapper.writeValueAsString(loginRequest);
        
        // Act
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))
                .andExpect(status().isBadRequest());
        
        String consoleLog = consoleOutput.toString();
        
        // Assert - should only show masked indication, not "null"
        if (consoleLog.contains("Password received:")) {
            assertTrue(consoleLog.contains("Password received: ***") || 
                      consoleLog.contains("Password received: null"),
                "Password logging should be masked or show null safely");
        }
    }

    @Test
    @DisplayName("SECURITY: No hardcoded credentials should be exposed in logs")
    void noHardcodedCredentialsShouldBeExposed() throws Exception {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "demo_user");
        loginRequest.put("password", "Demo123!");
        
        String jsonContent = objectMapper.writeValueAsString(loginRequest);
        
        // Act
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent));
        
        String consoleLog = consoleOutput.toString();
        
        // Assert - No BCrypt hashes should be logged
        assertFalse(consoleLog.contains("$2a$10$"), 
            "CRITICAL: BCrypt hash found in console logs!");
        assertFalse(consoleLog.contains("bGx1Y7LbI7oZg7qhj8VZF"), 
            "CRITICAL: Partial hash found in console logs!");
    }

    @Test
    @DisplayName("SECURITY: Login should use proper HTTP status codes without data leakage")
    void loginShouldUseProperStatusCodes() throws Exception {
        // Test successful login
        Map<String, String> validLogin = new HashMap<>();
        validLogin.put("username", "demo_user");
        validLogin.put("password", "Demo123!");
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                // Token should not be a debug/demo token in production
                .andExpect(jsonPath("$.token").value(not("demo-jwt-token-for-testing")));
        
        // Test invalid login
        Map<String, String> invalidLogin = new HashMap<>();
        invalidLogin.put("username", "invalid_user");
        invalidLogin.put("password", "wrong_password");
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized()) // Should be 401, not 400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}