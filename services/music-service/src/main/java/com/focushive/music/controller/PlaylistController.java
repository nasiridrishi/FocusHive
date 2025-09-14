package com.focushive.music.controller;

import com.focushive.music.dto.AddTrackRequest;
import com.focushive.music.dto.CreatePlaylistRequest;
import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.dto.UpdatePlaylistRequest;
import com.focushive.music.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for playlist management.
 */
@RestController
@RequestMapping("/playlists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Playlist Management", description = "Playlist CRUD operations")
public class PlaylistController {

    private final PlaylistService playlistService;

    @Operation(summary = "Get user playlists with pagination")
    @GetMapping
    public ResponseEntity<Page<PlaylistDTO>> getUserPlaylists(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PlaylistDTO> playlists = playlistService.getUserPlaylistsWithPagination(userId, pageable);
        return ResponseEntity.ok(playlists);
    }

    @Operation(summary = "Get playlist by ID")
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDTO> getPlaylistById(
            @PathVariable UUID playlistId,
            @RequestParam String userId) {
        PlaylistService.PlaylistAccessResult result = playlistService.getPlaylistById(playlistId, userId);

        if (result.isAccessDenied()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return result.getPlaylist().map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get playlists by focus mode")
    @GetMapping("/focus-mode/{focusMode}")
    public ResponseEntity<List<PlaylistDTO>> getPlaylistsByFocusMode(
            @PathVariable String focusMode,
            @RequestParam String userId) {
        List<PlaylistDTO> playlists = playlistService.getPlaylistsByFocusMode(userId, focusMode);
        return ResponseEntity.ok(playlists);
    }

    @Operation(summary = "Create new playlist")
    @PostMapping
    public ResponseEntity<PlaylistDTO> createPlaylist(@RequestBody CreatePlaylistRequest request) {
        PlaylistDTO playlist = playlistService.createPlaylist(
            request.getUserId(),
            request.getName(),
            request.getDescription(),
            request.getFocusMode(),
            request.getIsPublic() != null ? request.getIsPublic() : false
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    @Operation(summary = "Update playlist")
    @PutMapping("/{playlistId}")
    public ResponseEntity<PlaylistDTO> updatePlaylist(
            @PathVariable UUID playlistId,
            @RequestBody UpdatePlaylistRequest request) {

        Optional<PlaylistDTO> updated = playlistService.updatePlaylist(
            playlistId,
            request.getUserId(),
            request.getName(),
            request.getDescription(),
            request.getFocusMode(),
            request.getIsPublic());
        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete playlist")
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable UUID playlistId,
            @RequestParam String userId) {
        
        boolean deleted = playlistService.deletePlaylist(playlistId, userId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get public playlists")
    @GetMapping("/public")
    public ResponseEntity<List<PlaylistDTO>> getPublicPlaylists() {
        List<PlaylistDTO> playlists = playlistService.getPublicPlaylists();
        return ResponseEntity.ok(playlists);
    }

    @Operation(summary = "Add track to playlist")
    @PostMapping("/{playlistId}/tracks")
    public ResponseEntity<PlaylistDTO> addTrackToPlaylist(
            @PathVariable UUID playlistId,
            @RequestBody AddTrackRequest request) {
        Optional<PlaylistDTO> updated = playlistService.addTrackToPlaylist(
            playlistId,
            request.getUserId(),
            request.getSpotifyTrackId());
        return updated.map(p -> ResponseEntity.status(HttpStatus.CREATED).body(p))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Remove track from playlist")
    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<Void> removeTrackFromPlaylist(
            @PathVariable UUID playlistId,
            @PathVariable String trackId,
            @RequestParam String userId) {
        boolean removed = playlistService.removeTrackFromPlaylist(playlistId, userId, trackId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}