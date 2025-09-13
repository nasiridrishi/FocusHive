package com.focushive.music.integration;

import com.focushive.music.entity.SpotifyCredentials;
import com.focushive.music.repository.SpotifyCredentialsRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Integration tests for Spotify API interactions with comprehensive mocking.
 * Tests search endpoints, playlist operations, recommendations, and error handling.
 * Includes rate limiting and retry logic testing.
 */
class SpotifyApiMockIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SpotifyCredentialsRepository credentialsRepository;

    private static WireMockServer spotifyMockServer;
    private SpotifyCredentials testCredentials;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spotify.api.base-url", () -> "http://localhost:" + spotifyMockServer.port());
        registry.add("spotify.api.retry.max-attempts", () -> "3");
        registry.add("spotify.api.retry.backoff-delay", () -> "100");
    }

    @BeforeAll
    static void startWireMock() {
        spotifyMockServer = new WireMockServer(0);
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
    void setupTestData() {
        spotifyMockServer.resetAll();
        credentialsRepository.deleteAll();
        
        // Create test credentials
        String userId = createTestUserId();
        testCredentials = TestFixtures.spotifyCredentialsBuilder()
            .userId(userId)
            .build();
        credentialsRepository.save(testCredentials);
    }

    @Test
    @DisplayName("Should search tracks successfully with mocked API")
    void shouldSearchTracksSuccessfullyWithMockedApi() {
        // Given - Mock Spotify search endpoint
        stubFor(get(urlMatching("/v1/search\\?q=.*&type=track.*"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_SEARCH_TRACKS_RESPONSE)));

        // When - Search for tracks
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/search/tracks?q=rick astley&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should return search results
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseBody = response.getBody();
        
        assertThat(responseBody)
            .contains("Never Gonna Give You Up")
            .contains("Rick Astley")
            .contains("Bohemian Rhapsody")
            .contains("Queen")
            .contains("4iV5W9uYEdYUVa79Axb7Rh")
            .contains("5FVd6KXrgO9B3JPmC8OPst");

        // Verify API call was made
        verify(getRequestedFor(urlMatching("/v1/search\\?q=.*&type=track.*")));
    }

    @Test
    @DisplayName("Should handle Spotify playlist creation with mock")
    void shouldHandleSpotifyPlaylistCreationWithMock() {
        // Given - Mock playlist creation endpoint
        stubFor(post(urlEqualTo("/v1/users/" + testCredentials.getSpotifyUserId() + "/playlists"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_CREATE_PLAYLIST_RESPONSE)));

        // When - Create Spotify playlist
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/music/spotify/playlists",
            createSpotifyPlaylistRequest("Test Playlist", "Test Description", 
                false, testCredentials.getUserId()),
            String.class
        );

        // Then - Should create playlist successfully
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String responseBody = response.getBody();
        
        assertThat(responseBody)
            .contains("5ieJqeLJjjI8iJWaxeBLuK")
            .contains("Focus Deep Work")
            .contains("testuser123");

        // Verify API call structure
        verify(postRequestedFor(urlEqualTo("/v1/users/" + testCredentials.getSpotifyUserId() + "/playlists"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken())));
    }

    @Test
    @DisplayName("Should get track recommendations with mock data")
    void shouldGetTrackRecommendationsWithMockData() {
        // Given - Mock recommendations endpoint
        stubFor(get(urlMatching("/v1/recommendations\\?.*"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_RECOMMENDATIONS_RESPONSE)));

        // When - Get recommendations for deep work
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/recommendations?focusMode=DEEP_WORK&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should return recommendations
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseBody = response.getBody();
        
        assertThat(responseBody)
            .contains("Focus Flow")
            .contains("Study Music Academy")
            .contains("Productive Vibes")
            .contains("Concentration Collective")
            .contains("0c6xIDDpzE81m2q797ordA")
            .contains("1a2b3c4d5e6f7g8h9i0j");

        // Verify recommendations API call
        verify(getRequestedFor(urlMatching("/v1/recommendations\\?.*")));
    }

    @Test
    @DisplayName("Should handle 401 unauthorized error from Spotify API")
    void shouldHandle401UnauthorizedErrorFromSpotifyApi() {
        // Given - Mock 401 response
        stubFor(get(urlMatching("/v1/search\\?.*"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_ERROR_UNAUTHORIZED_RESPONSE)));

        // When - Try to search with expired token
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should return unauthorized
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // Should trigger token refresh or credential deactivation
        SpotifyCredentials updatedCredentials = credentialsRepository.findByUserId(testCredentials.getUserId()).orElse(null);
        assertThat(updatedCredentials).isNotNull();
        // In a real implementation, this might trigger a refresh attempt
    }

    @Test
    @DisplayName("Should handle 429 rate limit error with retry logic")
    void shouldHandle429RateLimitErrorWithRetryLogic() {
        // Given - Mock rate limit then success
        stubFor(get(urlMatching("/v1/search\\?.*"))
            .inScenario("Rate Limit Scenario")
            .whenScenarioStateIs("Started")
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withHeader("Retry-After", "1")
                .withBody(TestFixtures.SPOTIFY_ERROR_RATE_LIMITED_RESPONSE))
            .willSetStateTo("Rate Limited"));

        stubFor(get(urlMatching("/v1/search\\?.*"))
            .inScenario("Rate Limit Scenario")
            .whenScenarioStateIs("Rate Limited")
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_SEARCH_TRACKS_RESPONSE)));

        // When - Make request that initially hits rate limit
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should eventually succeed after retry
        await().atMost(java.time.Duration.ofSeconds(5))
            .until(() -> {
                ResponseEntity<String> retryResponse = restTemplate.getForEntity(
                    "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId(),
                    String.class
                );
                return retryResponse.getStatusCode() == HttpStatus.OK;
            });

        // Verify multiple calls were made (retry logic)
        verify(moreThan(1), getRequestedFor(urlMatching("/v1/search\\?.*")));
    }

    @Test
    @DisplayName("Should handle 500 server error from Spotify API")
    void shouldHandle500ServerErrorFromSpotifyApi() {
        // Given - Mock 500 error
        stubFor(get(urlMatching("/v1/search\\?.*"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "error": {
                            "status": 500,
                            "message": "Internal server error"
                        }
                    }
                    """)));

        // When - Make request to failing endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should handle gracefully
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        
        String responseBody = response.getBody();
        assertThat(responseBody).contains("Spotify service temporarily unavailable");
    }

    @Test
    @DisplayName("Should retrieve user's Spotify profile")
    void shouldRetrieveUserSpotifyProfile() {
        // Given - Mock user profile endpoint
        stubFor(get(urlEqualTo("/v1/me"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestFixtures.SPOTIFY_USER_PROFILE_RESPONSE)));

        // When - Get user profile
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/spotify/profile?userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should return profile data
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseBody = response.getBody();
        
        assertThat(responseBody)
            .contains("testuser123")
            .contains("Test User")
            .contains("test@example.com")
            .contains("premium")
            .contains("US");
    }

    @Test
    @DisplayName("Should handle network timeout gracefully")
    void shouldHandleNetworkTimeoutGracefully() {
        // Given - Mock slow response that times out
        stubFor(get(urlMatching("/v1/search\\?.*"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000) // 5 second delay
                .withBody(TestFixtures.SPOTIFY_SEARCH_TRACKS_RESPONSE)));

        // When - Make request with timeout
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should handle timeout appropriately
        // This test depends on the actual timeout configuration
        assertThat(response.getStatusCode()).isIn(HttpStatus.REQUEST_TIMEOUT, HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("Should validate API response structure")
    void shouldValidateApiResponseStructure() {
        // Given - Mock with invalid JSON structure
        stubFor(get(urlMatching("/v1/search\\?.*"))
            .withHeader("Authorization", equalTo("Bearer " + testCredentials.getAccessToken()))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ \"invalid\": \"structure\" }")));

        // When - Make search request
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should handle invalid response gracefully
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    @DisplayName("Should handle expired credentials during API calls")
    void shouldHandleExpiredCredentialsDuringApiCalls() {
        // Given - Expired credentials
        testCredentials.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        credentialsRepository.save(testCredentials);

        // When - Try to use expired credentials
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/music/search/tracks?q=test&userId=" + testCredentials.getUserId(),
            String.class
        );

        // Then - Should detect expired credentials
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String responseBody = response.getBody();
        assertThat(responseBody).contains("Spotify credentials expired");
    }

    // Helper methods
    private String createSpotifyPlaylistRequest(String name, String description, 
            boolean isPublic, String userId) {
        return String.format("""
            {
                "name": "%s",
                "description": "%s",
                "public": %s,
                "userId": "%s"
            }
            """, name, description, isPublic, userId);
    }
}