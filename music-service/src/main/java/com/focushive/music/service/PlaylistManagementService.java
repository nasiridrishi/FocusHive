package com.focushive.music.service;

import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.model.Playlist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade service for managing playlists in the music service.
 * 
 * This service implements the Facade Pattern, delegating operations to
 * specialized service classes that follow the Single Responsibility Principle:
 * 
 * - PlaylistCrudService: Basic CRUD operations
 * - PlaylistTrackService: Track management within playlists
 * - SmartPlaylistService: AI/smart playlist generation
 * - PlaylistSharingService: Sharing and collaboration features
 * 
 * This refactoring improves maintainability, testability, and follows
 * SOLID principles while maintaining backward compatibility.
 * 
 * @author FocusHive Development Team
 * @version 2.0.0 - Refactored to use Facade Pattern
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistManagementService {

    // Specialized service delegates
    private final PlaylistCrudService playlistCrudService;
    private final PlaylistTrackService playlistTrackService;
    private final SmartPlaylistService smartPlaylistService;
    private final PlaylistSharingService playlistSharingService;

    // ===============================
    // PLAYLIST CRUD OPERATIONS
    // ===============================

    /**
     * Creates a new playlist.
     * Delegates to PlaylistCrudService and PlaylistTrackService for initial tracks.
     * 
     * @param createRequest The playlist creation request
     * @param userId The user creating the playlist
     * @return Created playlist response
     */
    public PlaylistDTO.Response createPlaylist(PlaylistDTO.CreateRequest createRequest, String userId) {
        log.info("Creating playlist '{}' for user {} [FACADE]", createRequest.getName(), userId);
        
        // Delegate to CRUD service for basic playlist creation
        PlaylistDTO.Response response = playlistCrudService.createPlaylist(createRequest, userId);
        
        // Add initial tracks if provided
        if (createRequest.getInitialTracks() != null && !createRequest.getInitialTracks().isEmpty()) {
            for (PlaylistDTO.AddTrackRequest trackRequest : createRequest.getInitialTracks()) {
                playlistTrackService.addTrackToPlaylist(response.getId(), trackRequest, userId);
            }
            // Fetch updated playlist with tracks
            response = playlistCrudService.getPlaylistById(response.getId(), userId);
        }
        
        log.info("Created playlist with ID: {} [FACADE]", response.getId());
        return response;
    }

    /**
     * Gets a playlist by ID.
     * Delegates to PlaylistCrudService.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     * @return Playlist response
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.Response getPlaylistById(Long playlistId, String userId) {
        return playlistCrudService.getPlaylistById(playlistId, userId);
    }

    /**
     * Updates a playlist.
     * Delegates to PlaylistCrudService.
     * 
     * @param playlistId The playlist ID
     * @param updateRequest The update request
     * @param userId The requesting user ID
     * @return Updated playlist response
     */
    public PlaylistDTO.Response updatePlaylist(Long playlistId, PlaylistDTO.UpdateRequest updateRequest, String userId) {
        return playlistCrudService.updatePlaylist(playlistId, updateRequest, userId);
    }

    /**
     * Deletes a playlist.
     * Delegates to PlaylistCrudService.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     */
    public void deletePlaylist(Long playlistId, String userId) {
        playlistCrudService.deletePlaylist(playlistId, userId);
    }

    /**
     * Gets user's playlists with pagination.
     * Delegates to PlaylistCrudService.
     * 
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Paginated playlist results
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getUserPlaylists(String userId, Pageable pageable) {
        return playlistCrudService.getUserPlaylists(userId, pageable);
    }

    // ===============================
    // TRACK MANAGEMENT
    // ===============================

    /**
     * Adds a track to a playlist.
     * Delegates to PlaylistTrackService.
     * 
     * @param playlistId The playlist ID
     * @param addTrackRequest The track to add
     * @param userId The requesting user ID
     * @return Added track information
     */
    public PlaylistDTO.TrackInfo addTrackToPlaylist(Long playlistId, PlaylistDTO.AddTrackRequest addTrackRequest, String userId) {
        return playlistTrackService.addTrackToPlaylist(playlistId, addTrackRequest, userId);
    }

    /**
     * Removes a track from a playlist.
     * Delegates to PlaylistTrackService.
     * 
     * @param playlistId The playlist ID
     * @param trackId The track ID to remove
     * @param userId The requesting user ID
     */
    public void removeTrackFromPlaylist(Long playlistId, Long trackId, String userId) {
        playlistTrackService.removeTrackFromPlaylist(playlistId, trackId, userId);
    }

    /**
     * Reorders tracks in a playlist.
     * Delegates to PlaylistTrackService.
     * 
     * @param playlistId The playlist ID
     * @param reorderRequest The reorder request
     * @param userId The requesting user ID
     */
    public void reorderPlaylistTracks(Long playlistId, PlaylistDTO.TrackReorderRequest reorderRequest, String userId) {
        playlistTrackService.reorderPlaylistTracks(playlistId, reorderRequest, userId);
    }

    // ===============================
    // SMART PLAYLIST FEATURES
    // ===============================

    /**
     * Creates a smart playlist with criteria-based filtering.
     * Delegates to SmartPlaylistService.
     * 
     * @param criteriaRequest The smart playlist criteria
     * @param userId The requesting user ID
     * @return Created smart playlist response
     */
    public PlaylistDTO.Response createSmartPlaylist(PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest, String userId) {
        return smartPlaylistService.createSmartPlaylist(criteriaRequest, userId);
    }

    /**
     * Refreshes a smart playlist based on its criteria.
     * Delegates to SmartPlaylistService.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     * @return Updated playlist response
     */
    public PlaylistDTO.Response refreshSmartPlaylist(Long playlistId, String userId) {
        return smartPlaylistService.refreshSmartPlaylist(playlistId, userId);
    }

    // ===============================
    // COLLABORATIVE FEATURES
    // ===============================

    /**
     * Shares a playlist with hive members.
     * Delegates to PlaylistSharingService.
     * 
     * @param playlistId The playlist ID
     * @param shareRequest The share request
     * @param userId The requesting user ID
     * @return Sharing response with details
     */
    public PlaylistDTO.SharingResponse sharePlaylistWithHive(Long playlistId, PlaylistDTO.SharePlaylistRequest shareRequest, String userId) {
        return playlistSharingService.sharePlaylistWithHive(playlistId, shareRequest, userId);
    }

    /**
     * Adds a collaborator to a playlist.
     * Delegates to PlaylistSharingService.
     * 
     * @param playlistId The playlist ID
     * @param collaboratorRequest The collaborator request
     * @param userId The requesting user ID
     * @return Added collaborator information
     */
    public PlaylistDTO.CollaboratorInfo addCollaborator(Long playlistId, 
                                                       PlaylistDTO.AddCollaboratorRequest collaboratorRequest, 
                                                       String userId) {
        return playlistSharingService.addCollaborator(playlistId, collaboratorRequest, userId);
    }

    /**
     * Gets playlists for a hive.
     * Delegates to PlaylistSharingService.
     * 
     * @param hiveId The hive ID
     * @param userId The requesting user ID
     * @param pageable Pagination information
     * @return Paginated hive playlist results
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getHivePlaylists(String hiveId, String userId, Pageable pageable) {
        return playlistSharingService.getHivePlaylists(hiveId, userId, pageable);
    }

    // ===============================
    // IMPORT/EXPORT FEATURES
    // ===============================

    /**
     * Exports a playlist.
     * Delegates to PlaylistSharingService.
     * 
     * @param playlistId The playlist ID
     * @param exportRequest The export request
     * @param userId The requesting user ID
     * @return Export response
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.ExportResponse exportPlaylist(Long playlistId, PlaylistDTO.ExportRequest exportRequest, String userId) {
        return playlistSharingService.exportPlaylist(playlistId, exportRequest, userId);
    }

    /**
     * Imports a playlist from external source.
     * Delegates to PlaylistSharingService.
     * 
     * @param importRequest The import request
     * @param userId The requesting user ID
     * @return Imported playlist response
     */
    public PlaylistDTO.Response importPlaylist(PlaylistDTO.ImportRequest importRequest, String userId) {
        return playlistSharingService.importPlaylist(importRequest, userId);
    }

    /**
     * Duplicates a playlist.
     * Delegates to PlaylistSharingService.
     * 
     * @param playlistId The playlist ID to duplicate
     * @param duplicateRequest The duplicate request
     * @param userId The requesting user ID
     * @return Duplicated playlist response
     */
    public PlaylistDTO.Response duplicatePlaylist(Long playlistId, 
                                                 PlaylistDTO.DuplicateRequest duplicateRequest, 
                                                 String userId) {
        return playlistSharingService.duplicatePlaylist(playlistId, duplicateRequest, userId);
    }

    // ===============================
    // POPULAR PLAYLISTS
    // ===============================

    /**
     * Gets popular public playlists.
     * Delegates to PlaylistCrudService.
     * 
     * @param pageable Pagination information
     * @return Popular playlists
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getPopularPlaylists(Pageable pageable) {
        return playlistCrudService.getPopularPlaylists(pageable);
    }

    // ===============================
    // PERMISSION SYSTEM
    // ===============================

    /**
     * Checks if a user can modify a playlist.
     * Delegates to PlaylistCrudService.
     * 
     * @param playlistId The playlist ID
     * @param userId The user ID
     * @return true if user can modify
     */
    @Transactional(readOnly = true)
    public boolean canUserModifyPlaylist(Long playlistId, String userId) {
        return playlistCrudService.canUserModifyPlaylist(playlistId, userId);
    }

    // ===============================
    // ADDITIONAL FACADE METHODS
    // ===============================

    /**
     * Gets shared playlists where user is a collaborator.
     * Delegates to PlaylistSharingService.
     * 
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Paginated shared playlist results
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getSharedPlaylists(String userId, Pageable pageable) {
        return playlistSharingService.getSharedPlaylists(userId, pageable);
    }

    /**
     * Gets smart playlist criteria.
     * Delegates to SmartPlaylistService.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     * @return Smart playlist criteria
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SmartPlaylistCriteriaRequest getSmartPlaylistCriteria(Long playlistId, String userId) {
        return smartPlaylistService.getSmartPlaylistCriteria(playlistId, userId);
    }

    /**
     * Updates smart playlist criteria.
     * Delegates to SmartPlaylistService.
     * 
     * @param playlistId The smart playlist ID
     * @param criteriaRequest The new criteria
     * @param userId The requesting user ID
     * @param autoRefresh Whether to automatically refresh tracks
     * @return Updated playlist response
     */
    public PlaylistDTO.Response updateSmartPlaylistCriteria(Long playlistId, 
                                                           PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest, 
                                                           String userId, 
                                                           boolean autoRefresh) {
        return smartPlaylistService.updateSmartPlaylistCriteria(playlistId, criteriaRequest, userId, autoRefresh);
    }

    /**
     * Converts a regular playlist to a smart playlist.
     * Delegates to SmartPlaylistService.
     * 
     * @param playlistId The playlist ID
     * @param criteriaRequest The smart criteria to apply
     * @param userId The requesting user ID
     * @param preserveExistingTracks Whether to keep existing tracks
     * @return Updated playlist response
     */
    public PlaylistDTO.Response convertToSmartPlaylist(Long playlistId, 
                                                      PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest, 
                                                      String userId, 
                                                      boolean preserveExistingTracks) {
        return smartPlaylistService.convertToSmartPlaylist(playlistId, criteriaRequest, userId, preserveExistingTracks);
    }

    /**
     * Converts a smart playlist back to a regular playlist.
     * Delegates to SmartPlaylistService.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     * @return Updated playlist response
     */
    public PlaylistDTO.Response convertToRegularPlaylist(Long playlistId, String userId) {
        return smartPlaylistService.convertToRegularPlaylist(playlistId, userId);
    }

    /**
     * Removes a collaborator from a playlist.
     * Delegates to PlaylistSharingService.
     * 
     * @param playlistId The playlist ID
     * @param collaboratorUserId The collaborator user ID to remove
     * @param userId The requesting user ID
     */
    public void removeCollaborator(Long playlistId, String collaboratorUserId, String userId) {
        playlistSharingService.removeCollaborator(playlistId, collaboratorUserId, userId);
    }

    /**
     * Updates collaborator permissions.
     * Delegates to PlaylistSharingService.
     * 
     * @param playlistId The playlist ID
     * @param collaboratorUserId The collaborator user ID
     * @param updateRequest The permission update request
     * @param userId The requesting user ID
     * @return Updated collaborator information
     */
    public PlaylistDTO.CollaboratorInfo updateCollaboratorPermissions(Long playlistId, 
                                                                     String collaboratorUserId,
                                                                     PlaylistDTO.UpdateCollaboratorRequest updateRequest, 
                                                                     String userId) {
        return playlistSharingService.updateCollaboratorPermissions(playlistId, collaboratorUserId, updateRequest, userId);
    }

    /**
     * Gets collaboration statistics for a playlist.
     * Delegates to PlaylistSharingService.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     * @return Collaboration statistics
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.CollaborationStats getCollaborationStats(Long playlistId, String userId) {
        return playlistSharingService.getCollaborationStats(playlistId, userId);
    }

    /**
     * Enhanced track operations - batch add tracks.
     * Delegates to PlaylistTrackService.
     * 
     * @param playlistId The playlist ID
     * @param addTracksRequest The batch tracks request
     * @param userId The requesting user ID
     * @return Batch operation response
     */
    public PlaylistDTO.BatchTrackResponse addTracksToPlaylist(Long playlistId, 
                                                            PlaylistDTO.BatchAddTracksRequest addTracksRequest, 
                                                            String userId) {
        return playlistTrackService.addTracksToPlaylist(playlistId, addTracksRequest, userId);
    }

    /**
     * Enhanced track operations - batch remove tracks.
     * Delegates to PlaylistTrackService.
     * 
     * @param playlistId The playlist ID
     * @param removeTracksRequest The batch removal request
     * @param userId The requesting user ID
     * @return Batch operation response
     */
    public PlaylistDTO.BatchTrackResponse removeTracksFromPlaylist(Long playlistId, 
                                                                 PlaylistDTO.BatchRemoveTracksRequest removeTracksRequest, 
                                                                 String userId) {
        return playlistTrackService.removeTracksFromPlaylist(playlistId, removeTracksRequest, userId);
    }

    /**
     * Moves a track to a specific position.
     * Delegates to PlaylistTrackService.
     * 
     * @param playlistId The playlist ID
     * @param trackId The track ID to move
     * @param newPosition The new position
     * @param userId The requesting user ID
     */
    public void moveTrackToPosition(Long playlistId, Long trackId, Integer newPosition, String userId) {
        playlistTrackService.moveTrackToPosition(playlistId, trackId, newPosition, userId);
    }

    /**
     * Shuffles all tracks in a playlist.
     * Delegates to PlaylistTrackService.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     */
    public void shufflePlaylistTracks(Long playlistId, String userId) {
        playlistTrackService.shufflePlaylistTracks(playlistId, userId);
    }
}