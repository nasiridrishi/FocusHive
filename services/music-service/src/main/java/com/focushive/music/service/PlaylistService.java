package com.focushive.music.service;

import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.entity.Playlist;
import com.focushive.music.entity.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Playlist playlist = Playlist.builder()
            .name(name)
            .description(description)
            .userId(userId)
            .type(Playlist.PlaylistType.CUSTOM)
            .focusMode(Playlist.FocusMode.valueOf(focusMode.toUpperCase()))
            .isPublic(isPublic)
            .build();

        Playlist saved = playlistRepository.save(playlist);
        log.info("Created playlist: {} for user: {}", name, userId);
        return convertToDTO(saved);
    }

    /**
     * Update an existing playlist.
     */
    @Transactional
    public Optional<PlaylistDTO> updatePlaylist(UUID playlistId, String userId, String name, String description, Boolean isPublic) {
        Optional<Playlist> playlistOpt = playlistRepository.findById(playlistId);
        
        if (playlistOpt.isEmpty() || !playlistOpt.get().getUserId().equals(userId)) {
            return Optional.empty();
        }

        Playlist playlist = playlistOpt.get();
        if (name != null) playlist.setName(name);
        if (description != null) playlist.setDescription(description);
        if (isPublic != null) playlist.setIsPublic(isPublic);

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
}