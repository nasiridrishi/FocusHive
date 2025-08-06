package com.focushive.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.backend.client.IdentityServiceClient;
import com.focushive.backend.client.dto.IdentityDto;
import com.focushive.backend.client.dto.PersonaDto;
import com.focushive.backend.client.dto.TokenValidationResponse;
import com.focushive.backend.service.IdentityIntegrationService;
import com.focushive.test.TestApplication;
import com.focushive.test.UnifiedTestConfig;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Identity Service communication.
 * Uses WireMock to simulate Identity Service responses.
 */
@SpringBootTest(classes = TestApplication.class)
@Import(UnifiedTestConfig.class)
@ActiveProfiles("test")
@WireMockTest(httpPort = 8081)
class IdentityServiceIntegrationTest {

    @Autowired
    private IdentityIntegrationService identityIntegrationService;
    
    @MockBean
    private IdentityServiceClient identityServiceClient;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final UUID testUserId = UUID.randomUUID();
    private final UUID testPersonaId = UUID.randomUUID();
    private final String testToken = "Bearer test-jwt-token";
    
    @BeforeEach
    void setUp() {
        // Clear caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> 
            cacheManager.getCache(cacheName).clear()
        );
    }
    
    @Test
    void testValidateToken_Success() throws Exception {
        // Given
        TokenValidationResponse expectedResponse = TokenValidationResponse.builder()
                .valid(true)
                .userId(testUserId)
                .email("test@example.com")
                .authorities(List.of("ROLE_USER", "ROLE_STUDENT"))
                .activePersonaId(testPersonaId.toString())
                .build();
        
        when(identityServiceClient.validateToken(testToken))
                .thenReturn(expectedResponse);
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAuthorities()).containsExactly("ROLE_USER", "ROLE_STUDENT");
    }
    
    @Test
    void testValidateToken_Invalid() {
        // Given
        TokenValidationResponse expectedResponse = TokenValidationResponse.builder()
                .valid(false)
                .errorMessage("Token expired")
                .build();
        
        when(identityServiceClient.validateToken(testToken))
                .thenReturn(expectedResponse);
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Token expired");
    }
    
    @Test
    void testGetCurrentUser_Cached() {
        // Given
        IdentityDto expectedIdentity = IdentityDto.builder()
                .id(testUserId)
                .primaryEmail("test@example.com")
                .username("testuser")
                .emailVerified(true)
                .activePersonaId(testPersonaId)
                .build();
        
        when(identityServiceClient.getCurrentUser(testToken))
                .thenReturn(expectedIdentity);
        
        // When - First call should hit the service
        IdentityDto firstCall = identityIntegrationService.getCurrentUser(testToken);
        
        // When - Second call should be cached
        IdentityDto secondCall = identityIntegrationService.getCurrentUser(testToken);
        
        // Then
        assertThat(firstCall).isNotNull();
        assertThat(firstCall.getId()).isEqualTo(testUserId);
        assertThat(secondCall).isEqualTo(firstCall);
    }
    
    @Test
    void testGetActivePersona() {
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
        
        when(identityServiceClient.getActivePersona(testUserId, testToken))
                .thenReturn(expectedPersona);
        
        // When
        PersonaDto persona = identityIntegrationService.getActivePersona(testUserId, testToken);
        
        // Then
        assertThat(persona).isNotNull();
        assertThat(persona.getId()).isEqualTo(testPersonaId);
        assertThat(persona.getName()).isEqualTo("Work Persona");
        assertThat(persona.getType()).isEqualTo("WORK");
        assertThat(persona.isActive()).isTrue();
    }
    
    @Test
    void testActivatePersona_ClearsCache() {
        // Given
        UUID newPersonaId = UUID.randomUUID();
        PersonaDto activatedPersona = PersonaDto.builder()
                .id(newPersonaId)
                .identityId(testUserId)
                .name("Gaming Persona")
                .type("GAMING")
                .isActive(true)
                .build();
        
        // Pre-populate cache
        PersonaDto oldPersona = PersonaDto.builder()
                .id(testPersonaId)
                .identityId(testUserId)
                .name("Work Persona")
                .type("WORK")
                .isActive(true)
                .build();
        
        when(identityServiceClient.getActivePersona(testUserId, testToken))
                .thenReturn(oldPersona)
                .thenReturn(activatedPersona);
        
        when(identityServiceClient.activatePersona(newPersonaId, testToken))
                .thenReturn(activatedPersona);
        
        // When - Get current active persona (should cache)
        PersonaDto cachedPersona = identityIntegrationService.getActivePersona(testUserId, testToken);
        assertThat(cachedPersona.getId()).isEqualTo(testPersonaId);
        
        // When - Activate new persona (should clear cache)
        PersonaDto activated = identityIntegrationService.activatePersona(newPersonaId, testUserId, testToken);
        
        // Then
        assertThat(activated).isNotNull();
        assertThat(activated.getId()).isEqualTo(newPersonaId);
        assertThat(activated.getName()).isEqualTo("Gaming Persona");
        
        // Verify cache was cleared by checking next call hits service
        PersonaDto newActive = identityIntegrationService.getActivePersona(testUserId, testToken);
        assertThat(newActive.getId()).isEqualTo(newPersonaId);
    }
    
    @Test
    void testHealthCheck_ServiceUp() {
        // Given
        when(identityServiceClient.healthCheck())
                .thenReturn("{\"status\":\"UP\",\"components\":{}}");
        
        // When
        boolean isHealthy = identityIntegrationService.isIdentityServiceHealthy();
        
        // Then
        assertThat(isHealthy).isTrue();
    }
    
    @Test
    void testHealthCheck_ServiceDown() {
        // Given
        when(identityServiceClient.healthCheck())
                .thenReturn("{\"status\":\"DOWN\",\"error\":\"Database connection failed\"}");
        
        // When
        boolean isHealthy = identityIntegrationService.isIdentityServiceHealthy();
        
        // Then
        assertThat(isHealthy).isFalse();
    }
    
    @Test
    void testCircuitBreakerFallback() {
        // Given - Service throws exception
        when(identityServiceClient.validateToken(anyString()))
                .thenThrow(new RuntimeException("Service unavailable"));
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then - Should return fallback response
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).contains("Failed to validate token");
    }
}