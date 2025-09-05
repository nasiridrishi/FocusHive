package com.focushive.music.repository;

import com.focushive.music.entity.SpotifyCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SpotifyCredentials entity.
 */
@Repository
public interface SpotifyCredentialsRepository extends JpaRepository<SpotifyCredentials, UUID> {

    Optional<SpotifyCredentials> findByUserId(String userId);

    Optional<SpotifyCredentials> findBySpotifyUserId(String spotifyUserId);

    void deleteByUserId(String userId);

    @Query("SELECT sc FROM SpotifyCredentials sc WHERE sc.expiresAt < :expiryDate AND sc.isActive = true")
    List<SpotifyCredentials> findExpiredCredentials(LocalDateTime expiryDate);

    @Query("SELECT sc FROM SpotifyCredentials sc WHERE sc.userId = :userId AND sc.isActive = true AND sc.expiresAt > :currentTime")
    Optional<SpotifyCredentials> findActiveCredentials(String userId, LocalDateTime currentTime);

    boolean existsByUserIdAndIsActiveTrue(String userId);
}