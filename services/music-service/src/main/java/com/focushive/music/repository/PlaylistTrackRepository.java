package com.focushive.music.repository;

import com.focushive.music.entity.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing playlist tracks.
 */
@Repository
public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, UUID> {
    
    /**
     * Find all tracks for a specific playlist.
     */
    List<PlaylistTrack> findByPlaylistIdOrderByOrderAsc(UUID playlistId);
    
    /**
     * Find all tracks for a specific playlist (unordered).
     */
    List<PlaylistTrack> findByPlaylistId(UUID playlistId);
    
    /**
     * Delete all tracks for a specific playlist.
     */
    void deleteByPlaylistId(UUID playlistId);
    
    /**
     * Count tracks in a playlist.
     */
    long countByPlaylistId(UUID playlistId);

    /**
     * Find all tracks for a specific playlist ordered by their order field.
     */
    List<PlaylistTrack> findByPlaylistIdOrderByOrder(UUID playlistId);
}