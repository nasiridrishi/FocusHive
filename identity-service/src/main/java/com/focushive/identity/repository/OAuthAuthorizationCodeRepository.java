package com.focushive.identity.repository;

import com.focushive.identity.entity.OAuthAuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OAuth2 authorization code management.
 */
@Repository
public interface OAuthAuthorizationCodeRepository extends JpaRepository<OAuthAuthorizationCode, UUID> {
    
    /**
     * Find authorization code by code value.
     */
    Optional<OAuthAuthorizationCode> findByCode(String code);
    
    /**
     * Find valid authorization code (not used and not expired).
     */
    @Query("SELECT ac FROM OAuthAuthorizationCode ac WHERE ac.code = :code AND ac.used = false AND ac.expiresAt > :now")
    Optional<OAuthAuthorizationCode> findValidCode(@Param("code") String code, @Param("now") Instant now);
    
    /**
     * Find authorization code for specific client and redirect URI.
     */
    @Query("SELECT ac FROM OAuthAuthorizationCode ac WHERE ac.code = :code AND ac.clientId = :clientId AND ac.redirectUri = :redirectUri AND ac.used = false AND ac.expiresAt > :now")
    Optional<OAuthAuthorizationCode> findValidCodeForClient(@Param("code") String code, 
                                                           @Param("clientId") UUID clientId, 
                                                           @Param("redirectUri") String redirectUri, 
                                                           @Param("now") Instant now);
    
    /**
     * Find all codes for a user.
     */
    List<OAuthAuthorizationCode> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find all codes for a client.
     */
    List<OAuthAuthorizationCode> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    
    /**
     * Find codes by session ID.
     */
    List<OAuthAuthorizationCode> findBySessionId(String sessionId);
    
    /**
     * Find expired codes.
     */
    @Query("SELECT ac FROM OAuthAuthorizationCode ac WHERE ac.used = false AND ac.expiresAt <= :now")
    List<OAuthAuthorizationCode> findExpiredCodes(@Param("now") Instant now);
    
    /**
     * Mark code as used.
     */
    @Modifying
    @Query("UPDATE OAuthAuthorizationCode ac SET ac.used = true, ac.usedAt = :now WHERE ac.id = :codeId")
    int markCodeAsUsed(@Param("codeId") UUID codeId, @Param("now") Instant now);
    
    /**
     * Count active codes for a user.
     */
    @Query("SELECT COUNT(ac) FROM OAuthAuthorizationCode ac WHERE ac.userId = :userId AND ac.used = false AND ac.expiresAt > :now")
    long countActiveCodesByUser(@Param("userId") UUID userId, @Param("now") Instant now);
    
    /**
     * Count active codes for a client.
     */
    @Query("SELECT COUNT(ac) FROM OAuthAuthorizationCode ac WHERE ac.clientId = :clientId AND ac.used = false AND ac.expiresAt > :now")
    long countActiveCodesByClient(@Param("clientId") UUID clientId, @Param("now") Instant now);
    
    /**
     * Delete expired codes.
     */
    @Modifying
    @Query("DELETE FROM OAuthAuthorizationCode ac WHERE ac.expiresAt <= :threshold")
    int deleteExpiredCodes(@Param("threshold") Instant threshold);
    
    /**
     * Delete used codes older than threshold.
     */
    @Modifying
    @Query("DELETE FROM OAuthAuthorizationCode ac WHERE ac.used = true AND ac.usedAt <= :threshold")
    int deleteOldUsedCodes(@Param("threshold") Instant threshold);
}