package com.focushive.music.service;

import com.focushive.music.config.RedisCacheConfig;
import com.focushive.music.dto.*;
import com.focushive.music.model.MusicSession;
import com.focushive.music.model.RecommendationFeedback;
import com.focushive.music.model.RecommendationHistory;
import com.focushive.music.client.UserServiceClient;
import com.focushive.music.client.HiveServiceClient;
import com.focushive.music.client.SessionServiceClient;
import com.focushive.music.client.AnalyticsServiceClient;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.repository.MusicSessionRepository;
import com.focushive.music.repository.RecommendationFeedbackRepository;
import com.focushive.music.repository.RecommendationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for generating music recommendations based on user preferences, 
 * session context, and hive dynamics.
 * 
 * Integrates with multiple data sources including user preferences, 
 * analytics data, and external streaming services to provide personalized
 * music recommendations for focus sessions.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MusicRecommendationService {

    private final UserServiceClient userServiceClient;
    private final HiveServiceClient hiveServiceClient;
    private final SessionServiceClient sessionServiceClient;
    private final AnalyticsServiceClient analyticsServiceClient;
    private final SpotifyIntegrationService spotifyIntegrationService;
    private final MusicSessionRepository musicSessionRepository;
    private final RecommendationHistoryRepository recommendationHistoryRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Algorithm version for tracking
    private static final String ALGORITHM_VERSION = "v2.1.0";
    
    // Recommendation scoring weights
    private static final double PRODUCTIVITY_WEIGHT = 0.4;
    private static final double USER_PREFERENCE_WEIGHT = 0.3;
    private static final double TASK_MOOD_ALIGNMENT_WEIGHT = 0.2;
    private static final double DIVERSITY_WEIGHT = 0.1;

    /**
     * Generates music recommendations for a user session.
     * 
     * @param userId The user ID
     * @param sessionId The session ID
     * @param sessionType The type of session (focus, break, collaborative)
     * @param preferences User's session preferences
     * @return List of music recommendations
     */
    @Transactional
    public List<RecommendationDTO> generateSessionRecommendations(
            UUID userId, UUID sessionId, String sessionType, Map<String, Object> preferences) {
        
        log.info("Generating music recommendations for userId: {}, sessionId: {}, sessionType: {}", 
            userId, sessionId, sessionType);
        
        try {
            // Get user music preferences
            var userPreferences = getUserMusicPreferences(userId);
            
            // Get analytics insights for better recommendations
            var userInsights = getMusicRecommendationInsights(userId);
            
            // Create or update music session
            MusicSession musicSession = createOrUpdateMusicSession(userId, sessionId, sessionType, preferences);
            
            // Generate recommendations based on context
            List<RecommendationDTO> recommendations = switch (sessionType.toLowerCase()) {
                case "focus", "deep_work" -> generateFocusRecommendations(userId, userPreferences, userInsights);
                case "break", "relaxation" -> generateBreakRecommendations(userId, userPreferences, userInsights);
                case "collaborative" -> generateCollaborativeRecommendations(userId, sessionId, userPreferences);
                default -> generateGeneralRecommendations(userId, userPreferences, userInsights);
            };
            
            // Store recommendations in session
            musicSession.setRecommendations(recommendations.stream()
                .map(RecommendationDTO::trackId)
                .toList());
            musicSessionRepository.save(musicSession);
            
            log.info("Generated {} recommendations for sessionId: {}", recommendations.size(), sessionId);
            return recommendations;
            
        } catch (Exception e) {
            log.error("Error generating recommendations for sessionId: {}", sessionId, e);
            throw new MusicServiceException.RecommendationException(
                "Failed to generate music recommendations", e);
        }
    }

    /**
     * Updates session recommendations based on new preferences.
     * 
     * @param sessionId The session ID
     * @param userId The user ID
     * @param newPreferences Updated music preferences
     * @return Updated recommendations
     */
    @Transactional
    public List<RecommendationDTO> updateSessionRecommendations(
            UUID sessionId, UUID userId, Map<String, Object> newPreferences) {
        
        log.info("Updating recommendations for sessionId: {}, userId: {}", sessionId, userId);
        
        try {
            MusicSession musicSession = musicSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new MusicServiceException.ResourceNotFoundException("MusicSession", sessionId.toString()));
            
            // Update session preferences
            musicSession.setPreferences(newPreferences);
            
            // Regenerate recommendations with new preferences
            List<RecommendationDTO> updatedRecommendations = generateSessionRecommendations(
                userId, sessionId, musicSession.getSessionType(), newPreferences);
            
            log.info("Updated {} recommendations for sessionId: {}", updatedRecommendations.size(), sessionId);
            return updatedRecommendations;
            
        } catch (Exception e) {
            log.error("Error updating recommendations for sessionId: {}", sessionId, e);
            throw new MusicServiceException.RecommendationException(
                "Failed to update music recommendations", e);
        }
    }

    /**
     * Finalizes music session data when session ends.
     * 
     * @param sessionId The session ID
     * @param userId The user ID
     * @param duration Session duration
     * @param metrics Session metrics
     */
    @Transactional
    public void finalizeSessionMusic(UUID sessionId, UUID userId, long duration, Map<String, Object> metrics) {
        log.info("Finalizing music session for sessionId: {}, userId: {}", sessionId, userId);
        
        try {
            musicSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .ifPresent(musicSession -> {
                    musicSession.setEndTime(System.currentTimeMillis());
                    musicSession.setDuration(duration);
                    musicSession.setMetrics(metrics);
                    musicSessionRepository.save(musicSession);
                });
                
        } catch (Exception e) {
            log.error("Error finalizing music session for sessionId: {}", sessionId, e);
        }
    }

    /**
     * Gets user's music preferences from user service.
     */
    private UserServiceClient.MusicPreferencesData getUserMusicPreferences(UUID userId) {
        try {
            var response = userServiceClient.getUserPreferences(userId, null);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().musicPreferences();
            }
        } catch (Exception e) {
            log.warn("Failed to get user preferences for userId: {}, using defaults", userId, e);
        }
        
        // Return default preferences if service call fails
        return new UserServiceClient.MusicPreferencesData(
            List.of("ambient", "classical", "instrumental"),
            List.of(),
            "spotify",
            false,
            70,
            Map.of("energy", "low", "tempo", "slow")
        );
    }

    /**
     * Gets music recommendation insights from analytics service.
     */
    private AnalyticsServiceClient.MusicRecommendationInsightsResponse getMusicRecommendationInsights(UUID userId) {
        try {
            var response = analyticsServiceClient.getMusicRecommendationInsights(userId, null);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to get recommendation insights for userId: {}", userId, e);
        }
        
        // Return default insights if service call fails
        return new AnalyticsServiceClient.MusicRecommendationInsightsResponse(
            userId,
            List.of("ambient", "classical"),
            List.of(),
            "neutral",
            "anytime",
            Map.of(),
            List.of("explore_new_genres")
        );
    }

    /**
     * Creates or updates a music session record.
     */
    private MusicSession createOrUpdateMusicSession(UUID userId, UUID sessionId, String sessionType, 
                                                   Map<String, Object> preferences) {
        return musicSessionRepository.findBySessionIdAndUserId(sessionId, userId)
            .map(existing -> {
                existing.setSessionType(sessionType);
                existing.setPreferences(preferences);
                existing.setUpdatedAt(System.currentTimeMillis());
                return existing;
            })
            .orElseGet(() -> {
                MusicSession newSession = new MusicSession();
                newSession.setSessionId(sessionId);
                newSession.setUserId(userId);
                newSession.setSessionType(sessionType);
                newSession.setPreferences(preferences);
                newSession.setStartTime(System.currentTimeMillis());
                newSession.setCreatedAt(System.currentTimeMillis());
                newSession.setUpdatedAt(System.currentTimeMillis());
                return newSession;
            });
    }

    /**
     * Generates focus-specific recommendations.
     */
    private List<RecommendationDTO> generateFocusRecommendations(
            UUID userId, 
            UserServiceClient.MusicPreferencesData userPrefs, 
            AnalyticsServiceClient.MusicRecommendationInsightsResponse insights) {
        
        // Focus sessions prefer low-energy, instrumental music
        List<String> focusGenres = List.of("ambient", "classical", "instrumental", "lo-fi", "nature sounds");
        
        return spotifyIntegrationService.getRecommendationsByGenres(userId, focusGenres, Map.of(
            "energy", "low",
            "tempo", "slow",
            "instrumental", "high",
            "valence", "neutral"
        ));
    }

    /**
     * Generates break-specific recommendations.
     */
    private List<RecommendationDTO> generateBreakRecommendations(
            UUID userId,
            UserServiceClient.MusicPreferencesData userPrefs,
            AnalyticsServiceClient.MusicRecommendationInsightsResponse insights) {
        
        // Break sessions can include more energetic music
        List<String> breakGenres = userPrefs.favoriteGenres().isEmpty() ? 
            List.of("pop", "indie", "electronic", "jazz") : userPrefs.favoriteGenres();
        
        return spotifyIntegrationService.getRecommendationsByGenres(userId, breakGenres, Map.of(
            "energy", "medium",
            "tempo", "medium",
            "valence", "positive"
        ));
    }

    /**
     * Generates collaborative recommendations based on hive preferences.
     */
    private List<RecommendationDTO> generateCollaborativeRecommendations(
            UUID userId, UUID sessionId, UserServiceClient.MusicPreferencesData userPrefs) {
        
        // For collaborative sessions, consider hive music preferences
        // This is a simplified implementation - in reality, we'd aggregate preferences from all hive members
        List<String> collaborativeGenres = List.of("pop", "indie", "alternative", "electronic");
        
        return spotifyIntegrationService.getRecommendationsByGenres(userId, collaborativeGenres, Map.of(
            "energy", "medium",
            "tempo", "medium",
            "popularity", "high" // Popular tracks work better for groups
        ));
    }

    /**
     * Generates general recommendations.
     */
    private List<RecommendationDTO> generateGeneralRecommendations(
            UUID userId,
            UserServiceClient.MusicPreferencesData userPrefs,
            AnalyticsServiceClient.MusicRecommendationInsightsResponse insights) {
        
        List<String> genres = userPrefs.favoriteGenres().isEmpty() ? 
            insights.recommendedGenres() : userPrefs.favoriteGenres();
        
        if (genres.isEmpty()) {
            genres = List.of("pop", "indie", "alternative");
        }
        
        return spotifyIntegrationService.getRecommendationsByGenres(userId, genres, Map.of(
            "energy", "medium",
            "tempo", "medium"
        ));
    }
    
    /**
     * Gets music recommendations based on smart playlist criteria.
     * 
     * @param criteria The smart playlist criteria
     * @param userId The user ID
     * @return List of Spotify track IDs
     */
    public List<String> getRecommendationsBySmartCriteria(
            com.focushive.music.dto.PlaylistDTO.SmartPlaylistCriteriaRequest criteria, 
            String userId) {
        
        log.debug("Getting recommendations for smart playlist criteria: {}", criteria.getName());
        
        try {
            // Build recommendation parameters from criteria
            Map<String, Object> params = new java.util.HashMap<>();
            
            if (criteria.getMinEnergy() != null) {
                params.put("min_energy", criteria.getMinEnergy());
            }
            if (criteria.getMaxEnergy() != null) {
                params.put("max_energy", criteria.getMaxEnergy());
            }
            if (criteria.getMinDanceability() != null) {
                params.put("min_danceability", criteria.getMinDanceability());
            }
            if (criteria.getMaxDanceability() != null) {
                params.put("max_danceability", criteria.getMaxDanceability());
            }
            if (criteria.getMinValence() != null) {
                params.put("min_valence", criteria.getMinValence());
            }
            if (criteria.getMaxValence() != null) {
                params.put("max_valence", criteria.getMaxValence());
            }
            
            // Use genres if specified
            List<String> genres = criteria.getGenres() != null && !criteria.getGenres().isEmpty() ?
                                 criteria.getGenres() : List.of("pop", "indie", "alternative");
            
            // Get recommendations from Spotify
            UUID userUuid = UUID.fromString(userId);
            List<RecommendationDTO> recommendations = spotifyIntegrationService
                .getRecommendationsByGenres(userUuid, genres, params);
            
            // Extract track IDs
            List<String> trackIds = recommendations.stream()
                .map(RecommendationDTO::getSpotifyTrackId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
            
            // Limit results if maxTracks is specified
            if (criteria.getMaxTracks() != null && criteria.getMaxTracks() > 0) {
                int limit = Math.min(trackIds.size(), criteria.getMaxTracks());
                trackIds = trackIds.subList(0, limit);
            }
            
            log.debug("Generated {} track recommendations for smart playlist", trackIds.size());
            return trackIds;
            
        } catch (Exception e) {
            log.warn("Failed to get recommendations for smart playlist criteria: {}", e.getMessage());
            return List.of(); // Return empty list on failure
        }
    }
}