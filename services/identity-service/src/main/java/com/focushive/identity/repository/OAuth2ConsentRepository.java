package com.focushive.identity.repository;

import com.focushive.identity.entity.OAuth2Consent;
import com.focushive.identity.entity.OAuthClient;
import com.focushive.identity.entity.User;
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
 * Repository for OAuth2 consent management.
 */
@Repository
public interface OAuth2ConsentRepository extends JpaRepository<OAuth2Consent, UUID> {

    /**
     * Find consent by user and client.
     */
    Optional<OAuth2Consent> findByUserAndClient(User user, OAuthClient client);

    /**
     * Find consent by user ID and client ID.
     */
    @Query("SELECT c FROM OAuth2Consent c WHERE c.user.id = :userId AND c.client.id = :clientId")
    Optional<OAuth2Consent> findByUserIdAndClientId(@Param("userId") UUID userId, @Param("clientId") UUID clientId);

    /**
     * Find valid consent (not revoked and not expired).
     */
    @Query("SELECT c FROM OAuth2Consent c WHERE c.user.id = :userId AND c.client.id = :clientId " +
           "AND c.revoked = false AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    Optional<OAuth2Consent> findValidConsent(@Param("userId") UUID userId,
                                            @Param("clientId") UUID clientId,
                                            @Param("now") Instant now);

    /**
     * Find all consents for a user.
     */
    List<OAuth2Consent> findByUser(User user);

    /**
     * Find all consents for a user by user ID.
     */
    @Query("SELECT c FROM OAuth2Consent c WHERE c.user.id = :userId")
    List<OAuth2Consent> findByUserId(@Param("userId") UUID userId);

    /**
     * Find all valid consents for a user.
     */
    @Query("SELECT c FROM OAuth2Consent c WHERE c.user.id = :userId " +
           "AND c.revoked = false AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<OAuth2Consent> findValidConsentsByUser(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Find all consents for a client.
     */
    List<OAuth2Consent> findByClient(OAuthClient client);

    /**
     * Find all valid consents for a client.
     */
    @Query("SELECT c FROM OAuth2Consent c WHERE c.client.id = :clientId " +
           "AND c.revoked = false AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<OAuth2Consent> findValidConsentsByClient(@Param("clientId") UUID clientId, @Param("now") Instant now);

    /**
     * Revoke all consents for a user.
     */
    @Modifying
    @Query("UPDATE OAuth2Consent c SET c.revoked = true, c.revokedAt = :now, " +
           "c.revocationReason = :reason WHERE c.user.id = :userId AND c.revoked = false")
    int revokeAllConsentsForUser(@Param("userId") UUID userId,
                                 @Param("now") Instant now,
                                 @Param("reason") String reason);

    /**
     * Revoke all consents for a client.
     */
    @Modifying
    @Query("UPDATE OAuth2Consent c SET c.revoked = true, c.revokedAt = :now, " +
           "c.revocationReason = :reason WHERE c.client.id = :clientId AND c.revoked = false")
    int revokeAllConsentsForClient(@Param("clientId") UUID clientId,
                                   @Param("now") Instant now,
                                   @Param("reason") String reason);

    /**
     * Revoke consent for specific user and client.
     */
    @Modifying
    @Query("UPDATE OAuth2Consent c SET c.revoked = true, c.revokedAt = :now, " +
           "c.revocationReason = :reason WHERE c.user.id = :userId AND c.client.id = :clientId " +
           "AND c.revoked = false")
    int revokeConsent(@Param("userId") UUID userId,
                     @Param("clientId") UUID clientId,
                     @Param("now") Instant now,
                     @Param("reason") String reason);

    /**
     * Delete expired consents.
     */
    @Modifying
    @Query("DELETE FROM OAuth2Consent c WHERE c.expiresAt IS NOT NULL AND c.expiresAt <= :threshold")
    int deleteExpiredConsents(@Param("threshold") Instant threshold);

    /**
     * Count consents for a user.
     */
    @Query("SELECT COUNT(c) FROM OAuth2Consent c WHERE c.user.id = :userId AND c.revoked = false")
    long countActiveConsentsByUser(@Param("userId") UUID userId);

    /**
     * Count consents for a client.
     */
    @Query("SELECT COUNT(c) FROM OAuth2Consent c WHERE c.client.id = :clientId AND c.revoked = false")
    long countActiveConsentsByClient(@Param("clientId") UUID clientId);

    /**
     * Check if user has granted specific scope to client.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM OAuth2Consent c " +
           "JOIN c.grantedScopes s WHERE c.user.id = :userId AND c.client.id = :clientId " +
           "AND s = :scope AND c.revoked = false AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    boolean hasUserGrantedScope(@Param("userId") UUID userId,
                               @Param("clientId") UUID clientId,
                               @Param("scope") String scope,
                               @Param("now") Instant now);

    /**
     * Find consents that need renewal (expiring soon).
     */
    @Query("SELECT c FROM OAuth2Consent c WHERE c.revoked = false " +
           "AND c.expiresAt IS NOT NULL AND c.expiresAt BETWEEN :now AND :expiryThreshold")
    List<OAuth2Consent> findConsentsNeedingRenewal(@Param("now") Instant now,
                                                   @Param("expiryThreshold") Instant expiryThreshold);
}