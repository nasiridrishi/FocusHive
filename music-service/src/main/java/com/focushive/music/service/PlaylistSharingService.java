package com.focushive.music.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for playlist sharing and collaboration features.
 * 
 * This service follows the Single Responsibility Principle by handling
 * only sharing and collaboration-related operations:
 * - Sharing playlists with hive members
 * - Managing playlist collaborators
 * - Import/export functionality
 * - Playlist duplication
 * - Hive playlist management
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistSharingService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistCrudService playlistCrudService;
    private final HiveServiceClient hiveServiceClient;
    private final UserServiceClient userServiceClient;
    private final SpotifyIntegrationService spotifyIntegrationService;
    private final ObjectMapper objectMapper;

    // Cache Keys
    private static final String CACHE_PLAYLISTS = "playlists";
    private static final String CACHE_HIVE_PLAYLISTS = "hivePlaylists";

    // ===============================
    // SHARING OPERATIONS
    // ===============================

    /**
     * Shares a playlist with hive members.
     * 
     * @param playlistId The playlist ID
     * @param shareRequest The share request
     * @param userId The requesting user ID
     * @return Sharing response with details
     */
    @CacheEvict(value = {CACHE_PLAYLISTS, CACHE_HIVE_PLAYLISTS}, allEntries = true)
    public PlaylistDTO.SharingResponse sharePlaylistWithHive(Long playlistId, 
                                                            PlaylistDTO.SharePlaylistRequest shareRequest, 
                                                            String userId) {
        log.info("Sharing playlist {} with hive {} by user {}", 
                playlistId, shareRequest.getHiveId(), userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserIsOwner(playlist, userId);
        
        // Get hive members
        List<String> hiveMembers = hiveServiceClient.getHiveMembers(shareRequest.getHiveId());
        
        List<String> addedCollaborators = new ArrayList<>();
        List<String> skippedUsers = new ArrayList<>();
        
        // Add hive members as collaborators
        for (String memberId : hiveMembers) {
            if (!memberId.equals(userId)) { // Don't add owner as collaborator
                try {
                    // Check if already a collaborator
                    boolean isAlreadyCollaborator = playlist.getCollaborators().stream()
                        .anyMatch(c -> c.getUserId().equals(memberId));
                    
                    if (!isAlreadyCollaborator) {
                        addCollaboratorToPlaylist(playlist, memberId, shareRequest.getPermissionLevel());
                        addedCollaborators.add(memberId);
                    } else {
                        skippedUsers.add(memberId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to add collaborator {} to playlist {}: {}", 
                            memberId, playlistId, e.getMessage());
                    skippedUsers.add(memberId);
                }
            }
        }
        
        // Update playlist hive association if not already set
        if (!shareRequest.getHiveId().equals(playlist.getHiveId())) {
            playlist.setHiveId(shareRequest.getHiveId());
        }
        
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Shared playlist {} with hive {}: {} added, {} skipped", 
                playlistId, shareRequest.getHiveId(), addedCollaborators.size(), skippedUsers.size());
        
        return PlaylistDTO.SharingResponse.builder()
            .playlistId(playlistId)
            .hiveId(shareRequest.getHiveId())
            .totalHiveMembers(hiveMembers.size())
            .collaboratorsAdded(addedCollaborators.size())
            .usersSkipped(skippedUsers.size())
            .addedCollaboratorIds(addedCollaborators)
            .skippedUserIds(skippedUsers)
            .permissionLevel(shareRequest.getPermissionLevel())
            .sharedAt(LocalDateTime.now())
            .build();
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
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserIsOwner(playlist, userId);
        
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
     * Removes a collaborator from a playlist.
     * 
     * @param playlistId The playlist ID
     * @param collaboratorUserId The collaborator user ID to remove
     * @param userId The requesting user ID
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public void removeCollaborator(Long playlistId, String collaboratorUserId, String userId) {
        log.info("Removing collaborator {} from playlist {} by user {}", 
                collaboratorUserId, playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserIsOwner(playlist, userId);
        
        PlaylistCollaborator collaboratorToRemove = playlist.getCollaborators().stream()
            .filter(c -> c.getUserId().equals(collaboratorUserId))
            .findFirst()
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Collaborator", collaboratorUserId));
        
        playlist.removeCollaborator(collaboratorToRemove);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Removed collaborator {} from playlist {}", collaboratorUserId, playlistId);
    }

    /**
     * Updates collaborator permissions.
     * 
     * @param playlistId The playlist ID
     * @param collaboratorUserId The collaborator user ID
     * @param updateRequest The permission update request
     * @param userId The requesting user ID
     * @return Updated collaborator information
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.CollaboratorInfo updateCollaboratorPermissions(Long playlistId, 
                                                                     String collaboratorUserId,
                                                                     PlaylistDTO.UpdateCollaboratorRequest updateRequest, 
                                                                     String userId) {
        log.info("Updating permissions for collaborator {} in playlist {} by user {}", 
                collaboratorUserId, playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserIsOwner(playlist, userId);
        
        PlaylistCollaborator collaborator = playlist.getCollaborators().stream()
            .filter(c -> c.getUserId().equals(collaboratorUserId))
            .findFirst()
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Collaborator", collaboratorUserId));
        
        updateCollaboratorFromRequest(collaborator, updateRequest);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Updated permissions for collaborator {} in playlist {}", collaboratorUserId, playlistId);
        return convertCollaboratorToInfo(collaborator);
    }

    // ===============================
    // HIVE PLAYLIST OPERATIONS
    // ===============================

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

    /**
     * Gets shared playlists where user is a collaborator.
     * 
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Paginated shared playlist results
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SearchResult getSharedPlaylists(String userId, Pageable pageable) {
        log.debug("Getting shared playlists for user {}", userId);
        
        Page<Playlist> playlistPage = playlistRepository.findPlaylistsWhereUserIsCollaborator(userId, pageable);
        
        return convertToSearchResult(playlistPage, userId);
    }

    // ===============================
    // IMPORT/EXPORT OPERATIONS
    // ===============================

    /**
     * Exports a playlist.
     * 
     * @param playlistId The playlist ID
     * @param exportRequest The export request
     * @param userId The requesting user ID
     * @return Export response
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.ExportResponse exportPlaylist(Long playlistId, 
                                                    PlaylistDTO.ExportRequest exportRequest, 
                                                    String userId) {
        log.info("Exporting playlist {} for user {} in format {}", 
                playlistId, userId, exportRequest.getFormat());
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanViewPlaylist(playlist, userId);
        
        String content = generateExportContent(playlist, exportRequest);
        
        return PlaylistDTO.ExportResponse.builder()
            .playlistId(playlistId)
            .playlistName(playlist.getName())
            .format(exportRequest.getFormat())
            .content(content)
            .exportedAt(LocalDateTime.now())
            .totalTracks(playlist.getTotalTracks())
            .fileSizeBytes((long) content.length())
            .includeMetadata(Boolean.TRUE.equals(exportRequest.getIncludeMetadata()))
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
        
        log.info("Imported playlist with ID: {} containing {} tracks", 
                playlist.getId(), playlist.getTotalTracks());
        return playlistCrudService.convertToResponse(playlist, userId);
    }

    /**
     * Duplicates a playlist.
     * 
     * @param playlistId The playlist ID to duplicate
     * @param duplicateRequest The duplicate request
     * @param userId The requesting user ID
     * @return Duplicated playlist response
     */
    @CacheEvict(value = {CACHE_HIVE_PLAYLISTS}, allEntries = true)
    public PlaylistDTO.Response duplicatePlaylist(Long playlistId, 
                                                 PlaylistDTO.DuplicateRequest duplicateRequest, 
                                                 String userId) {
        log.info("Duplicating playlist {} for user {}", playlistId, userId);
        
        Playlist originalPlaylist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanViewPlaylist(originalPlaylist, userId);
        
        Playlist duplicatedPlaylist = buildDuplicatePlaylist(originalPlaylist, duplicateRequest, userId);
        duplicatedPlaylist = playlistRepository.save(duplicatedPlaylist);
        
        log.info("Duplicated playlist with new ID: {} containing {} tracks", 
                duplicatedPlaylist.getId(), duplicatedPlaylist.getTotalTracks());
        return playlistCrudService.convertToResponse(duplicatedPlaylist, userId);
    }

    // ===============================
    // COLLABORATION ANALYTICS
    // ===============================

    /**
     * Gets collaboration statistics for a playlist.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     * @return Collaboration statistics
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.CollaborationStats getCollaborationStats(Long playlistId, String userId) {
        log.debug("Getting collaboration stats for playlist {} by user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanViewPlaylist(playlist, userId);
        
        Map<String, Long> contributionsByUser = playlist.getTracks().stream()
            .collect(Collectors.groupingBy(
                PlaylistTrack::getAddedBy,
                Collectors.counting()
            ));
        
        return PlaylistDTO.CollaborationStats.builder()
            .playlistId(playlistId)
            .totalCollaborators(playlist.getCollaborators().size())
            .totalContributions(playlist.getTotalTracks())
            .contributionsByUser(contributionsByUser)
            .mostActiveContributor(contributionsByUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(playlist.getCreatedBy()))
            .lastActivity(playlist.getUpdatedAt())
            .isCollaborative(Boolean.TRUE.equals(playlist.getIsCollaborative()))
            .build();
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    private void addCollaboratorToPlaylist(Playlist playlist, String userId, String permissionLevel) {
        PlaylistCollaborator collaborator = new PlaylistCollaborator();
        collaborator.setPlaylist(playlist);
        collaborator.setUserId(userId);
        collaborator.setPermissionLevel(permissionLevel);
        collaborator.setAddedAt(LocalDateTime.now());
        collaborator.setAddedBy(playlist.getCreatedBy());
        
        // Set permissions based on level
        switch (permissionLevel.toUpperCase()) {
            case "EDITOR":
                collaborator.setCanAddTracks(true);
                collaborator.setCanRemoveTracks(true);
                collaborator.setCanReorderTracks(true);
                collaborator.setCanEditPlaylist(false);
                collaborator.setCanInviteOthers(false);
                break;
            case "CONTRIBUTOR":
                collaborator.setCanAddTracks(true);
                collaborator.setCanRemoveTracks(false);
                collaborator.setCanReorderTracks(false);
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

    private void updateCollaboratorFromRequest(PlaylistCollaborator collaborator, 
                                             PlaylistDTO.UpdateCollaboratorRequest request) {
        if (request.getPermissionLevel() != null) {
            collaborator.setPermissionLevel(request.getPermissionLevel());
        }
        if (request.getCanAddTracks() != null) {
            collaborator.setCanAddTracks(request.getCanAddTracks());
        }
        if (request.getCanRemoveTracks() != null) {
            collaborator.setCanRemoveTracks(request.getCanRemoveTracks());
        }
        if (request.getCanReorderTracks() != null) {
            collaborator.setCanReorderTracks(request.getCanReorderTracks());
        }
        if (request.getCanEditPlaylist() != null) {
            collaborator.setCanEditPlaylist(request.getCanEditPlaylist());
        }
        if (request.getCanInviteOthers() != null) {
            collaborator.setCanInviteOthers(request.getCanInviteOthers());
        }
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

    private PlaylistDTO.SearchResult convertToSearchResult(Page<Playlist> playlistPage, String userId) {
        List<PlaylistDTO.Response> responses = playlistPage.getContent().stream()
            .map(playlist -> playlistCrudService.convertToResponse(playlist, userId))
            .collect(Collectors.toList());
        
        return PlaylistDTO.SearchResult.builder()
            .playlists(responses)
            .totalResults(playlistPage.getTotalElements())
            .currentPage(playlistPage.getNumber())
            .totalPages(playlistPage.getTotalPages())
            .build();
    }

    private String generateExportContent(Playlist playlist, PlaylistDTO.ExportRequest request) {
        Map<String, Object> exportData = new HashMap<>();
        
        // Basic playlist info
        exportData.put("name", playlist.getName());
        exportData.put("description", playlist.getDescription());
        exportData.put("totalTracks", playlist.getTotalTracks());
        exportData.put("exportedAt", LocalDateTime.now());
        
        // Include metadata if requested
        if (Boolean.TRUE.equals(request.getIncludeMetadata())) {
            exportData.put("createdBy", playlist.getCreatedBy());
            exportData.put("createdAt", playlist.getCreatedAt());
            exportData.put("isPublic", playlist.getIsPublic());
            exportData.put("isCollaborative", playlist.getIsCollaborative());
            exportData.put("totalDurationMs", playlist.getTotalDurationMs());
        }
        
        // Track list
        List<Map<String, Object>> tracks = playlist.getTracks().stream()
            .map(track -> {
                Map<String, Object> trackData = new HashMap<>();
                trackData.put("spotifyTrackId", track.getSpotifyTrackId());
                trackData.put("trackName", track.getTrackName());
                trackData.put("artistName", track.getArtistName());
                trackData.put("position", track.getPositionInPlaylist());
                
                if (Boolean.TRUE.equals(request.getIncludeMetadata())) {
                    trackData.put("albumName", track.getAlbumName());
                    trackData.put("durationMs", track.getDurationMs());
                    trackData.put("addedBy", track.getAddedBy());
                    trackData.put("addedAt", track.getAddedAt());
                }
                
                return trackData;
            }).collect(Collectors.toList());
        
        exportData.put("tracks", tracks);
        
        try {
            return switch (request.getFormat().toUpperCase()) {
                case "JSON" -> objectMapper.writeValueAsString(exportData);
                case "CSV" -> generateCSVContent(playlist, request);
                default -> objectMapper.writeValueAsString(exportData);
            };
        } catch (JsonProcessingException e) {
            log.error("Error generating export content", e);
            return "{}";
        }
    }

    private String generateCSVContent(Playlist playlist, PlaylistDTO.ExportRequest request) {
        StringBuilder csv = new StringBuilder();
        
        // Header
        if (Boolean.TRUE.equals(request.getIncludeMetadata())) {
            csv.append("Position,Track Name,Artist,Album,Duration (ms),Added By,Added At,Spotify ID\n");
        } else {
            csv.append("Position,Track Name,Artist,Spotify ID\n");
        }
        
        // Tracks
        for (PlaylistTrack track : playlist.getTracks()) {
            csv.append(track.getPositionInPlaylist()).append(",");
            csv.append(escapeCSV(track.getTrackName())).append(",");
            csv.append(escapeCSV(track.getArtistName())).append(",");
            
            if (Boolean.TRUE.equals(request.getIncludeMetadata())) {
                csv.append(escapeCSV(track.getAlbumName())).append(",");
                csv.append(track.getDurationMs()).append(",");
                csv.append(escapeCSV(track.getAddedBy())).append(",");
                csv.append(track.getAddedAt()).append(",");
            }
            
            csv.append(track.getSpotifyTrackId()).append("\n");
        }
        
        return csv.toString();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void validateImportRequest(PlaylistDTO.ImportRequest importRequest) {
        if (!StringUtils.hasText(importRequest.getSource()) || 
            !StringUtils.hasText(importRequest.getExternalPlaylistId())) {
            throw new IllegalArgumentException("Source and external playlist ID are required");
        }
        
        if (!Arrays.asList("SPOTIFY", "APPLE_MUSIC", "YOUTUBE_MUSIC").contains(importRequest.getSource().toUpperCase())) {
            throw new IllegalArgumentException("Unsupported import source: " + importRequest.getSource());
        }
    }

    private Object getExternalPlaylistInfo(PlaylistDTO.ImportRequest importRequest) {
        return switch (importRequest.getSource().toUpperCase()) {
            case "SPOTIFY" -> spotifyIntegrationService.getPlaylistInfo(importRequest.getExternalPlaylistId());
            case "APPLE_MUSIC", "YOUTUBE_MUSIC" -> {
                // Placeholder for future implementation
                Map<String, Object> placeholderInfo = new HashMap<>();
                placeholderInfo.put("name", "Imported Playlist");
                placeholderInfo.put("tracks", new ArrayList<>());
                yield placeholderInfo;
            }
            default -> throw new MusicServiceException.UnsupportedOperationException(
                "Import source not supported: " + importRequest.getSource());
        };
    }

    private Playlist buildPlaylistFromImport(PlaylistDTO.ImportRequest importRequest, 
                                           Object externalPlaylistInfo, String userId) {
        String playlistName = StringUtils.hasText(importRequest.getNewPlaylistName()) ? 
                             importRequest.getNewPlaylistName() : "Imported Playlist";
        
        return Playlist.builder()
            .name(playlistName)
            .description("Imported from " + importRequest.getSource())
            .createdBy(userId)
            .hiveId(importRequest.getTargetHiveId())
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
            duplicateTrack.setImageUrl(originalTrack.getImageUrl());
            duplicateTrack.setPreviewUrl(originalTrack.getPreviewUrl());
            duplicateTrack.setExternalUrl(originalTrack.getExternalUrl());
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