package com.focushive.api.security;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.api.config.SimpleRateLimitingFilter;
import com.focushive.api.dto.identity.PersonaDto;
import com.focushive.api.dto.identity.TokenValidationResponse;
import com.focushive.api.service.IdentityIntegrationService;
import com.focushive.backend.security.IdentityServiceAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Test to verify authentication delegation to Identity Service works correctly.
 * This test should initially FAIL because authentication is disabled.
 * After fixing the configuration, this test should PASS.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("authtest")
@Import(TestSecurityConfiguration.class)
public class IdentityServiceAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityServiceClient identityServiceClient;

    @MockBean
    private IdentityIntegrationService identityIntegrationService;

    @MockBean
    private IdentityServiceAuthenticationFilter identityServiceAuthenticationFilter;

    @MockBean
    private SimpleRateLimitingFilter simpleRateLimitingFilter;

    private static final String VALID_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    private static final String INVALID_JWT = "invalid.jwt.token";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USERNAME = "testuser";
    private static final String EMAIL = "testuser@focushive.com";

    @BeforeEach
    void setUp() {
        // Setup valid token response
        TokenValidationResponse validResponse = new TokenValidationResponse();
        validResponse.setValid(true);
        validResponse.setUserId(USER_ID);
        validResponse.setUsername(USERNAME);
        validResponse.setEmail(EMAIL);
        validResponse.setAuthorities(Arrays.asList("ROLE_USER"));

        PersonaDto persona = new PersonaDto();
        persona.setId(UUID.randomUUID());
        persona.setName("Default");
        persona.setType(PersonaDto.PersonaType.PERSONAL);
        validResponse.setActivePersona(persona);

        // Setup invalid token response
        TokenValidationResponse invalidResponse = new TokenValidationResponse();
        invalidResponse.setValid(false);
        invalidResponse.setErrorMessage("Token is expired");

        // Mock the service calls
        when(identityServiceClient.validateToken("Bearer " + VALID_JWT))
            .thenReturn(validResponse);
        when(identityIntegrationService.validateToken("Bearer " + VALID_JWT))
            .thenReturn(validResponse);

        when(identityServiceClient.validateToken("Bearer " + INVALID_JWT))
            .thenReturn(invalidResponse);
        when(identityIntegrationService.validateToken("Bearer " + INVALID_JWT))
            .thenReturn(invalidResponse);
    }

    @Test
    @DisplayName("Should allow access to protected endpoint with valid JWT from Identity Service")
    void testValidJwtAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/hives")
                .header("Authorization", "Bearer " + VALID_JWT))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access to protected endpoint with invalid JWT")
    void testInvalidJwtAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/hives")
                .header("Authorization", "Bearer " + INVALID_JWT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access to protected endpoint without JWT")
    void testNoJwtAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/hives"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow access to public endpoint without JWT")
    void testPublicEndpointAccess() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should extract user information from validated JWT")
    void testUserInfoExtraction() throws Exception {
        mockMvc.perform(get("/api/v1/presence/status")
                .header("Authorization", "Bearer " + VALID_JWT))
                .andExpect(status().isOk());
    }
}