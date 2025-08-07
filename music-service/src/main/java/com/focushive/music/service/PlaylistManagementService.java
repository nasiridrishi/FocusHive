package com.focushive.music.service;

import com.focushive.music.client.HiveServiceClient;
import com.focushive.music.client.UserServiceClient;
import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.Playlist;
import com.focushive.music.model.PlaylistCollaborator;
import com.focushive.music.model.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive service for managing playlists in the music service.
 * 
 * Handles all playlist operations including:
 * - CRUD operations for playlists
 * - Track management (add, remove, reorder)
 * - Smart playlist generation and management
 * - Collaborative features and sharing
 * - Import/export functionality
 * - Permission management
 * - Caching strategies
 * 
 * Implements sophisticated playlist management features suitable
 * for a university-level project demonstrating advanced Spring Boot concepts.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistManagementService {

    private final PlaylistRepository playlistRepository;
    private final HiveServiceClient hiveServiceClient;
    private final UserServiceClient userServiceClient;
    private final SpotifyIntegrationService spotifyIntegrationService;
    private final ObjectMapper objectMapper;

    // Cache Keys
    private static final String CACHE_PLAYLISTS = "playlists";
    private static final String CACHE_USER_PLAYLISTS = "userPlaylists";
    private static final String CACHE_HIVE_PLAYLISTS = "hivePlaylists";
    private static final String CACHE_POPULAR_PLAYLISTS = "popularPlaylists";

    // ===============================
    // PLAYLIST CRUD OPERATIONS
    // ===============================

    /**
     * Creates a new playlist.
     * 
     * @param createRequest The playlist creation request
     * @param userId The user creating the playlist
     * @return Created playlist response
     */
    @CachePut(value = CACHE_PLAYLISTS, key = "#result.id")
    @CacheEvict(value = {CACHE_USER_PLAYLISTS, CACHE_HIVE_PLAYLISTS}, allEntries = true)
    public PlaylistDTO.Response createPlaylist(PlaylistDTO.CreateRequest createRequest, String userId) {
        log.info("Creating playlist '{}' for user {}", createRequest.getName(), userId);
        
        validateCreateRequest(createRequest, userId);
        
        Playlist playlist = buildPlaylistFromRequest(createRequest, userId);
        playlist = playlistRepository.save(playlist);
        
        // Add initial tracks if provided
        if (createRequest.getInitialTracks() != null && !createRequest.getInitialTracks().isEmpty()) {
            for (PlaylistDTO.AddTrackRequest trackRequest : createRequest.getInitialTracks()) {
                addTrackToPlaylistInternal(playlist, trackRequest, userId);
            }
            playlist = playlistRepository.save(playlist);
        }
        
        log.info("Created playlist with ID: {}", playlist.getId());
        return convertToResponse(playlist, userId);
    }

    /**
     * Gets a playlist by ID.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     * @return Playlist response
     */
    @Cacheable(value = CACHE_PLAYLISTS, key = "#playlistId")
    @Transactional(readOnly = true)
    public PlaylistDTO.Response getPlaylistById(Long playlistId, String userId) {
        log.debug("Getting playlist {} for user {}", playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserCanViewPlaylist(playlist, userId);
        
        return convertToResponse(playlist, userId);
    }

    /**
     * Updates a playlist.
     * 
     * @param playlistId The playlist ID
     * @param updateRequest The update request
     * @param userId The requesting user ID
     * @return Updated playlist response
     */
    @CachePut(value = CACHE_PLAYLISTS, key = "#playlistId")
    @CacheEvict(value = {CACHE_USER_PLAYLISTS, CACHE_HIVE_PLAYLISTS}, allEntries = true)
    public PlaylistDTO.Response updatePlaylist(Long playlistId, PlaylistDTO.UpdateRequest updateRequest, String userId) {
        log.info("Updating playlist {} for user {}", playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserCanModifyPlaylist(playlist, userId);
        
        updatePlaylistFromRequest(playlist, updateRequest);
        playlist.setUpdatedAt(LocalDateTime.now());
        
        playlist = playlistRepository.save(playlist);
        
        log.info("Updated playlist {}", playlistId);
        return convertToResponse(playlist, userId);
    }

    /**
     * Deletes a playlist.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     */
    @CacheEvict(value = {CACHE_PLAYLISTS, CACHE_USER_PLAYLISTS, CACHE_HIVE_PLAYLISTS}, allEntries = true)
    public void deletePlaylist(Long playlistId, String userId) {
        log.info("Deleting playlist {} for user {}", playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserIsOwner(playlist, userId);
        
        playlistRepository.delete(playlist);
        
        log.info("Deleted playlist {}", playlistId);
    }

    /**
     * Gets user's playlists with pagination.
     * 
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Paginated playlist results
     */
    @Cacheable(value = CACHE_USER_PLAYLISTS, key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getUserPlaylists(String userId, Pageable pageable) {
        log.debug("Getting playlists for user {} (page: {}, size: {})", 
                 userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Playlist> playlistPage = playlistRepository.findByCreatedByOrderByUpdatedAtDesc(userId, pageable);
        
        return convertToSearchResult(playlistPage, userId);
    }

    // ===============================
    // TRACK MANAGEMENT
    // ===============================

    /**
     * Adds a track to a playlist.
     * 
     * @param playlistId The playlist ID
     * @param addTrackRequest The track to add
     * @param userId The requesting user ID
     * @return Added track information
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.TrackInfo addTrackToPlaylist(Long playlistId, PlaylistDTO.AddTrackRequest addTrackRequest, String userId) {
        log.info("Adding track {} to playlist {} by user {}", 
                addTrackRequest.getSpotifyTrackId(), playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserCanModifyPlaylist(playlist, userId);
        
        // Check for duplicate tracks
        if (playlist.containsTrack(addTrackRequest.getSpotifyTrackId())) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Track already exists in playlist");
        }
        
        PlaylistTrack playlistTrack = addTrackToPlaylistInternal(playlist, addTrackRequest, userId);
        playlistRepository.save(playlist);
        
        return convertTrackToInfo(playlistTrack);
    }

    /**
     * Removes a track from a playlist.
     * 
     * @param playlistId The playlist ID
     * @param trackId The track ID to remove
     * @param userId The requesting user ID
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public void removeTrackFromPlaylist(Long playlistId, Long trackId, String userId) {
        log.info("Removing track {} from playlist {} by user {}", trackId, playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserCanModifyPlaylist(playlist, userId);
        
        PlaylistTrack trackToRemove = playlist.getTracks().stream()
            .filter(track -> track.getId().equals(trackId))
            .findFirst()
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Track", trackId.toString()));
        
        playlist.removeTrack(trackToRemove);
        playlistRepository.save(playlist);
        
        log.info("Removed track {} from playlist {}", trackId, playlistId);
    }

    /**
     * Reorders tracks in a playlist.
     * 
     * @param playlistId The playlist ID
     * @param reorderRequest The reorder request
     * @param userId The requesting user ID
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public void reorderPlaylistTracks(Long playlistId, PlaylistDTO.TrackReorderRequest reorderRequest, String userId) {
        log.info("Reordering tracks in playlist {} by user {}", playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserCanModifyPlaylist(playlist, userId);
        
        // Update track positions
        for (Map.Entry<Long, Integer> entry : reorderRequest.getTrackOrders().entrySet()) {
            Long trackId = entry.getKey();
            Integer newPosition = entry.getValue();
            
            playlist.getTracks().stream()
                .filter(track -> track.getId().equals(trackId))
                .findFirst()
                .ifPresent(track -> track.setPositionInPlaylist(newPosition));
        }
        
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Reordered tracks in playlist {}", playlistId);
    }

    // ===============================
    // SMART PLAYLIST FEATURES
    // ===============================

    /**
     * Creates a smart playlist with criteria-based filtering.
     * 
     * @param criteriaRequest The smart playlist criteria
     * @param userId The requesting user ID
     * @return Created smart playlist response
     */
    public PlaylistDTO.Response createSmartPlaylist(PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest, String userId) {
        log.info("Creating smart playlist '{}' for user {}", criteriaRequest.getName(), userId);
        
        validateSmartPlaylistCriteria(criteriaRequest);
        
        Playlist playlist = buildSmartPlaylistFromCriteria(criteriaRequest, userId);
        playlist.setIsSmartPlaylist(true);
        playlist.setSmartCriteria(convertCriteriaToJson(criteriaRequest));
        
        // Generate initial tracks based on criteria
        List<PlaylistTrack> initialTracks = generateTracksForSmartPlaylist(criteriaRequest, userId);
        playlist.getTracks().addAll(initialTracks);
        
        playlist = playlistRepository.save(playlist);
        
        log.info("Created smart playlist with ID: {} containing {} tracks", 
                playlist.getId(), playlist.getTotalTracks());
        
        return convertToResponse(playlist, userId);
    }

    /**
     * Refreshes a smart playlist based on its criteria.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public void refreshSmartPlaylist(Long playlistId, String userId) {
        log.info("Refreshing smart playlist {} for user {}", playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserCanModifyPlaylist(playlist, userId);
        
        if (!playlist.isSmartPlaylist()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Cannot refresh non-smart playlist");
        }
        
        // Parse criteria and regenerate tracks
        PlaylistDTO.SmartPlaylistCriteriaRequest criteria = parseCriteriaFromJson(playlist.getSmartCriteria());
        List<PlaylistTrack> newTracks = generateTracksForSmartPlaylist(criteria, userId);
        
        // Clear existing tracks and add new ones
        playlist.getTracks().clear();
        playlist.getTracks().addAll(newTracks);
        playlist.setLastAutoUpdate(LocalDateTime.now());
        playlist.setUpdatedAt(LocalDateTime.now());
        
        playlistRepository.save(playlist);
        
        log.info("Refreshed smart playlist {} with {} tracks", playlistId, newTracks.size());
    }

    // ===============================
    // COLLABORATIVE FEATURES
    // ===============================

    /**
     * Shares a playlist with hive members.
     * 
     * @param playlistId The playlist ID
     * @param shareRequest The share request
     * @param userId The requesting user ID
     */
    @CacheEvict(value = {CACHE_PLAYLISTS, CACHE_HIVE_PLAYLISTS}, allEntries = true)
    public void sharePlaylistWithHive(Long playlistId, PlaylistDTO.SharePlaylistRequest shareRequest, String userId) {
        log.info("Sharing playlist {} with hive {} by user {}", 
                playlistId, shareRequest.getHiveId(), userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserIsOwner(playlist, userId);
        
        // Get hive members
        List<String> hiveMembers = hiveServiceClient.getHiveMembers(shareRequest.getHiveId());
        
        // Add hive members as collaborators
        for (String memberId : hiveMembers) {
            if (!memberId.equals(userId)) { // Don't add owner as collaborator
                addCollaboratorToPlaylist(playlist, memberId, shareRequest.getPermissionLevel());
            }
        }
        
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Shared playlist {} with {} hive members", playlistId, hiveMembers.size());
    }

    /**
     * Adds a collaborator to a playlist.
     * 
     * @param playlistId The playlist ID
     * @param collaboratorRequest The collaborator request
     * @param userId The requesting user ID
     * @return Added collaborator information
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.CollaboratorInfo addCollaborator(Long playlistId, 
                                                       PlaylistDTO.AddCollaboratorRequest collaboratorRequest, 
                                                       String userId) {
        log.info("Adding collaborator {} to playlist {} by user {}", 
                collaboratorRequest.getUserId(), playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserIsOwner(playlist, userId);
        
        // Verify collaborator user exists
        ResponseEntity<Boolean> userExistsResponse = userServiceClient.userExists(collaboratorRequest.getUserId());
        if (!Boolean.TRUE.equals(userExistsResponse.getBody())) {
            throw new MusicServiceException.ResourceNotFoundException(
                "User", collaboratorRequest.getUserId());
        }
        
        // Check if user is already a collaborator
        boolean alreadyCollaborator = playlist.getCollaborators().stream()
            .anyMatch(c -> c.getUserId().equals(collaboratorRequest.getUserId()));
        
        if (alreadyCollaborator) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "User is already a collaborator");
        }
        
        PlaylistCollaborator collaborator = buildCollaboratorFromRequest(collaboratorRequest, playlist, userId);
        playlist.addCollaborator(collaborator);
        playlist.setUpdatedAt(LocalDateTime.now());
        
        playlistRepository.save(playlist);
        
        log.info("Added collaborator {} to playlist {}", collaboratorRequest.getUserId(), playlistId);
        return convertCollaboratorToInfo(collaborator);
    }

    /**
     * Gets playlists for a hive.
     * 
     * @param hiveId The hive ID
     * @param userId The requesting user ID
     * @param pageable Pagination information
     * @return Paginated hive playlist results
     */
    @Cacheable(value = CACHE_HIVE_PLAYLISTS, key = "#hiveId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getHivePlaylists(String hiveId, String userId, Pageable pageable) {
        log.debug("Getting playlists for hive {} by user {}", hiveId, userId);
        
        // Verify user is member of hive
        if (!hiveServiceClient.verifyHiveMembership(hiveId, userId)) {
            throw new MusicServiceException.UnauthorizedOperationException(
                "User is not a member of hive " + hiveId);
        }
        
        Page<Playlist> playlistPage = playlistRepository.findByHiveIdOrderByUpdatedAtDesc(hiveId, pageable);
        
        return convertToSearchResult(playlistPage, userId);
    }

    // ===============================
    // IMPORT/EXPORT FEATURES
    // ===============================

    /**
     * Exports a playlist.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     * @return Export response
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.ExportResponse exportPlaylist(Long playlistId, String userId) {
        log.info("Exporting playlist {} for user {}", playlistId, userId);
        
        Playlist playlist = findPlaylistById(playlistId);
        validateUserCanViewPlaylist(playlist, userId);
        
        String content = generateExportContent(playlist);
        
        return PlaylistDTO.ExportResponse.builder()
            .playlistName(playlist.getName())
            .format("JSON")
            .content(content)
            .exportedAt(LocalDateTime.now())
            .totalTracks(playlist.getTotalTracks())
            .fileSizeBytes((long) content.length())
            .build();
    }

    /**
     * Imports a playlist from external source.
     * 
     * @param importRequest The import request
     * @param userId The requesting user ID
     * @return Imported playlist response
     */
    public PlaylistDTO.Response importPlaylist(PlaylistDTO.ImportRequest importRequest, String userId) {
        log.info("Importing playlist from {} for user {}", importRequest.getSource(), userId);
        
        validateImportRequest(importRequest);
        
        // Get external playlist info based on source
        Object externalPlaylistInfo = getExternalPlaylistInfo(importRequest);
        
        // Create playlist from imported data
        Playlist playlist = buildPlaylistFromImport(importRequest, externalPlaylistInfo, userId);
        playlist = playlistRepository.save(playlist);
        
        log.info("Imported playlist with ID: {}", playlist.getId());
        return convertToResponse(playlist, userId);
    }

    /**
     * Duplicates a playlist.
     * 
     * @param playlistId The playlist ID to duplicate
     * @param duplicateRequest The duplicate request
     * @param userId The requesting user ID
     * @return Duplicated playlist response
     */
    @CacheEvict(value = {CACHE_USER_PLAYLISTS, CACHE_HIVE_PLAYLISTS}, allEntries = true)
    public PlaylistDTO.Response duplicatePlaylist(Long playlistId, 
                                                 PlaylistDTO.DuplicateRequest duplicateRequest, 
                                                 String userId) {
        log.info("Duplicating playlist {} for user {}", playlistId, userId);
        
        Playlist originalPlaylist = findPlaylistById(playlistId);
        validateUserCanViewPlaylist(originalPlaylist, userId);
        
        Playlist duplicatedPlaylist = buildDuplicatePlaylist(originalPlaylist, duplicateRequest, userId);
        duplicatedPlaylist = playlistRepository.save(duplicatedPlaylist);
        
        log.info("Duplicated playlist with new ID: {}", duplicatedPlaylist.getId());
        return convertToResponse(duplicatedPlaylist, userId);
    }

    // ===============================
    // CACHING & POPULAR PLAYLISTS
    // ===============================

    /**
     * Gets popular public playlists.
     * 
     * @param pageable Pagination information
     * @return Popular playlists
     */
    @Cacheable(value = CACHE_POPULAR_PLAYLISTS, key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getPopularPlaylists(Pageable pageable) {
        log.debug("Getting popular playlists (page: {}, size: {})", 
                 pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Playlist> playlistPage = playlistRepository.findMostPopularPublicPlaylists(pageable);
        
        return convertToSearchResult(playlistPage, null);
    }

    // ===============================
    // PERMISSION SYSTEM
    // ===============================

    /**
     * Checks if a user can modify a playlist.
     * 
     * @param playlistId The playlist ID
     * @param userId The user ID
     * @return true if user can modify
     */
    @Transactional(readOnly = true)
    public boolean canUserModifyPlaylist(Long playlistId, String userId) {
        Playlist playlist = findPlaylistById(playlistId);
        return playlist.canUserModify(userId);
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    private void validateCreateRequest(PlaylistDTO.CreateRequest createRequest, String userId) {
        if (!StringUtils.hasText(createRequest.getName())) {
            throw new IllegalArgumentException("Playlist name is required");
        }
        
        // If hive playlist, verify user is member
        if (StringUtils.hasText(createRequest.getHiveId())) {
            if (!hiveServiceClient.verifyHiveMembership(createRequest.getHiveId(), userId)) {
                throw new MusicServiceException.UnauthorizedOperationException(
                    "User is not a member of hive " + createRequest.getHiveId());
            }
        }
    }

    private Playlist buildPlaylistFromRequest(PlaylistDTO.CreateRequest createRequest, String userId) {
        return Playlist.builder()
            .name(createRequest.getName())
            .description(createRequest.getDescription())
            .createdBy(userId)
            .hiveId(createRequest.getHiveId())
            .isCollaborative(Boolean.TRUE.equals(createRequest.getIsCollaborative()))
            .isPublic(Boolean.TRUE.equals(createRequest.getIsPublic()))
            .imageUrl(createRequest.getImageUrl())
            .totalTracks(0)
            .totalDurationMs(0L)
            .isSmartPlaylist(false)
            .isActive(true)
            .playCount(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .tracks(new ArrayList<>())
            .collaborators(new ArrayList<>())
            .build();
    }

    private Playlist findPlaylistById(Long playlistId) {
        return playlistRepository.findById(playlistId)
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Playlist not found with ID: " + playlistId));
    }

    private void validateUserCanViewPlaylist(Playlist playlist, String userId) {
        // Public playlists can be viewed by anyone
        if (Boolean.TRUE.equals(playlist.getIsPublic())) {
            return;
        }
        
        // Owner can always view
        if (playlist.getCreatedBy().equals(userId)) {
            return;
        }
        
        // Collaborators can view
        boolean isCollaborator = playlist.getCollaborators().stream()
            .anyMatch(c -> c.getUserId().equals(userId));
        
        if (isCollaborator) {
            return;
        }
        
        // Hive members can view hive playlists
        if (StringUtils.hasText(playlist.getHiveId())) {
            if (hiveServiceClient.verifyHiveMembership(playlist.getHiveId(), userId)) {
                return;
            }
        }
        
        throw new MusicServiceException.UnauthorizedOperationException(
            "User not authorized to view this playlist");
    }

    private void validateUserCanModifyPlaylist(Playlist playlist, String userId) {
        if (!playlist.canUserModify(userId)) {
            throw new MusicServiceException.UnauthorizedOperationException(
                "User not authorized to modify this playlist");
        }
    }

    private void validateUserIsOwner(Playlist playlist, String userId) {
        if (!playlist.getCreatedBy().equals(userId)) {
            throw new MusicServiceException.UnauthorizedOperationException(
                "User is not the owner of this playlist");
        }
    }

    private void updatePlaylistFromRequest(Playlist playlist, PlaylistDTO.UpdateRequest updateRequest) {
        if (StringUtils.hasText(updateRequest.getName())) {
            playlist.setName(updateRequest.getName());
        }
        if (updateRequest.getDescription() != null) {
            playlist.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getIsPublic() != null) {
            playlist.setIsPublic(updateRequest.getIsPublic());
        }
        if (StringUtils.hasText(updateRequest.getImageUrl())) {
            playlist.setImageUrl(updateRequest.getImageUrl());
        }
    }

    private PlaylistTrack addTrackToPlaylistInternal(Playlist playlist, 
                                                   PlaylistDTO.AddTrackRequest addTrackRequest, 
                                                   String userId) {
        PlaylistTrack playlistTrack = new PlaylistTrack();
        playlistTrack.setPlaylist(playlist);
        playlistTrack.setSpotifyTrackId(addTrackRequest.getSpotifyTrackId());
        playlistTrack.setAddedBy(userId);
        playlistTrack.setAddedAt(LocalDateTime.now());
        
        // Set position
        int position = addTrackRequest.getPosition() != null ? 
                      addTrackRequest.getPosition() : playlist.getTracks().size();
        playlistTrack.setPositionInPlaylist(position);
        
        // Add track metadata (would be fetched from Spotify in real implementation)
        // For now, using placeholder data
        playlistTrack.setTrackName("Track " + addTrackRequest.getSpotifyTrackId());
        playlistTrack.setArtistName("Unknown Artist");
        playlistTrack.setDurationMs(180000); // 3 minutes default
        
        playlist.addTrack(playlistTrack);
        return playlistTrack;
    }

    private PlaylistDTO.Response convertToResponse(Playlist playlist, String userId) {
        return PlaylistDTO.Response.builder()
            .id(playlist.getId())
            .name(playlist.getName())
            .description(playlist.getDescription())
            .createdBy(playlist.getCreatedBy())
            .hiveId(playlist.getHiveId())
            .isCollaborative(playlist.getIsCollaborative())
            .isPublic(playlist.getIsPublic())
            .spotifyPlaylistId(playlist.getSpotifyPlaylistId())
            .totalTracks(playlist.getTotalTracks())
            .totalDurationMs(playlist.getTotalDurationMs())
            .imageUrl(playlist.getImageUrl())
            .createdAt(playlist.getCreatedAt())
            .updatedAt(playlist.getUpdatedAt())
            .tracks(convertTracksToInfo(playlist.getTracks()))
            .collaborators(convertCollaboratorsToInfo(playlist.getCollaborators()))
            .userPermission(determineUserPermission(playlist, userId))
            .canModify(playlist.canUserModify(userId))
            .formattedDuration(playlist.getFormattedDuration())
            .build();
    }

    private PlaylistDTO.SearchResult convertToSearchResult(Page<Playlist> playlistPage, String userId) {
        List<PlaylistDTO.Response> responses = playlistPage.getContent().stream()
            .map(playlist -> convertToResponse(playlist, userId))
            .collect(Collectors.toList());
        
        return PlaylistDTO.SearchResult.builder()
            .playlists(responses)
            .totalResults(playlistPage.getTotalElements())
            .currentPage(playlistPage.getNumber())
            .totalPages(playlistPage.getTotalPages())
            .build();
    }

    private PlaylistDTO.TrackInfo convertTrackToInfo(PlaylistTrack track) {
        return PlaylistDTO.TrackInfo.builder()
            .id(track.getId())
            .spotifyTrackId(track.getSpotifyTrackId())
            .trackName(track.getTrackName())
            .artistName(track.getArtistName())
            .albumName(track.getAlbumName())
            .durationMs(track.getDurationMs())
            .previewUrl(track.getPreviewUrl())
            .externalUrl(track.getExternalUrl())
            .imageUrl(track.getImageUrl())
            .positionInPlaylist(track.getPositionInPlaylist())
            .addedBy(track.getAddedBy())
            .addedAt(track.getAddedAt())
            .displayName(track.getArtistName() + " - " + track.getTrackName())
            .build();
    }

    private List<PlaylistDTO.TrackInfo> convertTracksToInfo(List<PlaylistTrack> tracks) {
        return tracks.stream()
            .map(this::convertTrackToInfo)
            .collect(Collectors.toList());
    }

    private PlaylistDTO.CollaboratorInfo convertCollaboratorToInfo(PlaylistCollaborator collaborator) {
        return PlaylistDTO.CollaboratorInfo.builder()
            .userId(collaborator.getUserId())
            .permissionLevel(collaborator.getPermissionLevel())
            .canAddTracks(collaborator.getCanAddTracks())
            .canRemoveTracks(collaborator.getCanRemoveTracks())
            .canReorderTracks(collaborator.getCanReorderTracks())
            .canEditPlaylist(collaborator.getCanEditPlaylist())
            .canInviteOthers(collaborator.getCanInviteOthers())
            .addedBy(collaborator.getAddedBy())
            .addedAt(collaborator.getAddedAt())
            .build();
    }

    private List<PlaylistDTO.CollaboratorInfo> convertCollaboratorsToInfo(List<PlaylistCollaborator> collaborators) {
        return collaborators.stream()
            .map(this::convertCollaboratorToInfo)
            .collect(Collectors.toList());
    }

    private String determineUserPermission(Playlist playlist, String userId) {
        if (userId == null) return "NONE";
        
        if (playlist.getCreatedBy().equals(userId)) {
            return "OWNER";
        }
        
        return playlist.getCollaborators().stream()
            .filter(c -> c.getUserId().equals(userId))
            .map(PlaylistCollaborator::getPermissionLevel)
            .findFirst()
            .orElse("NONE");
    }

    // Placeholder methods for advanced features
    private void validateSmartPlaylistCriteria(PlaylistDTO.SmartPlaylistCriteriaRequest criteria) {
        // Validate smart playlist criteria
    }

    private Playlist buildSmartPlaylistFromCriteria(PlaylistDTO.SmartPlaylistCriteriaRequest criteria, String userId) {
        return buildPlaylistFromRequest(
            PlaylistDTO.CreateRequest.builder()
                .name(criteria.getName())
                .description(criteria.getDescription())
                .isPublic(false)
                .isCollaborative(false)
                .build(),
            userId
        );
    }

    private List<PlaylistTrack> generateTracksForSmartPlaylist(PlaylistDTO.SmartPlaylistCriteriaRequest criteria, 
                                                              String userId) {
        // Placeholder: In real implementation, this would use recommendation service
        // and apply the criteria to find matching tracks
        return new ArrayList<>();
    }

    private String convertCriteriaToJson(PlaylistDTO.SmartPlaylistCriteriaRequest criteria) {
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            log.error("Error converting criteria to JSON", e);
            return "{}";
        }
    }

    private PlaylistDTO.SmartPlaylistCriteriaRequest parseCriteriaFromJson(String json) {
        try {
            return objectMapper.readValue(json, PlaylistDTO.SmartPlaylistCriteriaRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing criteria from JSON", e);
            return PlaylistDTO.SmartPlaylistCriteriaRequest.builder().build();
        }
    }

    private void addCollaboratorToPlaylist(Playlist playlist, String userId, String permissionLevel) {
        PlaylistCollaborator collaborator = new PlaylistCollaborator();
        collaborator.setUserId(userId);
        collaborator.setPermissionLevel(permissionLevel);
        collaborator.setAddedAt(LocalDateTime.now());
        
        // Set permissions based on level
        switch (permissionLevel.toUpperCase()) {
            case "EDITOR":
                collaborator.setCanAddTracks(true);
                collaborator.setCanRemoveTracks(true);
                collaborator.setCanReorderTracks(true);
                collaborator.setCanEditPlaylist(false);
                collaborator.setCanInviteOthers(false);
                break;
            case "VIEWER":
            default:
                collaborator.setCanAddTracks(false);
                collaborator.setCanRemoveTracks(false);
                collaborator.setCanReorderTracks(false);
                collaborator.setCanEditPlaylist(false);
                collaborator.setCanInviteOthers(false);
                break;
        }
        
        playlist.addCollaborator(collaborator);
    }

    private PlaylistCollaborator buildCollaboratorFromRequest(PlaylistDTO.AddCollaboratorRequest request, 
                                                            Playlist playlist, String addedBy) {
        PlaylistCollaborator collaborator = new PlaylistCollaborator();
        collaborator.setPlaylist(playlist);
        collaborator.setUserId(request.getUserId());
        collaborator.setPermissionLevel(request.getPermissionLevel());
        collaborator.setAddedBy(addedBy);
        collaborator.setAddedAt(LocalDateTime.now());
        
        // Use custom permissions if provided, otherwise use defaults for permission level
        collaborator.setCanAddTracks(request.getCanAddTracks() != null ? 
                                    request.getCanAddTracks() : "EDITOR".equals(request.getPermissionLevel()));
        collaborator.setCanRemoveTracks(request.getCanRemoveTracks() != null ? 
                                       request.getCanRemoveTracks() : "EDITOR".equals(request.getPermissionLevel()));
        collaborator.setCanReorderTracks(request.getCanReorderTracks() != null ? 
                                        request.getCanReorderTracks() : "EDITOR".equals(request.getPermissionLevel()));
        collaborator.setCanEditPlaylist(request.getCanEditPlaylist() != null ? 
                                       request.getCanEditPlaylist() : false);
        collaborator.setCanInviteOthers(request.getCanInviteOthers() != null ? 
                                       request.getCanInviteOthers() : false);
        
        return collaborator;
    }

    private String generateExportContent(Playlist playlist) {
        // Placeholder: Generate export content in specified format
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("name", playlist.getName());
        exportData.put("description", playlist.getDescription());
        exportData.put("tracks", playlist.getTracks().stream()
            .map(track -> Map.of(
                "spotifyTrackId", track.getSpotifyTrackId(),
                "trackName", track.getTrackName(),
                "artistName", track.getArtistName(),
                "position", track.getPositionInPlaylist()
            )).collect(Collectors.toList()));
        
        try {
            return objectMapper.writeValueAsString(exportData);
        } catch (JsonProcessingException e) {
            log.error("Error generating export content", e);
            return "{}";
        }
    }

    private void validateImportRequest(PlaylistDTO.ImportRequest importRequest) {
        if (!StringUtils.hasText(importRequest.getSource()) || 
            !StringUtils.hasText(importRequest.getExternalPlaylistId())) {
            throw new IllegalArgumentException("Source and external playlist ID are required");
        }
    }

    private Object getExternalPlaylistInfo(PlaylistDTO.ImportRequest importRequest) {
        // Placeholder: Get playlist info from external source
        switch (importRequest.getSource().toUpperCase()) {
            case "SPOTIFY":
                return spotifyIntegrationService.getPlaylistInfo(importRequest.getExternalPlaylistId());
            default:
                throw new MusicServiceException.UnsupportedOperationException(
                    "Import source not supported: " + importRequest.getSource());
        }
    }

    private Playlist buildPlaylistFromImport(PlaylistDTO.ImportRequest importRequest, 
                                           Object externalPlaylistInfo, String userId) {
        // Placeholder: Build playlist from external data
        String playlistName = StringUtils.hasText(importRequest.getNewPlaylistName()) ? 
                             importRequest.getNewPlaylistName() : "Imported Playlist";
        
        return Playlist.builder()
            .name(playlistName)
            .description("Imported from " + importRequest.getSource())
            .createdBy(userId)
            .isPublic(Boolean.TRUE.equals(importRequest.getMakePublic()))
            .isCollaborative(false)
            .totalTracks(0)
            .totalDurationMs(0L)
            .isSmartPlaylist(false)
            .isActive(true)
            .playCount(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .tracks(new ArrayList<>())
            .collaborators(new ArrayList<>())
            .build();
    }

    private Playlist buildDuplicatePlaylist(Playlist originalPlaylist, 
                                          PlaylistDTO.DuplicateRequest duplicateRequest, 
                                          String userId) {
        Playlist duplicate = Playlist.builder()
            .name(duplicateRequest.getNewName())
            .description(duplicateRequest.getNewDescription() != null ? 
                        duplicateRequest.getNewDescription() : originalPlaylist.getDescription())
            .createdBy(userId)
            .hiveId(duplicateRequest.getTargetHiveId())
            .isPublic(Boolean.TRUE.equals(duplicateRequest.getMakePublic()))
            .isCollaborative(false) // Duplicates are not collaborative by default
            .imageUrl(originalPlaylist.getImageUrl())
            .isSmartPlaylist(false)
            .isActive(true)
            .playCount(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .tracks(new ArrayList<>())
            .collaborators(new ArrayList<>())
            .build();
        
        // Copy tracks
        for (PlaylistTrack originalTrack : originalPlaylist.getTracks()) {
            PlaylistTrack duplicateTrack = new PlaylistTrack();
            duplicateTrack.setPlaylist(duplicate);
            duplicateTrack.setSpotifyTrackId(originalTrack.getSpotifyTrackId());
            duplicateTrack.setTrackName(originalTrack.getTrackName());
            duplicateTrack.setArtistName(originalTrack.getArtistName());
            duplicateTrack.setAlbumName(originalTrack.getAlbumName());
            duplicateTrack.setDurationMs(originalTrack.getDurationMs());
            duplicateTrack.setPositionInPlaylist(originalTrack.getPositionInPlaylist());
            duplicateTrack.setAddedBy(userId);
            duplicateTrack.setAddedAt(LocalDateTime.now());
            
            duplicate.addTrack(duplicateTrack);
        }
        
        // Copy collaborators if requested
        if (Boolean.TRUE.equals(duplicateRequest.getIncludeCollaborators())) {
            for (PlaylistCollaborator originalCollaborator : originalPlaylist.getCollaborators()) {
                PlaylistCollaborator duplicateCollaborator = new PlaylistCollaborator();
                duplicateCollaborator.setPlaylist(duplicate);
                duplicateCollaborator.setUserId(originalCollaborator.getUserId());
                duplicateCollaborator.setPermissionLevel(originalCollaborator.getPermissionLevel());
                duplicateCollaborator.setCanAddTracks(originalCollaborator.getCanAddTracks());
                duplicateCollaborator.setCanRemoveTracks(originalCollaborator.getCanRemoveTracks());
                duplicateCollaborator.setCanReorderTracks(originalCollaborator.getCanReorderTracks());
                duplicateCollaborator.setCanEditPlaylist(originalCollaborator.getCanEditPlaylist());
                duplicateCollaborator.setCanInviteOthers(originalCollaborator.getCanInviteOthers());
                duplicateCollaborator.setAddedBy(userId);
                duplicateCollaborator.setAddedAt(LocalDateTime.now());
                
                duplicate.addCollaborator(duplicateCollaborator);
            }
        }
        
        return duplicate;
    }
}