package com.focushive.music.repository;

import com.focushive.music.model.MusicPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing user music preferences.
 * 
 * Provides data access methods for user music preferences including
 * Spotify integration, genre preferences, and listening activity tracking.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Repository
public interface UserMusicPreferenceRepository extends JpaRepository<MusicPreference, Long> {

    /**
     * Finds music preferences by user ID.
     * 
     * @param userId The user ID to search for
     * @return Optional containing the user's music preferences if found
     */
    Optional<MusicPreference> findByUserId(String userId);

    /**
     * Checks if a user has music preferences configured.
     * 
     * @param userId The user ID to check
     * @return true if preferences exist for the user
     */
    boolean existsByUserId(String userId);

    /**
     * Finds all users who have connected their Spotify accounts.
     * 
     * @return List of music preferences for users with Spotify connections
     */
    @Query("SELECT mp FROM MusicPreference mp WHERE mp.spotifyConnected = true")
    List<MusicPreference> findAllWithSpotifyConnected();

    /**
     * Finds users with expired Spotify tokens that need refresh.
     * 
     * @param currentTime The current timestamp
     * @return List of preferences with expired tokens
     */
    @Query("SELECT mp FROM MusicPreference mp WHERE mp.spotifyConnected = true " +
           "AND mp.spotifyExpiresAt < :currentTime")
    List<MusicPreference> findWithExpiredSpotifyTokens(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Finds users who prefer a specific genre.
     * 
     * @param genre The genre to search for
     * @return List of preferences containing the specified genre
     */
    @Query("SELECT mp FROM MusicPreference mp WHERE :genre MEMBER OF mp.preferredGenres")
    List<MusicPreference> findByPreferredGenresContaining(@Param("genre") String genre);

    /**
     * Finds users with specific energy level preferences.
     * 
     * @param minEnergy Minimum energy level
     * @param maxEnergy Maximum energy level
     * @return List of preferences within the energy range
     */
    @Query("SELECT mp FROM MusicPreference mp WHERE mp.preferredEnergyLevel >= :minEnergy " +
           "AND mp.preferredEnergyLevel <= :maxEnergy")
    List<MusicPreference> findByEnergyLevelRange(@Param("minEnergy") Integer minEnergy,
                                                @Param("maxEnergy") Integer maxEnergy);

    /**
     * Finds users with specific mood preferences.
     * 
     * @param mood The mood to search for
     * @return List of preferences with the specified mood
     */
    List<MusicPreference> findByPreferredMood(String mood);

    /**
     * Finds users who haven't been active recently.
     * 
     * @param threshold Timestamp threshold for inactive users
     * @return List of preferences for inactive users
     */
    @Query("SELECT mp FROM MusicPreference mp WHERE mp.lastListeningActivity < :threshold " +
           "OR mp.lastListeningActivity IS NULL")
    List<MusicPreference> findInactiveUsers(@Param("threshold") LocalDateTime threshold);

    /**
     * Updates the last listening activity for a user.
     * 
     * @param userId The user ID to update
     * @param timestamp The new activity timestamp
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE MusicPreference mp SET mp.lastListeningActivity = :timestamp " +
           "WHERE mp.userId = :userId")
    int updateLastListeningActivity(@Param("userId") String userId, 
                                  @Param("timestamp") LocalDateTime timestamp);

    /**
     * Updates Spotify token information for a user.
     * 
     * @param userId The user ID to update
     * @param accessToken The new access token
     * @param refreshToken The new refresh token
     * @param expiresAt The token expiration timestamp
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE MusicPreference mp SET mp.spotifyAccessToken = :accessToken, " +
           "mp.spotifyRefreshToken = :refreshToken, mp.spotifyExpiresAt = :expiresAt, " +
           "mp.spotifyConnected = true WHERE mp.userId = :userId")
    int updateSpotifyTokens(@Param("userId") String userId,
                          @Param("accessToken") String accessToken,
                          @Param("refreshToken") String refreshToken,
                          @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * Disconnects Spotify for a user by clearing tokens.
     * 
     * @param userId The user ID to disconnect
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE MusicPreference mp SET mp.spotifyConnected = false, " +
           "mp.spotifyAccessToken = null, mp.spotifyRefreshToken = null, " +
           "mp.spotifyExpiresAt = null WHERE mp.userId = :userId")
    int disconnectSpotify(@Param("userId") String userId);

    /**
     * Finds users with similar music preferences for recommendation purposes.
     * 
     * @param genres List of genres to match against
     * @param mood The mood preference to match
     * @param energyLevel The energy level to match (with tolerance)
     * @param tolerance Energy level tolerance range
     * @return List of users with similar preferences
     */
    @Query("SELECT mp FROM MusicPreference mp WHERE " +
           "(mp.preferredGenres IS NULL OR EXISTS (SELECT g FROM mp.preferredGenres g WHERE g IN :genres)) " +
           "AND (mp.preferredMood = :mood OR mp.preferredMood IS NULL) " +
           "AND (mp.preferredEnergyLevel BETWEEN :energyLevel - :tolerance AND :energyLevel + :tolerance " +
           "     OR mp.preferredEnergyLevel IS NULL)")
    List<MusicPreference> findUsersWithSimilarPreferences(@Param("genres") List<String> genres,
                                                         @Param("mood") String mood,
                                                         @Param("energyLevel") Integer energyLevel,
                                                         @Param("tolerance") Integer tolerance);

    /**
     * Gets statistics about music preferences in the system.
     * 
     * @return List of objects containing preference statistics
     */
    @Query("SELECT mp.preferredMood as mood, COUNT(*) as count FROM MusicPreference mp " +
           "WHERE mp.preferredMood IS NOT NULL GROUP BY mp.preferredMood ORDER BY count DESC")
    List<Object[]> getMoodStatistics();

    /**
     * Counts the number of users with Spotify connected.
     * 
     * @return Number of users with Spotify connections
     */
    @Query("SELECT COUNT(*) FROM MusicPreference mp WHERE mp.spotifyConnected = true")
    long countUsersWithSpotifyConnected();
}