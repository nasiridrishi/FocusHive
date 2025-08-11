package com.focushive.music.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.Playlist;
import com.focushive.music.model.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for smart playlist generation and management.
 * 
 * This service follows the Single Responsibility Principle by handling
 * only AI/smart playlist-related operations:
 * - Creating smart playlists with criteria-based filtering
 * - Refreshing smart playlists based on criteria
 * - Managing smart playlist criteria
 * - AI-based track recommendation integration
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SmartPlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistCrudService playlistCrudService;
    private final ObjectMapper objectMapper;
    private final MusicRecommendationService musicRecommendationService;

    // Cache Keys
    private static final String CACHE_PLAYLISTS = "playlists";

    // ===============================
    // SMART PLAYLIST OPERATIONS
    // ===============================

    /**
     * Creates a smart playlist with criteria-based filtering.
     * 
     * @param criteriaRequest The smart playlist criteria
     * @param userId The requesting user ID
     * @return Created smart playlist response
     */
    public PlaylistDTO.Response createSmartPlaylist(PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest, String userId) {
        log.info("Creating smart playlist '{}' for user {}", criteriaRequest.getName(), userId);
        
        validateSmartPlaylistCriteria(criteriaRequest);
        
        Playlist playlist = buildSmartPlaylistFromCriteria(criteriaRequest, userId);
        playlist.setIsSmartPlaylist(true);
        playlist.setSmartCriteria(convertCriteriaToJson(criteriaRequest));
        
        // Generate initial tracks based on criteria
        List<PlaylistTrack> initialTracks = generateTracksForSmartPlaylist(criteriaRequest, userId);
        playlist.getTracks().addAll(initialTracks);
        
        // Update playlist metadata based on generated tracks
        updatePlaylistMetadataFromTracks(playlist);
        
        playlist = playlistRepository.save(playlist);
        
        log.info("Created smart playlist with ID: {} containing {} tracks", 
                playlist.getId(), playlist.getTotalTracks());
        
        return playlistCrudService.convertToResponse(playlist, userId);
    }

    /**
     * Refreshes a smart playlist based on its criteria.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     * @return Updated playlist response
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.Response refreshSmartPlaylist(Long playlistId, String userId) {
        log.info("Refreshing smart playlist {} for user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        if (!playlist.isSmartPlaylist()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Cannot refresh non-smart playlist");
        }
        
        // Parse criteria and regenerate tracks
        PlaylistDTO.SmartPlaylistCriteriaRequest criteria = parseCriteriaFromJson(playlist.getSmartCriteria());
        List<PlaylistTrack> newTracks = generateTracksForSmartPlaylist(criteria, userId);
        
        // Clear existing tracks and add new ones
        playlist.getTracks().clear();
        playlist.getTracks().addAll(newTracks);
        updatePlaylistMetadataFromTracks(playlist);
        
        playlist.setLastAutoUpdate(LocalDateTime.now());
        playlist.setUpdatedAt(LocalDateTime.now());
        
        playlist = playlistRepository.save(playlist);
        
        log.info("Refreshed smart playlist {} with {} tracks", playlistId, newTracks.size());
        return playlistCrudService.convertToResponse(playlist, userId);
    }

    /**
     * Updates the criteria for a smart playlist.
     * 
     * @param playlistId The smart playlist ID
     * @param criteriaRequest The new criteria
     * @param userId The requesting user ID
     * @param autoRefresh Whether to automatically refresh tracks after updating criteria
     * @return Updated playlist response
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.Response updateSmartPlaylistCriteria(Long playlistId, 
                                                           PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest, 
                                                           String userId, 
                                                           boolean autoRefresh) {
        log.info("Updating criteria for smart playlist {} by user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanModifyPlaylist(playlist, userId);
        
        if (!playlist.isSmartPlaylist()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Cannot update criteria for non-smart playlist");
        }
        
        validateSmartPlaylistCriteria(criteriaRequest);
        
        // Update criteria
        playlist.setSmartCriteria(convertCriteriaToJson(criteriaRequest));
        playlist.setUpdatedAt(LocalDateTime.now());
        
        // Update basic playlist info if provided
        if (criteriaRequest.getName() != null) {
            playlist.setName(criteriaRequest.getName());
        }
        if (criteriaRequest.getDescription() != null) {
            playlist.setDescription(criteriaRequest.getDescription());
        }
        
        // Auto-refresh tracks if requested
        if (autoRefresh) {
            List<PlaylistTrack> newTracks = generateTracksForSmartPlaylist(criteriaRequest, userId);
            playlist.getTracks().clear();
            playlist.getTracks().addAll(newTracks);
            updatePlaylistMetadataFromTracks(playlist);
            playlist.setLastAutoUpdate(LocalDateTime.now());
        }
        
        playlist = playlistRepository.save(playlist);
        
        log.info("Updated criteria for smart playlist {}", playlistId);
        return playlistCrudService.convertToResponse(playlist, userId);
    }

    /**
     * Gets the criteria for a smart playlist.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     * @return Smart playlist criteria
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SmartPlaylistCriteriaRequest getSmartPlaylistCriteria(Long playlistId, String userId) {
        log.debug("Getting criteria for smart playlist {} by user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanViewPlaylist(playlist, userId);
        
        if (!playlist.isSmartPlaylist()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Playlist is not a smart playlist");
        }
        
        return parseCriteriaFromJson(playlist.getSmartCriteria());
    }

    /**
     * Converts a regular playlist to a smart playlist.
     * 
     * @param playlistId The playlist ID
     * @param criteriaRequest The smart criteria to apply
     * @param userId The requesting user ID
     * @param preserveExistingTracks Whether to keep existing tracks
     * @return Updated playlist response
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.Response convertToSmartPlaylist(Long playlistId, 
                                                      PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest, 
                                                      String userId, 
                                                      boolean preserveExistingTracks) {
        log.info("Converting playlist {} to smart playlist by user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserIsOwner(playlist, userId);
        
        if (playlist.isSmartPlaylist()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Playlist is already a smart playlist");
        }
        
        validateSmartPlaylistCriteria(criteriaRequest);
        
        // Convert to smart playlist
        playlist.setIsSmartPlaylist(true);
        playlist.setSmartCriteria(convertCriteriaToJson(criteriaRequest));
        playlist.setUpdatedAt(LocalDateTime.now());
        
        // Generate new tracks based on criteria
        if (!preserveExistingTracks) {
            playlist.getTracks().clear();
        }
        
        List<PlaylistTrack> newTracks = generateTracksForSmartPlaylist(criteriaRequest, userId);
        playlist.getTracks().addAll(newTracks);
        updatePlaylistMetadataFromTracks(playlist);
        
        playlist.setLastAutoUpdate(LocalDateTime.now());
        playlist = playlistRepository.save(playlist);
        
        log.info("Converted playlist {} to smart playlist with {} total tracks", 
                playlistId, playlist.getTotalTracks());
        return playlistCrudService.convertToResponse(playlist, userId);
    }

    /**
     * Converts a smart playlist back to a regular playlist.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     * @return Updated playlist response
     */
    @CacheEvict(value = CACHE_PLAYLISTS, key = "#playlistId")
    public PlaylistDTO.Response convertToRegularPlaylist(Long playlistId, String userId) {
        log.info("Converting smart playlist {} to regular playlist by user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserIsOwner(playlist, userId);
        
        if (!playlist.isSmartPlaylist()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Playlist is not a smart playlist");
        }
        
        // Convert to regular playlist
        playlist.setIsSmartPlaylist(false);
        playlist.setSmartCriteria(null);
        playlist.setLastAutoUpdate(null);
        playlist.setUpdatedAt(LocalDateTime.now());
        
        playlist = playlistRepository.save(playlist);
        
        log.info("Converted smart playlist {} to regular playlist", playlistId);
        return playlistCrudService.convertToResponse(playlist, userId);
    }

    /**
     * Gets smart playlist statistics.
     * 
     * @param playlistId The smart playlist ID
     * @param userId The requesting user ID
     * @return Smart playlist statistics
     */
    @Transactional(readOnly = true)
    public PlaylistDTO.SmartPlaylistStats getSmartPlaylistStats(Long playlistId, String userId) {
        log.debug("Getting statistics for smart playlist {} by user {}", playlistId, userId);
        
        Playlist playlist = playlistCrudService.findPlaylistById(playlistId);
        playlistCrudService.validateUserCanViewPlaylist(playlist, userId);
        
        if (!playlist.isSmartPlaylist()) {
            throw new MusicServiceException.BusinessRuleViolationException(
                "Playlist is not a smart playlist");
        }
        
        PlaylistDTO.SmartPlaylistCriteriaRequest criteria = parseCriteriaFromJson(playlist.getSmartCriteria());
        
        return PlaylistDTO.SmartPlaylistStats.builder()
            .playlistId(playlistId)
            .totalTracks(playlist.getTotalTracks())
            .lastRefresh(playlist.getLastAutoUpdate())
            .criteriaSet(criteria != null)
            .genreCount(calculateGenreCount(playlist))
            .averageTrackLength(calculateAverageTrackLength(playlist))
            .dateRange(calculateDateRange(playlist))
            .energyLevel(calculateEnergyLevel(criteria))
            .build();
    }

    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    private void validateSmartPlaylistCriteria(PlaylistDTO.SmartPlaylistCriteriaRequest criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Smart playlist criteria is required");
        }
        
        if (criteria.getName() == null || criteria.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Smart playlist name is required");
        }
        
        // Validate at least one criteria is provided
        boolean hasCriteria = (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) ||
                             (criteria.getArtists() != null && !criteria.getArtists().isEmpty()) ||
                             criteria.getMinEnergy() != null ||
                             criteria.getMaxEnergy() != null ||
                             criteria.getMinDanceability() != null ||
                             criteria.getMaxDanceability() != null ||
                             criteria.getMinValence() != null ||
                             criteria.getMaxValence() != null ||
                             criteria.getReleaseDateStart() != null ||
                             criteria.getReleaseDateEnd() != null;
        
        if (!hasCriteria) {
            throw new IllegalArgumentException("At least one criteria must be specified for smart playlist");
        }
        
        // Validate range values
        validateRange(criteria.getMinEnergy(), criteria.getMaxEnergy(), "Energy");
        validateRange(criteria.getMinDanceability(), criteria.getMaxDanceability(), "Danceability");
        validateRange(criteria.getMinValence(), criteria.getMaxValence(), "Valence");
        
        // Validate max tracks limit
        if (criteria.getMaxTracks() != null && criteria.getMaxTracks() <= 0) {
            throw new IllegalArgumentException("Max tracks must be greater than 0");
        }
    }

    private void validateRange(Double min, Double max, String attributeName) {
        if (min != null && (min < 0.0 || min > 1.0)) {
            throw new IllegalArgumentException(attributeName + " min value must be between 0.0 and 1.0");
        }
        if (max != null && (max < 0.0 || max > 1.0)) {
            throw new IllegalArgumentException(attributeName + " max value must be between 0.0 and 1.0");
        }
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(attributeName + " min value cannot be greater than max value");
        }
    }

    private Playlist buildSmartPlaylistFromCriteria(PlaylistDTO.SmartPlaylistCriteriaRequest criteria, String userId) {
        return Playlist.builder()
            .name(criteria.getName())
            .description(criteria.getDescription() != null ? criteria.getDescription() : 
                        "Smart playlist created with AI-based criteria")
            .createdBy(userId)
            .hiveId(criteria.getHiveId())
            .isCollaborative(Boolean.TRUE.equals(criteria.getIsCollaborative()))
            .isPublic(Boolean.TRUE.equals(criteria.getIsPublic()))
            .imageUrl(criteria.getImageUrl())
            .totalTracks(0)
            .totalDurationMs(0L)
            .isSmartPlaylist(true)
            .isActive(true)
            .playCount(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .lastAutoUpdate(LocalDateTime.now())
            .tracks(new ArrayList<>())
            .collaborators(new ArrayList<>())
            .build();
    }

    private List<PlaylistTrack> generateTracksForSmartPlaylist(PlaylistDTO.SmartPlaylistCriteriaRequest criteria, 
                                                              String userId) {
        try {
            // Use the music recommendation service to get tracks based on criteria
            List<String> recommendedTrackIds = musicRecommendationService
                .getRecommendationsBySmartCriteria(criteria, userId);
            
            // Convert track IDs to PlaylistTrack objects
            List<PlaylistTrack> tracks = new ArrayList<>();
            int position = 0;
            
            int maxTracks = criteria.getMaxTracks() != null ? criteria.getMaxTracks() : 50;
            int trackCount = Math.min(recommendedTrackIds.size(), maxTracks);
            
            for (int i = 0; i < trackCount; i++) {
                String spotifyTrackId = recommendedTrackIds.get(i);
                
                PlaylistTrack track = new PlaylistTrack();
                track.setSpotifyTrackId(spotifyTrackId);
                track.setPositionInPlaylist(position++);
                track.setAddedBy(userId);
                track.setAddedAt(LocalDateTime.now());
                
                // Set metadata (in real implementation, would fetch from Spotify)
                track.setTrackName("Smart Track " + spotifyTrackId);
                track.setArtistName("Smart Artist");
                track.setDurationMs(180000); // 3 minutes default
                
                tracks.add(track);
            }
            
            log.debug("Generated {} tracks for smart playlist based on criteria", tracks.size());
            return tracks;
            
        } catch (Exception e) {
            log.warn("Failed to generate tracks for smart playlist: {}", e.getMessage());
            return new ArrayList<>(); // Return empty list on failure
        }
    }

    private String convertCriteriaToJson(PlaylistDTO.SmartPlaylistCriteriaRequest criteria) {
        try {
            return objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            log.error("Error converting criteria to JSON", e);
            return "{}";
        }
    }

    private PlaylistDTO.SmartPlaylistCriteriaRequest parseCriteriaFromJson(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                return PlaylistDTO.SmartPlaylistCriteriaRequest.builder().build();
            }
            return objectMapper.readValue(json, PlaylistDTO.SmartPlaylistCriteriaRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing criteria from JSON: {}", json, e);
            return PlaylistDTO.SmartPlaylistCriteriaRequest.builder().build();
        }
    }

    private void updatePlaylistMetadataFromTracks(Playlist playlist) {
        List<PlaylistTrack> tracks = playlist.getTracks();
        
        playlist.setTotalTracks(tracks.size());
        playlist.setTotalDurationMs(tracks.stream()
            .mapToLong(track -> track.getDurationMs() != null ? track.getDurationMs() : 0L)
            .sum());
    }

    private int calculateGenreCount(Playlist playlist) {
        // In real implementation, would analyze track genres
        return (int) Math.ceil(playlist.getTotalTracks() / 10.0); // Placeholder
    }

    private long calculateAverageTrackLength(Playlist playlist) {
        if (playlist.getTotalTracks() == 0) return 0;
        return playlist.getTotalDurationMs() / playlist.getTotalTracks();
    }

    private String calculateDateRange(Playlist playlist) {
        // In real implementation, would analyze track release dates
        return "2020-2024"; // Placeholder
    }

    private String calculateEnergyLevel(PlaylistDTO.SmartPlaylistCriteriaRequest criteria) {
        if (criteria == null) return "Medium";
        
        if (criteria.getMinEnergy() != null && criteria.getMaxEnergy() != null) {
            double avg = (criteria.getMinEnergy() + criteria.getMaxEnergy()) / 2.0;
            if (avg < 0.3) return "Low";
            if (avg > 0.7) return "High";
            return "Medium";
        }
        
        if (criteria.getMinEnergy() != null) {
            return criteria.getMinEnergy() > 0.7 ? "High" : "Medium";
        }
        
        if (criteria.getMaxEnergy() != null) {
            return criteria.getMaxEnergy() < 0.3 ? "Low" : "Medium";
        }
        
        return "Medium";
    }
}