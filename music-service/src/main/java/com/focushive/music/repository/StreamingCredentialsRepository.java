package com.focushive.music.repository;

import com.focushive.music.model.StreamingCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for StreamingCredentials entity.
 * 
 * Provides data access operations for managing user streaming service
 * credentials including OAuth tokens and service-specific metadata.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Repository
public interface StreamingCredentialsRepository extends JpaRepository<StreamingCredentials, UUID> {

    /**
     * Finds credentials by user ID and platform.
     * 
     * @param userId User ID
     * @param platform Platform (e.g., "SPOTIFY", "APPLE_MUSIC")
     * @return Optional StreamingCredentials
     */
    Optional<StreamingCredentials> findByUserIdAndPlatform(String userId, StreamingCredentials.StreamingPlatform platform);

    /**
     * Finds all credentials for a user.
     * 
     * @param userId User ID
     * @return List of StreamingCredentials
     */
    List<StreamingCredentials> findByUserId(UUID userId);

    /**
     * Finds all credentials for a platform.
     * 
     * @param platform Platform
     * @return List of StreamingCredentials
     */
    List<StreamingCredentials> findByPlatform(StreamingCredentials.StreamingPlatform platform);


    /**
     * Finds credentials that expire before the specified date.
     * 
     * @param expiryDate Expiry date threshold
     * @return List of expiring credentials
     */
    @Query("SELECT sc FROM StreamingCredentials sc WHERE sc.expiresAt < :expiryDate")
    List<StreamingCredentials> findExpiringCredentials(@Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Finds active credentials (not expired) for a user and platform.
     * 
     * @param userId User ID
     * @param platform Platform
     * @param currentTime Current timestamp
     * @return Optional StreamingCredentials
     */
    @Query("SELECT sc FROM StreamingCredentials sc WHERE sc.userId = :userId " +
           "AND sc.platform = :platform AND sc.expiresAt > :currentTime AND sc.isActive = true")
    Optional<StreamingCredentials> findActiveCredentials(
        @Param("userId") String userId, 
        @Param("platform") StreamingCredentials.StreamingPlatform platform, 
        @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * Deletes all credentials for a user and platform.
     * 
     * @param userId User ID
     * @param platform Platform
     */
    void deleteByUserIdAndPlatform(String userId, StreamingCredentials.StreamingPlatform platform);

    /**
     * Deletes all credentials for a user.
     * 
     * @param userId User ID
     */
    void deleteByUserId(String userId);

    /**
     * Checks if user has active credentials for a platform.
     * 
     * @param userId User ID
     * @param platform Platform
     * @param currentTime Current timestamp
     * @return true if active credentials exist
     */
    @Query("SELECT COUNT(sc) > 0 FROM StreamingCredentials sc WHERE sc.userId = :userId " +
           "AND sc.platform = :platform AND sc.expiresAt > :currentTime AND sc.isActive = true")
    boolean hasActiveCredentials(
        @Param("userId") String userId, 
        @Param("platform") StreamingCredentials.StreamingPlatform platform, 
        @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * Gets count of users connected to a specific platform.
     * 
     * @param platform Platform
     * @return Number of connected users
     */
    @Query("SELECT COUNT(DISTINCT sc.userId) FROM StreamingCredentials sc WHERE sc.platform = :platform AND sc.isActive = true")
    long countConnectedUsers(@Param("platform") StreamingCredentials.StreamingPlatform platform);

    /**
     * Gets count of active connections for a platform.
     * 
     * @param platform Platform
     * @param currentTime Current timestamp
     * @return Number of active connections
     */
    @Query("SELECT COUNT(sc) FROM StreamingCredentials sc WHERE sc.platform = :platform " +
           "AND sc.expiresAt > :currentTime AND sc.isActive = true")
    long countActiveConnections(@Param("platform") StreamingCredentials.StreamingPlatform platform, @Param("currentTime") LocalDateTime currentTime);
}