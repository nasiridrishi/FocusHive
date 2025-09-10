package com.focushive.backend.integration;

import com.focushive.backend.client.IdentityServiceClient;
import com.focushive.backend.client.dto.IdentityDto;
import com.focushive.backend.client.dto.PersonaDto;
import com.focushive.backend.client.dto.TokenValidationResponse;
import com.focushive.backend.service.IdentityIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Identity Service integration.
 * Uses Mockito to simulate Identity Service responses.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class IdentityServiceIntegrationTest {

    @Mock
    private IdentityServiceClient identityServiceClient;

    @InjectMocks
    private IdentityIntegrationService identityIntegrationService;
    
    private final UUID testUserId = UUID.randomUUID();
    private final UUID testPersonaId = UUID.randomUUID();
    private final String testToken = "Bearer test-jwt-token";
    
    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(identityServiceClient);
    }
    
    @Test
    @Order(1)
    void testValidateToken_Success() throws Exception {
        // Given
        TokenValidationResponse expectedResponse = TokenValidationResponse.builder()
                .valid(true)
                .userId(testUserId)
                .email("test@example.com")
                .authorities(List.of("ROLE_USER", "ROLE_STUDENT"))
                .activePersonaId(testPersonaId.toString())
                .build();
        
        when(identityServiceClient.validateToken(testToken)).thenReturn(expectedResponse);
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAuthorities()).containsExactly("ROLE_USER", "ROLE_STUDENT");
        
        verify(identityServiceClient).validateToken(testToken);
    }
    
    @Test
    @Order(2)
    void testValidateToken_Invalid() throws Exception {
        // Given
        TokenValidationResponse expectedResponse = TokenValidationResponse.builder()
                .valid(false)
                .errorMessage("Token expired")
                .build();
        
        when(identityServiceClient.validateToken(testToken)).thenReturn(expectedResponse);
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Token expired");
        
        verify(identityServiceClient).validateToken(testToken);
    }
    
    @Test
    @Order(3)
    void testGetCurrentUser() throws Exception {
        // Given
        IdentityDto expectedIdentity = IdentityDto.builder()
                .id(testUserId)
                .primaryEmail("test@example.com")
                .username("testuser")
                .emailVerified(true)
                .activePersonaId(testPersonaId)
                .build();
        
        when(identityServiceClient.getCurrentUser(testToken)).thenReturn(expectedIdentity);
        
        // When
        IdentityDto result = identityIntegrationService.getCurrentUser(testToken);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getPrimaryEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isEmailVerified()).isTrue();
        
        verify(identityServiceClient).getCurrentUser(testToken);
    }
    
    @Test
    @Order(4)
    void testGetActivePersona() throws Exception {
        // Given
        PersonaDto expectedPersona = PersonaDto.builder()
                .id(testPersonaId)
                .identityId(testUserId)
                .name("Work Persona")
                .type("WORK")
                .displayName("Professional Me")
                .isActive(true)
                .isDefault(false)
                .build();
        
        when(identityServiceClient.getActivePersona(testUserId, testToken)).thenReturn(expectedPersona);
        
        // When
        PersonaDto persona = identityIntegrationService.getActivePersona(testUserId, testToken);
        
        // Then
        assertThat(persona).isNotNull();
        assertThat(persona.getId()).isEqualTo(testPersonaId);
        assertThat(persona.getName()).isEqualTo("Work Persona");
        assertThat(persona.getType()).isEqualTo("WORK");
        assertThat(persona.isActive()).isTrue();
        
        verify(identityServiceClient).getActivePersona(testUserId, testToken);
    }
    
    @Test
    @Order(5)
    void testActivatePersona() throws Exception {
        // Given
        UUID newPersonaId = UUID.randomUUID();
        PersonaDto activatedPersona = PersonaDto.builder()
                .id(newPersonaId)
                .identityId(testUserId)
                .name("Gaming Persona")
                .type("GAMING")
                .isActive(true)
                .build();
        
        when(identityServiceClient.activatePersona(newPersonaId, testToken)).thenReturn(activatedPersona);
        
        // When
        PersonaDto activated = identityIntegrationService.activatePersona(newPersonaId, testUserId, testToken);
        
        // Then
        assertThat(activated).isNotNull();
        assertThat(activated.getId()).isEqualTo(newPersonaId);
        assertThat(activated.getName()).isEqualTo("Gaming Persona");
        assertThat(activated.getType()).isEqualTo("GAMING");
        assertThat(activated.isActive()).isTrue();
        
        verify(identityServiceClient).activatePersona(newPersonaId, testToken);
    }
    
    @Test
    @Order(6)
    void testHealthCheck_ServiceUp() {
        // Given
        when(identityServiceClient.healthCheck()).thenReturn("{\"status\":\"UP\",\"components\":{}}");
        
        // When
        boolean isHealthy = identityIntegrationService.isIdentityServiceHealthy();
        
        // Then
        assertThat(isHealthy).isTrue();
        
        verify(identityServiceClient).healthCheck();
    }
    
    @Test
    @Order(7)
    void testHealthCheck_ServiceDown() {
        // Given
        when(identityServiceClient.healthCheck()).thenReturn("{\"status\":\"DOWN\",\"error\":\"Database connection failed\"}");
        
        // When
        boolean isHealthy = identityIntegrationService.isIdentityServiceHealthy();
        
        // Then
        assertThat(isHealthy).isFalse();
        
        verify(identityServiceClient).healthCheck();
    }
    
    @Test
    @Order(8)
    void testCircuitBreakerFallback() {
        // Given - Service throws exception
        when(identityServiceClient.validateToken(testToken)).thenThrow(new RuntimeException("Service unavailable"));
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then - Should return fallback response
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).contains("Failed to validate token");
        
        verify(identityServiceClient).validateToken(testToken);
    }
}