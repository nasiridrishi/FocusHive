package com.focushive.buddy.repository;

import com.focushive.buddy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 * Handles basic CRUD and custom queries for user data.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find active users (not deactivated).
     */
    List<User> findByActiveTrue();

    /**
     * Find users who have been seen recently.
     */
    @Query("SELECT u FROM User u WHERE u.lastSeenAt >= :since AND u.active = true")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * Find users by timezone.
     */
    List<User> findByTimezoneAndActiveTrue(String timezone);

    /**
     * Find users with specific interests using PostgreSQL array operations.
     */
    @Query(value = "SELECT * FROM buddy_users u WHERE u.active = true AND " +
           "u.interests && CAST(:interests AS text[])",
           nativeQuery = true)
    List<User> findByInterestsIn(@Param("interests") String[] interests);

    /**
     * Find users with ANY of the specified interests (optimized for large datasets).
     */
    @Query(value = "SELECT * FROM buddy_users u WHERE u.active = true AND " +
           "EXISTS (SELECT 1 FROM unnest(u.interests) AS interest WHERE interest = ANY(:interests))",
           nativeQuery = true)
    List<User> findByAnyInterest(@Param("interests") String[] interests);

    /**
     * Find users with specific focus times using PostgreSQL array operations.
     */
    @Query(value = "SELECT * FROM buddy_users u WHERE u.active = true AND " +
           "u.preferred_focus_times && CAST(:focusTimes AS text[])",
           nativeQuery = true)
    List<User> findByPreferredFocusTimesIn(@Param("focusTimes") String[] focusTimes);

    /**
     * Find users by multiple criteria with interest overlap scoring.
     */
    @Query(value = """
        SELECT u.*,
               array_length(
                   ARRAY(
                       SELECT unnest(u.interests)
                       INTERSECT
                       SELECT unnest(CAST(:targetInterests AS text[]))
                   ), 1
               ) as interest_overlap_count
        FROM buddy_users u
        WHERE u.active = true
        AND (:timezone IS NULL OR u.timezone = :timezone)
        AND (:experienceLevel IS NULL OR u.experience_level = :experienceLevel)
        AND (:communicationStyle IS NULL OR u.communication_style = :communicationStyle)
        AND u.last_seen_at >= :minLastSeen
        ORDER BY interest_overlap_count DESC NULLS LAST, u.last_seen_at DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<User> findCompatibleUsers(
        @Param("targetInterests") String[] targetInterests,
        @Param("timezone") String timezone,
        @Param("experienceLevel") String experienceLevel,
        @Param("communicationStyle") String communicationStyle,
        @Param("minLastSeen") LocalDateTime minLastSeen,
        @Param("maxResults") Integer maxResults
    );

    /**
     * Find users by experience level.
     */
    List<User> findByExperienceLevelAndActiveTrue(String experienceLevel);

    /**
     * Find users by communication style.
     */
    List<User> findByCommunicationStyleAndActiveTrue(String communicationStyle);

    /**
     * Check if user exists and is active.
     */
    boolean existsByIdAndActiveTrue(String id);

    /**
     * Update user's last seen timestamp.
     */
    @Query("UPDATE User u SET u.lastSeenAt = :lastSeen WHERE u.id = :userId")
    void updateLastSeenAt(@Param("userId") String userId, @Param("lastSeen") LocalDateTime lastSeen);
}