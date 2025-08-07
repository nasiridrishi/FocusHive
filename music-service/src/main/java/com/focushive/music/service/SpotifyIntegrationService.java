package com.focushive.music.service;

import com.focushive.music.dto.RecommendationDTO;
import com.focushive.music.dto.SpotifyDTO.*;
import com.focushive.music.model.StreamingCredentials;
import com.focushive.music.repository.StreamingCredentialsRepository;
import com.focushive.music.exception.MusicServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for integrating with Spotify API.
 * 
 * Handles OAuth flow, token management, and music data retrieval
 * from Spotify's Web API for recommendations and playlist management.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SpotifyIntegrationService {

    private final StreamingCredentialsRepository credentialsRepository;
    private final RestTemplate restTemplate;
    private final TokenEncryptionService tokenEncryptionService;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    @Value("${spotify.api-url}")
    private String apiUrl;

    @Value("${spotify.accounts-url}")
    private String accountsUrl;

    /**
     * Initiates Spotify OAuth flow.
     * 
     * @param userId The user ID
     * @param state State parameter for CSRF protection
     * @return Authorization URL
     */
    public String initiateSpotifyAuth(UUID userId, String state) {
        log.info("Initiating Spotify auth for userId: {}", userId);
        
        String scope = "user-read-private user-read-email streaming user-modify-playback-state " +
                      "playlist-modify-public playlist-modify-private user-library-read user-top-read";
        
        return UriComponentsBuilder.fromHttpUrl(accountsUrl + "/authorize")
            .queryParam("client_id", clientId)
            .queryParam("response_type", "code")
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", scope)
            .queryParam("state", userId + ":" + state)
            .build()
            .toUriString();
    }

    /**
     * Handles Spotify OAuth callback and exchanges code for tokens.
     * 
     * @param code Authorization code from Spotify
     * @param state State parameter containing user ID
     * @return Success status
     */
    public boolean handleSpotifyCallback(String code, String state) {
        log.info("Handling Spotify callback with state: {}", state);
        
        try {
            String[] stateParts = state.split(":");
            if (stateParts.length != 2) {
                throw new MusicServiceException.ValidationException("Invalid state parameter");
            }
            
            UUID userId = UUID.fromString(stateParts[0]);
            
            // Exchange code for tokens
            TokenResponse tokenResponse = exchangeCodeForTokens(code);
            
            // Get user profile to verify connection
            SpotifyUser spotifyUser = getSpotifyUserProfile(tokenResponse.accessToken());
            
            // Store encrypted credentials
            storeSpotifyCredentials(userId, spotifyUser, tokenResponse);
            
            log.info("Successfully connected Spotify account for userId: {}, spotifyId: {}", 
                userId, spotifyUser.id());
            return true;
            
        } catch (Exception e) {
            log.error("Error handling Spotify callback", e);
            throw new MusicServiceException.StreamingServiceException("Spotify", "OAuth callback", e);
        }
    }

    /**
     * Gets music recommendations from Spotify based on genres and audio features.
     * 
     * @param userId The user ID
     * @param genres List of seed genres
     * @param audioFeatures Audio feature preferences
     * @return List of recommendations
     */
    @Transactional(readOnly = true)
    public List<RecommendationDTO> getRecommendationsByGenres(
            UUID userId, List<String> genres, Map<String, Object> audioFeatures) {
        
        log.info("Getting Spotify recommendations for userId: {}, genres: {}", userId, genres);
        
        try {
            StreamingCredentials credentials = getValidCredentials(userId, "spotify");
            
            String seedGenres = genres.stream()
                .limit(5) // Spotify allows max 5 seed values
                .collect(Collectors.joining(","));
            
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/recommendations")
                .queryParam("seed_genres", seedGenres)
                .queryParam("limit", 20);
            
            // Add audio feature parameters
            audioFeatures.forEach((key, value) -> {
                switch (key) {
                    case "energy" -> addAudioFeatureParam(uriBuilder, "energy", value);
                    case "tempo" -> addAudioFeatureParam(uriBuilder, "tempo", value);
                    case "valence" -> addAudioFeatureParam(uriBuilder, "valence", value);
                    case "instrumental" -> addAudioFeatureParam(uriBuilder, "instrumentalness", value);
                    case "popularity" -> addAudioFeatureParam(uriBuilder, "popularity", value);
                }
            });
            
            String decryptedToken = tokenEncryptionService.decrypt(credentials.getAccessToken());
            HttpHeaders headers = createAuthHeaders(decryptedToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SpotifyRecommendationsResponse> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                entity,
                SpotifyRecommendationsResponse.class
            );
            
            if (response.getBody() != null && response.getBody().tracks() != null) {
                return response.getBody().tracks().stream()
                    .map(this::convertToRecommendationDTO)
                    .collect(Collectors.toList());
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error getting Spotify recommendations for userId: {}", userId, e);
            return Collections.emptyList(); // Return empty list instead of throwing exception
        }
    }

    /**
     * Gets user's top tracks from Spotify.
     * 
     * @param userId The user ID
     * @param timeRange Time range (short_term, medium_term, long_term)
     * @param limit Number of tracks to return
     * @return List of top tracks
     */
    @Transactional(readOnly = true)
    public List<RecommendationDTO> getUserTopTracks(UUID userId, String timeRange, int limit) {
        log.info("Getting Spotify top tracks for userId: {}, timeRange: {}", userId, timeRange);
        
        try {
            StreamingCredentials credentials = getValidCredentials(userId, "spotify");
            
            String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/me/top/tracks")
                .queryParam("time_range", timeRange)
                .queryParam("limit", Math.min(limit, 50))
                .toUriString();
            
            String decryptedToken = tokenEncryptionService.decrypt(credentials.getAccessToken());
            HttpHeaders headers = createAuthHeaders(decryptedToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SpotifyTracksResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SpotifyTracksResponse.class);
            
            if (response.getBody() != null && response.getBody().items() != null) {
                return response.getBody().items().stream()
                    .map(this::convertToRecommendationDTO)
                    .collect(Collectors.toList());
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error getting user top tracks for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Refreshes access token if needed.
     */
    private StreamingCredentials refreshTokenIfNeeded(StreamingCredentials credentials) {
        if (credentials.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            log.info("Refreshing Spotify token for userId: {}", credentials.getUserId());
            
            try {
                String decryptedRefreshToken = tokenEncryptionService.decrypt(credentials.getRefreshToken());
                TokenResponse newTokens = refreshAccessToken(decryptedRefreshToken);
                
                credentials.setAccessToken(tokenEncryptionService.encrypt(newTokens.accessToken()));
                credentials.setExpiresAt(LocalDateTime.now().plusSeconds(newTokens.expiresIn()));
                credentials.setUpdatedAt(LocalDateTime.now());
                
                return credentialsRepository.save(credentials);
            } catch (Exception e) {
                log.error("Error refreshing Spotify token", e);
                throw new MusicServiceException.StreamingServiceException(
                    "Spotify", "Token refresh", e);
            }
        }
        
        return credentials;
    }

    /**
     * Gets valid credentials, refreshing if necessary.
     */
    private StreamingCredentials getValidCredentials(UUID userId, String serviceName) {
        StreamingCredentials.StreamingPlatform platform = StreamingCredentials.StreamingPlatform.SPOTIFY;
        StreamingCredentials credentials = credentialsRepository
            .findByUserIdAndPlatform(userId.toString(), platform)
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Streaming credentials", userId + ":" + serviceName));
        
        return refreshTokenIfNeeded(credentials);
    }

    /**
     * Exchanges authorization code for access tokens.
     */
    private TokenResponse exchangeCodeForTokens(String code) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            accountsUrl + "/api/token", request, TokenResponse.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new MusicServiceException.StreamingServiceException(
                "Spotify", "Token exchange failed");
        }
        
        return response.getBody();
    }

    /**
     * Refreshes access token using refresh token.
     */
    private TokenResponse refreshAccessToken(String refreshToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
            accountsUrl + "/api/token", request, TokenResponse.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new MusicServiceException.StreamingServiceException(
                "Spotify", "Token refresh failed");
        }
        
        return response.getBody();
    }

    /**
     * Gets Spotify user profile.
     */
    private SpotifyUser getSpotifyUserProfile(String accessToken) throws Exception {
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<SpotifyUser> response = restTemplate.exchange(
            apiUrl + "/me", HttpMethod.GET, entity, SpotifyUser.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new MusicServiceException.StreamingServiceException(
                "Spotify", "Failed to get user profile");
        }
        
        return response.getBody();
    }

    /**
     * Stores Spotify credentials in database.
     */
    private void storeSpotifyCredentials(UUID userId, SpotifyUser spotifyUser, TokenResponse tokens) {
        StreamingCredentials credentials = credentialsRepository
            .findByUserIdAndServiceName(userId, "spotify")
            .orElseGet(StreamingCredentials::new);
        
        credentials.setUserId(userId.toString());
        credentials.setPlatform(StreamingCredentials.StreamingPlatform.SPOTIFY);
        credentials.setAccessToken(tokenEncryptionService.encrypt(tokens.accessToken()));
        credentials.setRefreshToken(tokenEncryptionService.encrypt(tokens.refreshToken()));
        credentials.setExpiresAt(LocalDateTime.now().plusSeconds(tokens.expiresIn()));
        credentials.setScope(tokens.scope());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("displayName", spotifyUser.display_name());
        metadata.put("email", spotifyUser.email());
        metadata.put("country", spotifyUser.country());
        metadata.put("followers", spotifyUser.followers() != null ? spotifyUser.followers().total() : 0);
        credentials.setMetadata(metadata);
        
        credentials.setCreatedAt(credentials.getCreatedAt() != null ? credentials.getCreatedAt() : LocalDateTime.now());
        credentials.setUpdatedAt(LocalDateTime.now());
        
        credentialsRepository.save(credentials);
    }

    /**
     * Creates authorization headers for Spotify API calls.
     */
    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    /**
     * Adds audio feature parameters to URI builder.
     */
    private void addAudioFeatureParam(UriComponentsBuilder builder, String param, Object value) {
        if (value instanceof String stringValue) {
            double numericValue = switch (stringValue.toLowerCase()) {
                case "low" -> 0.3;
                case "medium" -> 0.6;
                case "high" -> 0.9;
                default -> 0.5;
            };
            builder.queryParam("target_" + param, numericValue);
        } else if (value instanceof Number numberValue) {
            builder.queryParam("target_" + param, numberValue);
        }
    }

    /**
     * Converts Spotify track to RecommendationDTO.
     */
    private RecommendationDTO convertToRecommendationDTO(SpotifyTrack track) {
        return RecommendationDTO.builder()
            .trackId(track.id())
            .trackName(track.name())
            .artistId(track.artists().get(0).id())
            .artistName(track.artists().get(0).name())
            .albumName(track.album() != null ? track.album().name() : null)
            .previewUrl(track.preview_url())
            .externalUrl(track.external_urls() != null ? track.external_urls().get("spotify") : null)
            .durationMs(track.duration_ms())
            .popularity(track.popularity())
            .explicit(track.explicit())
            .source("spotify")
            .reason("Generated based on your preferences")
            .confidence(0.8) // Default confidence score
            .build();
    }

    // Spotify API Response Records
    
    record TokenResponse(
        String access_token,
        String token_type,
        int expires_in,
        String refresh_token,
        String scope
    ) {
        public String accessToken() { return access_token; }
        public String refreshToken() { return refresh_token; }
        public int expiresIn() { return expires_in; }
    }

    record SpotifyUser(
        String id,
        String display_name,
        String email,
        String country,
        SpotifyFollowers followers
    ) {}

    record SpotifyFollowers(int total) {}

    record SpotifyRecommendationsResponse(List<SpotifyTrack> tracks) {}

    record SpotifyTracksResponse(List<SpotifyTrack> items) {}

    record SpotifyTrack(
        String id,
        String name,
        List<SpotifyArtist> artists,
        SpotifyAlbum album,
        String preview_url,
        Map<String, String> external_urls,
        int duration_ms,
        int popularity,
        boolean explicit
    ) {}

    record SpotifyArtist(String id, String name) {}

    record SpotifyAlbum(String id, String name) {}

    // Additional Service Methods

    /**
     * Checks if user has active Spotify connection.
     * 
     * @param userId The user ID
     * @return true if connected and token is valid
     */
    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        try {
            Optional<StreamingCredentials> credentials = credentialsRepository
                .findByUserIdAndPlatform(userId.toString(), StreamingCredentials.StreamingPlatform.SPOTIFY);
            
            if (credentials.isEmpty()) {
                return false;
            }

            StreamingCredentials creds = credentials.get();
            return creds.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5));
            
        } catch (Exception e) {
            log.error("Error checking Spotify connection for userId: {}", userId, e);
            return false;
        }
    }

    /**
     * Gets connection status with detailed information.
     * 
     * @param userId The user ID
     * @return Connection status details
     */
    @Transactional(readOnly = true)
    public SpotifyConnectionStatusDTO getConnectionStatus(UUID userId) {
        Optional<StreamingCredentials> credentials = credentialsRepository
            .findByUserIdAndPlatform(userId.toString(), StreamingCredentials.StreamingPlatform.SPOTIFY);
        
        if (credentials.isEmpty()) {
            return SpotifyConnectionStatusDTO.builder()
                .connected(false)
                .tokenExpired(false)
                .build();
        }

        StreamingCredentials creds = credentials.get();
        boolean tokenExpired = creds.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5));
        
        return SpotifyConnectionStatusDTO.builder()
            .connected(true)
            .connectedAt(creds.getCreatedAt())
            .displayName((String) creds.getMetadata().get("displayName"))
            .expiresAt(creds.getExpiresAt())
            .tokenExpired(tokenExpired)
            .build();
    }

    /**
     * Refreshes access token manually.
     * 
     * @param userId The user ID
     * @return New token response or null if refresh failed
     */
    public SpotifyTokenResponseDTO refreshUserAccessToken(UUID userId) {
        try {
            StreamingCredentials credentials = credentialsRepository
                .findByUserIdAndPlatform(userId.toString(), StreamingCredentials.StreamingPlatform.SPOTIFY)
                .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                    "Spotify credentials", userId.toString()));

            String decryptedRefreshToken = tokenEncryptionService.decrypt(credentials.getRefreshToken());
            TokenResponse newTokens = refreshAccessToken(decryptedRefreshToken);
            
            credentials.setAccessToken(tokenEncryptionService.encrypt(newTokens.accessToken()));
            credentials.setExpiresAt(LocalDateTime.now().plusSeconds(newTokens.expiresIn()));
            credentials.setUpdatedAt(LocalDateTime.now());
            
            credentialsRepository.save(credentials);
            
            log.info("Successfully refreshed Spotify token for userId: {}", userId);
            
            return SpotifyTokenResponseDTO.builder()
                .accessToken(tokenEncryptionService.maskToken(newTokens.accessToken()))
                .tokenType(newTokens.tokenType())
                .expiresIn(newTokens.expiresIn())
                .scope(newTokens.scope())
                .build();
            
        } catch (Exception e) {
            log.error("Failed to refresh Spotify token for userId: {}", userId, e);
            throw new MusicServiceException.StreamingServiceException("Spotify", "Token refresh", e);
        }
    }

    /**
     * Disconnects Spotify account and revokes tokens.
     * 
     * @param userId The user ID
     * @return true if successfully disconnected
     */
    public boolean disconnect(UUID userId) {
        try {
            Optional<StreamingCredentials> credentials = credentialsRepository
                .findByUserIdAndPlatform(userId.toString(), StreamingCredentials.StreamingPlatform.SPOTIFY);
            
            if (credentials.isEmpty()) {
                log.warn("No Spotify credentials found for userId: {}", userId);
                return false;
            }

            // TODO: Call Spotify API to revoke tokens
            // For now, just delete from database
            credentialsRepository.deleteByUserIdAndPlatform(userId.toString(), StreamingCredentials.StreamingPlatform.SPOTIFY);
            
            log.info("Successfully disconnected Spotify for userId: {}", userId);
            return true;
            
        } catch (Exception e) {
            log.error("Error disconnecting Spotify for userId: {}", userId, e);
            return false;
        }
    }

    /**
     * Searches for tracks on Spotify.
     * 
     * @param userId The user ID
     * @param query Search query
     * @param limit Number of results (1-50)
     * @return List of search results
     */
    @Transactional(readOnly = true)
    public List<SpotifyTrackDTO> searchTracks(UUID userId, String query, int limit) {
        log.info("Searching Spotify tracks for userId: {}, query: {}", userId, query);
        
        try {
            StreamingCredentials credentials = getValidCredentials(userId, "spotify");
            
            String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/search")
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("limit", Math.min(Math.max(limit, 1), 50))
                .queryParam("market", "US")
                .toUriString();
            
            String decryptedToken = tokenEncryptionService.decrypt(credentials.getAccessToken());
            HttpHeaders headers = createAuthHeaders(decryptedToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SpotifySearchResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SpotifySearchResponse.class);
            
            if (response.getBody() != null && response.getBody().tracks() != null) {
                return response.getBody().tracks().items().stream()
                    .map(this::convertToSpotifyTrackDTO)
                    .collect(Collectors.toList());
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error searching Spotify tracks for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets user's Spotify playlists.
     * 
     * @param userId The user ID
     * @param limit Number of playlists to return (1-50)
     * @return List of user playlists
     */
    @Transactional(readOnly = true)
    public List<SpotifyPlaylistDTO> getUserPlaylists(UUID userId, int limit) {
        log.info("Getting Spotify playlists for userId: {}", userId);
        
        try {
            StreamingCredentials credentials = getValidCredentials(userId, "spotify");
            
            String url = UriComponentsBuilder
                .fromHttpUrl(apiUrl + "/me/playlists")
                .queryParam("limit", Math.min(Math.max(limit, 1), 50))
                .toUriString();
            
            String decryptedToken = tokenEncryptionService.decrypt(credentials.getAccessToken());
            HttpHeaders headers = createAuthHeaders(decryptedToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SpotifyPlaylistsResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SpotifyPlaylistsResponse.class);
            
            if (response.getBody() != null && response.getBody().items() != null) {
                return response.getBody().items().stream()
                    .map(this::convertToSpotifyPlaylistDTO)
                    .collect(Collectors.toList());
            }
            
            return Collections.emptyList();
            
        } catch (Exception e) {
            log.error("Error getting Spotify playlists for userId: {}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Gets audio features for a track.
     * 
     * @param userId The user ID
     * @param trackId Spotify track ID
     * @return Audio features or null if not found
     */
    @Transactional(readOnly = true)
    public SpotifyAudioFeaturesDTO getAudioFeatures(UUID userId, String trackId) {
        log.info("Getting audio features for track: {} (userId: {})", trackId, userId);
        
        try {
            StreamingCredentials credentials = getValidCredentials(userId, "spotify");
            
            String url = apiUrl + "/audio-features/" + trackId;
            
            String decryptedToken = tokenEncryptionService.decrypt(credentials.getAccessToken());
            HttpHeaders headers = createAuthHeaders(decryptedToken);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            ResponseEntity<SpotifyAudioFeaturesDTO> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, SpotifyAudioFeaturesDTO.class);
            
            return response.getBody();
            
        } catch (Exception e) {
            log.error("Error getting audio features for track: {} (userId: {})", trackId, userId, e);
            return null;
        }
    }

    // Helper conversion methods

    private SpotifyTrackDTO convertToSpotifyTrackDTO(SpotifyTrack track) {
        return SpotifyTrackDTO.builder()
            .id(track.id())
            .name(track.name())
            .artists(track.artists().stream()
                .map(artist -> SpotifyArtistDTO.builder()
                    .id(artist.id())
                    .name(artist.name())
                    .build())
                .collect(Collectors.toList()))
            .album(track.album() != null ? SpotifyAlbumDTO.builder()
                .id(track.album().id())
                .name(track.album().name())
                .build() : null)
            .durationMs(track.duration_ms())
            .popularity(track.popularity())
            .explicit(track.explicit())
            .previewUrl(track.preview_url())
            .externalUrls(track.external_urls())
            .build();
    }

    private SpotifyPlaylistDTO convertToSpotifyPlaylistDTO(SpotifyPlaylist playlist) {
        return SpotifyPlaylistDTO.builder()
            .id(playlist.id())
            .name(playlist.name())
            .description(playlist.description())
            .isPublic(playlist.isPublic())
            .collaborative(playlist.collaborative())
            .externalUrls(playlist.externalUrls())
            .build();
    }

    // Additional response records for new API calls

    record SpotifySearchResponse(SpotifyTracksResponse tracks) {}

    record SpotifyPlaylistsResponse(List<SpotifyPlaylist> items) {}

    record SpotifyPlaylist(
        String id,
        String name,
        String description,
        Boolean isPublic,
        Boolean collaborative,
        Map<String, String> externalUrls
    ) {}
}