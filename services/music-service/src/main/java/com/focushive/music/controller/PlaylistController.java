package com.focushive.music.controller;

import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Operation(summary = "Get user playlists")
    @GetMapping
    public ResponseEntity<List<PlaylistDTO>> getUserPlaylists(@RequestParam String userId) {
        List<PlaylistDTO> playlists = playlistService.getUserPlaylists(userId);
        return ResponseEntity.ok(playlists);
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
    public ResponseEntity<PlaylistDTO> createPlaylist(
            @RequestParam String userId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam String focusMode,
            @RequestParam(defaultValue = "false") boolean isPublic) {
        
        PlaylistDTO playlist = playlistService.createPlaylist(userId, name, description, focusMode, isPublic);
        return ResponseEntity.status(HttpStatus.CREATED).body(playlist);
    }

    @Operation(summary = "Update playlist")
    @PutMapping("/{playlistId}")
    public ResponseEntity<PlaylistDTO> updatePlaylist(
            @PathVariable UUID playlistId,
            @RequestParam String userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isPublic) {
        
        Optional<PlaylistDTO> updated = playlistService.updatePlaylist(playlistId, userId, name, description, isPublic);
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
}