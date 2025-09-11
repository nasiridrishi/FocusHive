package com.focushive.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.dto.*;
import com.focushive.identity.service.OAuth2AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Comprehensive tests for OAuth2AuthorizationController.
 * Tests all OAuth2 endpoints including authorization, token exchange, introspection, and client management.
 */
@WebMvcTest(controllers = OAuth2AuthorizationController.class)
@Import(OAuth2AuthorizationControllerTest.TestConfig.class)
@ActiveProfiles("test")
@DisplayName("OAuth2AuthorizationController Tests")
class OAuth2AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OAuth2AuthorizationService oauth2AuthorizationService;

    private OAuth2TokenResponse mockTokenResponse;
    private OAuth2IntrospectionResponse mockIntrospectionResponse;
    private OAuth2UserInfoResponse mockUserInfoResponse;
    private OAuth2ServerMetadata mockServerMetadata;
    private OAuth2ClientResponse mockClientResponse;
    private OAuth2ClientListResponse mockClientListResponse;
    private Map<String, Object> mockJwkSet;

    @BeforeEach
    void setUp() {
        // Reset mock before each test
        Mockito.reset(oauth2AuthorizationService);
        
        
        // Mock token response
        mockTokenResponse = OAuth2TokenResponse.builder()
                .accessToken("access_token_123")
                .tokenType("Bearer")
                .expiresIn(3600)
                .refreshToken("refresh_token_123")
                .scope("openid profile email")
                .build();

        // Mock introspection response
        mockIntrospectionResponse = OAuth2IntrospectionResponse.builder()
                .active(true)
                .scope("openid profile")
                .clientId("test-client")
                .username("testuser")
                .tokenType("Bearer")
                .exp(1234567890L)
                .build();

        // Mock user info response
        mockUserInfoResponse = OAuth2UserInfoResponse.builder()
                .sub("user-123")
                .email("test@example.com")
                .emailVerified(true)
                .name("Test User")
                .givenName("Test")
                .familyName("User")
                .build();

        // Mock server metadata
        mockServerMetadata = OAuth2ServerMetadata.builder()
                .issuer("http://localhost:8081")
                .authorizationEndpoint("http://localhost:8081/api/v1/oauth2/authorize")
                .tokenEndpoint("http://localhost:8081/api/v1/oauth2/token")
                .userinfoEndpoint("http://localhost:8081/api/v1/oauth2/userinfo")
                .jwksUri("http://localhost:8081/api/v1/oauth2/jwks")
                .scopesSupported(Arrays.asList("openid", "profile", "email"))
                .responseTypesSupported(Arrays.asList("code"))
                .grantTypesSupported(Arrays.asList("authorization_code", "refresh_token"))
                .build();

        // Mock JWK Set
        mockJwkSet = new HashMap<>();
        mockJwkSet.put("keys", Arrays.asList(
                Map.of("kty", "RSA", "use", "sig", "kid", "test-key-id")
        ));

        // Mock client response
        mockClientResponse = OAuth2ClientResponse.builder()
                .clientId("test-client")
                .clientName("Test Client")
                .redirectUris(Set.of("http://localhost:3000/callback"))
                .scopes(Set.of("openid", "profile"))
                .grantTypes(Set.of("authorization_code"))
                .build();

        // Mock client list response
        mockClientListResponse = OAuth2ClientListResponse.builder()
                .clients(Arrays.asList(mockClientResponse))
                .build();
    }

    @Test
    @DisplayName("Should handle authorization endpoint successfully")
    @WithMockUser
    void authorize_ShouldRedirectSuccessfully() throws Exception {
        // Given
        doNothing().when(oauth2AuthorizationService)
                .authorize(any(OAuth2AuthorizeRequest.class), any(Authentication.class), 
                          any(HttpServletRequest.class), any(HttpServletResponse.class));

        // When & Then
        mockMvc.perform(get("/api/v1/oauth2/authorize")
                .param("client_id", "test-client")
                .param("response_type", "code")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("scope", "openid profile")
                .param("state", "random-state"))
                .andExpect(status().isOk());

        verify(oauth2AuthorizationService).authorize(
                any(OAuth2AuthorizeRequest.class), 
                any(Authentication.class),
                any(HttpServletRequest.class), 
                any(HttpServletResponse.class)
        );
    }

    @Test
    @DisplayName("Should handle authorization endpoint with PKCE")
    @WithMockUser
    void authorize_WithPKCE_ShouldRedirectSuccessfully() throws Exception {
        // Given
        doNothing().when(oauth2AuthorizationService)
                .authorize(any(OAuth2AuthorizeRequest.class), any(Authentication.class), 
                          any(HttpServletRequest.class), any(HttpServletResponse.class));

        // When & Then
        mockMvc.perform(get("/api/v1/oauth2/authorize")
                .param("client_id", "test-client")
                .param("response_type", "code")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("scope", "openid profile")
                .param("state", "random-state")
                .param("code_challenge", "challenge123")
                .param("code_challenge_method", "S256"))
                .andExpect(status().isOk());

        verify(oauth2AuthorizationService).authorize(
                argThat(request -> request.getCodeChallenge().equals("challenge123") &&
                                 request.getCodeChallengeMethod().equals("S256")),
                any(Authentication.class),
                any(HttpServletRequest.class), 
                any(HttpServletResponse.class)
        );
    }

    @Test
    @DisplayName("Should exchange authorization code for tokens")
    void token_AuthorizationCodeGrant_ShouldReturnTokens() throws Exception {
        // Given
        when(oauth2AuthorizationService.token(any(OAuth2TokenRequest.class), any(HttpServletRequest.class)))
                .thenReturn(mockTokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "auth_code_123")
                .param("redirect_uri", "http://localhost:3000/callback")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access_token_123"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.refresh_token").value("refresh_token_123"))
                .andExpect(jsonPath("$.scope").value("openid profile email"));

        verify(oauth2AuthorizationService).token(
                argThat(request -> request.getGrantType().equals("authorization_code") &&
                                 request.getCode().equals("auth_code_123")),
                any(HttpServletRequest.class)
        );
    }

    @Test
    @DisplayName("Should refresh access tokens")
    void token_RefreshTokenGrant_ShouldReturnNewTokens() throws Exception {
        // Given
        when(oauth2AuthorizationService.token(any(OAuth2TokenRequest.class), any(HttpServletRequest.class)))
                .thenReturn(mockTokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", "refresh_token_123")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access_token_123"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));

        verify(oauth2AuthorizationService).token(
                argThat(request -> request.getGrantType().equals("refresh_token") &&
                                 request.getRefreshToken().equals("refresh_token_123")),
                any(HttpServletRequest.class)
        );
    }

    @Test
    @DisplayName("Should handle token exchange with Authorization header")
    void token_WithAuthorizationHeader_ShouldReturnTokens() throws Exception {
        // Given
        when(oauth2AuthorizationService.token(any(OAuth2TokenRequest.class), any(HttpServletRequest.class)))
                .thenReturn(mockTokenResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=")
                .param("grant_type", "authorization_code")
                .param("code", "auth_code_123")
                .param("redirect_uri", "http://localhost:3000/callback")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access_token_123"));

        verify(oauth2AuthorizationService).token(
                argThat(request -> request.getAuthorizationHeader().equals("Basic dGVzdC1jbGllbnQ6dGVzdC1zZWNyZXQ=")),
                any(HttpServletRequest.class)
        );
    }

    @Test
    @DisplayName("Should introspect tokens successfully")
    void introspect_ValidToken_ShouldReturnTokenInfo() throws Exception {
        // Given
        when(oauth2AuthorizationService.introspect(any(OAuth2IntrospectionRequest.class)))
                .thenReturn(mockIntrospectionResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/introspect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "access_token_123")
                .param("token_type_hint", "access_token")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.scope").value("openid profile"))
                .andExpect(jsonPath("$.client_id").value("test-client"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));

        verify(oauth2AuthorizationService).introspect(
                argThat(request -> request.getToken().equals("access_token_123") &&
                                 request.getTokenTypeHint().equals("access_token"))
        );
    }

    @Test
    @DisplayName("Should revoke tokens successfully")
    void revoke_ValidToken_ShouldRevoke() throws Exception {
        // Given
        doNothing().when(oauth2AuthorizationService).revoke(any(OAuth2RevocationRequest.class));

        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "access_token_123")
                .param("token_type_hint", "access_token")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(oauth2AuthorizationService).revoke(
                argThat(request -> request.getToken().equals("access_token_123"))
        );
    }

    @Test
    @DisplayName("Should return user info for valid token")
    @WithMockUser
    void userInfo_ValidToken_ShouldReturnUserInfo() throws Exception {
        // Given
        when(oauth2AuthorizationService.getUserInfo(anyString()))
                .thenReturn(mockUserInfoResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/oauth2/userinfo")
                .header("Authorization", "Bearer access_token_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("user-123"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.email_verified").value(true))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.given_name").value("Test"))
                .andExpect(jsonPath("$.family_name").value("User"));

        verify(oauth2AuthorizationService).getUserInfo("Bearer access_token_123");
    }

    @Test
    @DisplayName("Should return authorization server metadata")
    void serverMetadata_ShouldReturnMetadata() throws Exception {
        // Given
        when(oauth2AuthorizationService.getServerMetadata(any(HttpServletRequest.class)))
                .thenReturn(mockServerMetadata);

        // When & Then
        mockMvc.perform(get("/api/v1/oauth2/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8081"))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8081/api/v1/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8081/api/v1/oauth2/token"))
                .andExpect(jsonPath("$.userinfo_endpoint").value("http://localhost:8081/api/v1/oauth2/userinfo"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost:8081/api/v1/oauth2/jwks"))
                .andExpect(jsonPath("$.scopes_supported[0]").value("openid"))
                .andExpect(jsonPath("$.response_types_supported[0]").value("code"))
                .andExpect(jsonPath("$.grant_types_supported[0]").value("authorization_code"));

        verify(oauth2AuthorizationService).getServerMetadata(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Should return JWK Set")
    void jwkSet_ShouldReturnJWKSet() throws Exception {
        // Given
        when(oauth2AuthorizationService.getJwkSet()).thenReturn(mockJwkSet);

        // When & Then
        mockMvc.perform(get("/api/v1/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].kid").value("test-key-id"));

        verify(oauth2AuthorizationService).getJwkSet();
    }

    @Test
    @DisplayName("Should register OAuth2 client")
    @WithMockUser
    void registerClient_ValidRequest_ShouldCreateClient() throws Exception {
        // Given
        OAuth2ClientRegistrationRequest registrationRequest = OAuth2ClientRegistrationRequest.builder()
                .clientName("Test Client")
                .redirectUris(Set.of("http://localhost:3000/callback"))
                .scopes(Set.of("openid", "profile"))
                .grantTypes(Set.of("authorization_code"))
                .build();

        when(oauth2AuthorizationService.registerClient(any(OAuth2ClientRegistrationRequest.class), any(Authentication.class)))
                .thenReturn(mockClientResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.client_id").value("test-client"))
                .andExpect(jsonPath("$.client_name").value("Test Client"))
                .andExpect(jsonPath("$.redirect_uris").isArray())
                .andExpect(jsonPath("$.scopes").isArray())
                .andExpect(jsonPath("$.grant_types").isArray());

        verify(oauth2AuthorizationService).registerClient(any(OAuth2ClientRegistrationRequest.class), any(Authentication.class));
    }

    @Test
    @DisplayName("Should get user's OAuth2 clients")
    @WithMockUser
    void getClients_AuthenticatedUser_ShouldReturnClients() throws Exception {
        // Given
        when(oauth2AuthorizationService.getUserClients(any(Authentication.class)))
                .thenReturn(mockClientListResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/oauth2/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").isArray())
                .andExpect(jsonPath("$.clients[0].client_id").value("test-client"))
                .andExpect(jsonPath("$.clients[0].client_name").value("Test Client"));

        verify(oauth2AuthorizationService).getUserClients(any(Authentication.class));
    }

    @Test
    @DisplayName("Should update OAuth2 client")
    @WithMockUser
    void updateClient_ValidRequest_ShouldUpdateClient() throws Exception {
        // Given
        String clientId = "test-client";
        OAuth2ClientUpdateRequest updateRequest = OAuth2ClientUpdateRequest.builder()
                .clientName("Updated Test Client")
                .redirectUris(Set.of("http://localhost:3000/callback", "http://localhost:3000/callback2"))
                .build();

        OAuth2ClientResponse updatedResponse = OAuth2ClientResponse.builder()
                .clientId(clientId)
                .clientName("Updated Test Client")
                .redirectUris(Set.of("http://localhost:3000/callback", "http://localhost:3000/callback2"))
                .scopes(Set.of("openid", "profile"))
                .grantTypes(Set.of("authorization_code"))
                .build();

        when(oauth2AuthorizationService.updateClient(eq(clientId), any(OAuth2ClientUpdateRequest.class), any(Authentication.class)))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/oauth2/clients/{clientId}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client_id").value(clientId))
                .andExpect(jsonPath("$.client_name").value("Updated Test Client"))
                .andExpect(jsonPath("$.redirect_uris").isArray());

        verify(oauth2AuthorizationService).updateClient(eq(clientId), any(OAuth2ClientUpdateRequest.class), any(Authentication.class));
    }

    @Test
    @DisplayName("Should delete OAuth2 client")
    @WithMockUser
    void deleteClient_ValidClientId_ShouldDeleteClient() throws Exception {
        // Given
        String clientId = "test-client";
        doNothing().when(oauth2AuthorizationService).deleteClient(eq(clientId), any(Authentication.class));

        // When & Then
        mockMvc.perform(delete("/api/v1/oauth2/clients/{clientId}", clientId)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(oauth2AuthorizationService).deleteClient(eq(clientId), any(Authentication.class));
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    void protectedEndpoints_NotAuthenticated_ShouldReturnUnauthorized() throws Exception {
        // Test with register endpoint - this should return 401 when not authenticated
        mockMvc.perform(post("/api/v1/oauth2/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/oauth2/clients"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/oauth2/clients/test-client")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/oauth2/clients/test-client")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle missing parameters gracefully")
    void authorize_MissingClientId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/oauth2/authorize")
                .param("response_type", "code")
                .param("redirect_uri", "http://localhost:3000/callback"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle token endpoint with missing grant type")
    void token_MissingGrantType_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("code", "auth_code_123")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle introspect endpoint with missing token")
    void introspect_MissingToken_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/introspect")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("client_id", "test-client")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle revoke endpoint with missing token")
    void revoke_MissingToken_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("client_id", "test-client")
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle client credentials grant type")
    void token_ClientCredentialsGrant_ShouldReturnTokens() throws Exception {
        // Given
        OAuth2TokenResponse clientCredentialsResponse = OAuth2TokenResponse.builder()
                .accessToken("client_access_token")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("client:read client:write")
                .build();

        when(oauth2AuthorizationService.token(any(OAuth2TokenRequest.class), any(HttpServletRequest.class)))
                .thenReturn(clientCredentialsResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "client:read client:write")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("client_access_token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.scope").value("client:read client:write"))
                .andExpect(jsonPath("$.refresh_token").doesNotExist());

        verify(oauth2AuthorizationService).token(
                argThat(request -> request.getGrantType().equals("client_credentials") &&
                                 request.getScope().equals("client:read client:write")),
                any(HttpServletRequest.class)
        );
    }

    @Configuration
    @EnableWebSecurity
    static class TestConfig {

        @Bean
        public OAuth2AuthorizationService oauth2AuthorizationService() {
            return Mockito.mock(OAuth2AuthorizationService.class);
        }

        @Bean
        public OAuth2AuthorizationController oauth2AuthorizationController() {
            return new OAuth2AuthorizationController(oauth2AuthorizationService());
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            
            return http.build();
        }
    }
}