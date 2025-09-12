package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.controller.AuthController;
import com.focushive.identity.dto.LoginRequest;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.service.AuthenticationService;
import com.focushive.identity.service.CookieJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication endpoints.
 * Tests the /api/v1/auth/login endpoint with mocked authentication service.
 * Following TDD approach - testing controller behavior independently of service implementation.
 * Uses @WebMvcTest to focus only on the web layer without loading the full application context.
 */
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
class AuthEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;
    
    @MockBean
    private CookieJwtService cookieJwtService;

    private LoginRequest validLoginRequest;
    private AuthenticationResponse successResponse;

    @BeforeEach
    void setUp() {
        // Create valid login request
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsernameOrEmail("testuser");
        validLoginRequest.setPassword("testpassword123");

        // Create success response for mocking
        successResponse = new AuthenticationResponse();
        successResponse.setAccessToken("mock-access-token");
        successResponse.setRefreshToken("mock-refresh-token");
        successResponse.setTokenType("Bearer");
        successResponse.setUserId(UUID.randomUUID());
        successResponse.setUsername("testuser");
        successResponse.setEmail("test@example.com");
    }

    @Test
    void shouldReturnOk_WhenValidCredentialsProvided() throws Exception {
        // Step 1: Mock the authentication service to return success response
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(successResponse);

        // Step 2: Perform POST request to login endpoint
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Step 3: Verify response body
        String responseContent = result.getResponse().getContentAsString();
        AuthenticationResponse response = objectMapper.readValue(responseContent, AuthenticationResponse.class);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnUnauthorized_WhenInvalidCredentialsProvided() throws Exception {
        // Step 1: Create invalid credentials request
        LoginRequest invalidLoginRequest = new LoginRequest();
        invalidLoginRequest.setUsernameOrEmail("testuser");
        invalidLoginRequest.setPassword("wrongpassword");

        // Step 2: Mock the authentication service to throw BadCredentialsException
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        // Step 3: Perform POST request to login endpoint
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Step 4: Verify response contains appropriate error information
        String responseContent = result.getResponse().getContentAsString();
        assertThat(responseContent).isNotNull();
        
        // The exact error response format depends on the exception handler implementation
        // For now, we verify that we get a 401 status code
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}