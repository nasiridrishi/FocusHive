package com.focushive.music.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Objects for Spotify API integration.
 * 
 * Contains all DTOs for Spotify OAuth flow, API responses,
 * and user data management following OpenAPI 3.0 specifications.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
public class SpotifyDTO {

    // OAuth and Token DTOs

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify OAuth token response")
    public record SpotifyTokenResponseDTO(
        @JsonProperty("access_token")
        @Schema(description = "Access token for API calls", example = "BQA7K5m-YFk0Hh4...")
        @NotBlank(message = "Access token cannot be blank")
        String accessToken,

        @JsonProperty("token_type")
        @Schema(description = "Token type", example = "Bearer")
        @NotBlank(message = "Token type cannot be blank")
        String tokenType,

        @JsonProperty("expires_in")
        @Schema(description = "Token expiry in seconds", example = "3600")
        @Min(value = 1, message = "Expires in must be positive")
        Integer expiresIn,

        @JsonProperty("refresh_token")
        @Schema(description = "Refresh token for token renewal", example = "AQA7K5m-YFk0Hh4...")
        String refreshToken,

        @Schema(description = "Granted scopes", example = "user-read-private user-read-email")
        String scope
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Connection status response")
    public record SpotifyConnectionStatusDTO(
        @Schema(description = "Whether user is connected to Spotify")
        boolean connected,

        @Schema(description = "Connection timestamp")
        LocalDateTime connectedAt,

        @Schema(description = "User's display name on Spotify")
        String displayName,

        @Schema(description = "Granted permissions")
        List<String> scopes,

        @Schema(description = "Token expiry timestamp")
        LocalDateTime expiresAt,

        @Schema(description = "Whether token needs refresh")
        boolean tokenExpired
    ) {}

    // User Profile DTOs

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify user profile information")
    public record SpotifyUserDTO(
        @Schema(description = "Spotify user ID", example = "spotify_user_123")
        @NotBlank(message = "Spotify user ID cannot be blank")
        String id,

        @JsonProperty("display_name")
        @Schema(description = "User's display name", example = "John Doe")
        String displayName,

        @Schema(description = "User's email address", example = "john@example.com")
        @Email(message = "Email must be valid")
        String email,

        @Schema(description = "User's country code", example = "US")
        @Size(min = 2, max = 2, message = "Country must be 2-letter code")
        String country,

        @Schema(description = "User's follower information")
        SpotifyFollowersDTO followers,

        @Schema(description = "User's profile images")
        List<SpotifyImageDTO> images,

        @JsonProperty("external_urls")
        @Schema(description = "External URLs for the user")
        Map<String, String> externalUrls
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify user follower information")
    public record SpotifyFollowersDTO(
        @Schema(description = "Total number of followers", example = "1500")
        @Min(value = 0, message = "Followers count cannot be negative")
        Integer total,

        @Schema(description = "Link to followers (usually null for privacy)")
        String href
    ) {}

    // Track and Music DTOs

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify track information")
    public record SpotifyTrackDTO(
        @Schema(description = "Spotify track ID", example = "4iV5W9uYEdYUVa79Axb7Rh")
        @NotBlank(message = "Track ID cannot be blank")
        String id,

        @Schema(description = "Track name", example = "Bohemian Rhapsody")
        @NotBlank(message = "Track name cannot be blank")
        @Size(max = 500, message = "Track name must not exceed 500 characters")
        String name,

        @Schema(description = "Track artists")
        @NotEmpty(message = "Track must have at least one artist")
        List<SpotifyArtistDTO> artists,

        @Schema(description = "Track album information")
        SpotifyAlbumDTO album,

        @JsonProperty("duration_ms")
        @Schema(description = "Track duration in milliseconds", example = "355000")
        @Min(value = 1, message = "Duration must be positive")
        Integer durationMs,

        @Schema(description = "Track popularity (0-100)", example = "85")
        @Min(value = 0, message = "Popularity cannot be negative")
        @Max(value = 100, message = "Popularity cannot exceed 100")
        Integer popularity,

        @Schema(description = "Whether track is explicit")
        Boolean explicit,

        @JsonProperty("preview_url")
        @Schema(description = "30-second preview URL")
        String previewUrl,

        @JsonProperty("external_urls")
        @Schema(description = "External URLs for the track")
        Map<String, String> externalUrls,

        @Schema(description = "Available markets for the track")
        @JsonProperty("available_markets")
        List<String> availableMarkets,

        @JsonProperty("is_playable")
        @Schema(description = "Whether track is playable in current market")
        Boolean isPlayable
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify artist information")
    public record SpotifyArtistDTO(
        @Schema(description = "Spotify artist ID", example = "1dfeR4HaWDbWqFHLkxsg1d")
        @NotBlank(message = "Artist ID cannot be blank")
        String id,

        @Schema(description = "Artist name", example = "Queen")
        @NotBlank(message = "Artist name cannot be blank")
        @Size(max = 500, message = "Artist name must not exceed 500 characters")
        String name,

        @JsonProperty("external_urls")
        @Schema(description = "External URLs for the artist")
        Map<String, String> externalUrls,

        @Schema(description = "Artist genres")
        List<String> genres,

        @Schema(description = "Artist popularity (0-100)", example = "90")
        @Min(value = 0, message = "Popularity cannot be negative")
        @Max(value = 100, message = "Popularity cannot exceed 100")
        Integer popularity,

        @Schema(description = "Artist images")
        List<SpotifyImageDTO> images
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify album information")
    public record SpotifyAlbumDTO(
        @Schema(description = "Spotify album ID", example = "6i6folBtxKV28WX3msQ4FE")
        @NotBlank(message = "Album ID cannot be blank")
        String id,

        @Schema(description = "Album name", example = "A Night at the Opera")
        @NotBlank(message = "Album name cannot be blank")
        @Size(max = 500, message = "Album name must not exceed 500 characters")
        String name,

        @JsonProperty("album_type")
        @Schema(description = "Album type", example = "album", allowableValues = {"album", "single", "compilation"})
        String albumType,

        @JsonProperty("release_date")
        @Schema(description = "Album release date", example = "1975-11-21")
        String releaseDate,

        @JsonProperty("release_date_precision")
        @Schema(description = "Release date precision", example = "day", allowableValues = {"year", "month", "day"})
        String releaseDatePrecision,

        @JsonProperty("total_tracks")
        @Schema(description = "Total number of tracks", example = "12")
        @Min(value = 1, message = "Total tracks must be positive")
        Integer totalTracks,

        @Schema(description = "Album images")
        List<SpotifyImageDTO> images,

        @JsonProperty("external_urls")
        @Schema(description = "External URLs for the album")
        Map<String, String> externalUrls
    ) {}

    // Playlist DTOs

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify playlist information")
    public record SpotifyPlaylistDTO(
        @Schema(description = "Spotify playlist ID", example = "37i9dQZF1DX0XUsuxWHRQd")
        @NotBlank(message = "Playlist ID cannot be blank")
        String id,

        @Schema(description = "Playlist name", example = "RapCaviar")
        @NotBlank(message = "Playlist name cannot be blank")
        @Size(max = 255, message = "Playlist name must not exceed 255 characters")
        String name,

        @Schema(description = "Playlist description")
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @Schema(description = "Playlist owner")
        SpotifyUserDTO owner,

        @Schema(description = "Whether playlist is public")
        @JsonProperty("public")
        Boolean isPublic,

        @Schema(description = "Whether playlist is collaborative")
        Boolean collaborative,

        @Schema(description = "Playlist images")
        List<SpotifyImageDTO> images,

        @JsonProperty("external_urls")
        @Schema(description = "External URLs for the playlist")
        Map<String, String> externalUrls,

        @Schema(description = "Playlist tracks")
        SpotifyPlaylistTracksDTO tracks,

        @Schema(description = "Playlist follower information")
        SpotifyFollowersDTO followers
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify playlist tracks information")
    public record SpotifyPlaylistTracksDTO(
        @Schema(description = "Link to full track list")
        String href,

        @Schema(description = "Total number of tracks", example = "50")
        @Min(value = 0, message = "Total tracks cannot be negative")
        Integer total,

        @Schema(description = "Track items")
        List<SpotifyPlaylistTrackDTO> items
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify playlist track item")
    public record SpotifyPlaylistTrackDTO(
        @JsonProperty("added_at")
        @Schema(description = "When track was added to playlist")
        String addedAt,

        @JsonProperty("added_by")
        @Schema(description = "User who added the track")
        SpotifyUserDTO addedBy,

        @Schema(description = "Whether track is local")
        @JsonProperty("is_local")
        Boolean isLocal,

        @Schema(description = "Track information")
        SpotifyTrackDTO track
    ) {}

    // Audio Features and Recommendations

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify audio features for a track")
    public record SpotifyAudioFeaturesDTO(
        @Schema(description = "Track ID", example = "4iV5W9uYEdYUVa79Axb7Rh")
        @NotBlank(message = "Track ID cannot be blank")
        String id,

        @Schema(description = "Acousticness (0.0-1.0)", example = "0.123")
        @DecimalMin(value = "0.0", message = "Acousticness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Acousticness must be between 0.0 and 1.0")
        Double acousticness,

        @Schema(description = "Danceability (0.0-1.0)", example = "0.678")
        @DecimalMin(value = "0.0", message = "Danceability must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Danceability must be between 0.0 and 1.0")
        Double danceability,

        @Schema(description = "Energy (0.0-1.0)", example = "0.892")
        @DecimalMin(value = "0.0", message = "Energy must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Energy must be between 0.0 and 1.0")
        Double energy,

        @Schema(description = "Instrumentalness (0.0-1.0)", example = "0.000123")
        @DecimalMin(value = "0.0", message = "Instrumentalness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Instrumentalness must be between 0.0 and 1.0")
        Double instrumentalness,

        @Schema(description = "Liveness (0.0-1.0)", example = "0.0982")
        @DecimalMin(value = "0.0", message = "Liveness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Liveness must be between 0.0 and 1.0")
        Double liveness,

        @Schema(description = "Loudness in dB", example = "-5.883")
        Double loudness,

        @Schema(description = "Speechiness (0.0-1.0)", example = "0.0456")
        @DecimalMin(value = "0.0", message = "Speechiness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Speechiness must be between 0.0 and 1.0")
        Double speechiness,

        @Schema(description = "Tempo in BPM", example = "119.878")
        @Min(value = 0, message = "Tempo cannot be negative")
        Double tempo,

        @Schema(description = "Time signature", example = "4")
        @Min(value = 1, message = "Time signature must be positive")
        @Max(value = 7, message = "Time signature cannot exceed 7")
        Integer timeSignature,

        @Schema(description = "Valence (0.0-1.0)", example = "0.428")
        @DecimalMin(value = "0.0", message = "Valence must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Valence must be between 0.0 and 1.0")
        Double valence,

        @JsonProperty("duration_ms")
        @Schema(description = "Track duration in milliseconds", example = "355000")
        @Min(value = 1, message = "Duration must be positive")
        Integer durationMs
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Request for Spotify track recommendations")
    public record SpotifyRecommendationsRequestDTO(
        @Schema(description = "Seed genres (max 5)", example = "[\"rock\", \"jazz\"]")
        @Size(max = 5, message = "Maximum 5 seed genres allowed")
        List<String> seedGenres,

        @Schema(description = "Seed artists (max 5)", example = "[\"4Z8W4fKeB5YxbusRsdQVPb\"]")
        @Size(max = 5, message = "Maximum 5 seed artists allowed")
        List<String> seedArtists,

        @Schema(description = "Seed tracks (max 5)", example = "[\"0c6xIDDpzE81m2q797ordA\"]")
        @Size(max = 5, message = "Maximum 5 seed tracks allowed")
        List<String> seedTracks,

        @Schema(description = "Number of recommendations (1-100)", example = "20")
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 100, message = "Limit cannot exceed 100")
        Integer limit,

        @Schema(description = "Target audio features")
        SpotifyRecommendationTunableAttributesDTO tuneableAttributes
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Tuneable attributes for recommendations")
    public record SpotifyRecommendationTunableAttributesDTO(
        @Schema(description = "Target acousticness (0.0-1.0)")
        @DecimalMin(value = "0.0", message = "Target acousticness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Target acousticness must be between 0.0 and 1.0")
        Double targetAcousticness,

        @Schema(description = "Target danceability (0.0-1.0)")
        @DecimalMin(value = "0.0", message = "Target danceability must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Target danceability must be between 0.0 and 1.0")
        Double targetDanceability,

        @Schema(description = "Target energy (0.0-1.0)")
        @DecimalMin(value = "0.0", message = "Target energy must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Target energy must be between 0.0 and 1.0")
        Double targetEnergy,

        @Schema(description = "Target instrumentalness (0.0-1.0)")
        @DecimalMin(value = "0.0", message = "Target instrumentalness must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Target instrumentalness must be between 0.0 and 1.0")
        Double targetInstrumentalness,

        @Schema(description = "Target popularity (0-100)")
        @Min(value = 0, message = "Target popularity must be between 0 and 100")
        @Max(value = 100, message = "Target popularity must be between 0 and 100")
        Integer targetPopularity,

        @Schema(description = "Target tempo in BPM")
        @Min(value = 0, message = "Target tempo cannot be negative")
        Double targetTempo,

        @Schema(description = "Target valence (0.0-1.0)")
        @DecimalMin(value = "0.0", message = "Target valence must be between 0.0 and 1.0")
        @DecimalMax(value = "1.0", message = "Target valence must be between 0.0 and 1.0")
        Double targetValence
    ) {}

    // Common DTOs

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify image information")
    public record SpotifyImageDTO(
        @Schema(description = "Image URL")
        @NotBlank(message = "Image URL cannot be blank")
        String url,

        @Schema(description = "Image height in pixels")
        @Min(value = 1, message = "Height must be positive")
        Integer height,

        @Schema(description = "Image width in pixels")
        @Min(value = 1, message = "Width must be positive")
        Integer width
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Paging information for paginated responses")
    public record SpotifyPagingDTO<T>(
        @Schema(description = "Link to the full result")
        String href,

        @Schema(description = "Items in current page")
        List<T> items,

        @Schema(description = "Maximum number of items per page")
        @Min(value = 1, message = "Limit must be positive")
        Integer limit,

        @Schema(description = "URL to next page")
        String next,

        @Schema(description = "Offset for current page")
        @Min(value = 0, message = "Offset cannot be negative")
        Integer offset,

        @Schema(description = "URL to previous page")
        String previous,

        @Schema(description = "Total number of items available")
        @Min(value = 0, message = "Total cannot be negative")
        Integer total
    ) {}

    // Response wrapper DTOs

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify recommendations response")
    public record SpotifyRecommendationsResponseDTO(
        @Schema(description = "Recommended tracks")
        @NotEmpty(message = "Tracks list cannot be empty")
        List<SpotifyTrackDTO> tracks,

        @Schema(description = "Recommendation seeds used")
        List<SpotifyRecommendationSeedDTO> seeds
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Recommendation seed information")
    public record SpotifyRecommendationSeedDTO(
        @Schema(description = "Number of tracks available after filtering")
        @Min(value = 0, message = "After filtering count cannot be negative")
        Integer afterFilteringSize,

        @Schema(description = "Number of tracks available after relinking")
        @Min(value = 0, message = "After relinking count cannot be negative")
        Integer afterRelinkingSize,

        @Schema(description = "Link to full track list")
        String href,

        @Schema(description = "Seed ID")
        String id,

        @Schema(description = "Number of recommended tracks from this seed")
        @Min(value = 0, message = "Initial pool size cannot be negative")
        Integer initialPoolSize,

        @Schema(description = "Seed type", allowableValues = {"artist", "track", "genre"})
        String type
    ) {}

    // Error response DTOs

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify API error response")
    public record SpotifyErrorResponseDTO(
        @Schema(description = "Error details")
        SpotifyErrorDTO error
    ) {}

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Spotify API error details")
    public record SpotifyErrorDTO(
        @Schema(description = "HTTP status code", example = "401")
        @Min(value = 400, message = "Status must be a valid HTTP error code")
        @Max(value = 599, message = "Status must be a valid HTTP error code")
        Integer status,

        @Schema(description = "Error message", example = "The access token expired")
        @NotBlank(message = "Error message cannot be blank")
        String message
    ) {}
}