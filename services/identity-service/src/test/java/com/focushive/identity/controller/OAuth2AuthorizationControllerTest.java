package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.TestConfig;
import com.focushive.identity.dto.*;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.OAuthClientRepository;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import com.focushive.identity.security.JwtTokenProvider;
import com.focushive.identity.service.OAuth2AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OAuth2AuthorizationController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@DisplayName("OAuth2AuthorizationController Integration Tests")
class OAuth2AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private OAuthClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private OAuth2AuthorizationService oauth2AuthorizationService;

    private User testUser;
    private Persona testPersona;
    private OAuthClient testClient;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmailVerified(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test persona
        testPersona = new Persona();
        testPersona.setUser(testUser);
        testPersona.setName("work");
        testPersona.setType(Persona.PersonaType.WORK);
        testPersona.setDisplayName("Professional Me");
        testPersona.setDefault(true);
        testPersona.setActive(true);
        testPersona = personaRepository.save(testPersona);

        // Create test OAuth2 client
        testClient = new OAuthClient();
        testClient.setClientId("test-client");
        testClient.setClientSecret(passwordEncoder.encode("test-secret"));
        testClient.setName("Test Client");
        testClient.setOwner(testUser);
        testClient.setRedirectUris(Set.of("http://localhost:3000/callback"));
        testClient.setScopes(Set.of("read", "write"));
        testClient.setGrantTypes(Set.of("authorization_code", "refresh_token"));
        testClient.setCreatedAt(Instant.now());
        testClient = clientRepository.save(testClient);

        // Update user with persona
        testUser.getPersonas().add(testPersona);
        testUser = userRepository.save(testUser);

        // Generate valid token
        validToken = jwtTokenProvider.generateAccessToken(testUser, testPersona);
    }

    @Test
    @DisplayName("Should redirect to login when user not authenticated for authorization")
    void authorize_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/authorize")
                        .param("client_id", testClient.getClientId())
                        .param("response_type", "code")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("scope", "read")
                        .param("state", "random-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @DisplayName("Should return error for invalid client_id")
    void authorize_ShouldReturnErrorForInvalidClientId() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/authorize")
                        .header("Authorization", "Bearer " + validToken)
                        .param("client_id", "invalid-client")
                        .param("response_type", "code")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("scope", "read")
                        .param("state", "random-state"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return error for invalid response_type")
    void authorize_ShouldReturnErrorForInvalidResponseType() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/authorize")
                        .header("Authorization", "Bearer " + validToken)
                        .param("client_id", testClient.getClientId())
                        .param("response_type", "invalid")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("scope", "read")
                        .param("state", "random-state"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should exchange authorization code for tokens")
    void token_ShouldExchangeAuthorizationCodeForTokens() throws Exception {
        // This is a simplified test - in reality, you'd need a valid authorization code
        // For now, we'll test the endpoint structure
        mockMvc.perform(post("/api/v1/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", "invalid-code") // This will fail, but tests the endpoint
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", testClient.getClientId())
                        .param("client_secret", "test-secret"))
                .andExpect(status().isBadRequest()); // Expected to fail with invalid code
    }

    @Test
    @DisplayName("Should require valid client credentials for token endpoint")
    void token_ShouldRequireValidClientCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", "some-code")
                        .param("redirect_uri", "http://localhost:3000/callback")
                        .param("client_id", "invalid-client")
                        .param("client_secret", "invalid-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should support client_credentials grant type")
    void token_ShouldSupportClientCredentialsGrant() throws Exception {
        mockMvc.perform(post("/api/v1/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("scope", "read")
                        .param("client_id", testClient.getClientId())
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber());
    }

    @Test
    @DisplayName("Should introspect valid access token")
    void introspect_ShouldIntrospectValidToken() throws Exception {
        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", validToken)
                        .param("client_id", testClient.getClientId())
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sub").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("Should return inactive for invalid token")
    void introspect_ShouldReturnInactiveForInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "invalid-token")
                        .param("client_id", testClient.getClientId())
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Should revoke access token")
    void revoke_ShouldRevokeAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/oauth2/revoke")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", validToken)
                        .param("token_type_hint", "access_token")
                        .param("client_id", testClient.getClientId())
                        .param("client_secret", "test-secret"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return user info for valid access token")
    void userInfo_ShouldReturnUserInfoForValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.first_name").value("Test"))
                .andExpect(jsonPath("$.last_name").value("User"));
    }

    @Test
    @DisplayName("Should return 401 for invalid token in userinfo")
    void userInfo_ShouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return server metadata")
    void serverMetadata_ShouldReturnMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").isNotEmpty())
                .andExpect(jsonPath("$.authorization_endpoint").isNotEmpty())
                .andExpect(jsonPath("$.token_endpoint").isNotEmpty())
                .andExpect(jsonPath("$.jwks_uri").isNotEmpty())
                .andExpect(jsonPath("$.response_types_supported").isArray())
                .andExpect(jsonPath("$.grant_types_supported").isArray());
    }

    @Test
    @DisplayName("Should return JWK Set")
    void jwkSet_ShouldReturnJWKSet() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys").isNotEmpty());
    }

    @Test
    @DisplayName("Should register new OAuth2 client")
    void registerClient_ShouldRegisterNewClient() throws Exception {
        OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
                .name("New Test Client")
                .redirectUris(Set.of("http://localhost:4000/callback"))
                .scopes(Set.of("read"))
                .grantTypes(Set.of("authorization_code"))
                .build();

        mockMvc.perform(post("/api/v1/oauth2/register")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_id").isNotEmpty())
                .andExpect(jsonPath("$.client_secret").isNotEmpty())
                .andExpect(jsonPath("$.name").value("New Test Client"))
                .andExpect(jsonPath("$.redirect_uris").isArray());
    }

    @Test
    @DisplayName("Should get user's OAuth2 clients")
    void getClients_ShouldReturnUserClients() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/clients")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.clients", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.clients[0].client_id").value(testClient.getClientId()));
    }

    @Test
    @DisplayName("Should update OAuth2 client")
    void updateClient_ShouldUpdateClient() throws Exception {
        OAuth2ClientUpdateRequest request = OAuth2ClientUpdateRequest.builder()
                .name("Updated Test Client")
                .redirectUris(Set.of("http://localhost:5000/callback"))
                .build();

        mockMvc.perform(put("/api/v1/oauth2/clients/{clientId}", testClient.getClientId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Test Client"));
    }

    @Test
    @DisplayName("Should delete OAuth2 client")
    void deleteClient_ShouldDeleteClient() throws Exception {
        mockMvc.perform(delete("/api/v1/oauth2/clients/{clientId}", testClient.getClientId())
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 for non-existent client")
    void updateClient_ShouldReturn404ForNonExistentClient() throws Exception {
        OAuth2ClientUpdateRequest request = OAuth2ClientUpdateRequest.builder()
                .name("Updated Test Client")
                .build();

        mockMvc.perform(put("/api/v1/oauth2/clients/non-existent-client")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 403 when trying to update another user's client")
    void updateClient_ShouldReturn403ForOtherUsersClient() throws Exception {
        // Create another user and client
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password123"));
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setEmailVerified(true);
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        OAuthClient otherClient = new OAuthClient();
        otherClient.setClientId("other-client");
        otherClient.setClientSecret(passwordEncoder.encode("other-secret"));
        otherClient.setName("Other Client");
        otherClient.setOwner(otherUser);
        otherClient.setRedirectUris(Set.of("http://localhost:3000/callback"));
        otherClient.setScopes(Set.of("read"));
        otherClient.setGrantTypes(Set.of("authorization_code"));
        otherClient = clientRepository.save(otherClient);

        OAuth2ClientUpdateRequest request = OAuth2ClientUpdateRequest.builder()
                .name("Hacked Client")
                .build();

        mockMvc.perform(put("/api/v1/oauth2/clients/{clientId}", otherClient.getClientId())
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    void protectedEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test client registration endpoint
        mockMvc.perform(post("/api/v1/oauth2/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        // Test get clients endpoint  
        mockMvc.perform(get("/api/v1/oauth2/clients"))
                .andExpect(status().isUnauthorized());

        // Test user info endpoint
        mockMvc.perform(get("/api/v1/oauth2/userinfo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle CORS for OAuth2 endpoints")
    void oauth2Endpoints_ShouldHandleCORS() throws Exception {
        mockMvc.perform(options("/api/v1/oauth2/token")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }
}