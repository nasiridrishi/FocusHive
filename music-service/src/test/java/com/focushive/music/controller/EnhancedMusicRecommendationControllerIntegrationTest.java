package com.focushive.music.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.music.dto.RecommendationRequestDTO;
import com.focushive.music.dto.RecommendationFeedbackDTO;
import com.focushive.music.service.EnhancedMusicRecommendationService;
import com.focushive.music.config.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Integration tests for the Enhanced Music Recommendation Controller.
 * Tests all endpoints with proper authentication and validation.
 */
@WebMvcTest(EnhancedMusicRecommendationController.class)
@Import(TestConfig.class)
@DisplayName("Enhanced Music Recommendation Controller Integration Tests")
class EnhancedMusicRecommendationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnhancedMusicRecommendationService recommendationService;

    private static final String BASE_URL = "/api/v1/music/recommendations";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_SESSION_ID = UUID.randomUUID();
    private static final UUID TEST_RECOMMENDATION_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Session Recommendations Tests")
    class SessionRecommendationsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should generate session recommendations successfully")
        void shouldGenerateSessionRecommendations() throws Exception {
            // Given
            RecommendationRequestDTO request = createValidRecommendationRequest();
            
            when(recommendationService.generateSessionRecommendations(any()))
                .thenReturn(createMockRecommendationResponse());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recommendationId").exists())
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations").isNotEmpty())
                .andExpect(jsonPath("$.metadata.algorithmVersion").exists())
                .andDo(print());

            verify(recommendationService).generateSessionRecommendations(argThat(req -> 
                req.getTaskType() == request.getTaskType() &&
                req.getMood() == request.getMood()
            ));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should validate required fields in request")
        void shouldValidateRequiredFields() throws Exception {
            // Given - Invalid request with missing required fields
            RecommendationRequestDTO request = RecommendationRequestDTO.builder()
                .energyLevel(15) // Invalid - exceeds max of 10
                .limit(100) // Invalid - exceeds max of 50
                .build();

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andDo(print());

            verifyNoInteractions(recommendationService);
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            // Given
            RecommendationRequestDTO request = createValidRecommendationRequest();

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andDo(print());

            verifyNoInteractions(recommendationService);
        }
    }

    @Nested
    @DisplayName("Task-Based Recommendations Tests")
    class TaskRecommendationsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should generate task-specific recommendations")
        void shouldGenerateTaskRecommendations() throws Exception {
            // Given
            RecommendationRequestDTO.TaskType taskType = RecommendationRequestDTO.TaskType.DEEP_WORK;
            Map<String, Object> preferences = Map.of("genres", List.of("ambient", "classical"));
            
            when(recommendationService.generateTaskRecommendations(any(), eq(taskType), any()))
                .thenReturn(createMockRecommendationResponse());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/tasks/" + taskType)
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(preferences))
                    .param("limit", "15"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.context.taskType").value(taskType.toString()))
                .andDo(print());

            verify(recommendationService).generateTaskRecommendations(
                eq(TEST_USER_ID), eq(taskType), eq(preferences)
            );
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should validate task type parameter")
        void shouldValidateTaskType() throws Exception {
            // When & Then
            mockMvc.perform(post(BASE_URL + "/tasks/INVALID_TASK_TYPE")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest())
                .andDo(print());

            verifyNoInteractions(recommendationService);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should validate limit parameter bounds")
        void shouldValidateLimitBounds() throws Exception {
            // Given
            RecommendationRequestDTO.TaskType taskType = RecommendationRequestDTO.TaskType.CREATIVE;

            // When & Then - Test upper bound
            mockMvc.perform(post(BASE_URL + "/tasks/" + taskType)
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .param("limit", "100")) // Exceeds max of 50
                .andExpect(status().isBadRequest())
                .andDo(print());

            // When & Then - Test lower bound
            mockMvc.perform(post(BASE_URL + "/tasks/" + taskType)
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .param("limit", "0")) // Below min of 1
                .andExpect(status().isBadRequest())
                .andDo(print());

            verifyNoInteractions(recommendationService);
        }
    }

    @Nested
    @DisplayName("Mood-Based Recommendations Tests")
    class MoodRecommendationsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should generate mood-based recommendations")
        void shouldGenerateMoodRecommendations() throws Exception {
            // Given
            RecommendationRequestDTO.MoodType mood = RecommendationRequestDTO.MoodType.FOCUSED;
            Map<String, Object> preferences = Map.of("energy", "low");
            
            when(recommendationService.generateMoodRecommendations(any(), eq(mood), any()))
                .thenReturn(createMockRecommendationResponse());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/moods/" + mood)
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(preferences))
                    .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.context.mood").value(mood.toString()))
                .andDo(print());

            verify(recommendationService).generateMoodRecommendations(
                eq(TEST_USER_ID), eq(mood), eq(preferences)
            );
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle all mood types")
        void shouldHandleAllMoodTypes() throws Exception {
            // Test each mood type
            for (RecommendationRequestDTO.MoodType mood : RecommendationRequestDTO.MoodType.values()) {
                when(recommendationService.generateMoodRecommendations(any(), eq(mood), any()))
                    .thenReturn(createMockRecommendationResponse());

                mockMvc.perform(post(BASE_URL + "/moods/" + mood)
                        .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                    .andExpect(status().isOk())
                    .andDo(print());
            }

            verify(recommendationService, times(RecommendationRequestDTO.MoodType.values().length))
                .generateMoodRecommendations(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Feedback Management Tests")
    class FeedbackTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should record feedback successfully")
        void shouldRecordFeedback() throws Exception {
            // Given
            RecommendationFeedbackDTO feedback = createValidFeedbackDTO();
            
            doNothing().when(recommendationService)
                .recordRecommendationFeedback(any(), any(), any(), any());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/" + TEST_RECOMMENDATION_ID + "/feedback")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("RECORDED"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.recommendationId").value(TEST_RECOMMENDATION_ID.toString()))
                .andExpect(jsonPath("$.trackId").value(feedback.getTrackId()))
                .andDo(print());

            verify(recommendationService).recordRecommendationFeedback(
                eq(TEST_USER_ID), eq(TEST_RECOMMENDATION_ID), 
                eq(feedback.getTrackId()), any(RecommendationFeedbackDTO.class)
            );
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should validate feedback data")
        void shouldValidateFeedbackData() throws Exception {
            // Given - Invalid feedback
            RecommendationFeedbackDTO feedback = RecommendationFeedbackDTO.builder()
                .trackId("") // Invalid - empty string
                .overallRating(6) // Invalid - exceeds max of 5
                .productivityImpact(15) // Invalid - exceeds max of 10
                .build();

            // When & Then
            mockMvc.perform(post(BASE_URL + "/" + TEST_RECOMMENDATION_ID + "/feedback")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andDo(print());

            verifyNoInteractions(recommendationService);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle batch feedback")
        void shouldHandleBatchFeedback() throws Exception {
            // Given
            List<RecommendationFeedbackDTO> feedbackList = List.of(
                createValidFeedbackDTO(),
                createValidFeedbackDTO()
            );
            
            EnhancedMusicRecommendationController.BatchFeedbackRequestDTO batchRequest = 
                EnhancedMusicRecommendationController.BatchFeedbackRequestDTO.builder()
                    .feedbackEntries(feedbackList)
                    .build();

            doNothing().when(recommendationService)
                .recordRecommendationFeedback(any(), any(), any(), any());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/" + TEST_RECOMMENDATION_ID + "/feedback/batch")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalEntries").value(2))
                .andExpect(jsonPath("$.successfulEntries").value(2))
                .andExpect(jsonPath("$.failedEntries").value(0))
                .andDo(print());

            verify(recommendationService, times(2))
                .recordRecommendationFeedback(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("History and Analytics Tests")
    class HistoryAnalyticsTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get recommendation history with pagination")
        void shouldGetRecommendationHistory() throws Exception {
            // Given
            when(recommendationService.getRecommendationHistory(any(), any()))
                .thenReturn(List.of());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/history")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .param("page", "0")
                    .param("size", "10")
                    .param("period", "7d")
                    .param("sortBy", "createdAt")
                    .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andDo(print());

            verify(recommendationService).getRecommendationHistory(eq(TEST_USER_ID), eq("7d"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get analytics data")
        void shouldGetAnalytics() throws Exception {
            // When & Then
            mockMvc.perform(get(BASE_URL + "/analytics")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .param("period", "30d")
                    .param("detailed", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get user statistics")
        void shouldGetUserStats() throws Exception {
            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should clear recommendation cache")
        void shouldClearCache() throws Exception {
            // Given
            doNothing().when(recommendationService).invalidateUserCache(any());

            // When & Then
            mockMvc.perform(delete(BASE_URL + "/cache")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString()))))
                .andExpect(status().isNoContent())
                .andDo(print());

            verify(recommendationService).invalidateUserCache(eq(TEST_USER_ID));
        }
    }

    @Nested
    @DisplayName("Trending and Community Features Tests")
    class CommunityTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get trending recommendations")
        void shouldGetTrendingRecommendations() throws Exception {
            // When & Then
            mockMvc.perform(get(BASE_URL + "/trending")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .param("period", "24h")
                    .param("taskType", "DEEP_WORK")
                    .param("limit", "30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.period").value("24h"))
                .andExpect(jsonPath("$.trendingTracks").isArray())
                .andDo(print());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should validate trending parameters")
        void shouldValidateTrendingParameters() throws Exception {
            // When & Then - Test invalid limit
            mockMvc.perform(get(BASE_URL + "/trending")
                    .with(jwt().jwt(jwt -> jwt.subject(TEST_USER_ID.toString())))
                    .param("limit", "100")) // Exceeds max of 50
                .andExpect(status().isBadRequest())
                .andDo(print());
        }
    }

    // === HELPER METHODS ===

    private RecommendationRequestDTO createValidRecommendationRequest() {
        return RecommendationRequestDTO.builder()
            .sessionId(TEST_SESSION_ID)
            .taskType(RecommendationRequestDTO.TaskType.DEEP_WORK)
            .mood(RecommendationRequestDTO.MoodType.FOCUSED)
            .energyLevel(3)
            .preferredGenres(List.of("ambient", "classical"))
            .expectedDurationMinutes(60)
            .timeOfDay(LocalTime.of(14, 30))
            .includeExplicit(false)
            .limit(20)
            .prioritizeProductivity(true)
            .includeDiversity(true)
            .build();
    }

    private RecommendationFeedbackDTO createValidFeedbackDTO() {
        return RecommendationFeedbackDTO.builder()
            .trackId("spotify:track:example123")
            .feedbackType(RecommendationFeedbackDTO.FeedbackType.EXPLICIT_RATING)
            .overallRating(4)
            .liked(true)
            .productivityImpact(8)
            .focusEnhancement(7)
            .moodAppropriateness(9)
            .taskSuitability(8)
            .confidenceLevel(4)
            .build();
    }

    private Object createMockRecommendationResponse() {
        // This would return a properly constructed RecommendationResponseDTO
        // For now, returning a simple mock object
        return new Object();
    }
}