package com.focushive.chat.repository;

import com.focushive.chat.entity.TypingIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TypingIndicatorRepository extends JpaRepository<TypingIndicator, UUID> {
    
    /**
     * Find typing indicator for a specific user in a hive
     */
    Optional<TypingIndicator> findByHiveIdAndUserId(UUID hiveId, UUID userId);
    
    /**
     * Find all active (non-expired) typing indicators for a hive
     */
    @Query("SELECT t FROM TypingIndicator t WHERE t.hiveId = :hiveId AND t.expiresAt > :now")
    List<TypingIndicator> findActiveTypingIndicatorsInHive(@Param("hiveId") UUID hiveId, 
                                                          @Param("now") ZonedDateTime now);
    
    /**
     * Find all expired typing indicators
     */
    List<TypingIndicator> findByExpiresAtBefore(ZonedDateTime cutoff);
    
    /**
     * Delete expired typing indicators
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM TypingIndicator t WHERE t.expiresAt < :cutoff")
    int deleteExpiredIndicators(@Param("cutoff") ZonedDateTime cutoff);
    
    /**
     * Delete typing indicator for a specific user in a hive
     */
    @Modifying
    @Transactional
    void deleteByHiveIdAndUserId(UUID hiveId, UUID userId);
    
    /**
     * Update or extend the expiry time for an existing typing indicator
     */
    @Modifying
    @Transactional
    @Query("UPDATE TypingIndicator t SET t.expiresAt = :newExpiry WHERE t.hiveId = :hiveId AND t.userId = :userId")
    int updateTypingIndicatorExpiry(@Param("hiveId") UUID hiveId, 
                                   @Param("userId") UUID userId, 
                                   @Param("newExpiry") ZonedDateTime newExpiry);
    
    /**
     * Count active typing indicators in a hive
     */
    @Query("SELECT COUNT(t) FROM TypingIndicator t WHERE t.hiveId = :hiveId AND t.expiresAt > :now")
    long countActiveTypingIndicatorsInHive(@Param("hiveId") UUID hiveId, @Param("now") ZonedDateTime now);
}