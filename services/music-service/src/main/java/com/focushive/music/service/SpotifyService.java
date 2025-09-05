package com.focushive.music.service;

import com.focushive.music.config.SpotifyConfig;
import com.focushive.music.dto.RecommendationDTO;
import com.focushive.music.entity.SpotifyCredentials;
import com.focushive.music.repository.SpotifyCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for Spotify API integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyService {

    private final SpotifyConfig spotifyConfig;
    private final SpotifyCredentialsRepository credentialsRepository;

    /**
     * Get Spotify authorization URL for OAuth flow.
     */
    public String getAuthorizationUrl(String state) {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(spotifyConfig.getClientId())
            .setClientSecret(spotifyConfig.getClientSecret())
            .setRedirectUri(URI.create(spotifyConfig.getRedirectUri()))
            .build();

        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
            .scope(spotifyConfig.getScopes())
            .state(state)
            .show_dialog(true)
            .build();

        return authorizationCodeUriRequest.execute().toString();
    }

    /**
     * Handle OAuth callback and store credentials.
     */
    public SpotifyCredentials handleCallback(String code, String userId) throws Exception {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(spotifyConfig.getClientId())
            .setClientSecret(spotifyConfig.getClientSecret())
            .setRedirectUri(URI.create(spotifyConfig.getRedirectUri()))
            .build();

        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
        AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

        // Get user info
        SpotifyApi userApi = new SpotifyApi.Builder()
            .setAccessToken(authorizationCodeCredentials.getAccessToken())
            .build();
        
        String spotifyUserId = userApi.getCurrentUsersProfile().build().execute().getId();

        // Save or update credentials
        SpotifyCredentials credentials = credentialsRepository.findByUserId(userId)
            .orElse(SpotifyCredentials.builder()
                .userId(userId)
                .build());

        credentials.setAccessToken(authorizationCodeCredentials.getAccessToken());
        credentials.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
        credentials.setExpiresAt(LocalDateTime.now().plusSeconds(authorizationCodeCredentials.getExpiresIn()));
        credentials.setScope(authorizationCodeCredentials.getScope());
        credentials.setSpotifyUserId(spotifyUserId);
        credentials.setIsActive(true);

        return credentialsRepository.save(credentials);
    }

    /**
     * Get music recommendations based on focus session type.
     */
    public List<RecommendationDTO> getRecommendations(String userId, String sessionType, int limit) {
        Optional<SpotifyCredentials> credentialsOpt = credentialsRepository.findActiveCredentials(userId, LocalDateTime.now());
        
        if (credentialsOpt.isEmpty()) {
            log.warn("No active Spotify credentials found for user: {}", userId);
            return new ArrayList<>();
        }

        try {
            SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setAccessToken(credentialsOpt.get().getAccessToken())
                .build();

            // Get user's top tracks as seed
            Track[] topTracks = spotifyApi.getUsersTopTracks()
                .limit(5)
                .build()
                .execute()
                .getItems();

            if (topTracks.length == 0) {
                return new ArrayList<>();
            }

            // Extract track IDs for seeds
            String[] seedTracks = new String[Math.min(topTracks.length, 2)];
            for (int i = 0; i < seedTracks.length; i++) {
                seedTracks[i] = topTracks[i].getId();
            }

            // Adjust parameters based on session type
            Float targetEnergy = getTargetEnergyForSessionType(sessionType);
            Float targetValence = getTargetValenceForSessionType(sessionType);

            // Get recommendations
            var recommendations = spotifyApi.getRecommendations()
                .seed_tracks(String.join(",", seedTracks))
                .limit(limit)
                .target_energy(targetEnergy)
                .target_valence(targetValence)
                .build()
                .execute();

            List<RecommendationDTO> result = new ArrayList<>();
            for (Track track : recommendations.getTracks()) {
                result.add(RecommendationDTO.builder()
                    .spotifyTrackId(track.getId())
                    .title(track.getName())
                    .artist(track.getArtists()[0].getName())
                    .album(track.getAlbum().getName())
                    .albumArt(track.getAlbum().getImages().length > 0 ? 
                        track.getAlbum().getImages()[0].getUrl() : null)
                    .durationMs(track.getDurationMs())
                    .previewUrl(track.getPreviewUrl())
                    .reason("Based on your listening history and " + sessionType + " focus mode")
                    .confidenceScore(0.8)
                    .build());
            }

            return result;
        } catch (Exception e) {
            log.error("Error getting Spotify recommendations for user {}: {}", userId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private Float getTargetEnergyForSessionType(String sessionType) {
        return switch (sessionType.toLowerCase()) {
            case "deep_work" -> 0.3f;
            case "creative" -> 0.6f;
            case "study" -> 0.4f;
            case "relaxation" -> 0.2f;
            default -> 0.5f;
        };
    }

    private Float getTargetValenceForSessionType(String sessionType) {
        return switch (sessionType.toLowerCase()) {
            case "deep_work" -> 0.4f;
            case "creative" -> 0.7f;
            case "study" -> 0.5f;
            case "relaxation" -> 0.3f;
            default -> 0.5f;
        };
    }

    /**
     * Check if user has valid Spotify credentials.
     */
    public boolean hasValidCredentials(String userId) {
        return credentialsRepository.findActiveCredentials(userId, LocalDateTime.now()).isPresent();
    }
}