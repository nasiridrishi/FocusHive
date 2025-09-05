package com.focushive.music.repository;

import com.focushive.music.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Playlist entity.
 */
@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    List<Playlist> findByUserId(String userId);

    List<Playlist> findByUserIdAndType(String userId, Playlist.PlaylistType type);

    List<Playlist> findByUserIdAndFocusMode(String userId, Playlist.FocusMode focusMode);

    List<Playlist> findByIsPublicTrue();

    Optional<Playlist> findBySpotifyPlaylistId(String spotifyPlaylistId);

    @Query("SELECT p FROM Playlist p WHERE p.userId = :userId AND p.type = :type AND p.focusMode = :focusMode")
    List<Playlist> findByUserIdAndTypeAndFocusMode(
        @Param("userId") String userId,
        @Param("type") Playlist.PlaylistType type,
        @Param("focusMode") Playlist.FocusMode focusMode
    );

    long countByUserId(String userId);
}