package com.focushive.identity.repository;

import com.focushive.identity.entity.EncryptionKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing encryption keys persistence.
 * Ensures encryption keys survive service restarts.
 */
@Repository
public interface EncryptionKeyRepository extends JpaRepository<EncryptionKeyEntity, UUID> {

    /**
     * Find encryption key by version.
     *
     * @param version the key version
     * @return optional containing the key if found
     */
    Optional<EncryptionKeyEntity> findByVersion(String version);

    /**
     * Find the currently active encryption key.
     *
     * @return optional containing the active key if one exists
     */
    Optional<EncryptionKeyEntity> findByActiveTrue();

    /**
     * Find all active encryption keys.
     * Should typically return only one, but handles edge cases.
     *
     * @return list of active keys
     */
    List<EncryptionKeyEntity> findAllByActiveTrue();

    /**
     * Find all valid (active and not expired) encryption keys.
     *
     * @param currentTime the current time for expiration check
     * @return list of valid keys
     */
    @Query("SELECT k FROM EncryptionKeyEntity k WHERE k.active = true AND (k.expiresAt IS NULL OR k.expiresAt > :currentTime)")
    List<EncryptionKeyEntity> findValidKeys(@Param("currentTime") Instant currentTime);

    /**
     * Find keys that need rotation based on age.
     *
     * @param cutoffTime the time before which keys should be rotated
     * @return list of keys needing rotation
     */
    @Query("SELECT k FROM EncryptionKeyEntity k WHERE k.active = true AND k.createdAt < :cutoffTime")
    List<EncryptionKeyEntity> findKeysNeedingRotation(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Find expired keys for cleanup.
     *
     * @param currentTime the current time
     * @return list of expired keys
     */
    @Query("SELECT k FROM EncryptionKeyEntity k WHERE k.expiresAt IS NOT NULL AND k.expiresAt < :currentTime")
    List<EncryptionKeyEntity> findExpiredKeys(@Param("currentTime") Instant currentTime);

    /**
     * Deactivate all encryption keys except the specified one.
     * Used when setting a new active key.
     *
     * @param excludeId the ID of the key to keep active
     * @return number of keys deactivated
     */
    @Modifying
    @Transactional
    @Query("UPDATE EncryptionKeyEntity k SET k.active = false WHERE k.active = true AND k.id != :excludeId")
    int deactivateAllExcept(@Param("excludeId") UUID excludeId);

    /**
     * Deactivate all encryption keys.
     * Used during key rotation to ensure clean state.
     *
     * @return number of keys deactivated
     */
    @Modifying
    @Transactional
    @Query("UPDATE EncryptionKeyEntity k SET k.active = false WHERE k.active = true")
    int deactivateAll();

    /**
     * Check if a key version exists.
     *
     * @param version the key version
     * @return true if the version exists
     */
    boolean existsByVersion(String version);

    /**
     * Count active encryption keys.
     * Should typically return 1, but handles edge cases.
     *
     * @return count of active keys
     */
    long countByActiveTrue();

    /**
     * Find keys by creation date range.
     * Useful for audit and reporting.
     *
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return list of keys created in the range
     */
    List<EncryptionKeyEntity> findByCreatedAtBetween(Instant startTime, Instant endTime);

    /**
     * Find keys that were rotated from a specific version.
     *
     * @param previousVersion the previous key version
     * @return optional containing the rotated key if found
     */
    Optional<EncryptionKeyEntity> findByRotatedFrom(String previousVersion);

    /**
     * Delete keys older than a specific date that are not active.
     * Used for cleanup of old keys.
     *
     * @param cutoffTime the time before which to delete keys
     * @return number of keys deleted
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM EncryptionKeyEntity k WHERE k.createdAt < :cutoffTime AND k.active = false")
    int deleteOldInactiveKeys(@Param("cutoffTime") Instant cutoffTime);
}