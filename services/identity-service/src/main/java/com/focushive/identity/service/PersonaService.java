package com.focushive.identity.service;

import com.focushive.identity.config.CacheConfig;
import com.focushive.identity.dto.PersonaDto;
import com.focushive.identity.entity.Persona;
import com.focushive.identity.entity.User;
import com.focushive.identity.exception.ResourceNotFoundException;
import com.focushive.identity.repository.PersonaRepository;
import com.focushive.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing personas - multiple profiles/contexts per user.
 * Implements core identity management features including CRUD operations,
 * context switching, persona templates, and data isolation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PersonaService {

    private final PersonaRepository personaRepository;
    private final UserRepository userRepository;

    /**
     * Create a new persona for a user.
     */
    public PersonaDto createPersona(UUID userId, PersonaDto personaDto) {
        // Removed debug log to avoid logging user IDs frequently
        
        User user = findUserById(userId);
        
        // Check for duplicate persona name
        if (personaRepository.findByUserIdAndName(userId, personaDto.getName()).isPresent()) {
            throw new IllegalArgumentException("Persona with name '" + personaDto.getName() + "' already exists for this user");
        }
        
        // Create persona entity
        Persona persona = Persona.builder()
                .user(user)
                .name(personaDto.getName())
                .type(personaDto.getType())
                .displayName(personaDto.getDisplayName())
                .bio(personaDto.getBio())
                .statusMessage(personaDto.getStatusMessage())
                .avatarUrl(personaDto.getAvatarUrl())
                .isDefault(false) // New personas are not default by default
                .isActive(false)  // New personas are not active by default
                .privacySettings(buildPrivacySettings(personaDto))
                .customAttributes(personaDto.getCustomAttributes() != null ? 
                                personaDto.getCustomAttributes() : new HashMap<>())
                .themePreference(personaDto.getThemePreference())
                .languagePreference(personaDto.getLanguagePreference())
                .build();
        
        // If this is the user's first persona, make it default and active
        long personaCount = personaRepository.countByUserId(userId);
        if (personaCount == 0) {
            persona.setDefault(true);
            persona.setActive(true);
        }
        
        Persona saved = personaRepository.save(persona);
        log.info("Created persona {} for user {}", saved.getName(), userId);
        
        return convertToDto(saved);
    }

    /**
     * Get all personas for a user, ordered by priority (active, default, creation time).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.PERSONAS_CACHE, key = "#userId", unless = "#result.isEmpty()")
    public List<PersonaDto> getUserPersonas(UUID userId) {
        // Removed debug log to avoid logging user IDs frequently
        
        findUserById(userId); // Validate user exists
        
        List<Persona> personas = personaRepository.findByUserIdOrderByPriority(userId);
        return personas.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific persona by ID for a user.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.PERSONAS_CACHE, key = "'persona:' + #personaId + ':' + #userId", unless = "#result == null")
    public PersonaDto getPersona(UUID userId, UUID personaId) {
        // Removed debug log to avoid logging user and persona IDs frequently
        
        User user = findUserById(userId);
        Persona persona = personaRepository.findByIdAndUser(personaId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Persona not found with id: " + personaId));
        
        return convertToDto(persona);
    }

    /**
     * Update a persona.
     */
    @CacheEvict(value = CacheConfig.PERSONAS_CACHE, allEntries = true)
    public PersonaDto updatePersona(UUID userId, UUID personaId, PersonaDto personaDto) {
        // Removed debug log to avoid logging user and persona IDs frequently
        
        User user = findUserById(userId);
        Persona persona = personaRepository.findByIdAndUser(personaId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Persona not found with id: " + personaId));
        
        // Update persona fields
        if (personaDto.getDisplayName() != null) {
            persona.setDisplayName(personaDto.getDisplayName());
        }
        if (personaDto.getBio() != null) {
            persona.setBio(personaDto.getBio());
        }
        if (personaDto.getStatusMessage() != null) {
            persona.setStatusMessage(personaDto.getStatusMessage());
        }
        if (personaDto.getAvatarUrl() != null) {
            persona.setAvatarUrl(personaDto.getAvatarUrl());
        }
        if (personaDto.getThemePreference() != null) {
            persona.setThemePreference(personaDto.getThemePreference());
        }
        if (personaDto.getLanguagePreference() != null) {
            persona.setLanguagePreference(personaDto.getLanguagePreference());
        }
        if (personaDto.getCustomAttributes() != null) {
            persona.setCustomAttributes(personaDto.getCustomAttributes());
        }
        
        // Update privacy settings if provided
        if (personaDto.getPrivacySettings() != null) {
            updatePrivacySettings(persona, personaDto);
        }
        
        Persona updated = personaRepository.save(persona);
        log.info("Updated persona {} for user {}", personaId, userId);
        
        return convertToDto(updated);
    }

    /**
     * Delete a persona.
     */
    public void deletePersona(UUID userId, UUID personaId) {
        // Removed debug log to avoid logging user and persona IDs frequently
        
        User user = findUserById(userId);
        Persona persona = personaRepository.findByIdAndUser(personaId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Persona not found with id: " + personaId));
        
        // Cannot delete default persona
        if (persona.isDefault()) {
            throw new IllegalArgumentException("Cannot delete default persona. Set another persona as default first.");
        }
        
        // If deleting active persona, switch to default
        if (persona.isActive()) {
            Optional<Persona> defaultPersona = personaRepository.findByUserAndIsDefaultTrue(user);
            if (defaultPersona.isPresent()) {
                switchPersona(userId, defaultPersona.get().getId());
            }
        }
        
        personaRepository.delete(persona);
        log.info("Deleted persona {} for user {}", personaId, userId);
    }

    /**
     * Switch to a different persona (context switching).
     */
    public PersonaDto switchPersona(UUID userId, UUID targetPersonaId) {
        // Removed debug log to avoid logging user and persona IDs frequently
        
        User user = findUserById(userId);
        Persona targetPersona = personaRepository.findByIdAndUser(targetPersonaId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Persona not found with id: " + targetPersonaId));
        
        // Update active status in database (atomic operation)
        personaRepository.updateActivePersona(userId, targetPersonaId);
        
        // Update the entity
        targetPersona.setActive(true);
        targetPersona.setLastActiveAt(Instant.now());
        
        Persona updated = personaRepository.save(targetPersona);
        log.info("Switched to persona {} for user {}", targetPersonaId, userId);
        
        return convertToDto(updated);
    }

    /**
     * Get the currently active persona for a user.
     */
    @Transactional(readOnly = true)
    public Optional<PersonaDto> getActivePersona(UUID userId) {
        // Removed debug log to avoid logging user IDs frequently
        
        findUserById(userId); // Validate user exists
        
        return personaRepository.findByUserIdAndIsActiveTrue(userId)
                .map(this::convertToDto);
    }

    /**
     * Set a persona as the default persona.
     */
    public PersonaDto setDefaultPersona(UUID userId, UUID personaId) {
        // Removed debug log to avoid logging user and persona IDs frequently
        
        User user = findUserById(userId);
        Persona persona = personaRepository.findByIdAndUser(personaId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Persona not found with id: " + personaId));
        
        // Clear existing default
        personaRepository.clearDefaultPersonaExcept(userId, personaId);
        
        // Set new default
        persona.setDefault(true);
        Persona updated = personaRepository.save(persona);
        
        log.info("Set persona {} as default for user {}", personaId, userId);
        return convertToDto(updated);
    }

    /**
     * Create a persona from a predefined template.
     */
    public PersonaDto createPersonaFromTemplate(UUID userId, Persona.PersonaType type) {
        // Removed debug log to avoid logging user IDs frequently
        
        PersonaDto template = createPersonaTemplate(type);
        return createPersona(userId, template);
    }

    /**
     * Get available persona templates.
     */
    @Transactional(readOnly = true)
    public List<PersonaDto> getPersonaTemplates() {
        return List.of(
                createPersonaTemplate(Persona.PersonaType.WORK),
                createPersonaTemplate(Persona.PersonaType.PERSONAL),
                createPersonaTemplate(Persona.PersonaType.GAMING),
                createPersonaTemplate(Persona.PersonaType.STUDY)
        );
    }

    // Private helper methods

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

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
                .privacySettings(convertPrivacySettingsToDto(persona.getPrivacySettings()))
                .customAttributes(persona.getCustomAttributes())
                .themePreference(persona.getThemePreference())
                .languagePreference(persona.getLanguagePreference())
                .createdAt(persona.getCreatedAt())
                .updatedAt(persona.getUpdatedAt())
                .lastActiveAt(persona.getLastActiveAt())
                .build();
    }

    private PersonaDto.PrivacySettingsDto convertPrivacySettingsToDto(Persona.PrivacySettings settings) {
        if (settings == null) {
            return PersonaDto.PrivacySettingsDto.builder().build();
        }
        
        return PersonaDto.PrivacySettingsDto.builder()
                .showRealName(settings.isShowRealName())
                .showEmail(settings.isShowEmail())
                .showActivity(settings.isShowActivity())
                .allowDirectMessages(settings.isAllowDirectMessages())
                .visibilityLevel(settings.getVisibilityLevel())
                .searchable(settings.isSearchable())
                .showOnlineStatus(settings.isShowOnlineStatus())
                .shareFocusSessions(settings.isShareFocusSessions())
                .shareAchievements(settings.isShareAchievements())
                .build();
    }

    private Persona.PrivacySettings buildPrivacySettings(PersonaDto personaDto) {
        PersonaDto.PrivacySettingsDto dto = personaDto.getPrivacySettings();
        if (dto == null) {
            return new Persona.PrivacySettings();
        }
        
        return Persona.PrivacySettings.builder()
                .showRealName(dto.isShowRealName())
                .showEmail(dto.isShowEmail())
                .showActivity(dto.isShowActivity())
                .allowDirectMessages(dto.isAllowDirectMessages())
                .visibilityLevel(dto.getVisibilityLevel())
                .searchable(dto.isSearchable())
                .showOnlineStatus(dto.isShowOnlineStatus())
                .shareFocusSessions(dto.isShareFocusSessions())
                .shareAchievements(dto.isShareAchievements())
                .build();
    }

    private void updatePrivacySettings(Persona persona, PersonaDto personaDto) {
        PersonaDto.PrivacySettingsDto dto = personaDto.getPrivacySettings();
        Persona.PrivacySettings settings = persona.getPrivacySettings();
        if (settings == null) {
            settings = new Persona.PrivacySettings();
            persona.setPrivacySettings(settings);
        }
        
        settings.setShowRealName(dto.isShowRealName());
        settings.setShowEmail(dto.isShowEmail());
        settings.setShowActivity(dto.isShowActivity());
        settings.setAllowDirectMessages(dto.isAllowDirectMessages());
        settings.setVisibilityLevel(dto.getVisibilityLevel());
        settings.setSearchable(dto.isSearchable());
        settings.setShowOnlineStatus(dto.isShowOnlineStatus());
        settings.setShareFocusSessions(dto.isShareFocusSessions());
        settings.setShareAchievements(dto.isShareAchievements());
    }

    private PersonaDto createPersonaTemplate(Persona.PersonaType type) {
        PersonaDto.PersonaDtoBuilder builder = PersonaDto.builder()
                .type(type)
                .privacySettings(PersonaDto.PrivacySettingsDto.builder()
                        .showActivity(true)
                        .allowDirectMessages(true)
                        .visibilityLevel("FRIENDS")
                        .searchable(true)
                        .showOnlineStatus(true)
                        .build());

        switch (type) {
            case WORK:
                return builder
                        .name("Work")
                        .displayName("Work Profile")
                        .bio("Professional work profile for productivity and collaboration")
                        .themePreference("light")
                        .privacySettings(PersonaDto.PrivacySettingsDto.builder()
                                .showRealName(true)
                                .showActivity(true)
                                .allowDirectMessages(true)
                                .visibilityLevel("PUBLIC")
                                .searchable(true)
                                .showOnlineStatus(true)
                                .shareFocusSessions(true)
                                .shareAchievements(true)
                                .build())
                        .build();

            case PERSONAL:
                return builder
                        .name("Personal")
                        .displayName("Personal Profile")
                        .bio("Personal profile for leisure and personal activities")
                        .themePreference("system")
                        .privacySettings(PersonaDto.PrivacySettingsDto.builder()
                                .showRealName(false)
                                .showActivity(false)
                                .allowDirectMessages(false)
                                .visibilityLevel("PRIVATE")
                                .searchable(false)
                                .showOnlineStatus(false)
                                .shareFocusSessions(false)
                                .shareAchievements(false)
                                .build())
                        .build();

            case GAMING:
                return builder
                        .name("Gaming")
                        .displayName("Gaming Profile")
                        .bio("Gaming profile for entertainment and gaming activities")
                        .themePreference("dark")
                        .privacySettings(PersonaDto.PrivacySettingsDto.builder()
                                .showRealName(false)
                                .showActivity(true)
                                .allowDirectMessages(true)
                                .visibilityLevel("FRIENDS")
                                .searchable(true)
                                .showOnlineStatus(true)
                                .shareFocusSessions(false)
                                .shareAchievements(true)
                                .build())
                        .build();

            case STUDY:
                return builder
                        .name("Study")
                        .displayName("Study Profile")
                        .bio("Study profile for academic work and learning")
                        .themePreference("light")
                        .privacySettings(PersonaDto.PrivacySettingsDto.builder()
                                .showRealName(true)
                                .showActivity(true)
                                .allowDirectMessages(true)
                                .visibilityLevel("FRIENDS")
                                .searchable(true)
                                .showOnlineStatus(true)
                                .shareFocusSessions(true)
                                .shareAchievements(true)
                                .build())
                        .build();

            case CUSTOM:
            default:
                return builder
                        .name("Custom")
                        .displayName("Custom Profile")
                        .bio("Custom persona profile")
                        .themePreference("system")
                        .build();
        }
    }
}