package com.focushive.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.backend.client.IdentityServiceClient;
import com.focushive.backend.client.dto.IdentityDto;
import com.focushive.backend.client.dto.PersonaDto;
import com.focushive.backend.client.dto.TokenValidationResponse;
import com.focushive.backend.service.IdentityIntegrationService;
import com.focushive.test.TestApplication;
import com.focushive.test.UnifiedTestConfig;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.focushive.backend.client.fallback.IdentityServiceFallback;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Identity Service communication.
 * Uses WireMock to simulate Identity Service responses.
 * 
 * STATUS: Test infrastructure successfully configured (WireMock + caching resolved).
 * Current issue: Feign client configuration conflict with TestMockConfig mocking.
 * The test runs but the Feign client returns null instead of WireMock responses.
 * This represents significant progress from the original Redis connection errors.
 */
@SpringBootTest(classes = TestApplication.class, properties = {
    "spring.cache.type=simple",  // Use simple in-memory cache instead of Redis
    "spring.cloud.openfeign.client.config.default.loggerLevel=FULL",
    "resilience4j.circuitbreaker.instances.identity-service.enabled=false",
    "resilience4j.retry.instances.identity-service.enabled=false"
})
@Import({IntegrationTestConfig.class})
@EnableFeignClients(clients = {com.focushive.backend.client.IdentityServiceClient.class})
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class IdentityServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private IdentityIntegrationService identityIntegrationService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @MockBean
    private IdentityServiceFallback identityServiceFallback;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(0); // Use dynamic port
        wireMockServer.start();
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String wireMockUrl = "http://localhost:" + wireMockServer.port();
        registry.add("identity.service.url", () -> wireMockUrl);
        
        // Circuit breaker and retry configuration for testing
        registry.add("resilience4j.circuitbreaker.instances.identity-service.minimum-number-of-calls", () -> 1);
        registry.add("resilience4j.circuitbreaker.instances.identity-service.sliding-window-size", () -> 2);
        registry.add("resilience4j.retry.instances.identity-service.max-attempts", () -> 1);
        
        // Logging to debug
        System.out.println("WireMock server configured at: " + wireMockUrl);
        System.out.println("Feign client will use identity.service.url = " + wireMockUrl);
    }
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final UUID testUserId = UUID.randomUUID();
    private final UUID testPersonaId = UUID.randomUUID();
    private final String testToken = "Bearer test-jwt-token";
    
    @BeforeEach
    void setUp() {
        // Reset WireMock stubs before each test
        wireMockServer.resetAll();
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
        
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/validate"))
                .withHeader("Authorization", equalTo(testToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expectedResponse))));
        
        // When
        System.out.println("Calling validateToken with: " + testToken);
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        System.out.println("Response received: " + response);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(testUserId);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getAuthorities()).containsExactly("ROLE_USER", "ROLE_STUDENT");
    }
    
    @Test
    @Order(2)
    void testValidateToken_Invalid() throws Exception {
        // Given
        TokenValidationResponse expectedResponse = TokenValidationResponse.builder()
                .valid(false)
                .errorMessage("Token expired")
                .build();
        
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/validate"))
                .withHeader("Authorization", equalTo(testToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expectedResponse))));
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).isEqualTo("Token expired");
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
        
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/auth/user"))
                .withHeader("Authorization", equalTo(testToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expectedIdentity))));
        
        // When
        IdentityDto result = identityIntegrationService.getCurrentUser(testToken);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserId);
        assertThat(result.getPrimaryEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.isEmailVerified()).isTrue();
        
        // Verify WireMock was called
        wireMockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/api/v1/auth/user")));
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
        
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/personas/identity/" + testUserId + "/active"))
                .withHeader("Authorization", equalTo(testToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(expectedPersona))));
        
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
        
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/personas/" + newPersonaId + "/activate"))
                .withHeader("Authorization", equalTo(testToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(activatedPersona))));
        
        // When
        PersonaDto activated = identityIntegrationService.activatePersona(newPersonaId, testUserId, testToken);
        
        // Then
        assertThat(activated).isNotNull();
        assertThat(activated.getId()).isEqualTo(newPersonaId);
        assertThat(activated.getName()).isEqualTo("Gaming Persona");
        assertThat(activated.getType()).isEqualTo("GAMING");
        assertThat(activated.isActive()).isTrue();
    }
    
    @Test
    @Order(6)
    void testHealthCheck_ServiceUp() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UP\",\"components\":{}}")));
        
        // When
        boolean isHealthy = identityIntegrationService.isIdentityServiceHealthy();
        
        // Then
        assertThat(isHealthy).isTrue();
    }
    
    @Test
    @Order(7)
    void testHealthCheck_ServiceDown() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/actuator/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"DOWN\",\"error\":\"Database connection failed\"}")));
        
        // When
        boolean isHealthy = identityIntegrationService.isIdentityServiceHealthy();
        
        // Then
        assertThat(isHealthy).isFalse();
    }
    
    @Test
    @Order(8)
    void testCircuitBreakerFallback() {
        // Given - Service returns error status
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/auth/validate"))
                .withHeader("Authorization", equalTo(testToken))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Service unavailable\"}")));
        
        // When
        TokenValidationResponse response = identityIntegrationService.validateToken(testToken);
        
        // Then - Should return fallback response
        assertThat(response).isNotNull();
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrorMessage()).contains("Failed to validate token");
    }
}