package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.*;
import com.focushive.identity.entity.User;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for AuthController.
 * Tests all authentication endpoints with various scenarios.
 */
@WebMvcTest(AuthController.class)
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private AuthenticationResponse authResponse;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // Setup valid request objects
        validRegisterRequest = new RegisterRequest();
        validRegisterRequest.setUsername("testuser");
        validRegisterRequest.setEmail("test@example.com");
        validRegisterRequest.setPassword("SecurePassword123!");
        validRegisterRequest.setFirstName("Test");
        validRegisterRequest.setLastName("User");

        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsernameOrEmail("testuser");
        validLoginRequest.setPassword("SecurePassword123!");

        // Setup mock user
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build();

        // Setup persona info
        AuthenticationResponse.PersonaInfo personaInfo = AuthenticationResponse.PersonaInfo.builder()
                .id(UUID.randomUUID())
                .name("Default Persona")
                .type("WORK")
                .isActive(true)
                .isDefault(true)
                .build();

        // Setup authentication response
        authResponse = AuthenticationResponse.builder()
                .accessToken("eyJhbGciOiJIUzI1NiJ9.test.token")
                .refreshToken("refresh.token.here")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(mockUser.getId())
                .username(mockUser.getUsername())
                .email(mockUser.getEmail())
                .personas(Collections.singletonList(personaInfo))
                .activePersona(personaInfo)
                .build();
    }

    // ===== REGISTRATION TESTS =====

    @Test
    @DisplayName("POST /register - Should successfully register new user")
    void testRegister_Success() throws Exception {
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", is(authResponse.getAccessToken())))
                .andExpect(jsonPath("$.refreshToken", is(authResponse.getRefreshToken())))
                .andExpect(jsonPath("$.username", is(authResponse.getUsername())))
                .andExpect(jsonPath("$.email", is(authResponse.getEmail())));

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /register - Should fail with invalid email")
    void testRegister_InvalidEmail() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("SecurePassword123!");
        invalidRequest.setFirstName("Test");
        invalidRequest.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    @Test
    @DisplayName("POST /register - Should fail with missing required fields")
    void testRegister_MissingFields() throws Exception {
        RegisterRequest incompleteRequest = new RegisterRequest();
        incompleteRequest.setUsername("testuser");
        // Missing email, password, etc.

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleteRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any());
    }

    // ===== LOGIN TESTS =====

    @Test
    @DisplayName("POST /login - Should successfully login with valid credentials")
    void testLogin_Success() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", is(authResponse.getAccessToken())))
                .andExpect(jsonPath("$.refreshToken", is(authResponse.getRefreshToken())))
                .andExpect(jsonPath("$.username", is(authResponse.getUsername())));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /login - Should fail with missing credentials")
    void testLogin_MissingCredentials() throws Exception {
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsernameOrEmail("testuser");
        // Missing password

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    @Test
    @DisplayName("POST /login - Should login with email instead of username")
    void testLogin_WithEmail() throws Exception {
        LoginRequest emailLoginRequest = new LoginRequest();
        emailLoginRequest.setUsernameOrEmail("test@example.com");
        emailLoginRequest.setPassword("SecurePassword123!");

        when(authenticationService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emailLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is(authResponse.getAccessToken())));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    // ===== TOKEN REFRESH TESTS =====

    @Test
    @DisplayName("POST /refresh - Should successfully refresh access token")
    void testRefresh_Success() throws Exception {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                .refreshToken("valid.refresh.token")
                .build();

        when(authenticationService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is(authResponse.getAccessToken())))
                .andExpect(jsonPath("$.refreshToken", is(authResponse.getRefreshToken())));

        verify(authenticationService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("POST /refresh - Should fail with missing refresh token")
    void testRefresh_MissingToken() throws Exception {
        RefreshTokenRequest invalidRequest = RefreshTokenRequest.builder()
                // Missing refresh token
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).refreshToken(any());
    }

    // ===== TOKEN VALIDATION TESTS =====

    @Test
    @DisplayName("POST /validate - Should successfully validate token")
    void testValidateToken_Success() throws Exception {
        ValidateTokenRequest validateRequest = ValidateTokenRequest.builder()
                .token("valid.jwt.token")
                .build();

        ValidateTokenResponse validateResponse = ValidateTokenResponse.builder()
                .valid(true)
                .userId(mockUser.getId())
                .username(mockUser.getUsername())
                .email(mockUser.getEmail())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authenticationService.validateToken(any(ValidateTokenRequest.class)))
                .thenReturn(validateResponse);

        mockMvc.perform(post("/api/v1/auth/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.userId", is(mockUser.getId().toString())))
                .andExpect(jsonPath("$.username", is(mockUser.getUsername())));

        verify(authenticationService, times(1)).validateToken(any(ValidateTokenRequest.class));
    }

    @Test
    @DisplayName("POST /validate - Should return invalid for expired token")
    void testValidateToken_Expired() throws Exception {
        ValidateTokenRequest validateRequest = ValidateTokenRequest.builder()
                .token("expired.jwt.token")
                .build();

        ValidateTokenResponse validateResponse = ValidateTokenResponse.builder()
                .valid(false)
                .build();

        when(authenticationService.validateToken(any(ValidateTokenRequest.class)))
                .thenReturn(validateResponse);

        mockMvc.perform(post("/api/v1/auth/validate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));

        verify(authenticationService, times(1)).validateToken(any(ValidateTokenRequest.class));
    }

    // ===== TOKEN INTROSPECTION TESTS =====

    @Test
    @DisplayName("POST /introspect - Should successfully introspect token")
    void testIntrospectToken_Success() throws Exception {
        IntrospectTokenRequest introspectRequest = IntrospectTokenRequest.builder()
                .token("valid.jwt.token")
                .build();

        IntrospectTokenResponse introspectResponse = IntrospectTokenResponse.builder()
                .active(true)
                .sub(mockUser.getUsername())
                .exp(Instant.now().plusSeconds(3600).getEpochSecond())
                .iat(Instant.now().getEpochSecond())
                .scope("read write")
                .clientId("focushive-web")
                .build();

        when(authenticationService.introspectToken(any(IntrospectTokenRequest.class)))
                .thenReturn(introspectResponse);

        mockMvc.perform(post("/api/v1/auth/introspect")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(introspectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.sub", is(mockUser.getUsername())))
                .andExpect(jsonPath("$.scope", is("read write")));

        verify(authenticationService, times(1)).introspectToken(any(IntrospectTokenRequest.class));
    }

    // ===== LOGOUT TESTS =====

    @Test
    @DisplayName("POST /logout - Should successfully logout authenticated user")
    @WithMockUser(username = "testuser")
    void testLogout_Success() throws Exception {
        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken("refresh.token.here")
                .build();

        doNothing().when(authenticationService).logout(any(LogoutRequest.class), any(User.class));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .with(user(mockUser))
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logged out successfully")));

        verify(authenticationService, times(1)).logout(any(LogoutRequest.class), any());
    }

    @Test
    @DisplayName("POST /logout - Should fail without authentication")
    void testLogout_Unauthenticated() throws Exception {
        LogoutRequest logoutRequest = LogoutRequest.builder()
                .refreshToken("refresh.token.here")
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());

        verify(authenticationService, never()).logout(any(), any());
    }

    // ===== PASSWORD RESET TESTS =====

    @Test
    @DisplayName("POST /password/reset-request - Should initiate password reset")
    void testPasswordResetRequest_Success() throws Exception {
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("test@example.com")
                .build();

        doNothing().when(authenticationService).requestPasswordReset(anyString());

        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", 
                    is("If an account exists with this email, a reset link has been sent.")));

        verify(authenticationService, times(1)).requestPasswordReset(anyString());
    }

    @Test
    @DisplayName("POST /password/reset-request - Should return same message for non-existent email")
    void testPasswordResetRequest_NonExistentEmail() throws Exception {
        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .email("nonexistent@example.com")
                .build();

        doNothing().when(authenticationService).requestPasswordReset(anyString());

        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", 
                    is("If an account exists with this email, a reset link has been sent.")));

        verify(authenticationService, times(1)).requestPasswordReset(anyString());
    }

    @Test
    @DisplayName("POST /password/reset - Should successfully reset password")
    void testPasswordReset_Success() throws Exception {
        PasswordResetConfirmRequest resetConfirmRequest = PasswordResetConfirmRequest.builder()
                .token("valid-reset-token")
                .newPassword("NewSecurePassword123!")
                .confirmPassword("NewSecurePassword123!")
                .build();

        doNothing().when(authenticationService).resetPassword(any(PasswordResetConfirmRequest.class));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetConfirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password has been successfully reset.")));

        verify(authenticationService, times(1)).resetPassword(any(PasswordResetConfirmRequest.class));
    }

    @Test
    @DisplayName("POST /password/reset - Should fail with mismatched passwords")
    void testPasswordReset_MismatchedPasswords() throws Exception {
        PasswordResetConfirmRequest invalidRequest = PasswordResetConfirmRequest.builder()
                .token("valid-reset-token")
                .newPassword("NewSecurePassword123!")
                .confirmPassword("DifferentPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).resetPassword(any());
    }

    // ===== PERSONA SWITCHING TESTS =====

    @Test
    @DisplayName("POST /personas/switch - Should successfully switch persona")
    @WithMockUser(username = "testuser")
    void testSwitchPersona_Success() throws Exception {
        SwitchPersonaRequest switchRequest = SwitchPersonaRequest.builder()
                .personaId(UUID.randomUUID())
                .build();

        when(authenticationService.switchPersona(any(SwitchPersonaRequest.class), any(User.class)))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/personas/switch")
                        .with(csrf())
                        .with(user(mockUser))
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is(authResponse.getAccessToken())))
                .andExpect(jsonPath("$.activePersona", notNullValue()));

        verify(authenticationService, times(1)).switchPersona(any(SwitchPersonaRequest.class), any());
    }

    @Test
    @DisplayName("POST /personas/switch - Should fail without authentication")
    void testSwitchPersona_Unauthenticated() throws Exception {
        SwitchPersonaRequest switchRequest = SwitchPersonaRequest.builder()
                .personaId(UUID.randomUUID())
                .build();

        mockMvc.perform(post("/api/v1/auth/personas/switch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isUnauthorized());

        verify(authenticationService, never()).switchPersona(any(), any());
    }

    @Test
    @DisplayName("POST /personas/switch - Should fail with invalid persona ID")
    void testSwitchPersona_InvalidPersonaId() throws Exception {
        SwitchPersonaRequest invalidRequest = SwitchPersonaRequest.builder()
                // Missing persona ID
                .build();

        mockMvc.perform(post("/api/v1/auth/personas/switch")
                        .with(csrf())
                        .with(user(mockUser))
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).switchPersona(any(), any());
    }

    // ===== ERROR HANDLING TESTS =====

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void testServiceException_Handling() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should reject malformed JSON")
    void testMalformedJson_Rejection() throws Exception {
        String malformedJson = "{ invalid json }";

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any());
    }

    @Test
    @DisplayName("Should reject requests without CSRF token")
    void testCsrfProtection() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        // No CSRF token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isForbidden());

        verify(authenticationService, never()).login(any());
    }
}