package com.focushive.music.repository;

import com.focushive.music.model.Playlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing playlists.
 * 
 * Provides data access methods for playlist operations including
 * personal playlists, hive playlists, collaborative features,
 * and public playlist discovery.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    /**
     * Finds all playlists created by a specific user.
     * 
     * @param userId The creator's user ID
     * @param pageable Pagination information
     * @return Page of playlists created by the user
     */
    Page<Playlist> findByCreatedByOrderByUpdatedAtDesc(String userId, Pageable pageable);

    /**
     * Finds all playlists for a specific hive.
     * 
     * @param hiveId The hive ID
     * @param pageable Pagination information
     * @return Page of playlists for the hive
     */
    Page<Playlist> findByHiveIdOrderByUpdatedAtDesc(String hiveId, Pageable pageable);

    /**
     * Finds public playlists for discovery.
     * 
     * @param pageable Pagination information
     * @return Page of public playlists
     */
    Page<Playlist> findByIsPublicTrueOrderByUpdatedAtDesc(Pageable pageable);

    /**
     * Finds collaborative playlists.
     * 
     * @param pageable Pagination information
     * @return Page of collaborative playlists
     */
    Page<Playlist> findByIsCollaborativeTrueOrderByUpdatedAtDesc(Pageable pageable);

    /**
     * Finds playlists that a user can access (created by them or they're collaborators).
     * 
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of accessible playlists
     */
    @Query("SELECT DISTINCT p FROM Playlist p LEFT JOIN p.collaborators c " +
           "WHERE p.createdBy = :userId OR c.userId = :userId " +
           "ORDER BY p.updatedAt DESC")
    Page<Playlist> findAccessiblePlaylists(@Param("userId") String userId, Pageable pageable);

    /**
     * Finds playlists by name search (case-insensitive).
     * 
     * @param searchTerm The search term
     * @param pageable Pagination information
     * @return Page of matching playlists
     */
    @Query("SELECT p FROM Playlist p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY p.updatedAt DESC")
    Page<Playlist> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm, 
                                                  Pageable pageable);

    /**
     * Finds playlists with Spotify integration.
     * 
     * @return List of playlists synced with Spotify
     */
    @Query("SELECT p FROM Playlist p WHERE p.spotifyPlaylistId IS NOT NULL")
    List<Playlist> findAllWithSpotifyIntegration();

    /**
     * Finds the most popular public playlists by track count.
     * 
     * @param pageable Pagination information
     * @return Page of popular playlists
     */
    @Query("SELECT p FROM Playlist p WHERE p.isPublic = true " +
           "ORDER BY p.totalTracks DESC, p.updatedAt DESC")
    Page<Playlist> findMostPopularPublicPlaylists(Pageable pageable);

    /**
     * Finds recently updated playlists in a hive.
     * 
     * @param hiveId The hive ID
     * @param limit Maximum number of results
     * @return List of recently updated playlists
     */
    @Query("SELECT p FROM Playlist p WHERE p.hiveId = :hiveId " +
           "ORDER BY p.updatedAt DESC LIMIT :limit")
    List<Playlist> findRecentlyUpdatedInHive(@Param("hiveId") String hiveId, 
                                           @Param("limit") int limit);

    /**
     * Counts playlists created by a user.
     * 
     * @param userId The user ID
     * @return Number of playlists created by the user
     */
    long countByCreatedBy(String userId);

    /**
     * Counts playlists in a hive.
     * 
     * @param hiveId The hive ID
     * @return Number of playlists in the hive
     */
    long countByHiveId(String hiveId);

    /**
     * Counts collaborative playlists a user has access to.
     * 
     * @param userId The user ID
     * @return Number of collaborative playlists accessible to the user
     */
    @Query("SELECT COUNT(DISTINCT p) FROM Playlist p LEFT JOIN p.collaborators c " +
           "WHERE p.isCollaborative = true AND (p.createdBy = :userId OR c.userId = :userId)")
    long countCollaborativePlaylistsForUser(@Param("userId") String userId);

    /**
     * Updates the total tracks and duration for a playlist.
     * 
     * @param playlistId The playlist ID
     * @param totalTracks New total tracks count
     * @param totalDurationMs New total duration in milliseconds
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE Playlist p SET p.totalTracks = :totalTracks, " +
           "p.totalDurationMs = :totalDurationMs WHERE p.id = :playlistId")
    int updatePlaylistTotals(@Param("playlistId") Long playlistId,
                           @Param("totalTracks") Integer totalTracks,
                           @Param("totalDurationMs") Long totalDurationMs);

    /**
     * Updates the Spotify playlist ID for a playlist.
     * 
     * @param playlistId The playlist ID
     * @param spotifyPlaylistId The Spotify playlist ID
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE Playlist p SET p.spotifyPlaylistId = :spotifyPlaylistId " +
           "WHERE p.id = :playlistId")
    int updateSpotifyPlaylistId(@Param("playlistId") Long playlistId,
                              @Param("spotifyPlaylistId") String spotifyPlaylistId);

    /**
     * Finds playlists by multiple IDs.
     * 
     * @param ids List of playlist IDs
     * @return List of playlists
     */
    @Query("SELECT p FROM Playlist p WHERE p.id IN :ids")
    List<Playlist> findByIdIn(@Param("ids") List<Long> ids);

    /**
     * Finds empty playlists (with no tracks).
     * 
     * @return List of empty playlists
     */
    @Query("SELECT p FROM Playlist p WHERE p.totalTracks = 0 OR p.totalTracks IS NULL")
    List<Playlist> findEmptyPlaylists();

    /**
     * Searches playlists by name, description, or creator.
     * 
     * @param searchTerm The search term
     * @param pageable Pagination information
     * @return Page of matching playlists
     */
    @Query("SELECT p FROM Playlist p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY p.updatedAt DESC")
    Page<Playlist> searchPlaylists(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds playlists by hive ID and session ID.
     * 
     * @param hiveId The hive ID
     * @param sessionId The session ID
     * @return Optional playlist
     */
    @Query("SELECT p FROM Playlist p WHERE p.hiveId = :hiveId AND p.sessionId = :sessionId")
    Optional<Playlist> findByHiveIdAndSessionId(@Param("hiveId") String hiveId, @Param("sessionId") String sessionId);

    /**
     * Finds active collaborative playlists by hive ID.
     * 
     * @param hiveId The hive ID
     * @return List of active collaborative playlists
     */
    @Query("SELECT p FROM Playlist p WHERE p.hiveId = :hiveId AND p.isCollaborative = true AND p.isActive = true")
    List<Playlist> findActiveCollaborativePlaylistsByHiveId(@Param("hiveId") String hiveId);

    /**
     * Finds collaborative playlists by hive ID.
     * 
     * @param hiveId The hive ID
     * @return List of collaborative playlists
     */
    @Query("SELECT p FROM Playlist p WHERE p.hiveId = :hiveId AND p.isCollaborative = true")
    List<Playlist> findByHiveIdAndCollaborativeTrue(@Param("hiveId") String hiveId);
    
    /**
     * Finds smart playlists that need refreshing.
     * 
     * @return List of smart playlists to refresh
     */
    @Query("SELECT p FROM Playlist p WHERE p.isSmartPlaylist = true AND " +
           "(p.lastAutoUpdate IS NULL OR p.lastAutoUpdate < :cutoffTime)")
    List<Playlist> findSmartPlaylistsToRefresh(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Finds playlists by energy level criteria.
     * 
     * @param energyLevel The energy level
     * @param pageable Pagination information
     * @return Page of matching playlists
     */
    @Query("SELECT DISTINCT p FROM Playlist p JOIN p.tracks t WHERE " +
           "JSON_EXTRACT(t.metadata, '$.energyLevel') = :energyLevel")
    Page<Playlist> findByEnergyLevel(@Param("energyLevel") String energyLevel, Pageable pageable);
    
    /**
     * Finds playlists suitable for a task type.
     * 
     * @param taskType The task type
     * @param pageable Pagination information
     * @return Page of suitable playlists
     */
    @Query("SELECT DISTINCT p FROM Playlist p WHERE " +
           "JSON_EXTRACT(p.smartCriteria, '$.taskType') = :taskType OR " +
           "p.tags LIKE CONCAT('%', :taskType, '%')")
    Page<Playlist> findByTaskType(@Param("taskType") String taskType, Pageable pageable);

    /**
     * Finds playlists that a user can modify (owner or has modification permissions).
     * 
     * @param userId The user ID
     * @return List of modifiable playlists
     */
    @Query("SELECT DISTINCT p FROM Playlist p LEFT JOIN p.collaborators c " +
           "WHERE p.createdBy = :userId OR " +
           "(c.userId = :userId AND (c.canAddTracks = true OR c.canRemoveTracks = true OR c.canEditPlaylist = true))")
    List<Playlist> findModifiablePlaylistsForUser(@Param("userId") String userId);

    /**
     * Gets playlist statistics for analytics.
     * 
     * @return Array of playlist statistics
     */
    @Query("SELECT " +
           "COUNT(*) as totalPlaylists, " +
           "COUNT(CASE WHEN p.isCollaborative = true THEN 1 END) as collaborativePlaylists, " +
           "COUNT(CASE WHEN p.isPublic = true THEN 1 END) as publicPlaylists, " +
           "COUNT(CASE WHEN p.hiveId IS NOT NULL THEN 1 END) as hivePlaylists, " +
           "AVG(p.totalTracks) as avgTracksPerPlaylist " +
           "FROM Playlist p")
    Object[] getPlaylistStatistics();
}