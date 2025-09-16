package com.focushive.identity.repository;

import com.focushive.identity.entity.OAuthClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * Repository for OAuth2 client management.
 */
@Repository
public interface OAuthClientRepository extends JpaRepository<OAuthClient, UUID> {
    
    /**
     * Find client by client ID.
     */
    Optional<OAuthClient> findByClientId(String clientId);
    
    /**
     * Find all clients for a specific user.
     */
    List<OAuthClient> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find enabled clients by client ID.
     */
    Optional<OAuthClient> findByClientIdAndEnabledTrue(String clientId);
    
    /**
     * Check if client ID exists.
     */
    boolean existsByClientId(String clientId);
    
    /**
     * Count clients for a user.
     */
    long countByUserId(UUID userId);
    
    /**
     * Find clients that haven't been used recently.
     */
    @Query("SELECT c FROM OAuthClient c WHERE c.lastUsedAt IS NULL OR c.lastUsedAt < :threshold")
    List<OAuthClient> findUnusedClients(@Param("threshold") Instant threshold);
    
    /**
     * Update last used timestamp.
     */
    @Modifying
    @Query("UPDATE OAuthClient c SET c.lastUsedAt = :timestamp WHERE c.id = :clientId")
    void updateLastUsedAt(@Param("clientId") UUID clientId, @Param("timestamp") Instant timestamp);

    /**
     * Find clients by enabled status with pagination.
     */
    Page<OAuthClient> findByEnabled(boolean enabled, Pageable pageable);

    /**
     * Search clients by name with pagination.
     */
    @Query("SELECT c FROM OAuthClient c WHERE LOWER(c.clientName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<OAuthClient> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Search clients by name and enabled status with pagination.
     */
    @Query("SELECT c FROM OAuthClient c WHERE LOWER(c.clientName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND c.enabled = :enabled")
    Page<OAuthClient> searchByNameAndEnabled(@Param("searchTerm") String searchTerm,
                                            @Param("enabled") boolean enabled,
                                            Pageable pageable);
}