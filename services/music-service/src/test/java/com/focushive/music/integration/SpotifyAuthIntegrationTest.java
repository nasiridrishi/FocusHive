package com.focushive.music.integration;

import com.focushive.music.entity.SpotifyCredentials;
import com.focushive.music.repository.SpotifyCredentialsRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Spotify OAuth flow.
 * Follows TDD approach - tests are written first to define expected behavior.
 * Tests OAuth authorization, token exchange, and refresh flows with WireMock.
 */
class SpotifyAuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpotifyCredentialsRepository credentialsRepository;

    private static WireMockServer spotifyMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spotify.client-id", () -> "test-client-id");
        registry.add("spotify.client-secret", () -> "test-client-secret");
        registry.add("spotify.redirect-uri", () -> TestFixtures.OAUTH_REDIRECT_URI);
        registry.add("spotify.api.base-url", () -> "http://localhost:" + spotifyMockServer.port());
    }

    @BeforeAll
    static void startWireMock() {
        spotifyMockServer = new WireMockServer(0); // Random port
        spotifyMockServer.start();
        WireMock.configureFor(spotifyMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (spotifyMockServer != null) {
            spotifyMockServer.stop();
        }
    }

    @BeforeEach
    void setupMocks() {
        spotifyMockServer.resetAll();
        credentialsRepository.deleteAll();
    }

    @Test
    @DisplayName("Should generate valid Spotify OAuth authorization URL")
    void shouldGenerateValidOAuthAuthorizationUrl() {
        // Given
        String userId = createTestUserId();
        
        // When - Call authorization URL endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/auth/authorize?userId=" + userId, 
            String.class
        );

        // Then - Should return valid authorization URL
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String authUrl = response.getBody();
        
        assertThat(authUrl)
            .contains("https://accounts.spotify.com/authorize")
            .contains("client_id=test-client-id")
            .contains("response_type=code")
            .contains("redirect_uri=" + TestFixtures.OAUTH_REDIRECT_URI)
            .contains("scope=user-read-private")
            .contains("state=");
    }

    @Test
    @DisplayName("Should exchange authorization code for access token successfully")
    void shouldExchangeAuthorizationCodeForAccessToken() {
        // Given - Mock Spotify token endpoint
        stubFor(post(urlEqualTo("/api/token"))
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .withRequestBody(containing("grant_type=authorization_code"))
            .withRequestBody(containing("code=" + TestFixtures.OAUTH_AUTHORIZATION_CODE))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_AUTH_RESPONSE)));

        // Mock user profile endpoint
        stubFor(get(urlEqualTo("/v1/me"))
            .withHeader("Authorization", equalTo("Bearer BQDyP4h_mock_access_token_here"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_USER_PROFILE_RESPONSE)));

        String userId = createTestUserId();

        // When - Exchange code for token
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/auth/callback",
            createCallbackRequest(TestFixtures.OAUTH_AUTHORIZATION_CODE, TestFixtures.OAUTH_STATE, userId),
            String.class
        );

        // Then - Should save credentials and return success
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        Optional<SpotifyCredentials> credentials = credentialsRepository.findByUserId(userId);
        assertThat(credentials).isPresent();
        
        SpotifyCredentials savedCredentials = credentials.get();
        assertThat(savedCredentials.getAccessToken()).isEqualTo("BQDyP4h_mock_access_token_here");
        assertThat(savedCredentials.getRefreshToken()).isEqualTo("AQBmock_refresh_token_here");
        assertThat(savedCredentials.getSpotifyUserId()).isEqualTo("testuser123");
        assertThat(savedCredentials.getScope()).contains("user-read-private");
        assertThat(savedCredentials.isValid()).isTrue();

        // Verify Spotify API calls were made
        verify(postRequestedFor(urlEqualTo("/api/token")));
        verify(getRequestedFor(urlEqualTo("/v1/me")));
    }

    @Test
    @DisplayName("Should handle invalid authorization code gracefully")
    void shouldHandleInvalidAuthorizationCodeGracefully() {
        // Given - Mock Spotify error response
        stubFor(post(urlEqualTo("/api/token"))
            .withRequestBody(containing("code=invalid_code"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "invalid_grant",
                        "error_description": "Invalid authorization code"
                    }
                    """)));

        String userId = createTestUserId();

        // When - Exchange invalid code
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/auth/callback",
            createCallbackRequest("invalid_code", TestFixtures.OAUTH_STATE, userId),
            String.class
        );

        // Then - Should return error and not save credentials
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        Optional<SpotifyCredentials> credentials = credentialsRepository.findByUserId(userId);
        assertThat(credentials).isEmpty();
    }

    @Test
    @DisplayName("Should refresh expired access token successfully")
    void shouldRefreshExpiredAccessTokenSuccessfully() {
        // Given - Existing expired credentials
        SpotifyCredentials expiredCredentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(createTestUserId())
            .expiresAt(LocalDateTime.now().minusMinutes(10)) // Expired
            .build();
        credentialsRepository.save(expiredCredentials);

        // Mock refresh token endpoint
        stubFor(post(urlEqualTo("/api/token"))
            .withRequestBody(containing("grant_type=refresh_token"))
            .withRequestBody(containing("refresh_token=AQBmock_refresh_token_here"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "access_token": "BQDyP4h_new_access_token_here",
                        "token_type": "Bearer",
                        "scope": "user-read-private user-read-email",
                        "expires_in": 3600
                    }
                    """)));

        // When - Request token refresh
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/auth/refresh",
            createRefreshRequest(expiredCredentials.getUserId()),
            String.class
        );

        // Then - Should update credentials with new token
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        Optional<SpotifyCredentials> updatedCredentials = credentialsRepository.findByUserId(expiredCredentials.getUserId());
        assertThat(updatedCredentials).isPresent();
        
        SpotifyCredentials refreshedCredentials = updatedCredentials.get();
        assertThat(refreshedCredentials.getAccessToken()).isEqualTo("BQDyP4h_new_access_token_here");
        assertThat(refreshedCredentials.getRefreshToken()).isEqualTo("AQBmock_refresh_token_here"); // Should remain same
        assertThat(refreshedCredentials.isValid()).isTrue();
        assertThat(refreshedCredentials.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(postRequestedFor(urlEqualTo("/api/token"))
            .withRequestBody(containing("grant_type=refresh_token")));
    }

    @Test
    @DisplayName("Should handle refresh token failure gracefully")
    void shouldHandleRefreshTokenFailureGracefully() {
        // Given - Existing credentials with invalid refresh token
        SpotifyCredentials credentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(createTestUserId())
            .refreshToken("invalid_refresh_token")
            .expiresAt(LocalDateTime.now().minusMinutes(10))
            .build();
        credentialsRepository.save(credentials);

        // Mock refresh token error
        stubFor(post(urlEqualTo("/api/token"))
            .withRequestBody(containing("refresh_token=invalid_refresh_token"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": "invalid_grant",
                        "error_description": "Invalid refresh token"
                    }
                    """)));

        // When - Attempt to refresh with invalid token
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/auth/refresh",
            createRefreshRequest(credentials.getUserId()),
            String.class
        );

        // Then - Should return unauthorized and deactivate credentials
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        Optional<SpotifyCredentials> updatedCredentials = credentialsRepository.findByUserId(credentials.getUserId());
        assertThat(updatedCredentials).isPresent();
        assertThat(updatedCredentials.get().getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should revoke user's Spotify access successfully")
    void shouldRevokeUserSpotifyAccessSuccessfully() {
        // Given - Existing active credentials
        SpotifyCredentials credentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(createTestUserId())
            .build();
        credentialsRepository.save(credentials);

        // When - Revoke access
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/music/auth/revoke?userId=" + credentials.getUserId(),
            HttpMethod.DELETE,
            null,
            String.class
        );

        // Then - Should deactivate credentials
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        Optional<SpotifyCredentials> updatedCredentials = credentialsRepository.findByUserId(credentials.getUserId());
        assertThat(updatedCredentials).isPresent();
        assertThat(updatedCredentials.get().getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle missing state parameter in callback")
    void shouldHandleMissingStateParameterInCallback() {
        // Given
        String userId = createTestUserId();

        // When - Call callback without state parameter
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/auth/callback",
            createCallbackRequestWithoutState(TestFixtures.OAUTH_AUTHORIZATION_CODE, userId),
            String.class
        );

        // Then - Should return bad request
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        Optional<SpotifyCredentials> credentials = credentialsRepository.findByUserId(userId);
        assertThat(credentials).isEmpty();
    }

    // Helper methods for creating test requests
    private HttpEntity<String> createCallbackRequest(String code, String state, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("""
            {
                "code": "%s",
                "state": "%s",
                "userId": "%s"
            }
            """, code, state, userId);
        
        return new HttpEntity<>(requestBody, headers);
    }

    private HttpEntity<String> createCallbackRequestWithoutState(String code, String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("""
            {
                "code": "%s",
                "userId": "%s"
            }
            """, code, userId);
        
        return new HttpEntity<>(requestBody, headers);
    }

    private HttpEntity<String> createRefreshRequest(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestBody = String.format("""
            {
                "userId": "%s"
            }
            """, userId);
        
        return new HttpEntity<>(requestBody, headers);
    }
}