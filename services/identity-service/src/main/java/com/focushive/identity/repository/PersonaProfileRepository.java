package com.focushive.identity.repository;

import com.focushive.identity.entity.PersonaProfile;
import com.focushive.identity.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PersonaProfile entity.
 */
@Repository
public interface PersonaProfileRepository extends JpaRepository<PersonaProfile, Long> {

    /**
     * Find profile by persona.
     */
    Optional<PersonaProfile> findByPersona(Persona persona);

    /**
     * Check if a profile exists for a persona.
     */
    boolean existsByPersona(Persona persona);
}