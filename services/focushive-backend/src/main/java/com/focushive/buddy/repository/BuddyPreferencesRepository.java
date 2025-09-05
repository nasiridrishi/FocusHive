package com.focushive.buddy.repository;

import com.focushive.buddy.entity.BuddyPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuddyPreferencesRepository extends JpaRepository<BuddyPreferences, String> {
    
    Optional<BuddyPreferences> findByUserId(String userId);
    
    @Query("SELECT bp FROM BuddyPreferences bp " +
           "WHERE bp.matchingEnabled = true " +
           "AND bp.user.id != :excludeUserId")
    List<BuddyPreferences> findAvailableForMatching(@Param("excludeUserId") String excludeUserId);
    
    @Query("SELECT bp FROM BuddyPreferences bp " +
           "WHERE bp.matchingEnabled = true " +
           "AND bp.preferredTimezone = :timezone " +
           "AND bp.user.id != :excludeUserId")
    List<BuddyPreferences> findByTimezoneForMatching(
        @Param("timezone") String timezone,
        @Param("excludeUserId") String excludeUserId
    );
    
    @Query(value = "SELECT * FROM buddy_preferences bp " +
           "WHERE bp.matching_enabled = true " +
           "AND bp.focus_areas && CAST(:focusAreas AS text[]) " +
           "AND bp.user_id != :excludeUserId", 
           nativeQuery = true)
    List<BuddyPreferences> findByFocusAreasForMatching(
        @Param("focusAreas") String[] focusAreas,
        @Param("excludeUserId") String excludeUserId
    );
    
    @Query("SELECT bp FROM BuddyPreferences bp " +
           "WHERE bp.matchingEnabled = true " +
           "AND bp.communicationStyle = :style " +
           "AND bp.user.id != :excludeUserId")
    List<BuddyPreferences> findByCommunicationStyleForMatching(
        @Param("style") BuddyPreferences.CommunicationStyle style,
        @Param("excludeUserId") String excludeUserId
    );
    
    @Query("SELECT COUNT(bp) FROM BuddyPreferences bp " +
           "WHERE bp.matchingEnabled = true")
    Long countUsersAvailableForMatching();
}