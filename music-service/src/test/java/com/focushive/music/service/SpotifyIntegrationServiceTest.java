package com.focushive.music.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.music.dto.RecommendationDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.StreamingCredentials;
import com.focushive.music.repository.StreamingCredentialsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for SpotifyIntegrationService.
 * Tests OAuth flow, token management, and API integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpotifyIntegrationService Tests")
class SpotifyIntegrationServiceTest {

    @Mock
    private StreamingCredentialsRepository credentialsRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @InjectMocks
    private SpotifyIntegrationService spotifyIntegrationService;

    private static final String CLIENT_ID = "test_client_id";
    private static final String CLIENT_SECRET = "test_client_secret";
    private static final String REDIRECT_URI = "http://localhost:8084/api/music/spotify/callback";
    private static final String API_URL = "https://api.spotify.com/v1";
    private static final String ACCOUNTS_URL = "https://accounts.spotify.com";

    private UUID testUserId;
    private String testState;
    private String testAccessToken;
    private String testRefreshToken;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testState = "test_state_123";
        testAccessToken = "BQA7K5m-YFk0Hh4-test-access-token";
        testRefreshToken = "AQA7K5m-YFk0Hh4-test-refresh-token";

        // Set private fields
        ReflectionTestUtils.setField(spotifyIntegrationService, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(spotifyIntegrationService, "clientSecret", CLIENT_SECRET);
        ReflectionTestUtils.setField(spotifyIntegrationService, "redirectUri", REDIRECT_URI);
        ReflectionTestUtils.setField(spotifyIntegrationService, "apiUrl", API_URL);
        ReflectionTestUtils.setField(spotifyIntegrationService, "accountsUrl", ACCOUNTS_URL);
    }

    @Nested
    @DisplayName("OAuth Authorization Flow")
    class OAuthAuthorizationFlow {

        @Test
        @DisplayName("Should generate valid authorization URL")
        void shouldGenerateValidAuthorizationUrl() {
            // When
            String authUrl = spotifyIntegrationService.initiateSpotifyAuth(testUserId, testState);

            // Then
            assertThat(authUrl)
                .contains(ACCOUNTS_URL + "/authorize")
                .contains("client_id=" + CLIENT_ID)
                .contains("response_type=code")
                .contains("redirect_uri=" + REDIRECT_URI)
                .contains("state=" + testUserId + ":" + testState)
                .contains("scope=");

            // Verify it contains required scopes
            assertThat(authUrl)
                .contains("user-read-private")
                .contains("user-read-email")
                .contains("streaming")
                .contains("user-modify-playback-state")
                .contains("playlist-modify-public")
                .contains("playlist-modify-private")
                .contains("user-library-read")
                .contains("user-top-read");
        }

        @Test
        @DisplayName("Should handle callback successfully")
        void shouldHandleCallbackSuccessfully() throws Exception {
            // Given
            String code = "test_authorization_code";
            String state = testUserId + ":" + testState;

            SpotifyIntegrationService.TokenResponse tokenResponse = 
                new SpotifyIntegrationService.TokenResponse(
                    testAccessToken, "Bearer", 3600, testRefreshToken, "user-read-private user-read-email");

            SpotifyIntegrationService.SpotifyUser spotifyUser = 
                new SpotifyIntegrationService.SpotifyUser(
                    "spotify_user_123", "Test User", "test@example.com", "US", 
                    new SpotifyIntegrationService.SpotifyFollowers(100));

            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), 
                eq(SpotifyIntegrationService.TokenResponse.class)))
                .willReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), 
                any(HttpEntity.class), eq(SpotifyIntegrationService.SpotifyUser.class)))
                .willReturn(new ResponseEntity<>(spotifyUser, HttpStatus.OK));

            given(tokenEncryptionService.encrypt(anyString())).willReturn("encrypted_token");
            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.empty());
            given(credentialsRepository.save(any(StreamingCredentials.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // When
            boolean result = spotifyIntegrationService.handleSpotifyCallback(code, state);

            // Then
            assertThat(result).isTrue();

            ArgumentCaptor<StreamingCredentials> credentialsCaptor = 
                ArgumentCaptor.forClass(StreamingCredentials.class);
            verify(credentialsRepository).save(credentialsCaptor.capture());

            StreamingCredentials savedCredentials = credentialsCaptor.getValue();
            assertThat(savedCredentials.getUserId()).isEqualTo(testUserId);
            assertThat(savedCredentials.getServiceName()).isEqualTo("spotify");
            assertThat(savedCredentials.getServiceUserId()).isEqualTo("spotify_user_123");
            assertThat(savedCredentials.getMetadata()).containsKey("displayName");
        }

        @Test
        @DisplayName("Should handle invalid state parameter")
        void shouldHandleInvalidStateParameter() {
            // Given
            String code = "test_code";
            String invalidState = "invalid_state_format";

            // When & Then
            assertThatThrownBy(() -> spotifyIntegrationService.handleSpotifyCallback(code, invalidState))
                .isInstanceOf(MusicServiceException.ValidationException.class)
                .hasMessageContaining("Invalid state parameter");
        }

        @Test
        @DisplayName("Should handle token exchange failure")
        void shouldHandleTokenExchangeFailure() {
            // Given
            String code = "test_code";
            String state = testUserId + ":" + testState;

            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), 
                eq(SpotifyIntegrationService.TokenResponse.class)))
                .willReturn(new ResponseEntity<>(HttpStatus.BAD_REQUEST));

            // When & Then
            assertThatThrownBy(() -> spotifyIntegrationService.handleSpotifyCallback(code, state))
                .isInstanceOf(MusicServiceException.StreamingServiceException.class)
                .hasMessageContaining("Token exchange failed");
        }
    }

    @Nested
    @DisplayName("Token Management")
    class TokenManagement {

        @Test
        @DisplayName("Should refresh token when expired")
        void shouldRefreshTokenWhenExpired() throws Exception {
            // Given
            StreamingCredentials expiredCredentials = createExpiredCredentials();
            SpotifyIntegrationService.TokenResponse newTokenResponse = 
                new SpotifyIntegrationService.TokenResponse(
                    "new_access_token", "Bearer", 3600, testRefreshToken, "user-read-private");

            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(expiredCredentials));

            given(tokenEncryptionService.decrypt(anyString())).willReturn(testRefreshToken);
            given(tokenEncryptionService.encrypt(anyString())).willReturn("encrypted_new_token");

            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), 
                eq(SpotifyIntegrationService.TokenResponse.class)))
                .willReturn(new ResponseEntity<>(newTokenResponse, HttpStatus.OK));

            given(credentialsRepository.save(any(StreamingCredentials.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // When
            List<RecommendationDTO> recommendations = spotifyIntegrationService
                .getRecommendationsByGenres(testUserId, Arrays.asList("rock", "jazz"), Map.of());

            // Then
            verify(restTemplate).postForEntity(
                eq(ACCOUNTS_URL + "/api/token"), 
                any(HttpEntity.class), 
                eq(SpotifyIntegrationService.TokenResponse.class));

            ArgumentCaptor<StreamingCredentials> credentialsCaptor = 
                ArgumentCaptor.forClass(StreamingCredentials.class);
            verify(credentialsRepository).save(credentialsCaptor.capture());

            StreamingCredentials savedCredentials = credentialsCaptor.getValue();
            assertThat(savedCredentials.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should handle refresh token failure")
        void shouldHandleRefreshTokenFailure() {
            // Given
            StreamingCredentials expiredCredentials = createExpiredCredentials();

            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(expiredCredentials));

            given(tokenEncryptionService.decrypt(anyString())).willReturn(testRefreshToken);

            given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), 
                eq(SpotifyIntegrationService.TokenResponse.class)))
                .willReturn(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));

            // When & Then
            assertThatThrownBy(() -> spotifyIntegrationService
                .getRecommendationsByGenres(testUserId, Arrays.asList("rock"), Map.of()))
                .isInstanceOf(MusicServiceException.StreamingServiceException.class)
                .hasMessageContaining("Token refresh failed");
        }

        @Test
        @DisplayName("Should not refresh valid token")
        void shouldNotRefreshValidToken() throws Exception {
            // Given
            StreamingCredentials validCredentials = createValidCredentials();

            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(validCredentials));

            given(tokenEncryptionService.decrypt(anyString())).willReturn(testAccessToken);

            SpotifyIntegrationService.SpotifyRecommendationsResponse response = 
                new SpotifyIntegrationService.SpotifyRecommendationsResponse(Arrays.asList());

            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), 
                any(HttpEntity.class), eq(SpotifyIntegrationService.SpotifyRecommendationsResponse.class)))
                .willReturn(new ResponseEntity<>(response, HttpStatus.OK));

            // When
            List<RecommendationDTO> recommendations = spotifyIntegrationService
                .getRecommendationsByGenres(testUserId, Arrays.asList("rock"), Map.of());

            // Then
            verify(restTemplate, never()).postForEntity(
                eq(ACCOUNTS_URL + "/api/token"), 
                any(HttpEntity.class), 
                eq(SpotifyIntegrationService.TokenResponse.class));

            assertThat(recommendations).isEmpty();
        }
    }

    @Nested
    @DisplayName("API Integration")
    class ApiIntegration {

        @Test
        @DisplayName("Should get recommendations successfully")
        void shouldGetRecommendationsSuccessfully() throws Exception {
            // Given
            StreamingCredentials validCredentials = createValidCredentials();
            
            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(validCredentials));

            given(tokenEncryptionService.decrypt(anyString())).willReturn(testAccessToken);

            List<SpotifyIntegrationService.SpotifyTrack> tracks = Arrays.asList(
                createMockSpotifyTrack("track1", "Track 1", "Artist 1"),
                createMockSpotifyTrack("track2", "Track 2", "Artist 2")
            );

            SpotifyIntegrationService.SpotifyRecommendationsResponse response = 
                new SpotifyIntegrationService.SpotifyRecommendationsResponse(tracks);

            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), 
                any(HttpEntity.class), eq(SpotifyIntegrationService.SpotifyRecommendationsResponse.class)))
                .willReturn(new ResponseEntity<>(response, HttpStatus.OK));

            // When
            List<RecommendationDTO> recommendations = spotifyIntegrationService
                .getRecommendationsByGenres(testUserId, Arrays.asList("rock", "jazz"), 
                    Map.of("energy", "high", "tempo", 120));

            // Then
            assertThat(recommendations).hasSize(2);
            assertThat(recommendations.get(0).getTrackName()).isEqualTo("Track 1");
            assertThat(recommendations.get(0).getArtistName()).isEqualTo("Artist 1");
            assertThat(recommendations.get(0).getSource()).isEqualTo("spotify");
        }

        @Test
        @DisplayName("Should handle API rate limiting")
        void shouldHandleApiRateLimiting() throws Exception {
            // Given
            StreamingCredentials validCredentials = createValidCredentials();
            
            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(validCredentials));

            given(tokenEncryptionService.decrypt(anyString())).willReturn(testAccessToken);

            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), 
                any(HttpEntity.class), eq(SpotifyIntegrationService.SpotifyRecommendationsResponse.class)))
                .willThrow(new RestClientException("429 Too Many Requests"));

            // When
            List<RecommendationDTO> recommendations = spotifyIntegrationService
                .getRecommendationsByGenres(testUserId, Arrays.asList("rock"), Map.of());

            // Then
            assertThat(recommendations).isEmpty(); // Service should gracefully return empty list
        }

        @Test
        @DisplayName("Should get user top tracks")
        void shouldGetUserTopTracks() throws Exception {
            // Given
            StreamingCredentials validCredentials = createValidCredentials();
            
            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(validCredentials));

            given(tokenEncryptionService.decrypt(anyString())).willReturn(testAccessToken);

            List<SpotifyIntegrationService.SpotifyTrack> tracks = Arrays.asList(
                createMockSpotifyTrack("top1", "Top Track 1", "Top Artist 1")
            );

            SpotifyIntegrationService.SpotifyTracksResponse response = 
                new SpotifyIntegrationService.SpotifyTracksResponse(tracks);

            given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), 
                any(HttpEntity.class), eq(SpotifyIntegrationService.SpotifyTracksResponse.class)))
                .willReturn(new ResponseEntity<>(response, HttpStatus.OK));

            // When
            List<RecommendationDTO> topTracks = spotifyIntegrationService
                .getUserTopTracks(testUserId, "short_term", 10);

            // Then
            assertThat(topTracks).hasSize(1);
            assertThat(topTracks.get(0).getTrackName()).isEqualTo("Top Track 1");
        }
    }

    @Nested
    @DisplayName("Service Management")
    class ServiceManagement {

        @Test
        @DisplayName("Should check connection status")
        void shouldCheckConnectionStatus() {
            // Given
            StreamingCredentials validCredentials = createValidCredentials();
            
            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(validCredentials));

            // When
            boolean isConnected = spotifyIntegrationService.isConnected(testUserId);

            // Then
            assertThat(isConnected).isTrue();
        }

        @Test
        @DisplayName("Should disconnect service")
        void shouldDisconnectService() {
            // Given
            StreamingCredentials existingCredentials = createValidCredentials();
            
            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.of(existingCredentials));

            // When
            boolean disconnected = spotifyIntegrationService.disconnect(testUserId);

            // Then
            assertThat(disconnected).isTrue();
            verify(credentialsRepository).deleteByUserIdAndServiceName(testUserId, "spotify");
        }

        @Test
        @DisplayName("Should handle disconnect when not connected")
        void shouldHandleDisconnectWhenNotConnected() {
            // Given
            given(credentialsRepository.findByUserIdAndServiceName(testUserId, "spotify"))
                .willReturn(Optional.empty());

            // When
            boolean disconnected = spotifyIntegrationService.disconnect(testUserId);

            // Then
            assertThat(disconnected).isFalse();
            verify(credentialsRepository, never()).deleteByUserIdAndServiceName(any(), any());
        }
    }

    // Helper methods

    private StreamingCredentials createValidCredentials() {
        StreamingCredentials credentials = new StreamingCredentials();
        credentials.setId(1L);
        credentials.setUserId(testUserId.toString());
        credentials.setServiceName("spotify");
        credentials.setServiceUserId("spotify_user_123");
        credentials.setAccessToken("encrypted_access_token");
        credentials.setRefreshToken("encrypted_refresh_token");
        credentials.setExpiresAt(LocalDateTime.now().plusHours(1));
        credentials.setScope("user-read-private user-read-email");
        credentials.setMetadata(Map.of("displayName", "Test User"));
        credentials.setCreatedAt(LocalDateTime.now().minusDays(1));
        credentials.setUpdatedAt(LocalDateTime.now());
        return credentials;
    }

    private StreamingCredentials createExpiredCredentials() {
        StreamingCredentials credentials = createValidCredentials();
        credentials.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        return credentials;
    }

    private SpotifyIntegrationService.SpotifyTrack createMockSpotifyTrack(
            String id, String name, String artistName) {
        SpotifyIntegrationService.SpotifyArtist artist = 
            new SpotifyIntegrationService.SpotifyArtist("artist_" + id, artistName);
        SpotifyIntegrationService.SpotifyAlbum album = 
            new SpotifyIntegrationService.SpotifyAlbum("album_" + id, "Album " + id);
        
        return new SpotifyIntegrationService.SpotifyTrack(
            id, name, Arrays.asList(artist), album, 
            "https://preview.url", Map.of("spotify", "https://open.spotify.com/track/" + id),
            210000, 75, false);
    }
}