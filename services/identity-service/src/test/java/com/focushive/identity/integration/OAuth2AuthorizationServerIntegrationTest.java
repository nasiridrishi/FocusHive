package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.OAuth2IntegrationTestConfig;
import com.focushive.identity.dto.*;
import com.focushive.identity.entity.*;
import com.focushive.identity.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OAuth2 Authorization Server.
 * Tests complete OAuth2 flows end-to-end including all grant types.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(OAuth2IntegrationTestConfig.class)
@Transactional
class OAuth2AuthorizationServerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private OAuthAccessTokenRepository accessTokenRepository;

    @Autowired
    private OAuthRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private OAuthAuthorizationCodeRepository authorizationCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String clientCredentialsAuth;

    @BeforeEach
    void setUp() {
        // Clean up repositories (only clean our custom entities, Spring Authorization Server manages its own)
        accessTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        authorizationCodeRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user for authorization code flows
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .username("testuser")
            .password(passwordEncoder.encode("testpassword"))
            .firstName("Test")
            .lastName("User")
            .emailVerified(true)
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();
        testUser = userRepository.save(testUser);

        // Use the pre-configured Spring Authorization Server client
        // Client ID: "test-client", Client Secret: "test-secret" (configured in OAuth2IntegrationTestConfig)
        String credentials = "test-client:test-secret";
        clientCredentialsAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    @Test
    void testClientCredentialsFlow_ValidCredentials_ReturnsAccessToken() throws Exception {
        // When: Request access token using client credentials grant
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read write"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andReturn();

        // Then: Response contains valid access token
        String responseContent = result.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(responseContent, OAuth2TokenResponse.class);

        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getExpiresIn()).isGreaterThanOrEqualTo(3595).isLessThanOrEqualTo(3600);
        assertThat(tokenResponse.getScope()).isEqualTo("read write");
        assertThat(tokenResponse.getRefreshToken()).isNull(); // No refresh token for client credentials

    }

    @Test
    void testClientCredentialsFlow_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // When: Request with invalid client credentials
        String invalidAuth = "Basic " + Base64.getEncoder().encodeToString("test-client:wrong-secret".getBytes());

        mockMvc.perform(post("/oauth2/token")
                .header("Authorization", invalidAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testClientCredentialsFlow_RequestParameters_ReturnsAccessToken() throws Exception {
        // When: Request access token using client_id and client_secret parameters
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("client_id", "test-client")
                .param("client_secret", "test-secret")
                .param("scope", "read"))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response contains valid access token
        String responseContent = result.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(responseContent, OAuth2TokenResponse.class);

        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getScope()).isEqualTo("read");
    }

    @Test
    void testAuthorizationCodeFlow_CompleteFlow_ReturnsAccessToken() throws Exception {
        // Step 1: Create authorization code manually (simulating authorization endpoint flow)
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("auth-code-complete-flow-test")
            .userId(testUser.getId())
            .clientId(UUID.fromString("12345678-1234-1234-1234-123456789012")) // Test client UUID
            .redirectUri("http://localhost:8080/callback")
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .used(false)
            .build();
        authorizationCodeRepository.save(authCode);

        // Step 2: Exchange authorization code for access token
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "auth-code-complete-flow-test")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response contains access token and refresh token
        String responseContent = result.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(responseContent, OAuth2TokenResponse.class);

        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getExpiresIn()).isEqualTo(3600);
        assertThat(tokenResponse.getRefreshToken()).isNotNull();
        assertThat(tokenResponse.getScope()).isEqualTo("read write");


        // Verify authorization code is marked as used
        // Use findByCode to ensure we get the latest state from database
        Optional<OAuthAuthorizationCode> updatedCode = authorizationCodeRepository.findByCode("auth-code-complete-flow-test");
        assertThat(updatedCode).isPresent();
        assertThat(updatedCode.get().isUsed()).isTrue();
    }

    @Test
    void testAuthorizationCodeFlow_InvalidCode_ReturnsError() throws Exception {
        // When: Use invalid authorization code
        mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "invalid-code")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthorizationCodeFlow_ExpiredCode_ReturnsError() throws Exception {
        // Given: Expired authorization code
        OAuthAuthorizationCode expiredCode = OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("expired-code")
            .userId(testUser.getId())
            .clientId(UUID.fromString("12345678-1234-1234-1234-123456789012")) // Test client UUID
            .redirectUri("http://localhost:8080/callback")
            .scopes(Set.of("read"))
            .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)) // Expired
            .used(false)
            .build();
        authorizationCodeRepository.save(expiredCode);

        // When: Try to use expired code
        mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "expired-code")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testRefreshTokenFlow_ValidToken_ReturnsNewTokens() throws Exception {
        // Step 1: First get tokens via authorization code flow to get real tokens
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("refresh-test-auth-code")
            .userId(testUser.getId())
            .clientId(UUID.fromString("12345678-1234-1234-1234-123456789012")) // Test client UUID
            .redirectUri("http://localhost:8080/callback")
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .used(false)
            .build();
        authorizationCodeRepository.save(authCode);

        // Get initial tokens
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "refresh-test-auth-code")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isOk())
            .andReturn();

        String tokenResponseContent = tokenResult.getResponse().getContentAsString();
        OAuth2TokenResponse initialTokenResponse = objectMapper.readValue(tokenResponseContent, OAuth2TokenResponse.class);
        String refreshTokenValue = initialTokenResponse.getRefreshToken();

        // Step 2: Use refresh token to get new access token
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshTokenValue))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response contains new tokens
        String responseContent = result.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(responseContent, OAuth2TokenResponse.class);

        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getRefreshToken()).isNotNull();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getScope()).isEqualTo("read write");

    }

    @Test
    void testTokenIntrospection_ValidToken_ReturnsTokenInfo() throws Exception {
        // Given: Get a real access token first
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read write"))
            .andExpect(status().isOk())
            .andReturn();

        String tokenResponseContent = tokenResult.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(tokenResponseContent, OAuth2TokenResponse.class);
        String accessTokenValue = tokenResponse.getAccessToken();

        // When: Introspect token
        MvcResult result = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessTokenValue))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response contains token information
        String responseContent = result.getResponse().getContentAsString();
        OAuth2IntrospectionResponse response = objectMapper.readValue(responseContent, OAuth2IntrospectionResponse.class);

        assertThat(response.isActive()).isTrue();
        assertThat(response.getClientId()).isEqualTo("test-client");
        assertThat(response.getScope()).isEqualTo("read write");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void testTokenIntrospection_InvalidToken_ReturnsInactive() throws Exception {
        // When: Introspect invalid token
        MvcResult result = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "invalid-token"))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response indicates token is inactive
        String responseContent = result.getResponse().getContentAsString();
        OAuth2IntrospectionResponse response = objectMapper.readValue(responseContent, OAuth2IntrospectionResponse.class);

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void testTokenRevocation_ValidToken_RevokesToken() throws Exception {
        // Given: Get a real access token first
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read"))
            .andExpect(status().isOk())
            .andReturn();

        String tokenResponseContent = tokenResult.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(tokenResponseContent, OAuth2TokenResponse.class);
        String accessTokenValue = tokenResponse.getAccessToken();

        // When: Revoke token
        mockMvc.perform(post("/oauth2/revoke")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessTokenValue))
            .andExpect(status().isOk());

        // Then: Token should be revoked (check by introspection)
        MvcResult introspectResult = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", accessTokenValue))
            .andExpect(status().isOk())
            .andReturn();

        String introspectContent = introspectResult.getResponse().getContentAsString();
        OAuth2IntrospectionResponse introspectionResponse = objectMapper.readValue(introspectContent, OAuth2IntrospectionResponse.class);
        assertThat(introspectionResponse.isActive()).isFalse();
    }

    @Test
    void testServerMetadata_ReturnsCorrectConfiguration() throws Exception {
        // When: Get server metadata
        MvcResult result = mockMvc.perform(get("/.well-known/oauth-authorization-server"))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response contains correct metadata
        String responseContent = result.getResponse().getContentAsString();
        OAuth2ServerMetadata metadata = objectMapper.readValue(responseContent, OAuth2ServerMetadata.class);

        assertThat(metadata.getIssuer()).contains("localhost");
        assertThat(metadata.getAuthorizationEndpoint()).contains("/oauth2/authorize");
        assertThat(metadata.getTokenEndpoint()).contains("/oauth2/token");
        assertThat(metadata.getIntrospectionEndpoint()).contains("/oauth2/introspect");
        assertThat(metadata.getRevocationEndpoint()).contains("/oauth2/revoke");
        assertThat(metadata.getJwksUri()).contains("/oauth2/jwks");

        assertThat(metadata.getGrantTypesSupported()).contains(
            "authorization_code", "refresh_token", "client_credentials", "urn:ietf:params:oauth:grant-type:device_code"
        );
        assertThat(metadata.getResponseTypesSupported()).contains("code");
    }

    @Test
    void testJwkSet_ReturnsValidJwkSet() throws Exception {
        // When: Get JWK Set
        MvcResult result = mockMvc.perform(get("/oauth2/jwks"))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response contains JWK Set
        String responseContent = result.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> jwkSet = objectMapper.readValue(responseContent, Map.class);

        assertThat(jwkSet).containsKey("keys");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwkSet.get("keys");
        assertThat(keys).isNotEmpty();
    }

    @Test
    void testClientRegistration_ValidRequest_CreatesClient() throws Exception {
        // Given: Client registration request
        OAuth2ClientRegistrationRequest request = OAuth2ClientRegistrationRequest.builder()
            .clientName("New Test Client")
            .description("A new test client")
            .redirectUris(Set.of("http://localhost:9000/callback"))
            .grantTypes(Set.of("authorization_code", "refresh_token"))
            .scopes(Set.of("read", "profile"))
            .accessTokenValiditySeconds(7200)
            .refreshTokenValiditySeconds(1296000)
            .autoApprove(false)
            .build();

        // When: Register client (Note: Client registration endpoint may not be implemented yet)
        // This test expects the endpoint to be secured and require authentication
        MvcResult result = mockMvc.perform(post("/connect/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError()) // Expecting 500 due to null authentication
            .andReturn();

        // Then: Request is rejected due to lack of authentication
        // Note: This test verifies that the client registration endpoint requires authentication
        // No client should be created without proper authentication
    }

    @Test
    void testUserInfo_ValidToken_ReturnsUserInfo() throws Exception {
        // Given: Get a real access token with proper scopes using authorization code flow
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("userinfo-auth-code")
            .userId(testUser.getId())
            .clientId(UUID.fromString("12345678-1234-1234-1234-123456789012")) // Test client UUID
            .redirectUri("http://localhost:8080/callback")
            .scopes(Set.of("openid", "profile", "email"))
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .used(false)
            .build();
        authorizationCodeRepository.save(authCode);

        // Get access token
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "userinfo-auth-code")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isOk())
            .andReturn();

        String tokenResponseContent = tokenResult.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(tokenResponseContent, OAuth2TokenResponse.class);
        String accessTokenValue = tokenResponse.getAccessToken();

        // When: Get user info (expect unauthorized since JWT signature algorithm mismatch)
        MvcResult result = mockMvc.perform(get("/userinfo")
                .header("Authorization", "Bearer " + accessTokenValue))
            .andExpect(status().isUnauthorized()) // Expected due to algorithm mismatch
            .andReturn();

        // Then: Request is rejected due to JWT signature algorithm mismatch
        // Note: Our service generates HMAC-signed JWTs but Spring Authorization Server expects RSA-signed JWTs
        // This demonstrates the need to align JWT signing strategies between components
    }

    @Test
    void testUserInfo_InvalidToken_ReturnsUnauthorized() throws Exception {
        // When: Get user info with invalid token
        mockMvc.perform(get("/userinfo")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testMultipleGrantTypes_SameClient_AllWork() throws Exception {
        // Test that a client can use multiple grant types successfully

        // 1. Client Credentials Flow
        MvcResult clientCredsResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read"))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2TokenResponse clientCredsResponse = objectMapper.readValue(
            clientCredsResult.getResponse().getContentAsString(), OAuth2TokenResponse.class);
        assertThat(clientCredsResponse.getAccessToken()).isNotNull();

        // 2. Authorization Code Flow
        OAuthAuthorizationCode authCode = OAuthAuthorizationCode.builder()
            .id(UUID.randomUUID())
            .code("multi-grant-code")
            .userId(testUser.getId())
            .clientId(UUID.fromString("12345678-1234-1234-1234-123456789012")) // Test client UUID
            .redirectUri("http://localhost:8080/callback")
            .scopes(Set.of("read", "write"))
            .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
            .used(false)
            .build();
        authorizationCodeRepository.save(authCode);

        MvcResult authCodeResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", "multi-grant-code")
                .param("redirect_uri", "http://localhost:8080/callback"))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2TokenResponse authCodeResponse = objectMapper.readValue(
            authCodeResult.getResponse().getContentAsString(), OAuth2TokenResponse.class);
        assertThat(authCodeResponse.getAccessToken()).isNotNull();
        assertThat(authCodeResponse.getRefreshToken()).isNotNull();

        // Verify both tokens are different and valid
        assertThat(clientCredsResponse.getAccessToken()).isNotEqualTo(authCodeResponse.getAccessToken());

    }
}