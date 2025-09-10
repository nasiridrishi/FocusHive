package com.focushive.identity.repository;

import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Persona entities.
 */
@Repository
public interface PersonaRepository extends JpaRepository<Persona, UUID> {
    
    List<Persona> findByUser(User user);
    
    List<Persona> findByUserId(UUID userId);
    
    Optional<Persona> findByIdAndUser(UUID id, User user);
    
    Optional<Persona> findByUserAndIsDefaultTrue(User user);
    
    Optional<Persona> findByUserAndIsActiveTrue(User user);
    
    Optional<Persona> findByUserIdAndIsActiveTrue(UUID userId);
    
    @Query("SELECT p FROM Persona p WHERE p.user.id = :userId AND p.name = :name")
    Optional<Persona> findByUserIdAndName(@Param("userId") UUID userId, @Param("name") String name);
    
    @Query("SELECT COUNT(p) FROM Persona p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    @Transactional
    @Modifying
    @Query("UPDATE Persona p SET p.isActive = CASE WHEN p.id = :personaId THEN true ELSE false END WHERE p.user.id = :userId")
    void updateActivePersona(@Param("userId") UUID userId, @Param("personaId") UUID personaId);
    
    @Transactional
    @Modifying
    @Query("UPDATE Persona p SET p.isDefault = false WHERE p.user.id = :userId AND p.id != :personaId")
    void clearDefaultPersonaExcept(@Param("userId") UUID userId, @Param("personaId") UUID personaId);
    
    @Query("SELECT p FROM Persona p WHERE p.user.id = :userId ORDER BY " +
           "CASE WHEN p.isActive = true THEN 0 ELSE 1 END, " +
           "CASE WHEN p.isDefault = true THEN 0 ELSE 1 END, " +
           "p.createdAt ASC")
    List<Persona> findByUserIdOrderByPriority(@Param("userId") UUID userId);
    
    // Performance optimized methods to prevent N+1 queries
    
    @Query("SELECT DISTINCT p FROM Persona p " +
           "LEFT JOIN FETCH p.customAttributes " +
           "WHERE p.user.id = :userId " +
           "ORDER BY CASE WHEN p.isActive = true THEN 0 ELSE 1 END, " +
           "CASE WHEN p.isDefault = true THEN 0 ELSE 1 END, " +
           "p.createdAt ASC")
    List<Persona> findByUserIdOrderByPriorityWithAttributes(@Param("userId") UUID userId);
    
    @Query("SELECT DISTINCT p FROM Persona p " +
           "LEFT JOIN FETCH p.customAttributes " +
           "WHERE p.user.id = :userId AND p.isActive = true")
    Optional<Persona> findByUserIdAndIsActiveTrueWithAttributes(@Param("userId") UUID userId);
}