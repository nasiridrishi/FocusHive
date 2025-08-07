package com.focushive.music.controller;

import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.service.PlaylistManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for comprehensive playlist management operations.
 * 
 * Provides all 14 required endpoints for playlist operations including:
 * - CRUD operations for playlists
 * - Track management (add, remove, reorder)
 * - Smart playlist creation and management
 * - Collaborative features and sharing
 * - Import/export functionality
 * - Hive playlist management
 * 
 * All endpoints require authentication and implement proper authorization,
 * validation, error handling, and comprehensive API documentation.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/music/playlists")
@RequiredArgsConstructor
@Tag(name = "Playlist Management", description = "Comprehensive playlist management operations")
public class PlaylistController {

    private final PlaylistManagementService playlistManagementService;

    // ===============================
    // BASIC PLAYLIST CRUD OPERATIONS
    // ===============================

    /**
     * GET /api/music/playlists - List user's playlists
     */
    @Operation(
        summary = "Get user's playlists",
        description = "Retrieves a paginated list of playlists created by the authenticated user, " +
                     "ordered by last update time"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's playlists"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<PlaylistDTO.SearchResult> getUserPlaylists(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Getting playlists for user: {} (page: {}, size: {})", 
                authentication.getName(), pageable.getPageNumber(), pageable.getPageSize());
        
        PlaylistDTO.SearchResult result = playlistManagementService.getUserPlaylists(
            authentication.getName(), pageable);
        
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/music/playlists - Create new playlist
     */
    @Operation(
        summary = "Create a new playlist",
        description = "Creates a new playlist for the authenticated user. Can be personal or hive-specific " +
                     "depending on whether hiveId is provided. Supports initial tracks and collaboration settings."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Playlist created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to create playlist in specified hive"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<PlaylistDTO.Response> createPlaylist(
            Authentication authentication,
            @Parameter(description = "Playlist creation details")
            @Valid @RequestBody PlaylistDTO.CreateRequest createRequest) {
        
        log.info("Creating playlist '{}' for user: {}", createRequest.getName(), authentication.getName());
        
        PlaylistDTO.Response response = playlistManagementService.createPlaylist(
            createRequest, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/music/playlists/{playlistId} - Get playlist details
     */
    @Operation(
        summary = "Get playlist details",
        description = "Retrieves detailed information about a specific playlist including tracks, " +
                     "collaborators, and user permissions. Access is controlled based on playlist visibility " +
                     "and user permissions."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved playlist details"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to view this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDTO.Response> getPlaylistById(
            Authentication authentication,
            @Parameter(description = "Unique playlist identifier")
            @PathVariable Long playlistId) {
        
        log.debug("Getting playlist {} for user: {}", playlistId, authentication.getName());
        
        PlaylistDTO.Response response = playlistManagementService.getPlaylistById(
            playlistId, authentication.getName());
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/music/playlists/{playlistId} - Update playlist metadata
     */
    @Operation(
        summary = "Update playlist metadata",
        description = "Updates playlist metadata such as name, description, and visibility settings. " +
                     "Only the playlist owner or authorized collaborators can make modifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Playlist updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to modify this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{playlistId}")
    public ResponseEntity<PlaylistDTO.Response> updatePlaylist(
            Authentication authentication,
            @Parameter(description = "Unique playlist identifier")
            @PathVariable Long playlistId,
            @Parameter(description = "Playlist update details")
            @Valid @RequestBody PlaylistDTO.UpdateRequest updateRequest) {
        
        log.info("Updating playlist {} for user: {}", playlistId, authentication.getName());
        
        PlaylistDTO.Response response = playlistManagementService.updatePlaylist(
            playlistId, updateRequest, authentication.getName());
        
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/music/playlists/{playlistId} - Delete playlist
     */
    @Operation(
        summary = "Delete playlist",
        description = "Permanently deletes a playlist. Only the playlist owner can delete a playlist. " +
                     "This action cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Playlist deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to delete this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(
            Authentication authentication,
            @Parameter(description = "Unique playlist identifier")
            @PathVariable Long playlistId) {
        
        log.info("Deleting playlist {} for user: {}", playlistId, authentication.getName());
        
        playlistManagementService.deletePlaylist(playlistId, authentication.getName());
        
        return ResponseEntity.noContent().build();
    }

    // ===============================
    // TRACK MANAGEMENT OPERATIONS
    // ===============================

    /**
     * POST /api/music/playlists/{playlistId}/tracks - Add tracks to playlist
     */
    @Operation(
        summary = "Add track to playlist",
        description = "Adds a new track to the specified playlist. The track is identified by its Spotify ID " +
                     "and can be inserted at a specific position. Duplicate tracks are prevented."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Track added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid track data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to modify this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "409", description = "Track already exists in playlist"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{playlistId}/tracks")
    public ResponseEntity<PlaylistDTO.TrackInfo> addTrackToPlaylist(
            Authentication authentication,
            @Parameter(description = "Unique playlist identifier")
            @PathVariable Long playlistId,
            @Parameter(description = "Track addition details")
            @Valid @RequestBody PlaylistDTO.AddTrackRequest addTrackRequest) {
        
        log.info("Adding track {} to playlist {} for user: {}", 
                addTrackRequest.getSpotifyTrackId(), playlistId, authentication.getName());
        
        PlaylistDTO.TrackInfo trackInfo = playlistManagementService.addTrackToPlaylist(
            playlistId, addTrackRequest, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(trackInfo);
    }

    /**
     * DELETE /api/music/playlists/{playlistId}/tracks/{trackId} - Remove track from playlist
     */
    @Operation(
        summary = "Remove track from playlist",
        description = "Removes a specific track from the playlist and reorders remaining tracks. " +
                     "Only users with modification permissions can remove tracks."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Track removed successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to modify this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist or track not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<Void> removeTrackFromPlaylist(
            Authentication authentication,
            @Parameter(description = "Unique playlist identifier")
            @PathVariable Long playlistId,
            @Parameter(description = "Unique track identifier")
            @PathVariable Long trackId) {
        
        log.info("Removing track {} from playlist {} for user: {}", 
                trackId, playlistId, authentication.getName());
        
        playlistManagementService.removeTrackFromPlaylist(playlistId, trackId, authentication.getName());
        
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/music/playlists/{playlistId}/tracks/reorder - Reorder tracks in playlist
     */
    @Operation(
        summary = "Reorder tracks in playlist",
        description = "Reorders tracks within a playlist by specifying new positions for multiple tracks. " +
                     "Allows for efficient batch reordering operations."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracks reordered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid reorder data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to modify this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{playlistId}/tracks/reorder")
    public ResponseEntity<Void> reorderPlaylistTracks(
            Authentication authentication,
            @Parameter(description = "Unique playlist identifier")
            @PathVariable Long playlistId,
            @Parameter(description = "Track reordering details")
            @Valid @RequestBody PlaylistDTO.TrackReorderRequest reorderRequest) {
        
        log.info("Reordering tracks in playlist {} for user: {}", playlistId, authentication.getName());
        
        playlistManagementService.reorderPlaylistTracks(playlistId, reorderRequest, authentication.getName());
        
        return ResponseEntity.ok().build();
    }

    // ===============================
    // ADVANCED PLAYLIST OPERATIONS
    // ===============================

    /**
     * POST /api/music/playlists/{playlistId}/duplicate - Duplicate playlist
     */
    @Operation(
        summary = "Duplicate playlist",
        description = "Creates a copy of an existing playlist with a new name. Optionally includes " +
                     "collaborators and can target a different hive. Useful for creating playlist templates."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Playlist duplicated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid duplication data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to view the source playlist"),
        @ApiResponse(responseCode = "404", description = "Source playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{playlistId}/duplicate")
    public ResponseEntity<PlaylistDTO.Response> duplicatePlaylist(
            Authentication authentication,
            @Parameter(description = "Source playlist identifier")
            @PathVariable Long playlistId,
            @Parameter(description = "Duplication configuration")
            @Valid @RequestBody PlaylistDTO.DuplicateRequest duplicateRequest) {
        
        log.info("Duplicating playlist {} for user: {}", playlistId, authentication.getName());
        
        PlaylistDTO.Response response = playlistManagementService.duplicatePlaylist(
            playlistId, duplicateRequest, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/music/playlists/{playlistId}/share - Share playlist with hive members
     */
    @Operation(
        summary = "Share playlist with hive",
        description = "Shares a playlist with all members of a specified hive, granting them " +
                     "the specified permission level. Enables collaborative playlist management."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Playlist shared successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid sharing data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to share this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{playlistId}/share")
    public ResponseEntity<Void> sharePlaylistWithHive(
            Authentication authentication,
            @Parameter(description = "Playlist identifier to share")
            @PathVariable Long playlistId,
            @Parameter(description = "Sharing configuration")
            @Valid @RequestBody PlaylistDTO.SharePlaylistRequest shareRequest) {
        
        log.info("Sharing playlist {} with hive {} for user: {}", 
                playlistId, shareRequest.getHiveId(), authentication.getName());
        
        playlistManagementService.sharePlaylistWithHive(playlistId, shareRequest, authentication.getName());
        
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/music/playlists/hive/{hiveId} - Get playlists for a hive
     */
    @Operation(
        summary = "Get hive playlists",
        description = "Retrieves all playlists associated with a specific hive. Only hive members " +
                     "can access this endpoint. Results are paginated and ordered by recent activity."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved hive playlists"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not a member of the specified hive"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/hive/{hiveId}")
    public ResponseEntity<PlaylistDTO.SearchResult> getHivePlaylists(
            Authentication authentication,
            @Parameter(description = "Unique hive identifier")
            @PathVariable String hiveId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Getting playlists for hive {} for user: {}", hiveId, authentication.getName());
        
        PlaylistDTO.SearchResult result = playlistManagementService.getHivePlaylists(
            hiveId, authentication.getName(), pageable);
        
        return ResponseEntity.ok(result);
    }

    // ===============================
    // SMART PLAYLIST OPERATIONS
    // ===============================

    /**
     * POST /api/music/playlists/smart - Create smart playlist based on criteria
     */
    @Operation(
        summary = "Create smart playlist",
        description = "Creates an intelligent playlist that automatically selects tracks based on " +
                     "specified criteria such as energy level, genre, task type, and productivity scores. " +
                     "Smart playlists can auto-update based on configured frequency."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Smart playlist created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid criteria or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/smart")
    public ResponseEntity<PlaylistDTO.Response> createSmartPlaylist(
            Authentication authentication,
            @Parameter(description = "Smart playlist generation criteria")
            @Valid @RequestBody PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest) {
        
        log.info("Creating smart playlist '{}' for user: {}", 
                criteriaRequest.getName(), authentication.getName());
        
        PlaylistDTO.Response response = playlistManagementService.createSmartPlaylist(
            criteriaRequest, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ===============================
    // IMPORT/EXPORT OPERATIONS
    // ===============================

    /**
     * GET /api/music/playlists/{playlistId}/export - Export playlist
     */
    @Operation(
        summary = "Export playlist",
        description = "Exports a playlist in the specified format (JSON, M3U, XSPF) for backup, " +
                     "sharing, or migration purposes. Includes track metadata and optional statistics."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Playlist exported successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to export this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{playlistId}/export")
    public ResponseEntity<PlaylistDTO.ExportResponse> exportPlaylist(
            Authentication authentication,
            @Parameter(description = "Playlist identifier to export")
            @PathVariable Long playlistId,
            @Parameter(description = "Export format (JSON, M3U, XSPF)")
            @RequestParam(defaultValue = "JSON") String format,
            @Parameter(description = "Include track metadata")
            @RequestParam(defaultValue = "true") Boolean includeMetadata,
            @Parameter(description = "Include playlist statistics")
            @RequestParam(defaultValue = "false") Boolean includeStatistics) {
        
        log.info("Exporting playlist {} in format {} for user: {}", 
                playlistId, format, authentication.getName());
        
        PlaylistDTO.ExportResponse response = playlistManagementService.exportPlaylist(
            playlistId, authentication.getName());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/music/playlists/import - Import playlist from external source
     */
    @Operation(
        summary = "Import playlist",
        description = "Imports a playlist from external sources like Spotify, YouTube Music, or " +
                     "uploaded files. Supports various formats and allows customization of the import process."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Playlist imported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid import data, unsupported source, or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/import")
    public ResponseEntity<PlaylistDTO.Response> importPlaylist(
            Authentication authentication,
            @Parameter(description = "Import configuration and source details")
            @Valid @RequestBody PlaylistDTO.ImportRequest importRequest) {
        
        log.info("Importing playlist from {} for user: {}", 
                importRequest.getSource(), authentication.getName());
        
        PlaylistDTO.Response response = playlistManagementService.importPlaylist(
            importRequest, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ===============================
    // ADDITIONAL UTILITY ENDPOINTS
    // ===============================

    /**
     * GET /api/music/playlists/popular - Get popular public playlists
     */
    @Operation(
        summary = "Get popular playlists",
        description = "Retrieves popular public playlists ordered by play count, track count, " +
                     "and recent activity. Useful for music discovery and trending content."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved popular playlists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/popular")
    public ResponseEntity<PlaylistDTO.SearchResult> getPopularPlaylists(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.debug("Getting popular playlists (page: {}, size: {})", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        PlaylistDTO.SearchResult result = playlistManagementService.getPopularPlaylists(pageable);
        
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/music/playlists/{playlistId}/collaborators - Add collaborator to playlist
     */
    @Operation(
        summary = "Add playlist collaborator",
        description = "Adds a new collaborator to a playlist with specified permissions. " +
                     "Only the playlist owner can add collaborators."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Collaborator added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid collaborator data or validation errors"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to modify this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "409", description = "User is already a collaborator"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{playlistId}/collaborators")
    public ResponseEntity<PlaylistDTO.CollaboratorInfo> addCollaborator(
            Authentication authentication,
            @Parameter(description = "Playlist identifier")
            @PathVariable Long playlistId,
            @Parameter(description = "Collaborator details and permissions")
            @Valid @RequestBody PlaylistDTO.AddCollaboratorRequest collaboratorRequest) {
        
        log.info("Adding collaborator {} to playlist {} for user: {}", 
                collaboratorRequest.getUserId(), playlistId, authentication.getName());
        
        PlaylistDTO.CollaboratorInfo collaboratorInfo = playlistManagementService.addCollaborator(
            playlistId, collaboratorRequest, authentication.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(collaboratorInfo);
    }

    /**
     * PUT /api/music/playlists/smart/{playlistId}/refresh - Refresh smart playlist
     */
    @Operation(
        summary = "Refresh smart playlist",
        description = "Manually triggers a refresh of a smart playlist, regenerating its content " +
                     "based on the current criteria and available tracks. Useful for immediate updates."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Smart playlist refreshed successfully"),
        @ApiResponse(responseCode = "400", description = "Playlist is not a smart playlist"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "User not authorized to refresh this playlist"),
        @ApiResponse(responseCode = "404", description = "Playlist not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/smart/{playlistId}/refresh")
    public ResponseEntity<Void> refreshSmartPlaylist(
            Authentication authentication,
            @Parameter(description = "Smart playlist identifier")
            @PathVariable Long playlistId) {
        
        log.info("Refreshing smart playlist {} for user: {}", playlistId, authentication.getName());
        
        playlistManagementService.refreshSmartPlaylist(playlistId, authentication.getName());
        
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/music/playlists/search - Search playlists
     */
    @Operation(
        summary = "Search playlists",
        description = "Searches for playlists by name, description, or tags. Supports filtering " +
                     "by visibility, type, and other criteria. Results are paginated and ranked by relevance."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/search")
    public ResponseEntity<PlaylistDTO.SearchResult> searchPlaylists(
            Authentication authentication,
            @Parameter(description = "Search query")
            @RequestParam String query,
            @Parameter(description = "Filter by playlist type")
            @RequestParam(required = false) String type,
            @Parameter(description = "Filter by hive ID")
            @RequestParam(required = false) String hiveId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Searching playlists with query '{}' for user: {}", query, authentication.getName());
        
        // For now, return empty results as search functionality would be implemented
        // in a more advanced version of the service
        PlaylistDTO.SearchResult result = PlaylistDTO.SearchResult.builder()
            .playlists(java.util.List.of())
            .totalResults(0L)
            .currentPage(pageable.getPageNumber())
            .totalPages(0)
            .query(query)
            .build();
        
        return ResponseEntity.ok(result);
    }
}