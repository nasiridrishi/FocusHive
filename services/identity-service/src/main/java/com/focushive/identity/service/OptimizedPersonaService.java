package com.focushive.identity.service;

import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Optimized version of PersonaService demonstrating N+1 query fixes.
 * This service uses optimized repository methods with @EntityGraph and JOIN FETCH
 * to prevent N+1 queries when loading user personas.
 * 
 * Performance Improvements:
 * - Uses @EntityGraph to eagerly load personas with users
 * - Uses JOIN FETCH to load custom attributes in single query
 * - Implements batch fetching for bulk operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizedPersonaService {

    private final PersonaRepository personaRepository;
    private final UserRepository userRepository;

    /**
     * Get all personas for a user using optimized query to prevent N+1 queries.
     * This method demonstrates the performance improvement from JOIN FETCH.
     */
    @Transactional(readOnly = true)
    public List<PersonaDto> getUserPersonasOptimized(UUID userId) {
        log.debug("Getting personas for user ID: {} (optimized)", userId);
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        
        // Use optimized query with JOIN FETCH to prevent N+1 queries
        List<Persona> personas = personaRepository.findByUserIdOrderByPriorityWithAttributes(userId);
        return personas.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get active persona using optimized query.
     */
    @Transactional(readOnly = true)
    public Optional<PersonaDto> getActivePersonaOptimized(UUID userId) {
        log.debug("Getting active persona for user ID: {} (optimized)", userId);
        
        // Use optimized query with JOIN FETCH
        return personaRepository.findByUserIdAndIsActiveTrueWithAttributes(userId)
                .map(this::convertToDto);
    }

    /**
     * Bulk load users with their personas using EntityGraph to prevent N+1 queries.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsersWithPersonas() {
        log.debug("Loading all users with personas (optimized)");
        
        // Use EntityGraph to eagerly load personas via findAll override
        return userRepository.findAll();
    }

    /**
     * Load specific users with personas using batch fetching.
     */
    @Transactional(readOnly = true)
    public List<User> getUsersWithPersonasBatch(List<UUID> userIds) {
        log.debug("Loading {} users with personas in batch (optimized)", userIds.size());
        
        // Use JOIN FETCH for batch loading
        return userRepository.findUsersWithPersonasAndAttributes(userIds);
    }

    /**
     * Load user by ID with personas using EntityGraph.
     */
    @Transactional(readOnly = true)  
    public Optional<User> findUserWithPersonas(UUID userId) {
        log.debug("Finding user {} with personas (optimized)", userId);
        
        // Use EntityGraph via findById override
        return userRepository.findById(userId);
    }

    /**
     * Load user with personas and all attributes using batch method.
     */
    @Transactional(readOnly = true)
    public Optional<User> findUserWithPersonasAndAttributes(UUID userId) {
        log.debug("Finding user {} with personas and attributes (optimized)", userId);
        
        // Use JOIN FETCH method for single user
        List<User> users = userRepository.findUsersWithPersonasAndAttributes(List.of(userId));
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    // Helper method for DTO conversion
    private PersonaDto convertToDto(Persona persona) {
        return PersonaDto.builder()
                .id(persona.getId())
                .name(persona.getName())
                .type(persona.getType())
                .displayName(persona.getDisplayName())
                .bio(persona.getBio())
                .statusMessage(persona.getStatusMessage())
                .avatarUrl(persona.getAvatarUrl())
                .isDefault(persona.isDefault())
                .isActive(persona.isActive())
                .customAttributes(persona.getCustomAttributes())
                .themePreference(persona.getThemePreference())
                .languagePreference(persona.getLanguagePreference())
                .createdAt(persona.getCreatedAt())
                .updatedAt(persona.getUpdatedAt())
                .lastActiveAt(persona.getLastActiveAt())
                .build();
    }
}