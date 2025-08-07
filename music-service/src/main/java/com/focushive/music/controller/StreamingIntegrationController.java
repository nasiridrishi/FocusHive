package com.focushive.music.controller;

import com.focushive.music.service.SpotifyIntegrationService;
import com.focushive.music.dto.SpotifyDTO.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for streaming service integration.
 * 
 * Handles OAuth flows and connection management for external
 * streaming services like Spotify, Apple Music, etc.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
@Tag(name = "Streaming Integration", description = "APIs for connecting external streaming services")
public class StreamingIntegrationController {

    private final SpotifyIntegrationService spotifyIntegrationService;

    /**
     * Initiates Spotify OAuth flow.
     * 
     * @param jwt JWT token containing user information
     * @return Redirect to Spotify authorization URL
     */
@GetMapping("/spotify/auth")
    @Operation(summary = "Connect Spotify", 
               description = "Initiate OAuth flow to connect Spotify account")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to Spotify authorization"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER')")
    public RedirectView connectSpotify(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String state = generateStateParameter();
        
        log.info("Initiating Spotify connection for userId: {}", userId);
        
        String authUrl = spotifyIntegrationService.initiateSpotifyAuth(userId, state);
        
        return new RedirectView(authUrl);
    }

    /**
     * Handles Spotify OAuth callback.
     * 
     * @param code Authorization code from Spotify
     * @param state State parameter for security
     * @param error Error parameter if authorization failed
     * @return Connection status response
     */
    @GetMapping("/spotify/callback")
    @Operation(summary = "Spotify OAuth callback", 
               description = "Handle OAuth callback from Spotify")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connection successful"),
        @ApiResponse(responseCode = "400", description = "Invalid authorization code or state"),
        @ApiResponse(responseCode = "500", description = "Connection failed")
    })
    public ResponseEntity<ConnectionResponse> handleSpotifyCallback(
            @Parameter(description = "Authorization code from Spotify")
            @RequestParam(required = false) String code,
            
            @Parameter(description = "State parameter for security")
            @RequestParam(required = false) String state,
            
            @Parameter(description = "Error from Spotify if authorization failed")
            @RequestParam(required = false) String error) {
        
        log.info("Handling Spotify callback with code: {}, state: {}, error: {}", 
            code != null ? "present" : "missing", 
            state != null ? "present" : "missing", 
            error);
        
        if (error != null) {
            log.warn("Spotify authorization failed with error: {}", error);
            return ResponseEntity.badRequest().body(
                new ConnectionResponse(false, "spotify", "Authorization failed: " + error, null)
            );
        }
        
        if (code == null || state == null) {
            log.warn("Missing required parameters in Spotify callback");
            return ResponseEntity.badRequest().body(
                new ConnectionResponse(false, "spotify", "Missing required parameters", null)
            );
        }
        
        try {
            boolean success = spotifyIntegrationService.handleSpotifyCallback(code, state);
            
            if (success) {
                return ResponseEntity.ok(
                    new ConnectionResponse(true, "spotify", "Successfully connected to Spotify", null)
                );
            } else {
                return ResponseEntity.badRequest().body(
                    new ConnectionResponse(false, "spotify", "Failed to connect to Spotify", null)
                );
            }
            
        } catch (Exception e) {
            log.error("Error handling Spotify callback", e);
            return ResponseEntity.internalServerError().body(
                new ConnectionResponse(false, "spotify", "Internal error: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Gets connection status for streaming services.
     * 
     * @param jwt JWT token containing user information
     * @return Connection status for all services
     */
    @GetMapping("/spotify/status")
    @Operation(summary = "Get Spotify connection status", 
               description = "Get detailed connection status for Spotify")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SpotifyConnectionStatusDTO> getSpotifyStatus(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Getting Spotify connection status for userId: {}", userId);
        
        SpotifyConnectionStatusDTO status = spotifyIntegrationService.getConnectionStatus(userId);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Refreshes Spotify access token.
     * 
     * @param jwt JWT token containing user information
     * @return New token information
     */
    @PostMapping("/spotify/refresh")
    @Operation(summary = "Refresh Spotify token", 
               description = "Manually refresh Spotify access token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Spotify not connected"),
        @ApiResponse(responseCode = "500", description = "Token refresh failed")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SpotifyTokenResponseDTO> refreshSpotifyToken(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Refreshing Spotify token for userId: {}", userId);
        
        try {
            SpotifyTokenResponseDTO tokenResponse = spotifyIntegrationService.refreshUserAccessToken(userId);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("Failed to refresh Spotify token for userId: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Disconnects Spotify service.
     * 
     * @param jwt JWT token containing user information
     * @return Disconnection status
     */
    @DeleteMapping("/spotify/disconnect")
    @Operation(summary = "Disconnect Spotify", 
               description = "Disconnect and remove Spotify credentials")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Spotify disconnected successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Spotify not connected"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ConnectionResponse> disconnectSpotify(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Disconnecting Spotify for userId: {}", userId);
        
        boolean success = spotifyIntegrationService.disconnect(userId);
        
        if (success) {
            return ResponseEntity.ok(
                new ConnectionResponse(true, "spotify", "Successfully disconnected from Spotify", null)
            );
        } else {
            return ResponseEntity.badRequest().body(
                new ConnectionResponse(false, "spotify", "Spotify not connected or disconnect failed", null)
            );
        }
    }

    /**
     * Searches for tracks on Spotify.
     * 
     * @param query Search query
     * @param limit Number of results (1-50)
     * @param jwt JWT token containing user information
     * @return Search results
     */
    @GetMapping("/spotify/search")
    @Operation(summary = "Search Spotify tracks", 
               description = "Search for tracks on Spotify")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Spotify not connected"),
        @ApiResponse(responseCode = "500", description = "Search failed")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SpotifyTrackDTO>> searchSpotifyTracks(
            @Parameter(description = "Search query", example = "Bohemian Rhapsody Queen")
            @RequestParam @NotBlank String query,
            
            @Parameter(description = "Number of results (1-50)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Searching Spotify tracks for userId: {}, query: {}", userId, query);
        
        List<SpotifyTrackDTO> results = spotifyIntegrationService.searchTracks(userId, query, limit);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Gets user's Spotify playlists.
     * 
     * @param limit Number of playlists (1-50)
     * @param jwt JWT token containing user information
     * @return User playlists
     */
    @GetMapping("/spotify/playlists")
    @Operation(summary = "Get user playlists", 
               description = "Get user's Spotify playlists")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Playlists retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Spotify not connected"),
        @ApiResponse(responseCode = "500", description = "Failed to get playlists")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<SpotifyPlaylistDTO>> getUserPlaylists(
            @Parameter(description = "Number of playlists (1-50)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Getting Spotify playlists for userId: {}", userId);
        
        List<SpotifyPlaylistDTO> playlists = spotifyIntegrationService.getUserPlaylists(userId, limit);
        
        return ResponseEntity.ok(playlists);
    }

    /**
     * Gets audio features for a track.
     * 
     * @param trackId Spotify track ID
     * @param jwt JWT token containing user information
     * @return Audio features
     */
    @GetMapping("/spotify/audio-features/{trackId}")
    @Operation(summary = "Get track audio features", 
               description = "Get audio features for a Spotify track")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Audio features retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Track or Spotify connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to get audio features")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SpotifyAudioFeaturesDTO> getAudioFeatures(
            @Parameter(description = "Spotify track ID", example = "4iV5W9uYEdYUVa79Axb7Rh")
            @PathVariable @NotBlank String trackId,
            
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        log.info("Getting audio features for track: {} (userId: {})", trackId, userId);
        
        SpotifyAudioFeaturesDTO audioFeatures = spotifyIntegrationService.getAudioFeatures(userId, trackId);
        
        if (audioFeatures != null) {
            return ResponseEntity.ok(audioFeatures);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Generates a secure state parameter for OAuth flow.
     */
    private String generateStateParameter() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // Response DTOs

    /**
     * Response object for connection operations.
     */
    public record ConnectionResponse(
        boolean success,
        String serviceName,
        String message,
        Map<String, Object> metadata
    ) {}

    /**
     * Response object for streaming service status.
     */
    public record StreamingStatusResponse(
        UUID userId,
        Map<String, ServiceStatus> services
    ) {}

    /**
     * Service status information.
     */
    public record ServiceStatus(
        boolean connected,
        String connectedAt,
        Map<String, Object> metadata
    ) {}
}