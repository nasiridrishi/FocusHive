package com.focushive.music.service;

import com.focushive.music.dto.RecommendationDTO;
import com.focushive.music.entity.MusicPreference;
import com.focushive.music.repository.MusicPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for generating music recommendations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final SpotifyService spotifyService;
    private final MusicPreferenceRepository musicPreferenceRepository;

    /**
     * Generate recommendations for a focus session.
     */
    @Cacheable(value = "recommendations", key = "#userId + '_' + #sessionType + '_' + #limit")
    public List<RecommendationDTO> generateRecommendations(String userId, String sessionType, int limit, Map<String, Object> context) {
        log.info("Generating recommendations for user: {}, sessionType: {}, limit: {}", userId, sessionType, limit);

        // Get user preferences
        MusicPreference preferences = musicPreferenceRepository.findByUserId(userId)
            .orElse(getDefaultPreferences(userId));

        // Check if user has Spotify connected
        if (spotifyService.hasValidCredentials(userId)) {
            return spotifyService.getRecommendations(userId, sessionType, limit);
        }

        // Fallback to generic recommendations
        return getGenericRecommendations(sessionType, preferences, limit);
    }

    /**
     * Get recommendations based on task type.
     */
    public List<RecommendationDTO> getRecommendationsByTaskType(String userId, String taskType, int limit) {
        String sessionType = mapTaskTypeToSessionType(taskType);
        return generateRecommendations(userId, sessionType, limit, Map.of("taskType", taskType));
    }

    private String mapTaskTypeToSessionType(String taskType) {
        return switch (taskType.toLowerCase()) {
            case "coding", "writing", "analysis" -> "deep_work";
            case "brainstorming", "design", "art" -> "creative";
            case "reading", "learning", "research" -> "study";
            case "break", "meditation" -> "relaxation";
            default -> "general";
        };
    }

    private MusicPreference getDefaultPreferences(String userId) {
        return MusicPreference.builder()
            .userId(userId)
            .energyLevel(0.6)
            .tempoPreference(120.0)
            .ambientSoundsEnabled(true)
            .autoStartMusic(true)
            .defaultVolume(0.5)
            .build();
    }

    private List<RecommendationDTO> getGenericRecommendations(String sessionType, MusicPreference preferences, int limit) {
        // This would typically fetch from a curated database of focus music
        // For now, return empty list as fallback
        log.info("Returning generic recommendations for session type: {}", sessionType);
        return List.of();
    }
}