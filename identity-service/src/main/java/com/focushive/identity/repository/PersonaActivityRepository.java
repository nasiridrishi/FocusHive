package com.focushive.identity.repository;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.PersonaActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PersonaActivity entities.
 * Provides data isolation between personas for activity tracking.
 */
@Repository
public interface PersonaActivityRepository extends JpaRepository<PersonaActivity, UUID> {
    
    // Basic queries by persona
    List<PersonaActivity> findByPersona(Persona persona);
    
    List<PersonaActivity> findByPersonaId(UUID personaId);
    
    Page<PersonaActivity> findByPersonaId(UUID personaId, Pageable pageable);
    
    // Activity type queries
    List<PersonaActivity> findByPersonaIdAndActivityType(UUID personaId, String activityType);
    
    Page<PersonaActivity> findByPersonaIdAndActivityType(UUID personaId, String activityType, Pageable pageable);
    
    // Time-based queries
    List<PersonaActivity> findByPersonaIdAndActivityTimestampBetween(
            UUID personaId, Instant startTime, Instant endTime);
    
    @Query("SELECT pa FROM PersonaActivity pa WHERE pa.persona.id = :personaId " +
           "AND pa.activityTimestamp >= :startTime " +
           "ORDER BY pa.activityTimestamp DESC")
    List<PersonaActivity> findRecentActivities(@Param("personaId") UUID personaId, 
                                              @Param("startTime") Instant startTime);
    
    // Session-based queries
    List<PersonaActivity> findByPersonaIdAndSessionId(UUID personaId, UUID sessionId);
    
    Optional<PersonaActivity> findByPersonaIdAndSessionIdAndStatus(
            UUID personaId, UUID sessionId, String status);
    
    // Active activities
    @Query("SELECT pa FROM PersonaActivity pa WHERE pa.persona.id = :personaId " +
           "AND pa.status = 'ACTIVE' " +
           "AND (pa.endTime IS NULL OR pa.endTime > :now)")
    List<PersonaActivity> findActiveActivities(@Param("personaId") UUID personaId, 
                                              @Param("now") Instant now);
    
    // Analytics queries
    @Query("SELECT COUNT(pa) FROM PersonaActivity pa WHERE pa.persona.id = :personaId " +
           "AND pa.activityType = :activityType " +
           "AND pa.activityTimestamp >= :startTime")
    long countByPersonaIdAndActivityTypeAndTimestampAfter(
            @Param("personaId") UUID personaId, 
            @Param("activityType") String activityType,
            @Param("startTime") Instant startTime);
    
    @Query("SELECT COALESCE(SUM(pa.durationMinutes), 0) FROM PersonaActivity pa " +
           "WHERE pa.persona.id = :personaId " +
           "AND pa.activityType = :activityType " +
           "AND pa.activityTimestamp >= :startTime " +
           "AND pa.durationMinutes IS NOT NULL")
    long sumDurationByPersonaIdAndActivityTypeAndTimestampAfter(
            @Param("personaId") UUID personaId, 
            @Param("activityType") String activityType,
            @Param("startTime") Instant startTime);
    
    @Query("SELECT AVG(pa.productivityScore) FROM PersonaActivity pa " +
           "WHERE pa.persona.id = :personaId " +
           "AND pa.activityTimestamp >= :startTime " +
           "AND pa.productivityScore IS NOT NULL")
    Optional<Double> getAverageProductivityScore(@Param("personaId") UUID personaId, 
                                                @Param("startTime") Instant startTime);
    
    @Query("SELECT AVG(pa.focusScore) FROM PersonaActivity pa " +
           "WHERE pa.persona.id = :personaId " +
           "AND pa.activityTimestamp >= :startTime " +
           "AND pa.focusScore IS NOT NULL")
    Optional<Double> getAverageFocusScore(@Param("personaId") UUID personaId, 
                                         @Param("startTime") Instant startTime);
    
    // Data isolation - ensure user can only access their own persona activities
    @Query("SELECT pa FROM PersonaActivity pa WHERE pa.persona.user.id = :userId " +
           "AND pa.persona.id = :personaId")
    List<PersonaActivity> findByUserIdAndPersonaId(@Param("userId") UUID userId, 
                                                   @Param("personaId") UUID personaId);
    
    @Query("SELECT COUNT(pa) FROM PersonaActivity pa WHERE pa.persona.user.id = :userId " +
           "AND pa.persona.id = :personaId")
    long countByUserIdAndPersonaId(@Param("userId") UUID userId, 
                                   @Param("personaId") UUID personaId);
    
    // Tag-based queries
    @Query("SELECT pa FROM PersonaActivity pa JOIN pa.tags t " +
           "WHERE pa.persona.id = :personaId AND t = :tag")
    List<PersonaActivity> findByPersonaIdAndTag(@Param("personaId") UUID personaId, 
                                               @Param("tag") String tag);
    
    // Cleanup queries
    @Query("DELETE FROM PersonaActivity pa WHERE pa.persona.id = :personaId " +
           "AND pa.activityTimestamp < :cutoffTime")
    void deleteOldActivities(@Param("personaId") UUID personaId, 
                            @Param("cutoffTime") Instant cutoffTime);
}