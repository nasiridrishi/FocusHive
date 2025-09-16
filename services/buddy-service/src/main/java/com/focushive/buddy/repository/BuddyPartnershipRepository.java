package com.focushive.buddy.repository;

import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.entity.BuddyPartnership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for BuddyPartnership entity operations.
 * Provides CRUD operations and custom queries for buddy partnership management.
 */
@Repository
public interface BuddyPartnershipRepository extends JpaRepository<BuddyPartnership, UUID> {

    // Basic queries
    Optional<BuddyPartnership> findByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    // Bidirectional partnership lookup
    @Query("SELECT bp FROM BuddyPartnership bp WHERE (bp.user1Id = :userId1 AND bp.user2Id = :userId2) OR (bp.user1Id = :userId2 AND bp.user2Id = :userId1)")
    Optional<BuddyPartnership> findPartnershipBetweenUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    // User partnerships
    @Query("SELECT bp FROM BuddyPartnership bp WHERE bp.user1Id = :userId OR bp.user2Id = :userId")
    List<BuddyPartnership> findAllPartnershipsByUserId(@Param("userId") UUID userId);

    // Find active partnerships for a user
    @Query("SELECT bp FROM BuddyPartnership bp WHERE (bp.user1Id = :userId OR bp.user2Id = :userId) AND bp.status = 'ACTIVE'")
    List<BuddyPartnership> findActivePartnershipsByUserId(@Param("userId") UUID userId);

    // Count active partnerships for a user
    @Query("SELECT COUNT(bp) FROM BuddyPartnership bp WHERE (bp.user1Id = :userId OR bp.user2Id = :userId) AND bp.status = 'ACTIVE'")
    long countActivePartnershipsByUserId(@Param("userId") UUID userId);

    // Check if active partnership exists between two users (UUID version)
    default boolean existsActivePartnershipBetweenUsers(UUID userId1, UUID userId2) {
        return findPartnershipBetweenUsers(userId1, userId2)
            .map(partnership -> partnership.getStatus() == PartnershipStatus.ACTIVE)
            .orElse(false);
    }

    // Status-based queries
    List<BuddyPartnership> findByStatus(PartnershipStatus status);

    @Query("SELECT bp FROM BuddyPartnership bp WHERE (bp.user1Id = :userId OR bp.user2Id = :userId) AND bp.status = 'PENDING'")
    List<BuddyPartnership> findPendingPartnershipsByUserId(@Param("userId") UUID userId);

    // Ended partnerships with reasons
    @Query("SELECT bp FROM BuddyPartnership bp WHERE bp.status = 'ENDED' AND bp.endReason IS NOT NULL")
    List<BuddyPartnership> findEndedPartnershipsWithReasons();

    // Health score queries
    List<BuddyPartnership> findByHealthScoreGreaterThan(BigDecimal healthScore);

    // Date range queries
    List<BuddyPartnership> findPartnershipsByStartedAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);

    // Inactive partnerships (based on last interaction)
    @Query("SELECT bp FROM BuddyPartnership bp WHERE bp.lastInteractionAt < :threshold AND bp.status = 'ACTIVE'")
    List<BuddyPartnership> findInactivePartnerships(@Param("threshold") ZonedDateTime threshold);

    // Convert int days to find inactive partnerships - simplified for H2 compatibility
    default List<BuddyPartnership> findInactivePartnerships(int inactiveDays) {
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(inactiveDays);
        return findInactivePartnerships(threshold);
    }

    // Version that accepts a specific time (for testing with fixed clock)
    default List<BuddyPartnership> findInactivePartnerships(int inactiveDays, ZonedDateTime currentTime) {
        ZonedDateTime threshold = currentTime.minusDays(inactiveDays);
        return findInactivePartnerships(threshold);
    }

    // Expired pending requests - simplified for H2 compatibility
    default List<BuddyPartnership> findExpiredPendingRequests(int expirationHours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(expirationHours);
        return findByStatusAndCreatedAtBefore(PartnershipStatus.PENDING, threshold);
    }

    // Version that accepts a specific time (for testing with fixed clock)
    default List<BuddyPartnership> findExpiredPendingRequests(int expirationHours, LocalDateTime currentTime) {
        LocalDateTime threshold = currentTime.minusHours(expirationHours);
        return findByStatusAndCreatedAtBefore(PartnershipStatus.PENDING, threshold);
    }

    // Helper for expired requests - Spring Data JPA derived query method
    List<BuddyPartnership> findByStatusAndCreatedAtBefore(PartnershipStatus status, LocalDateTime threshold);

    // Partnership duration calculation - simplified for H2
    default Long calculatePartnershipDurationInDays(UUID id) {
        return findById(id)
            .map(bp -> {
                if (bp.getStartedAt() != null && bp.getEndedAt() != null) {
                    return java.time.temporal.ChronoUnit.DAYS.between(
                        bp.getStartedAt().toLocalDate(),
                        bp.getEndedAt().toLocalDate()
                    );
                }
                return 0L;
            })
            .orElse(0L);
    }

    // Bulk update operations
    @Modifying
    @Transactional
    @Query("UPDATE BuddyPartnership bp SET bp.healthScore = :score WHERE bp.id = :id")
    void updateHealthScore(@Param("id") UUID id, @Param("score") BigDecimal score);

    @Modifying
    @Transactional
    @Query("UPDATE BuddyPartnership bp SET bp.lastInteractionAt = :timestamp WHERE bp.id = :id")
    void updateLastInteractionAt(@Param("id") UUID id, @Param("timestamp") ZonedDateTime timestamp);

    /**
     * Saves a partnership with duplicate checking.
     * This method ensures no duplicate partnerships are created between the same users.
     */
    default BuddyPartnership saveWithDuplicateCheck(BuddyPartnership partnership) {
        // Check for existing partnership in both directions
        Optional<BuddyPartnership> existing = findPartnershipBetweenUsers(
            partnership.getUser1Id(),
            partnership.getUser2Id()
        );

        if (existing.isPresent() && !existing.get().getId().equals(partnership.getId())) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                "Partnership already exists between users " +
                partnership.getUser1Id() + " and " + partnership.getUser2Id()
            );
        }

        return save(partnership);
    }

    /**
     * Validates a partnership before save to prevent invalid status transitions.
     * Prevents reactivation of ended partnerships.
     *
     * Note: This validation is now handled at the entity level via @PreUpdate callback.
     * This method is kept for potential future use but is not currently needed.
     */
    default void validateStatusTransition(BuddyPartnership partnership) {
        // Entity-level validation in @PreUpdate handles status transition validation
        // This method is available for additional repository-level validations if needed
    }

    /**
     * Gets the current status directly from database, bypassing JPA cache.
     */
    @Query(value = "SELECT status FROM buddy_partnerships WHERE id = :id", nativeQuery = true)
    String getCurrentStatusFromDatabase(@Param("id") UUID id);

    /**
     * Saves a partnership with status transition validation.
     * Prevents reactivation of ended partnerships.
     */
    default BuddyPartnership saveWithValidation(BuddyPartnership partnership) {
        validateStatusTransition(partnership);
        return save(partnership);
    }

    /**
     * Saves and flushes a partnership with status transition validation.
     * Prevents reactivation of ended partnerships.
     */
    default BuddyPartnership saveAndFlushWithValidation(BuddyPartnership partnership) {
        validateStatusTransition(partnership);
        return saveAndFlush(partnership);
    }
}