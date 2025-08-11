package com.focushive.music.service;

import com.focushive.music.client.HiveServiceClient;
import com.focushive.music.client.UserServiceClient;
import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.Playlist;
import com.focushive.music.model.PlaylistCollaborator;
import com.focushive.music.model.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for basic CRUD operations on playlists.
 * 
 * This service follows the Single Responsibility Principle by handling
 * only the core playlist lifecycle operations:
 * - Create, Read, Update, Delete operations
 * - User playlist queries
 * - Popular playlist queries
 * - Basic permission validation
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistCrudService {

    private final PlaylistRepository playlistRepository;
    private final HiveServiceClient hiveServiceClient;
    private final UserServiceClient userServiceClient;

    // Cache Keys
    private static final String CACHE_PLAYLISTS = "playlists";
    private static final String CACHE_USER_PLAYLISTS = "userPlaylists";
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
    @CacheEvict(value = CACHE_USER_PLAYLISTS, allEntries = true)
    public PlaylistDTO.Response createPlaylist(PlaylistDTO.CreateRequest createRequest, String userId) {
        log.info("Creating playlist '{}' for user {}", createRequest.getName(), userId);
        
        validateCreateRequest(createRequest, userId);
        
        Playlist playlist = buildPlaylistFromRequest(createRequest, userId);
        playlist = playlistRepository.save(playlist);
        
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
    @CacheEvict(value = CACHE_USER_PLAYLISTS, allEntries = true)
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
    @CacheEvict(value = {CACHE_PLAYLISTS, CACHE_USER_PLAYLISTS}, allEntries = true)
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
    // PUBLIC UTILITY METHODS
    // ===============================

    /**
     * Finds a playlist by ID with proper error handling.
     * 
     * @param playlistId The playlist ID
     * @return The playlist entity
     * @throws MusicServiceException.ResourceNotFoundException if not found
     */
    public Playlist findPlaylistById(Long playlistId) {
        return playlistRepository.findById(playlistId)
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Playlist not found with ID: " + playlistId));
    }

    /**
     * Validates if a user can view a playlist.
     * 
     * @param playlist The playlist
     * @param userId The user ID
     * @throws MusicServiceException.UnauthorizedOperationException if not authorized
     */
    public void validateUserCanViewPlaylist(Playlist playlist, String userId) {
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

    /**
     * Validates if a user can modify a playlist.
     * 
     * @param playlist The playlist
     * @param userId The user ID
     * @throws MusicServiceException.UnauthorizedOperationException if not authorized
     */
    public void validateUserCanModifyPlaylist(Playlist playlist, String userId) {
        if (!playlist.canUserModify(userId)) {
            throw new MusicServiceException.UnauthorizedOperationException(
                "User not authorized to modify this playlist");
        }
    }

    /**
     * Validates if a user is the owner of a playlist.
     * 
     * @param playlist The playlist
     * @param userId The user ID
     * @throws MusicServiceException.UnauthorizedOperationException if not owner
     */
    public void validateUserIsOwner(Playlist playlist, String userId) {
        if (!playlist.getCreatedBy().equals(userId)) {
            throw new MusicServiceException.UnauthorizedOperationException(
                "User is not the owner of this playlist");
        }
    }

    /**
     * Converts playlist entity to response DTO.
     * 
     * @param playlist The playlist entity
     * @param userId The requesting user ID
     * @return Playlist response DTO
     */
    public PlaylistDTO.Response convertToResponse(Playlist playlist, String userId) {
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

    private List<PlaylistDTO.TrackInfo> convertTracksToInfo(List<PlaylistTrack> tracks) {
        return tracks.stream()
            .map(this::convertTrackToInfo)
            .collect(Collectors.toList());
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

    private List<PlaylistDTO.CollaboratorInfo> convertCollaboratorsToInfo(List<PlaylistCollaborator> collaborators) {
        return collaborators.stream()
            .map(this::convertCollaboratorToInfo)
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
}