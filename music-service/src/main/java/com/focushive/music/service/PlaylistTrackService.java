package com.focushive.music.service;

import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.Playlist;
import com.focushive.music.model.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service responsible for managing tracks within playlists.
 * 
 * This service follows the Single Responsibility Principle by handling
 * only track-related operations within playlists:
 * - Adding tracks to playlists
 * - Removing tracks from playlists
 * - Reordering tracks within playlists
 * - Track metadata management
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistTrackService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistCrudService playlistCrudService;

    // Cache Keys
    private static final String CACHE_PLAYLISTS = "playlists";

    // ===============================
    // TRACK MANAGEMENT OPERATIONS
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
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        // Check for duplicate tracks
        if (playlist.containsTrack(addTrackRequest.getSpotifyTrackId())) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Track already exists in playlist");
        }
        
        PlaylistTrack playlistTrack = addTrackToPlaylistInternal(playlist, addTrackRequest, userId);
        playlistRepository.save(playlist);
        
        log.info("Added track {} to playlist {}", addTrackRequest.getSpotifyTrackId(), playlistId);
        return convertTrackToInfo(playlistTrack);
    }

    /**
     * Adds multiple tracks to a playlist in batch.
     * 
     * @param playlistId The playlist ID
     * @param addTracksRequest The tracks to add
     * @param userId The requesting user ID
     * @return List of added track information
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.BatchTrackResponse addTracksToPlaylist(Long playlistId, 
                                                            PlaylistDTO.BatchAddTracksRequest addTracksRequest, 
                                                            String userId) {
        log.info("Adding {} tracks to playlist {} by user {}", 
                addTracksRequest.getTracks().size(), playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        PlaylistDTO.BatchTrackResponse.Builder responseBuilder = PlaylistDTO.BatchTrackResponse.builder();
        int successCount = 0;
        int skipCount = 0;
        
        for (PlaylistDTO.AddTrackRequest trackRequest : addTracksRequest.getTracks()) {
            try {
                // Check for duplicates if requested
                if (Boolean.TRUE.equals(addTracksRequest.getSkipDuplicates()) && 
                    playlist.containsTrack(trackRequest.getSpotifyTrackId())) {
                    skipCount++;
                    continue;
                }
                
                PlaylistTrack playlistTrack = addTrackToPlaylistInternal(playlist, trackRequest, userId);
                responseBuilder.addedTrack(convertTrackToInfo(playlistTrack));
                successCount++;
                
            } catch (Exception e) {
                log.warn("Failed to add track {} to playlist {}: {}", 
                        trackRequest.getSpotifyTrackId(), playlistId, e.getMessage());
                responseBuilder.failedTrack(PlaylistDTO.FailedTrackInfo.builder()
                    .spotifyTrackId(trackRequest.getSpotifyTrackId())
                    .reason(e.getMessage())
                    .build());
            }
        }
        
        playlist = playlistRepository.save(playlist);
        
        log.info("Batch add completed for playlist {}: {} added, {} skipped, {} failed", 
                playlistId, successCount, skipCount, 
                addTracksRequest.getTracks().size() - successCount - skipCount);
        
        return responseBuilder
            .totalRequested(addTracksRequest.getTracks().size())
            .successCount(successCount)
            .skipCount(skipCount)
            .playlistId(playlistId)
            .build();
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
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        PlaylistTrack trackToRemove = playlist.getTracks().stream()
            .filter(track -> track.getId().equals(trackId))
            .findFirst()
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Track", trackId.toString()));
        
        playlist.removeTrack(trackToRemove);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Removed track {} from playlist {}", trackId, playlistId);
    }

    /**
     * Removes a track by Spotify ID from a playlist.
     * 
     * @param playlistId The playlist ID
     * @param spotifyTrackId The Spotify track ID to remove
     * @param userId The requesting user ID
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public void removeTrackBySpotifyId(Long playlistId, String spotifyTrackId, String userId) {
        log.info("Removing track with Spotify ID {} from playlist {} by user {}", 
                spotifyTrackId, playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        PlaylistTrack trackToRemove = playlist.getTracks().stream()
            .filter(track -> track.getSpotifyTrackId().equals(spotifyTrackId))
            .findFirst()
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Track with Spotify ID", spotifyTrackId));
        
        playlist.removeTrack(trackToRemove);
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Removed track with Spotify ID {} from playlist {}", spotifyTrackId, playlistId);
    }

    /**
     * Removes multiple tracks from a playlist.
     * 
     * @param playlistId The playlist ID
     * @param removeTracksRequest The tracks to remove
     * @param userId The requesting user ID
     * @return Batch removal response
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.BatchTrackResponse removeTracksFromPlaylist(Long playlistId, 
                                                                 PlaylistDTO.BatchRemoveTracksRequest removeTracksRequest, 
                                                                 String userId) {
        log.info("Removing {} tracks from playlist {} by user {}", 
                removeTracksRequest.getTrackIds().size(), playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        PlaylistDTO.BatchTrackResponse.Builder responseBuilder = PlaylistDTO.BatchTrackResponse.builder();
        int successCount = 0;
        
        for (Long trackId : removeTracksRequest.getTrackIds()) {
            try {
                PlaylistTrack trackToRemove = playlist.getTracks().stream()
                    .filter(track -> track.getId().equals(trackId))
                    .findFirst()
                    .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                        "Track", trackId.toString()));
                
                playlist.removeTrack(trackToRemove);
                responseBuilder.removedTrackId(trackId);
                successCount++;
                
            } catch (Exception e) {
                log.warn("Failed to remove track {} from playlist {}: {}", 
                        trackId, playlistId, e.getMessage());
                responseBuilder.failedTrack(PlaylistDTO.FailedTrackInfo.builder()
                    .trackId(trackId)
                    .reason(e.getMessage())
                    .build());
            }
        }
        
        playlist.setUpdatedAt(LocalDateTime.now());
        playlist = playlistRepository.save(playlist);
        
        log.info("Batch removal completed for playlist {}: {} removed, {} failed", 
                playlistId, successCount, removeTracksRequest.getTrackIds().size() - successCount);
        
        return responseBuilder
            .totalRequested(removeTracksRequest.getTrackIds().size())
            .successCount(successCount)
            .playlistId(playlistId)
            .build();
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
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        validateReorderRequest(playlist, reorderRequest);
        
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

    /**
     * Moves a track to a specific position in the playlist.
     * 
     * @param playlistId The playlist ID
     * @param trackId The track ID to move
     * @param newPosition The new position (0-based)
     * @param userId The requesting user ID
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public void moveTrackToPosition(Long playlistId, Long trackId, Integer newPosition, String userId) {
        log.info("Moving track {} to position {} in playlist {} by user {}", 
                trackId, newPosition, playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        PlaylistTrack trackToMove = playlist.getTracks().stream()
            .filter(track -> track.getId().equals(trackId))
            .findFirst()
            .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException(
                "Track", trackId.toString()));
        
        // Validate new position
        if (newPosition < 0 || newPosition >= playlist.getTracks().size()) {
            throw new IllegalArgumentException("Invalid position: " + newPosition);
        }
        
        // Move track to new position and adjust other tracks
        int oldPosition = trackToMove.getPositionInPlaylist();
        trackToMove.setPositionInPlaylist(newPosition);
        
        // Adjust positions of other tracks
        playlist.getTracks().stream()
            .filter(track -> !track.getId().equals(trackId))
            .forEach(track -> {
                int currentPosition = track.getPositionInPlaylist();
                if (oldPosition < newPosition) {
                    // Moving down: shift tracks up
                    if (currentPosition > oldPosition && currentPosition <= newPosition) {
                        track.setPositionInPlaylist(currentPosition - 1);
                    }
                } else {
                    // Moving up: shift tracks down
                    if (currentPosition >= newPosition && currentPosition < oldPosition) {
                        track.setPositionInPlaylist(currentPosition + 1);
                    }
                }
            });
        
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Moved track {} to position {} in playlist {}", trackId, newPosition, playlistId);
    }

    /**
     * Shuffles all tracks in a playlist.
     * 
     * @param playlistId The playlist ID
     * @param userId The requesting user ID
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public void shufflePlaylistTracks(Long playlistId, String userId) {
        log.info("Shuffling tracks in playlist {} by user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        playlist.shuffleTracks();
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);
        
        log.info("Shuffled tracks in playlist {}", playlistId);
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

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
        // For now, using placeholder data or data from request
        playlistTrack.setTrackName(addTrackRequest.getTrackName() != null ? 
                                  addTrackRequest.getTrackName() : "Track " + addTrackRequest.getSpotifyTrackId());
        playlistTrack.setArtistName(addTrackRequest.getArtistName() != null ? 
                                   addTrackRequest.getArtistName() : "Unknown Artist");
        playlistTrack.setAlbumName(addTrackRequest.getAlbumName());
        playlistTrack.setDurationMs(addTrackRequest.getDurationMs() != null ? 
                                   addTrackRequest.getDurationMs() : 180000); // 3 minutes default
        playlistTrack.setImageUrl(addTrackRequest.getImageUrl());
        playlistTrack.setPreviewUrl(addTrackRequest.getPreviewUrl());
        playlistTrack.setExternalUrl(addTrackRequest.getExternalUrl());
        
        playlist.addTrack(playlistTrack);
        return playlistTrack;
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

    private void validateReorderRequest(Playlist playlist, PlaylistDTO.TrackReorderRequest reorderRequest) {
        if (reorderRequest.getTrackOrders() == null || reorderRequest.getTrackOrders().isEmpty()) {
            throw new IllegalArgumentException("Track orders cannot be empty");
        }
        
        // Validate that all track IDs exist in the playlist
        for (Long trackId : reorderRequest.getTrackOrders().keySet()) {
            boolean trackExists = playlist.getTracks().stream()
                .anyMatch(track -> track.getId().equals(trackId));
            
            if (!trackExists) {
                throw new MusicServiceException.ResourceNotFoundException(
                    "Track", trackId.toString());
            }
        }
        
        // Validate position values
        int maxPosition = playlist.getTracks().size() - 1;
        for (Integer position : reorderRequest.getTrackOrders().values()) {
            if (position < 0 || position > maxPosition) {
                throw new IllegalArgumentException("Invalid position: " + position + 
                    ". Must be between 0 and " + maxPosition);
            }
        }
    }
}