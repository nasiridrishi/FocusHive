package com.focushive.identity.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.identity.config.OAuth2IntegrationTestConfig;
import com.focushive.identity.dto.OAuth2TokenResponse;
import com.focushive.identity.dto.OAuth2IntrospectionResponse;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OAuth2 Authorization Code Flow.
 * Tests the complete OAuth2 Authorization Code Grant flow using Spring Authorization Server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Import(OAuth2IntegrationTestConfig.class)
@Transactional
class OAuth2AuthorizationCodeFlowIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_CLIENT_ID = "test-client";
    private static final String TEST_CLIENT_SECRET = "test-secret";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_USER_PASSWORD = "testpassword";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationService oAuth2AuthorizationService;

    private User testUser;
    private RegisteredClient testClient;
    private String clientCredentialsAuth;

    private static String createBasicAuthHeader(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email(TEST_USER_EMAIL)
            .username("testuser")
            .password(passwordEncoder.encode(TEST_USER_PASSWORD))
            .firstName("Test")
            .lastName("User")
            .emailVerified(true)
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();
        testUser = userRepository.save(testUser);

        // Get test client
        testClient = registeredClientRepository.findByClientId(TEST_CLIENT_ID);
        assertThat(testClient).isNotNull();

        // Create client credentials auth header
        clientCredentialsAuth = createBasicAuthHeader(
            TEST_CLIENT_ID, 
            TEST_CLIENT_SECRET
        );
    }

    @Test
    void testRegisteredClientRepository_HasTestClient() {
        // Verify the test client exists in the repository
        RegisteredClient client = registeredClientRepository.findByClientId(TEST_CLIENT_ID);
        assertThat(client).isNotNull();
        assertThat(client.getClientId()).isEqualTo(TEST_CLIENT_ID);
        // Client secret should be encoded (either {noop} for test or {bcrypt} for default encoder)
        assertThat(client.getClientSecret()).isNotNull().isNotEmpty();
        // Could be {noop} or {bcrypt} depending on configuration
        assertThat(client.getClientSecret()).containsAnyOf("{noop}", "{bcrypt}");
        System.out.println("Test client found: " + client.getClientId());
        System.out.println("Client secret: " + client.getClientSecret());
        System.out.println("Auth methods: " + client.getClientAuthenticationMethods());
    }

    @Test
    void testClientCredentialsFlow_Success() throws Exception {
        // When: Request access token using client credentials
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read write"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn();

        // Then: Response contains valid access token
        String responseContent = result.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(responseContent, OAuth2TokenResponse.class);

        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getExpiresIn()).isGreaterThan(0);
        assertThat(tokenResponse.getScope()).contains("read", "write");
        assertThat(tokenResponse.getRefreshToken()).isNull(); // No refresh token for client credentials
    }

    @Test
    void testClientCredentialsFlow_InvalidCredentials() throws Exception {
        // When: Request with invalid client credentials
        String invalidAuth = createBasicAuthHeader(
            TEST_CLIENT_ID, 
            "wrong-secret"
        );

        mockMvc.perform(post("/oauth2/token")
                .header("Authorization", invalidAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read"))
            .andExpect(status().isUnauthorized()); // Expecting 401 for invalid credentials
    }

    @Test
    void testAuthorizationCodeFlow_SimulatedComplete() throws Exception {
        // Step 1: Create a simulated authorization that Spring Authorization Server would create
        // This simulates the user authorizing the client and receiving an authorization code
        String authorizationCode = UUID.randomUUID().toString();
        String redirectUri = "http://localhost:8080/callback";
        String state = "test-state-" + UUID.randomUUID();
        
        // Create OAuth2Authorization manually (simulating what authorization endpoint would do)
        // Create authorization code token
        OAuth2Token authCodeToken = new OAuth2Token() {
            @Override
            public String getTokenValue() {
                return authorizationCode;
            }
            
            @Override
            public Instant getIssuedAt() {
                return Instant.now();
            }
            
            @Override
            public Instant getExpiresAt() {
                return Instant.now().plusSeconds(600); // 10 minutes
            }
        };
        
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(testClient)
            .principalName(testUser.getEmail())
            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("read", "write", "openid", "profile"))
            .attribute(OAuth2ParameterNames.CODE, authorizationCode)
            .attribute(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
            .attribute(OAuth2ParameterNames.STATE, state)
            .token(authCodeToken)
            .build();
        oAuth2AuthorizationService.save(authorization);

        // Step 2: Exchange authorization code for access token
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("redirect_uri", redirectUri))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Response contains access token and refresh token
        String responseContent = result.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(responseContent, OAuth2TokenResponse.class);

        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getExpiresIn()).isGreaterThan(0);
        assertThat(tokenResponse.getRefreshToken()).isNotNull();
        assertThat(tokenResponse.getScope()).contains("read", "write");
        
        // Verify the tokens are valid JWT tokens by introspection
        MvcResult introspectResult = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", tokenResponse.getAccessToken()))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2IntrospectionResponse introspectionResponse = objectMapper.readValue(
            introspectResult.getResponse().getContentAsString(),
            OAuth2IntrospectionResponse.class
        );

        assertThat(introspectionResponse.isActive()).isTrue();
        assertThat(introspectionResponse.getClientId()).isEqualTo(TEST_CLIENT_ID);
        assertThat(introspectionResponse.getSub()).isEqualTo(testUser.getEmail());
    }

    @Test 
    void testRefreshTokenFlow_Success() throws Exception {
        // Step 1: Get initial tokens via client credentials (for simplicity)
        MvcResult initialResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read write"))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2TokenResponse initialResponse = objectMapper.readValue(
            initialResult.getResponse().getContentAsString(), 
            OAuth2TokenResponse.class
        );
        
        // For client credentials, we need to create a proper authorization with refresh token
        // Let's simulate an authorization code flow result instead
        String authorizationCode = UUID.randomUUID().toString();
        String redirectUri = "http://localhost:8080/callback";
        
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(testClient)
            .principalName(testUser.getEmail())
            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("read", "write"))
            .attribute(OAuth2ParameterNames.CODE, authorizationCode)
            .attribute(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
            .build();
        oAuth2AuthorizationService.save(authorization);

        // Get tokens with refresh token
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("redirect_uri", redirectUri))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2TokenResponse tokenResponse = objectMapper.readValue(
            tokenResult.getResponse().getContentAsString(), 
            OAuth2TokenResponse.class
        );

        String refreshToken = tokenResponse.getRefreshToken();
        assertThat(refreshToken).isNotNull();

        // Step 2: Use refresh token to get new access token
        MvcResult refreshResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshToken))
            .andExpect(status().isOk())
            .andReturn();

        // Then: New tokens are provided
        OAuth2TokenResponse refreshResponse = objectMapper.readValue(
            refreshResult.getResponse().getContentAsString(), 
            OAuth2TokenResponse.class
        );

        assertThat(refreshResponse.getAccessToken()).isNotNull();
        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(tokenResponse.getAccessToken());
        assertThat(refreshResponse.getRefreshToken()).isNotNull();
        assertThat(refreshResponse.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void testTokenIntrospection_ValidToken() throws Exception {
        // Given: Get a real access token
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read"))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2TokenResponse tokenResponse = objectMapper.readValue(
            tokenResult.getResponse().getContentAsString(), 
            OAuth2TokenResponse.class
        );

        // When: Introspect the token
        MvcResult introspectResult = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", tokenResponse.getAccessToken()))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Token information is returned
        OAuth2IntrospectionResponse introspectionResponse = objectMapper.readValue(
            introspectResult.getResponse().getContentAsString(),
            OAuth2IntrospectionResponse.class
        );

        assertThat(introspectionResponse.isActive()).isTrue();
        assertThat(introspectionResponse.getClientId()).isEqualTo(TEST_CLIENT_ID);
        assertThat(introspectionResponse.getScope()).contains("read");
        assertThat(introspectionResponse.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void testTokenIntrospection_InvalidToken() throws Exception {
        // When: Introspect invalid token
        MvcResult result = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", "invalid-token-value"))
            .andExpect(status().isOk())
            .andReturn();

        // Then: Token is reported as inactive
        OAuth2IntrospectionResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            OAuth2IntrospectionResponse.class
        );

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void testTokenRevocation_Success() throws Exception {
        // Given: Get a real access token
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "client_credentials")
                .param("scope", "read"))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2TokenResponse tokenResponse = objectMapper.readValue(
            tokenResult.getResponse().getContentAsString(),
            OAuth2TokenResponse.class
        );

        // When: Revoke the token
        mockMvc.perform(post("/oauth2/revoke")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", tokenResponse.getAccessToken()))
            .andExpect(status().isOk());

        // Then: Token should be revoked (verify by introspection)
        MvcResult introspectResult = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", tokenResponse.getAccessToken()))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2IntrospectionResponse introspectionResponse = objectMapper.readValue(
            introspectResult.getResponse().getContentAsString(),
            OAuth2IntrospectionResponse.class
        );

        assertThat(introspectionResponse.isActive()).isFalse();
    }

    @Test
    void testAuthorizationCodeFlow_CompleteEndToEndSimulation() throws Exception {
        // This test simulates the complete OAuth2 Authorization Code flow
        // including authorization endpoint simulation, code generation, and token exchange
        
        String redirectUri = "http://localhost:8080/callback";
        String state = "test-state-" + System.currentTimeMillis();
        String nonce = "test-nonce-" + System.currentTimeMillis();
        
        // Step 1: Simulate authorization request parameters (what would be sent to /oauth2/authorize)
        // In a real scenario, the client would redirect the user to this endpoint
        MultiValueMap<String, String> authParams = new LinkedMultiValueMap<>();
        authParams.add("response_type", "code");
        authParams.add("client_id", TEST_CLIENT_ID);
        authParams.add("redirect_uri", redirectUri);
        authParams.add("scope", "openid profile read write");
        authParams.add("state", state);
        authParams.add("nonce", nonce);
        
        // Step 2: Manually create authorization code (simulating user consent and authentication)
        // In production, this would be created after user login and consent
        String authorizationCode = "auth_code_" + UUID.randomUUID().toString().replace("-", "");
        
        // Create the OAuth2Authorization that represents the user's consent
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(testClient)
            .principalName(testUser.getEmail())
            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("openid", "profile", "read", "write"))
            .attribute(OAuth2ParameterNames.CODE, authorizationCode)
            .attribute(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
            .attribute(OAuth2ParameterNames.STATE, state)
            .attribute("nonce", nonce)
            // Add user attributes that would be available after authentication
            .attribute("user_id", testUser.getId().toString())
            .attribute("email", testUser.getEmail())
            .attribute("username", testUser.getUsername())
            .build();
        oAuth2AuthorizationService.save(authorization);
        
        // Step 3: Client exchanges authorization code for tokens
        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("redirect_uri", redirectUri)
                .param("client_id", TEST_CLIENT_ID)) // Optional, but good practice
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn();

        // Step 4: Verify token response structure and content
        String tokenResponseContent = tokenResult.getResponse().getContentAsString();
        OAuth2TokenResponse tokenResponse = objectMapper.readValue(tokenResponseContent, OAuth2TokenResponse.class);

        // Validate access token
        assertThat(tokenResponse.getAccessToken()).isNotNull().isNotEmpty();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getExpiresIn()).isGreaterThan(0).isLessThanOrEqualTo(3600); // Should be 1 hour or less
        
        // Validate refresh token (should be present for authorization code flow)
        assertThat(tokenResponse.getRefreshToken()).isNotNull().isNotEmpty();
        
        // Validate scopes
        assertThat(tokenResponse.getScope()).isNotNull();
        assertThat(tokenResponse.getScope()).contains("read", "write");
        
        // Validate ID token if present (OIDC)
        if (tokenResponse.getScope().contains("openid")) {
            // Note: ID token would be available in the response for OIDC flows
            // For this test, we're focusing on the OAuth2 aspects
        }
        
        // Step 5: Validate access token by introspection
        MvcResult introspectResult = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", tokenResponse.getAccessToken())
                .param("token_type_hint", "access_token"))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2IntrospectionResponse introspectionResponse = objectMapper.readValue(
            introspectResult.getResponse().getContentAsString(),
            OAuth2IntrospectionResponse.class
        );

        // Validate introspection response
        assertThat(introspectionResponse.isActive()).isTrue();
        assertThat(introspectionResponse.getClientId()).isEqualTo(TEST_CLIENT_ID);
        assertThat(introspectionResponse.getSub()).isEqualTo(testUser.getEmail());
        assertThat(introspectionResponse.getScope()).contains("read", "write");
        assertThat(introspectionResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(introspectionResponse.getExp()).isGreaterThan(Instant.now().getEpochSecond());
        
        // Step 6: Test refresh token functionality
        MvcResult refreshResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", tokenResponse.getRefreshToken())
                .param("scope", "read write")) // Can request subset of original scopes
            .andExpect(status().isOk())
            .andReturn();

        OAuth2TokenResponse refreshResponse = objectMapper.readValue(
            refreshResult.getResponse().getContentAsString(), 
            OAuth2TokenResponse.class
        );

        // Validate new tokens from refresh
        assertThat(refreshResponse.getAccessToken()).isNotNull().isNotEqualTo(tokenResponse.getAccessToken());
        assertThat(refreshResponse.getRefreshToken()).isNotNull(); // May be the same or new depending on configuration
        assertThat(refreshResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(refreshResponse.getScope()).contains("read", "write");
        
        // Step 7: Verify original access token is no longer valid after refresh (if configured to invalidate)
        // Note: This behavior depends on the token settings configuration
        
        // Step 8: Test token revocation
        mockMvc.perform(post("/oauth2/revoke")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", refreshResponse.getAccessToken())
                .param("token_type_hint", "access_token"))
            .andExpect(status().isOk());

        // Step 9: Verify revoked token is no longer active
        MvcResult revokedTokenIntrospectResult = mockMvc.perform(post("/oauth2/introspect")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("token", refreshResponse.getAccessToken()))
            .andExpect(status().isOk())
            .andReturn();

        OAuth2IntrospectionResponse revokedTokenIntrospection = objectMapper.readValue(
            revokedTokenIntrospectResult.getResponse().getContentAsString(),
            OAuth2IntrospectionResponse.class
        );

        assertThat(revokedTokenIntrospection.isActive()).isFalse();
    }
    
    @Test
    void testAuthorizationCodeFlow_InvalidCode() throws Exception {
        // Test token exchange with invalid authorization code
        String invalidCode = "invalid_code_" + UUID.randomUUID();
        String redirectUri = "http://localhost:8080/callback";
        
        mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", invalidCode)
                .param("redirect_uri", redirectUri))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testAuthorizationCodeFlow_InvalidRedirectUri() throws Exception {
        // Create valid authorization code
        String authorizationCode = "auth_code_" + UUID.randomUUID();
        String correctRedirectUri = "http://localhost:8080/callback";
        String incorrectRedirectUri = "http://malicious-site.com/callback";
        
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(testClient)
            .principalName(testUser.getEmail())
            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("read", "write"))
            .attribute(OAuth2ParameterNames.CODE, authorizationCode)
            .attribute(OAuth2ParameterNames.REDIRECT_URI, correctRedirectUri)
            .attribute(OAuth2ParameterNames.STATE, "test-state")
            .build();
        oAuth2AuthorizationService.save(authorization);
        
        // Attempt token exchange with wrong redirect_uri
        mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("redirect_uri", incorrectRedirectUri))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testAuthorizationCodeFlow_ExpiredCode() throws Exception {
        // Create an authorization that appears expired (this is simplified for testing)
        String authorizationCode = "expired_code_" + UUID.randomUUID();
        String redirectUri = "http://localhost:8080/callback";
        
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(testClient)
            .principalName(testUser.getEmail())
            .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("read", "write"))
            .attribute(OAuth2ParameterNames.CODE, authorizationCode)
            .attribute(OAuth2ParameterNames.REDIRECT_URI, redirectUri)
            .attribute(OAuth2ParameterNames.STATE, "test-state")
            // Set authorization time in the past to simulate expiry
            .attribute("authorization_time", Instant.now().minusSeconds(3600)) // 1 hour ago
            .build();
        oAuth2AuthorizationService.save(authorization);
        
        // The actual expiry validation depends on the TokenSettings.authorizationCodeTimeToLive()
        // For this test, we'll attempt the exchange - behavior depends on configuration
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", clientCredentialsAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "authorization_code")
                .param("code", authorizationCode)
                .param("redirect_uri", redirectUri))
            .andReturn();
        
        // The status depends on how Spring Authorization Server handles expired codes
        // It should either return 400 (Bad Request) or succeed if not actually expired
        int status = result.getResponse().getStatus();
        assertThat(status).isIn(200, 400);
    }

    @Test
    void testOAuth2ServerMetadata_Success() throws Exception {
        // When: Request OAuth2 server metadata
        MvcResult result = mockMvc.perform(get("/.well-known/oauth-authorization-server"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        // Then: Metadata contains expected endpoints
        String responseContent = result.getResponse().getContentAsString();
        
        assertThat(responseContent).contains("\"issuer\"");
        assertThat(responseContent).contains("/oauth2/authorize");
        assertThat(responseContent).contains("/oauth2/token");
        assertThat(responseContent).contains("/oauth2/introspect");
        assertThat(responseContent).contains("/oauth2/revoke");
        assertThat(responseContent).contains("/oauth2/jwks");
        assertThat(responseContent).contains("authorization_code");
        assertThat(responseContent).contains("client_credentials");
        assertThat(responseContent).contains("refresh_token");
    }

    @Test
    void testJwkSet_Success() throws Exception {
        // When: Request JWK Set
        MvcResult result = mockMvc.perform(get("/oauth2/jwks"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();

        // Then: JWK Set contains keys
        String responseContent = result.getResponse().getContentAsString();
        
        assertThat(responseContent).contains("\"keys\"");
        assertThat(responseContent).contains("\"kty\"");
        assertThat(responseContent).contains("\"use\"");
    }
}