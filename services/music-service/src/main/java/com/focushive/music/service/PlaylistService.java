package com.focushive.music.service;

import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.entity.Playlist;
import com.focushive.music.entity.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import com.focushive.music.repository.PlaylistTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for playlist management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final SpotifyService spotifyService;

    /**
     * Get all playlists for a user.
     */
    public List<PlaylistDTO> getUserPlaylists(String userId) {
        List<Playlist> playlists = playlistRepository.findByUserId(userId);
        return playlists.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get user playlists with pagination.
     */
    public Page<PlaylistDTO> getUserPlaylistsWithPagination(String userId, Pageable pageable) {
        // For now, we'll manually handle pagination since Spring Data pagination might not be set up
        List<Playlist> allPlaylists = playlistRepository.findByUserId(userId);
        List<PlaylistDTO> allPlaylistDTOs = allPlaylists.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allPlaylistDTOs.size());

        List<PlaylistDTO> pageContent = start < allPlaylistDTOs.size()
            ? allPlaylistDTOs.subList(start, end)
            : List.of();

        return new PageImpl<>(pageContent, pageable, allPlaylistDTOs.size());
    }

    /**
     * Result wrapper for playlist access operations.
     */
    public static class PlaylistAccessResult {
        private final PlaylistDTO playlist;
        private final boolean found;
        private final boolean accessDenied;

        private PlaylistAccessResult(PlaylistDTO playlist, boolean found, boolean accessDenied) {
            this.playlist = playlist;
            this.found = found;
            this.accessDenied = accessDenied;
        }

        public static PlaylistAccessResult success(PlaylistDTO playlist) {
            return new PlaylistAccessResult(playlist, true, false);
        }

        public static PlaylistAccessResult notFound() {
            return new PlaylistAccessResult(null, false, false);
        }

        public static PlaylistAccessResult accessDenied() {
            return new PlaylistAccessResult(null, true, true);
        }

        public Optional<PlaylistDTO> getPlaylist() { return Optional.ofNullable(playlist); }
        public boolean isFound() { return found; }
        public boolean isAccessDenied() { return accessDenied; }
    }

    /**
     * Get playlist by ID with user access check.
     */
    public PlaylistAccessResult getPlaylistById(UUID playlistId, String userId) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);

        if (playlistOpt.isEmpty()) {
            return PlaylistAccessResult.notFound();
        }

        Playlist playlist = playlistOpt.get();

        // Check access permissions
        if (!hasAccessToPlaylist(playlist, userId)) {
            return PlaylistAccessResult.accessDenied();
        }

        return PlaylistAccessResult.success(convertToDTO(playlist));
    }

    /**
     * Get playlists by focus mode.
     */
    public List<PlaylistDTO> getPlaylistsByFocusMode(String userId, String focusMode) {
        Playlist.FocusMode mode = Playlist.FocusMode.valueOf(focusMode.toUpperCase());
        List<Playlist> playlists = playlistRepository.findByUserIdAndFocusMode(userId, mode);
        return playlists.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Create a new playlist.
     */
    @Transactional
    public PlaylistDTO createPlaylist(String userId, String name, String description, String focusMode, boolean isPublic) {
        // Try to create playlist on Spotify if user has credentials
        String spotifyPlaylistId = null;
        if (spotifyService.hasValidCredentials(userId)) {
            spotifyPlaylistId = spotifyService.createSpotifyPlaylist(userId, name, description, isPublic);
        }

        Playlist playlist = Playlist.builder()
            .name(name)
            .description(description)
            .userId(userId)
            .type(Playlist.PlaylistType.CUSTOM)
            .focusMode(Playlist.FocusMode.valueOf(focusMode.toUpperCase()))
            .isPublic(isPublic)
            .spotifyPlaylistId(spotifyPlaylistId)
            .build();

        Playlist saved = playlistRepository.save(playlist);
        log.info("Created playlist: {} for user: {}", name, userId);
        return convertToDTO(saved);
    }

    /**
     * Update an existing playlist.
     */
    @Transactional
    public Optional<PlaylistDTO> updatePlaylist(UUID playlistId, String userId, String name, String description, Playlist.FocusMode focusMode, Boolean isPublic) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);

        if (playlistOpt.isEmpty() || !playlistOpt.get().getUserId().equals(userId)) {
            return Optional.empty();
        }

        Playlist playlist = playlistOpt.get();
        if (name != null) playlist.setName(name);
        if (description != null) playlist.setDescription(description);
        if (focusMode != null) playlist.setFocusMode(focusMode);
        if (isPublic != null) playlist.setIsPublic(isPublic);
        playlist.setUpdatedAt(LocalDateTime.now());

        Playlist updated = playlistRepository.save(playlist);
        log.info("Updated playlist: {} for user: {}", playlistId, userId);
        return Optional.of(convertToDTO(updated));
    }

    /**
     * Delete a playlist.
     */
    @Transactional
    public boolean deletePlaylist(UUID playlistId, String userId) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
        
        if (playlistOpt.isEmpty() || !playlistOpt.get().getUserId().equals(userId)) {
            return false;
        }

        playlistRepository.deleteById(playlistId);
        log.info("Deleted playlist: {} for user: {}", playlistId, userId);
        return true;
    }

    /**
     * Get public playlists.
     */
    public List<PlaylistDTO> getPublicPlaylists() {
        List<Playlist> playlists = playlistRepository.findByIsPublicTrue();
        return playlists.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Add track to playlist.
     */
    @Transactional
    public Optional<PlaylistDTO> addTrackToPlaylist(UUID playlistId, String userId, String spotifyTrackId) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);

        if (playlistOpt.isEmpty()) {
            return Optional.empty();
        }

        Playlist playlist = playlistOpt.get();

        // Check if user can add tracks (owner or collaborative playlist)
        if (!canModifyPlaylist(playlist, userId)) {
            return Optional.empty();
        }

        // Get next order number
        int nextOrder = playlist.getTracks().size() + 1;

        PlaylistTrack track = PlaylistTrack.builder()
            .playlist(playlist)
            .spotifyTrackId(spotifyTrackId)
            .title("Track Title") // This would be fetched from Spotify API in real implementation
            .artist("Artist Name")
            .album("Album Name")
            .durationMs(210000) // Default duration
            .order(nextOrder)
            .addedBy(userId)
            .createdAt(LocalDateTime.now())
            .build();

        playlistTrackRepository.save(track);

        // Update playlist timestamp
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);

        log.info("Added track {} to playlist {} by user {}", spotifyTrackId, playlistId, userId);
        return Optional.of(convertToDTO(playlist));
    }

    /**
     * Remove track from playlist.
     */
    @Transactional
    public boolean removeTrackFromPlaylist(UUID playlistId, String userId, String trackId) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);

        if (playlistOpt.isEmpty()) {
            return false;
        }

        Playlist playlist = playlistOpt.get();

        // Check if user can modify tracks
        if (!canModifyPlaylist(playlist, userId)) {
            return false;
        }

        // Find and remove track
        Optional<PlaylistTrack> trackOpt = playlist.getTracks().stream()
            .filter(t -> t.getSpotifyTrackId().equals(trackId))
            .findFirst();

        if (trackOpt.isEmpty()) {
            return false;
        }

        playlistTrackRepository.delete(trackOpt.get());

        // Update playlist timestamp
        playlist.setUpdatedAt(LocalDateTime.now());
        playlistRepository.save(playlist);

        log.info("Removed track {} from playlist {} by user {}", trackId, playlistId, userId);
        return true;
    }

    /**
     * Check if user has read access to playlist.
     */
    private boolean hasAccessToPlaylist(Playlist playlist, String userId) {
        // Owner has access
        if (playlist.getUserId().equals(userId)) {
            return true;
        }

        // Public playlists are accessible by everyone
        if (Boolean.TRUE.equals(playlist.getIsPublic())) {
            return true;
        }

        // Collaborative playlists are accessible by everyone (for now)
        if (playlist.getType() == Playlist.PlaylistType.COLLABORATIVE) {
            return true;
        }

        return false;
    }

    /**
     * Check if user can modify playlist (add/remove tracks).
     */
    private boolean canModifyPlaylist(Playlist playlist, String userId) {
        // Owner can always modify
        if (playlist.getUserId().equals(userId)) {
            return true;
        }

        // Collaborative playlists can be modified by anyone
        if (playlist.getType() == Playlist.PlaylistType.COLLABORATIVE) {
            return true;
        }

        return false;
    }

    private PlaylistDTO convertToDTO(Playlist playlist) {
        List<PlaylistDTO.TrackDTO> tracks = playlist.getTracks().stream()
            .map(this::convertTrackToDTO)
            .collect(Collectors.toList());

        return PlaylistDTO.builder()
            .id(playlist.getId())
            .name(playlist.getName())
            .description(playlist.getDescription())
            .userId(playlist.getUserId())
            .type(playlist.getType().name())
            .focusMode(playlist.getFocusMode().name())
            .isPublic(playlist.getIsPublic())
            .spotifyPlaylistId(playlist.getSpotifyPlaylistId())
            .tracks(tracks)
            .createdAt(playlist.getCreatedAt())
            .updatedAt(playlist.getUpdatedAt())
            .build();
    }

    private PlaylistDTO.TrackDTO convertTrackToDTO(PlaylistTrack track) {
        return PlaylistDTO.TrackDTO.builder()
            .id(track.getId())
            .spotifyTrackId(track.getSpotifyTrackId())
            .title(track.getTitle())
            .artist(track.getArtist())
            .album(track.getAlbum())
            .durationMs(track.getDurationMs())
            .order(track.getOrder())
            .addedBy(track.getAddedBy())
            .createdAt(track.getCreatedAt())
            .build();
    }

    /**
     * Get playlist by ID (without user access check for internal use)
     */
    public Playlist getPlaylist(UUID playlistId) {
        return playlistRepository.findById(playlistId).orElse(null);
    }

    /**
     * Add track to playlist (for WebSocket operations)
     */
    @Transactional
    public PlaylistTrack addTrack(UUID playlistId, String spotifyTrackId) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
        if (playlistOpt.isEmpty()) {
            throw new RuntimeException("Playlist not found");
        }

        Playlist playlist = playlistOpt.get();

        // Get next order number
        int nextOrder = playlist.getTracks().size() + 1;

        PlaylistTrack track = PlaylistTrack.builder()
            .playlist(playlist)
            .spotifyTrackId(spotifyTrackId)
            .title("Track Title") // Would be fetched from Spotify API in real implementation
            .artist("Artist Name")
            .album("Album Name")
            .durationMs(210000) // Default duration
            .order(nextOrder)
            .addedBy("system") // Default for WebSocket operations
            .createdAt(LocalDateTime.now())
            .build();

        return playlistTrackRepository.save(track);
    }

    /**
     * Remove track by ID (for WebSocket operations)
     */
    @Transactional
    public void removeTrack(UUID trackId) {
        playlistTrackRepository.deleteById(trackId);
    }

    /**
     * Reorder track to new position
     */
    @Transactional
    public void reorderTrack(UUID trackId, Integer newPosition) {
        Optional<PlaylistTrack> trackOpt = playlistTrackRepository.findById(trackId);
        if (trackOpt.isEmpty()) {
            throw new RuntimeException("Track not found");
        }

        PlaylistTrack track = trackOpt.get();
        Playlist playlist = track.getPlaylist();
        List<PlaylistTrack> allTracks = playlistTrackRepository.findByPlaylistIdOrderByOrder(playlist.getId());

        // Remove track from current position
        allTracks.remove(track);

        // Insert at new position (1-based)
        int newIndex = Math.max(0, Math.min(newPosition - 1, allTracks.size()));
        allTracks.add(newIndex, track);

        // Update order for all tracks
        for (int i = 0; i < allTracks.size(); i++) {
            allTracks.get(i).setOrder(i + 1);
            playlistTrackRepository.save(allTracks.get(i));
        }
    }
}