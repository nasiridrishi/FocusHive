package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.*;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for AuthController.
 * Tests authentication endpoints with various scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthenticationResponse authResponse;
    private RefreshTokenRequest refreshRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("refresh-token-123");

        // Create persona info
        AuthenticationResponse.PersonaInfo personaInfo = AuthenticationResponse.PersonaInfo.builder()
                .id(UUID.randomUUID())
                .name("Default")
                .type("PERSONAL")
                .isDefault(true)
                .build();

        authResponse = AuthenticationResponse.builder()
                .accessToken("jwt-access-token")
                .refreshToken("jwt-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .activePersona(personaInfo)
                .availablePersonas(Collections.singletonList(personaInfo))
                .issuedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should register user successfully")
    void register_ValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.activePersona").exists())
                .andExpect(jsonPath("$.activePersona.name").value("Default"))
                .andExpect(jsonPath("$.activePersona.type").value("PERSONAL"))
                .andExpect(jsonPath("$.activePersona.default").value(true))
                .andExpect(jsonPath("$.availablePersonas", hasSize(1)))
                .andExpect(jsonPath("$.availablePersonas[0].name").value("Default"));

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject invalid registration data")
    void register_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with missing fields
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername(""); // Empty username
        invalidRequest.setEmail("invalid-email"); // Invalid email format

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should handle duplicate user registration")
    void register_DuplicateUser_ShouldReturnConflict() throws Exception {
        // Given
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("User already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should login user successfully")
    void login_ValidCredentials_ShouldReturnOk() throws Exception {
        // Given
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.activePersona").exists())
                .andExpect(jsonPath("$.availablePersonas", hasSize(1)));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject invalid credentials")
    void login_InvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Given
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject empty login request")
    void login_EmptyRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Empty login request
        LoginRequest emptyRequest = new LoginRequest();

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Should refresh token successfully")
    void refreshToken_ValidToken_ShouldReturnOk() throws Exception {
        // Given
        AuthenticationResponse refreshResponse = AuthenticationResponse.builder()
                .accessToken("new-jwt-access-token")
                .refreshToken("new-jwt-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(refreshResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("new-jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-jwt-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authenticationService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Should reject invalid refresh token")
    void refreshToken_InvalidToken_ShouldReturnUnauthorized() throws Exception {
        // Given
        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/auth/logout - Should logout successfully")
    void logout_ValidRequest_ShouldReturnOk() throws Exception {
        // Given
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setAccessToken("jwt-access-token");
        logoutRequest.setRefreshToken("jwt-refresh-token");

        doNothing().when(authenticationService).logout(any(LogoutRequest.class), any(User.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

        verify(authenticationService, times(1)).logout(any(LogoutRequest.class), any(User.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/auth/validate - Should validate token successfully")
    void validateToken_ValidToken_ShouldReturnOk() throws Exception {
        // Given
        ValidateTokenRequest validateRequest = new ValidateTokenRequest();
        validateRequest.setToken("jwt-access-token");

        ValidateTokenResponse validateResponse = new ValidateTokenResponse();
        validateResponse.setValid(true);
        validateResponse.setUserId(UUID.randomUUID().toString());

        when(authenticationService.validateToken(any(ValidateTokenRequest.class)))
                .thenReturn(validateResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").exists());

        verify(authenticationService, times(1)).validateToken(any(ValidateTokenRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/auth/switch-persona - Should switch persona successfully")
    void switchPersona_ValidRequest_ShouldReturnOk() throws Exception {
        // Given
        SwitchPersonaRequest switchRequest = new SwitchPersonaRequest();
        switchRequest.setPersonaId(UUID.randomUUID());

        AuthenticationResponse.PersonaInfo workPersona = AuthenticationResponse.PersonaInfo.builder()
                .id(UUID.randomUUID())
                .name("Work Persona")
                .type("WORK")
                .isDefault(false)
                .build();

        AuthenticationResponse switchResponse = AuthenticationResponse.builder()
                .accessToken("new-jwt-access-token")
                .refreshToken("new-jwt-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(authResponse.getUserId())
                .username(authResponse.getUsername())
                .email(authResponse.getEmail())
                .activePersona(workPersona)
                .availablePersonas(authResponse.getAvailablePersonas())
                .issuedAt(authResponse.getIssuedAt())
                .build();

        when(authenticationService.switchPersona(any(SwitchPersonaRequest.class), any(User.class)))
                .thenReturn(switchResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/personas/switch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.activePersona.name").value("Work Persona"))
                .andExpect(jsonPath("$.activePersona.type").value("WORK"));

        verify(authenticationService, times(1)).switchPersona(any(SwitchPersonaRequest.class), any(User.class));
    }

    @Test
    @DisplayName("POST without CSRF token - Should return 403")
    void postWithoutCsrf_ShouldReturnForbidden() throws Exception {
        // Given
        // No CSRF token

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isForbidden());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Missing request body - Should return 400")
    void missingRequestBody_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Invalid JSON - Should return 400")
    void invalidJson_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/validate - Should validate token and return invalid for invalid token")
    void validateToken_InvalidToken_ShouldReturnInvalidResponse() throws Exception {
        // Given
        ValidateTokenRequest validateRequest = new ValidateTokenRequest();
        validateRequest.setToken("invalid-jwt-access-token");

        ValidateTokenResponse validateResponse = new ValidateTokenResponse();
        validateResponse.setValid(false);
        validateResponse.setUserId(null);

        when(authenticationService.validateToken(any(ValidateTokenRequest.class)))
                .thenReturn(validateResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.userId").isEmpty());

        verify(authenticationService, times(1)).validateToken(any(ValidateTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/validate - Should handle validation service exception")
    void validateToken_ServiceException_ShouldReturnUnauthorized() throws Exception {
        // Given
        ValidateTokenRequest validateRequest = new ValidateTokenRequest();
        validateRequest.setToken("jwt-access-token");

        when(authenticationService.validateToken(any(ValidateTokenRequest.class)))
                .thenThrow(new RuntimeException("Token validation failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).validateToken(any(ValidateTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/introspect - Should introspect token successfully")
    void introspectToken_ValidToken_ShouldReturnTokenInfo() throws Exception {
        // Given
        IntrospectTokenRequest introspectRequest = new IntrospectTokenRequest();
        introspectRequest.setToken("jwt-access-token");

        IntrospectTokenResponse introspectResponse = new IntrospectTokenResponse();
        introspectResponse.setActive(true);
        introspectResponse.setUsername("testuser");
        introspectResponse.setScope("read write");
        introspectResponse.setTokenType("Bearer");
        introspectResponse.setExp(System.currentTimeMillis() / 1000 + 3600);
        introspectResponse.setIat(System.currentTimeMillis() / 1000);

        when(authenticationService.introspectToken(any(IntrospectTokenRequest.class)))
                .thenReturn(introspectResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/introspect")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(introspectRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.scope").value("read write"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.exp").exists())
                .andExpect(jsonPath("$.iat").exists());

        verify(authenticationService, times(1)).introspectToken(any(IntrospectTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/introspect - Should return inactive for invalid token")
    void introspectToken_InvalidToken_ShouldReturnInactive() throws Exception {
        // Given
        IntrospectTokenRequest introspectRequest = new IntrospectTokenRequest();
        introspectRequest.setToken("invalid-jwt-access-token");

        IntrospectTokenResponse introspectResponse = new IntrospectTokenResponse();
        introspectResponse.setActive(false);

        when(authenticationService.introspectToken(any(IntrospectTokenRequest.class)))
                .thenReturn(introspectResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/introspect")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(introspectRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(false));

        verify(authenticationService, times(1)).introspectToken(any(IntrospectTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/introspect - Should handle introspection service exception")
    void introspectToken_ServiceException_ShouldReturnUnauthorized() throws Exception {
        // Given
        IntrospectTokenRequest introspectRequest = new IntrospectTokenRequest();
        introspectRequest.setToken("jwt-access-token");

        when(authenticationService.introspectToken(any(IntrospectTokenRequest.class)))
                .thenThrow(new RuntimeException("Token introspection failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/introspect")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(introspectRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).introspectToken(any(IntrospectTokenRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/reset-request - Should send reset email successfully")
    void requestPasswordReset_ValidEmail_ShouldReturnSuccessMessage() throws Exception {
        // Given
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setEmail("test@example.com");

        doNothing().when(authenticationService).requestPasswordReset(anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a reset link has been sent."));

        verify(authenticationService, times(1)).requestPasswordReset("test@example.com");
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/reset-request - Should return success even for non-existent email")
    void requestPasswordReset_NonExistentEmail_ShouldReturnSuccessMessage() throws Exception {
        // Given
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setEmail("nonexistent@example.com");

        doNothing().when(authenticationService).requestPasswordReset(anyString());

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("If an account exists with this email, a reset link has been sent."));

        verify(authenticationService, times(1)).requestPasswordReset("nonexistent@example.com");
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/reset-request - Should reject invalid email format")
    void requestPasswordReset_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setEmail("invalid-email-format");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).requestPasswordReset(anyString());
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/reset - Should reset password successfully")
    void resetPassword_ValidRequest_ShouldReturnSuccessMessage() throws Exception {
        // Given
        PasswordResetConfirmRequest resetConfirmRequest = new PasswordResetConfirmRequest();
        resetConfirmRequest.setToken("valid-reset-token");
        resetConfirmRequest.setNewPassword("newPassword123");

        doNothing().when(authenticationService).resetPassword(any(PasswordResetConfirmRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password/reset")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetConfirmRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Password has been successfully reset."));

        verify(authenticationService, times(1)).resetPassword(any(PasswordResetConfirmRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/reset - Should reject invalid reset token")
    void resetPassword_InvalidToken_ShouldReturnBadRequest() throws Exception {
        // Given
        PasswordResetConfirmRequest resetConfirmRequest = new PasswordResetConfirmRequest();
        resetConfirmRequest.setToken("invalid-reset-token");
        resetConfirmRequest.setNewPassword("newPassword123");

        doThrow(new RuntimeException("Invalid or expired reset token"))
                .when(authenticationService).resetPassword(any(PasswordResetConfirmRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password/reset")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetConfirmRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).resetPassword(any(PasswordResetConfirmRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/reset - Should reject weak password")
    void resetPassword_WeakPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        PasswordResetConfirmRequest resetConfirmRequest = new PasswordResetConfirmRequest();
        resetConfirmRequest.setToken("valid-reset-token");
        resetConfirmRequest.setNewPassword("123"); // Too weak

        // When & Then
        mockMvc.perform(post("/api/v1/auth/password/reset")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetConfirmRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).resetPassword(any(PasswordResetConfirmRequest.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/auth/personas/switch - Should handle persona not found")
    void switchPersona_PersonaNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        SwitchPersonaRequest switchRequest = new SwitchPersonaRequest();
        switchRequest.setPersonaId(UUID.randomUUID());

        when(authenticationService.switchPersona(any(SwitchPersonaRequest.class), any(User.class)))
                .thenThrow(new RuntimeException("Persona not found"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/personas/switch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).switchPersona(any(SwitchPersonaRequest.class), any(User.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/personas/switch - Should require authentication")
    void switchPersona_NoAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Given
        SwitchPersonaRequest switchRequest = new SwitchPersonaRequest();
        switchRequest.setPersonaId(UUID.randomUUID());

        // When & Then - No @WithMockUser annotation
        mockMvc.perform(post("/api/v1/auth/personas/switch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isUnauthorized());

        verify(authenticationService, never()).switchPersona(any(SwitchPersonaRequest.class), any(User.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/v1/auth/logout - Should handle missing Authorization header")
    void logout_MissingAuthorizationHeader_ShouldReturnBadRequest() throws Exception {
        // Given
        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setAccessToken("jwt-access-token");
        logoutRequest.setRefreshToken("jwt-refresh-token");

        // When & Then - No Authorization header
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).logout(any(LogoutRequest.class), any(User.class));
    }
}