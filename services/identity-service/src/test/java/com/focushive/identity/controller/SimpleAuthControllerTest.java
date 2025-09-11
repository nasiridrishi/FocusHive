package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.RegisterRequest;
import com.focushive.identity.dto.AuthenticationResponse;
import com.focushive.identity.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simple test for AuthController to validate basic functionality.
 */
@WebMvcTest(AuthController.class)
class SimpleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;
    private AuthenticationResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

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
    void register_ShouldWork() throws Exception {
        // Given
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
    }
}