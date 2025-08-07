package com.focushive.music.service;

import com.focushive.music.client.*;
import com.focushive.music.dto.RecommendationDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.MusicSession;
import com.focushive.music.repository.MusicSessionRepository;
import com.focushive.music.repository.RecommendationFeedbackRepository;
import com.focushive.music.repository.RecommendationHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for the Music Recommendation Service.
 * Tests cover algorithm logic, caching strategies, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Music Recommendation Service Tests")
class MusicRecommendationServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private HiveServiceClient hiveServiceClient;

    @Mock
    private SessionServiceClient sessionServiceClient;

    @Mock
    private AnalyticsServiceClient analyticsServiceClient;

    @Mock
    private SpotifyIntegrationService spotifyIntegrationService;

    @Mock
    private MusicSessionRepository musicSessionRepository;

    @Mock
    private RecommendationHistoryRepository recommendationHistoryRepository;

    @Mock
    private RecommendationFeedbackRepository recommendationFeedbackRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private MusicRecommendationService musicRecommendationService;

    private UUID testUserId;
    private UUID testSessionId;
    private Map<String, Object> testPreferences;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSessionId = UUID.randomUUID();
        testPreferences = new HashMap<>();
        testPreferences.put("energy", "medium");
        testPreferences.put("mood", "focused");
        testPreferences.put("genres", List.of("ambient", "classical"));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Session-Based Recommendations Tests")
    class SessionRecommendationTests {

        @Test
        @DisplayName("Should generate focus recommendations with low energy tracks")
        void shouldGenerateFocusRecommendations() {
            // Given
            var userPrefs = createMockUserPreferences();
            var insights = createMockAnalyticsInsights();
            var expectedTracks = createMockFocusTracks();

            when(userServiceClient.getUserPreferences(testUserId, null))
                .thenReturn(createMockResponse(userPrefs));
            when(analyticsServiceClient.getMusicRecommendationInsights(testUserId, null))
                .thenReturn(createMockResponse(insights));
            when(spotifyIntegrationService.getRecommendationsByGenres(eq(testUserId), anyList(), anyMap()))
                .thenReturn(expectedTracks);
            when(musicSessionRepository.findBySessionIdAndUserId(testSessionId, testUserId))
                .thenReturn(Optional.empty());
            when(musicSessionRepository.save(any(MusicSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            var recommendations = musicRecommendationService.generateSessionRecommendations(
                testUserId, testSessionId, "focus", testPreferences);

            // Then
            assertThat(recommendations).hasSize(expectedTracks.size());
            assertThat(recommendations.get(0).getEnergy()).isLessThan(0.6);
            assertThat(recommendations.get(0).getValence()).isLessThan(0.7);
            verify(spotifyIntegrationService).getRecommendationsByGenres(
                eq(testUserId), 
                argThat(genres -> genres.contains("ambient") && genres.contains("classical")),
                argThat(audioFeatures -> audioFeatures.containsKey("energy") && 
                    audioFeatures.get("energy").equals("low"))
            );
        }

        @Test
        @DisplayName("Should generate task-specific recommendations based on task type")
        void shouldGenerateTaskSpecificRecommendations() {
            // Test data for different task types
            Map<String, Map<String, Object>> taskConfigurations = Map.of(
                "deep-work", Map.of("energy", "low", "valence", "neutral", "instrumentalness", "high"),
                "creative", Map.of("energy", "medium", "valence", "positive", "danceability", "medium"),
                "administrative", Map.of("energy", "medium-high", "valence", "neutral", "tempo", "upbeat"),
                "casual", Map.of("energy", "high", "valence", "positive", "popularity", "high")
            );

            taskConfigurations.forEach((taskType, expectedFeatures) -> {
                // Given
                reset(spotifyIntegrationService);
                var userPrefs = createMockUserPreferences();
                var insights = createMockAnalyticsInsights();
                var expectedTracks = createMockTracksForTask(taskType);

                when(userServiceClient.getUserPreferences(testUserId, null))
                    .thenReturn(createMockResponse(userPrefs));
                when(analyticsServiceClient.getMusicRecommendationInsights(testUserId, null))
                    .thenReturn(createMockResponse(insights));
                when(spotifyIntegrationService.getTaskBasedRecommendations(eq(testUserId), eq(taskType), anyMap()))
                    .thenReturn(expectedTracks);

                // When
                var recommendations = musicRecommendationService.generateTaskRecommendations(
                    testUserId, taskType, testPreferences);

                // Then
                assertThat(recommendations).isNotEmpty();
                verify(spotifyIntegrationService).getTaskBasedRecommendations(
                    eq(testUserId), eq(taskType), anyMap());
            });
        }

        @Test
        @DisplayName("Should handle mood-based recommendations with emotion detection")
        void shouldGenerateMoodBasedRecommendations() {
            // Given
            String detectedMood = "stressed";
            Map<String, Object> moodPreferences = Map.of(
                "mood", detectedMood,
                "energy", "calming",
                "context", "stress-relief"
            );

            var expectedTracks = createMockCalmingTracks();
            when(spotifyIntegrationService.getMoodBasedRecommendations(testUserId, detectedMood, moodPreferences))
                .thenReturn(expectedTracks);

            // When
            var recommendations = musicRecommendationService.generateMoodRecommendations(
                testUserId, detectedMood, moodPreferences);

            // Then
            assertThat(recommendations).hasSize(expectedTracks.size());
            assertThat(recommendations.stream().allMatch(track -> track.getEnergy() < 0.5)).isTrue();
            assertThat(recommendations.stream().allMatch(track -> track.getValence() > 0.3)).isTrue();
        }
    }

    @Nested
    @DisplayName("Caching Strategy Tests")
    class CachingTests {

        @Test
        @DisplayName("Should cache recommendations with proper TTL")
        void shouldCacheRecommendationsWithTTL() {
            // Given
            String cacheKey = "music:recommendations:user:" + testUserId + ":task:focus";
            var expectedTracks = createMockFocusTracks();
            var userPrefs = createMockUserPreferences();
            var insights = createMockAnalyticsInsights();

            when(valueOperations.get(cacheKey)).thenReturn(null); // Cache miss
            when(userServiceClient.getUserPreferences(testUserId, null))
                .thenReturn(createMockResponse(userPrefs));
            when(analyticsServiceClient.getMusicRecommendationInsights(testUserId, null))
                .thenReturn(createMockResponse(insights));
            when(spotifyIntegrationService.getRecommendationsByGenres(anyUUID(), anyList(), anyMap()))
                .thenReturn(expectedTracks);

            // When
            var recommendations = musicRecommendationService.generateSessionRecommendations(
                testUserId, testSessionId, "focus", testPreferences);

            // Then
            verify(valueOperations).set(eq(cacheKey), eq(recommendations), eq(3600L)); // 1 hour TTL
            assertThat(recommendations).hasSize(expectedTracks.size());
        }

        @Test
        @DisplayName("Should return cached recommendations when available")
        void shouldReturnCachedRecommendations() {
            // Given
            String cacheKey = "music:recommendations:user:" + testUserId + ":task:focus";
            var cachedRecommendations = createMockFocusTracks();
            when(valueOperations.get(cacheKey)).thenReturn(cachedRecommendations);

            // When
            var recommendations = musicRecommendationService.generateSessionRecommendations(
                testUserId, testSessionId, "focus", testPreferences);

            // Then
            assertThat(recommendations).isEqualTo(cachedRecommendations);
            verify(spotifyIntegrationService, never()).getRecommendationsByGenres(anyUUID(), anyList(), anyMap());
        }

        @Test
        @DisplayName("Should invalidate cache when user preferences change")
        void shouldInvalidateCacheOnPreferenceChange() {
            // Given
            String cachePattern = "music:recommendations:user:" + testUserId + ":*";
            Map<String, Object> newPreferences = Map.of("genres", List.of("jazz", "blues"));

            // When
            musicRecommendationService.invalidateUserCache(testUserId);

            // Then
            verify(redisTemplate).delete(cachePattern);
        }
    }

    @Nested
    @DisplayName("Algorithm Integration Tests")
    class AlgorithmTests {

        @Test
        @DisplayName("Should apply productivity correlation weighting")
        void shouldApplyProductivityWeighting() {
            // Given
            var userPrefs = createMockUserPreferences();
            var insights = createMockAnalyticsInsights();
            var spotifyTracks = createMockSpotifyTracks();
            var productivityData = createMockProductivityData();

            when(analyticsServiceClient.getProductivityCorrelations(testUserId))
                .thenReturn(createMockResponse(productivityData));
            when(spotifyIntegrationService.getRecommendationsByGenres(anyUUID(), anyList(), anyMap()))
                .thenReturn(spotifyTracks);

            // When
            var recommendations = musicRecommendationService.generateProductivityBasedRecommendations(
                testUserId, "focus", testPreferences);

            // Then
            // Verify that tracks with higher productivity correlation are ranked higher
            assertThat(recommendations.get(0).getScore()).isGreaterThan(recommendations.get(1).getScore());
            assertThat(recommendations.stream().allMatch(track -> track.getReason().contains("productivity"))).isTrue();
        }

        @Test
        @DisplayName("Should apply time-of-day context")
        void shouldApplyTimeOfDayContext() {
            // Given - Morning context (8 AM)
            LocalTime morningTime = LocalTime.of(8, 0);
            var userPrefs = createMockUserPreferences();
            var expectedMorningTracks = createMockMorningTracks();

            when(spotifyIntegrationService.getTimeContextRecommendations(testUserId, morningTime, testPreferences))
                .thenReturn(expectedMorningTracks);

            // When
            var recommendations = musicRecommendationService.generateTimeContextRecommendations(
                testUserId, morningTime, testPreferences);

            // Then
            assertThat(recommendations).hasSize(expectedMorningTracks.size());
            assertThat(recommendations.stream().allMatch(track -> 
                track.getEnergy() > 0.6 && track.getValence() > 0.5)).isTrue();
        }

        @Test
        @DisplayName("Should blend collaborative filtering with content-based recommendations")
        void shouldBlendRecommendationTypes() {
            // Given
            var contentBasedTracks = createMockContentBasedTracks();
            var collaborativeTracks = createMockCollaborativeTracks();
            var userPrefs = createMockUserPreferences();

            when(spotifyIntegrationService.getContentBasedRecommendations(testUserId, testPreferences))
                .thenReturn(contentBasedTracks);
            when(spotifyIntegrationService.getCollaborativeRecommendations(testUserId, testPreferences))
                .thenReturn(collaborativeTracks);

            // When
            var recommendations = musicRecommendationService.generateBlendedRecommendations(
                testUserId, testPreferences);

            // Then
            assertThat(recommendations).hasSize(20); // Default limit
            // Verify blending ratio (60% content-based, 40% collaborative)
            long contentBasedCount = recommendations.stream()
                .mapToLong(track -> track.getReason().contains("content-based") ? 1 : 0).sum();
            assertThat(contentBasedCount).isBetween(10L, 14L);
        }
    }

    @Nested
    @DisplayName("Feedback and Learning Tests")
    class FeedbackTests {

        @Test
        @DisplayName("Should record and apply user feedback for future recommendations")
        void shouldRecordAndApplyUserFeedback() {
            // Given
            UUID recommendationId = UUID.randomUUID();
            String trackId = "spotify:track:test123";
            boolean liked = true;
            Integer productivityImpact = 8; // 1-10 scale

            var feedbackData = Map.of(
                "liked", liked,
                "productivityImpact", productivityImpact,
                "skipReason", "none",
                "context", "deep-work"
            );

            // When
            musicRecommendationService.recordRecommendationFeedback(
                testUserId, recommendationId, trackId, feedbackData);

            // Then
            verify(recommendationFeedbackRepository).save(argThat(feedback -> 
                feedback.getUserId().equals(testUserId) &&
                feedback.getTrackId().equals(trackId) &&
                feedback.isLiked() == liked &&
                feedback.getProductivityImpact() == productivityImpact
            ));
        }

        @Test
        @DisplayName("Should adapt recommendations based on historical feedback")
        void shouldAdaptBasedOnFeedback() {
            // Given
            var positiveGenres = List.of("ambient", "classical");
            var negativeGenres = List.of("pop", "rock");
            var feedbackHistory = createMockFeedbackHistory(positiveGenres, negativeGenres);

            when(recommendationFeedbackRepository.findByUserIdOrderByCreatedAtDesc(testUserId, any()))
                .thenReturn(feedbackHistory);

            // When
            var adaptedPreferences = musicRecommendationService.getAdaptedPreferences(testUserId, testPreferences);

            // Then
            assertThat(adaptedPreferences.get("boostedGenres")).asList().containsAll(positiveGenres);
            assertThat(adaptedPreferences.get("reducedGenres")).asList().containsAll(negativeGenres);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle Spotify API failures gracefully")
        void shouldHandleSpotifyApiFailures() {
            // Given
            when(spotifyIntegrationService.getRecommendationsByGenres(anyUUID(), anyList(), anyMap()))
                .thenThrow(new RuntimeException("Spotify API unavailable"));

            // When/Then
            assertThatThrownBy(() -> musicRecommendationService.generateSessionRecommendations(
                testUserId, testSessionId, "focus", testPreferences))
                .isInstanceOf(MusicServiceException.RecommendationException.class)
                .hasMessageContaining("Failed to generate music recommendations");
        }

        @Test
        @DisplayName("Should fallback to default preferences when user service fails")
        void shouldFallbackToDefaults() {
            // Given
            when(userServiceClient.getUserPreferences(testUserId, null))
                .thenThrow(new RuntimeException("User service unavailable"));
            var defaultTracks = createMockDefaultTracks();
            when(spotifyIntegrationService.getRecommendationsByGenres(anyUUID(), anyList(), anyMap()))
                .thenReturn(defaultTracks);

            // When
            var recommendations = musicRecommendationService.generateSessionRecommendations(
                testUserId, testSessionId, "focus", testPreferences);

            // Then
            assertThat(recommendations).hasSize(defaultTracks.size());
            verify(spotifyIntegrationService).getRecommendationsByGenres(
                eq(testUserId),
                argThat(genres -> genres.contains("ambient") && genres.contains("classical")),
                anyMap()
            );
        }
    }

    // Helper methods to create mock data
    private UserServiceClient.MusicPreferencesData createMockUserPreferences() {
        return new UserServiceClient.MusicPreferencesData(
            List.of("ambient", "classical", "instrumental"),
            List.of("jazz", "pop"),
            "spotify",
            false,
            70,
            Map.of("energy", "low", "tempo", "slow")
        );
    }

    private AnalyticsServiceClient.MusicRecommendationInsightsResponse createMockAnalyticsInsights() {
        return new AnalyticsServiceClient.MusicRecommendationInsightsResponse(
            testUserId,
            List.of("ambient", "classical"),
            List.of("spotify:track:productive1", "spotify:track:productive2"),
            "focused",
            "morning",
            Map.of("classical", 0.85, "ambient", 0.75),
            List.of("explore_instrumental")
        );
    }

    private List<RecommendationDTO> createMockFocusTracks() {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:focus1")
                .name("Peaceful Piano")
                .artist("Ambient Artist")
                .energy(0.3)
                .valence(0.5)
                .score(0.9)
                .reason("Low energy, perfect for focus sessions")
                .build(),
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:focus2")
                .name("Nature Sounds")
                .artist("Relaxation Masters")
                .energy(0.2)
                .valence(0.6)
                .score(0.85)
                .reason("Calming nature sounds for concentration")
                .build()
        );
    }

    private <T> org.springframework.http.ResponseEntity<T> createMockResponse(T body) {
        return org.springframework.http.ResponseEntity.ok(body);
    }

    private List<RecommendationDTO> createMockTracksForTask(String taskType) {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:" + taskType + "1")
                .name("Task Music for " + taskType)
                .artist("Productivity Artist")
                .energy(getEnergyForTask(taskType))
                .valence(0.6)
                .score(0.8)
                .reason("Optimized for " + taskType + " tasks")
                .build()
        );
    }

    private Double getEnergyForTask(String taskType) {
        return switch (taskType) {
            case "deep-work" -> 0.3;
            case "creative" -> 0.6;
            case "administrative" -> 0.7;
            default -> 0.5;
        };
    }

    private List<RecommendationDTO> createMockCalmingTracks() {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:calm1")
                .name("Stress Relief")
                .artist("Wellness Music")
                .energy(0.2)
                .valence(0.7)
                .score(0.9)
                .reason("Designed for stress reduction")
                .build()
        );
    }

    private List<RecommendationDTO> createMockSpotifyTracks() {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:test1")
                .name("Test Track 1")
                .artist("Test Artist")
                .energy(0.5)
                .valence(0.6)
                .score(0.7)
                .build(),
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:test2")
                .name("Test Track 2")
                .artist("Test Artist 2")
                .energy(0.4)
                .valence(0.5)
                .score(0.6)
                .build()
        );
    }

    private AnalyticsServiceClient.ProductivityCorrelationData createMockProductivityData() {
        return new AnalyticsServiceClient.ProductivityCorrelationData(
            Map.of(
                "spotify:track:productive1", 0.85,
                "spotify:track:productive2", 0.75
            ),
            Map.of(
                "classical", 0.8,
                "ambient", 0.7
            )
        );
    }

    private List<RecommendationDTO> createMockMorningTracks() {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:morning1")
                .name("Morning Energy")
                .artist("Wake Up Music")
                .energy(0.8)
                .valence(0.7)
                .score(0.9)
                .reason("Perfect for morning productivity")
                .build()
        );
    }

    private List<RecommendationDTO> createMockContentBasedTracks() {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:content1")
                .name("Content Based Track")
                .artist("Algorithm Artist")
                .score(0.8)
                .reason("content-based filtering")
                .build()
        );
    }

    private List<RecommendationDTO> createMockCollaborativeTracks() {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:collab1")
                .name("Collaborative Track")
                .artist("Community Artist")
                .score(0.7)
                .reason("collaborative filtering")
                .build()
        );
    }

    private List<RecommendationDTO> createMockDefaultTracks() {
        return List.of(
            RecommendationDTO.TrackRecommendation.builder()
                .spotifyTrackId("spotify:track:default1")
                .name("Default Track")
                .artist("Default Artist")
                .energy(0.5)
                .valence(0.5)
                .score(0.5)
                .reason("Default recommendation")
                .build()
        );
    }

    private Object createMockFeedbackHistory(List<String> positiveGenres, List<String> negativeGenres) {
        // Mock implementation would return feedback history data
        return new Object(); // Simplified for this example
    }
}