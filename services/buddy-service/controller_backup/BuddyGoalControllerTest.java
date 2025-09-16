package com.focushive.buddy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.buddy.config.TestSecurityConfig;
import com.focushive.buddy.constant.GoalStatus;
import com.focushive.buddy.controller.BuddyGoalController;
import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyGoalService;
import com.focushive.buddy.exception.BuddyServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for BuddyGoalController.
 * Tests all REST endpoints for goal CRUD, milestones, progress tracking, and analytics.
 *
 * TDD RED PHASE: These tests are designed to FAIL initially until controller is implemented.
 */
@WebMvcTest(BuddyGoalController.class)
@Import(TestSecurityConfig.class)
class BuddyGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuddyGoalService buddyGoalService;

    private UUID testGoalId;
    private UUID testMilestoneId;
    private UUID testUserId;
    private GoalCreationDto testGoalCreation;
    private GoalResponseDto testGoalResponse;
    private MilestoneDto testMilestone;
    private GoalAnalyticsDto testAnalytics;
    private ProgressUpdateDto testProgressUpdate;

    @BeforeEach
    void setUp() {
        testGoalId = UUID.randomUUID();
        testMilestoneId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        testGoalCreation = GoalCreationDto.builder()
                .title("Learn Spring Boot")
                .description("Master Spring Boot framework for backend development")
                .category("Programming")
                .targetDate(LocalDate.now().plusDays(90))
                .priority(5)
                .goalType(GoalCreationDto.GoalType.INDIVIDUAL)
                .tags(Arrays.asList("spring", "java", "backend"))
                .initialProgress(0)
                .createdBy(testUserId)
                .build();

        testGoalResponse = GoalResponseDto.builder()
                .id(testGoalId)
                .createdBy(testUserId)
                .title("Learn Spring Boot")
                .description("Master Spring Boot framework for backend development")
                .category("Programming")
                .status(GoalStatus.IN_PROGRESS)
                .targetDate(LocalDate.now().plusDays(90))
                .priority(5)
                .goalType(GoalCreationDto.GoalType.INDIVIDUAL)
                .tags(Arrays.asList("spring", "java", "backend"))
                .progressPercentage(15)
                .isShared(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testMilestone = MilestoneDto.builder()
                .id(testMilestoneId)
                .goalId(testGoalId)
                .title("Complete Spring Boot tutorial")
                .description("Finish the official Spring Boot getting started guide")
                .targetDate(LocalDate.now().plusDays(30))
                .order(1)
                .isCompleted(false)
                .priority(3)
                .createdAt(LocalDateTime.now())
                .build();

        testAnalytics = GoalAnalyticsDto.builder()
                .goalId(testGoalId)
                .goalTitle("Learn Spring Boot")
                .analyticsGeneratedAt(LocalDateTime.now())
                .analyticsVersion("1.0")
                .build();

        testProgressUpdate = ProgressUpdateDto.builder()
                .goalId(testGoalId)
                .updatedBy(testUserId)
                .progressPercentage(25)
                .progressNotes("Completed first tutorial chapter")
                .updateType(ProgressUpdateDto.ProgressUpdateType.MANUAL)
                .updateTimestamp(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Goal CRUD Operations")
    class GoalCrudTests {

        @Test
        @DisplayName("POST /api/v1/buddy/goals - Create individual goal")
        void createGoal_Individual_Success() throws Exception {
            when(buddyGoalService.createIndividualGoal(org.mockito.ArgumentMatchers.any(GoalCreationDto.class)))
                    .thenReturn(testGoalResponse);

            mockMvc.perform(post("/api/v1/buddy/goals")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testGoalCreation)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Learn Spring Boot"))
                    .andExpect(jsonPath("$.data.category").value("Programming"))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.data.isShared").value(false))
                    .andExpect(jsonPath("$.data.progressPercentage").value(15))
                    .andExpect(jsonPath("$.message").value("Goal created successfully"));

            verify(buddyGoalService).createIndividualGoal(org.mockito.ArgumentMatchers.any(GoalCreationDto.class));
        }

        @Test
        @DisplayName("POST /api/v1/buddy/goals - Create shared goal")
        void createGoal_Shared_Success() throws Exception {
            GoalCreationDto sharedGoal = testGoalCreation.toBuilder()
                    .goalType(GoalCreationDto.GoalType.SHARED)
                    .partnershipId(UUID.randomUUID())
                    .build();

            GoalResponseDto sharedResponse = testGoalResponse.toBuilder()
                    .goalType(GoalCreationDto.GoalType.SHARED)
                    .isShared(true)
                    .build();

            when(buddyGoalService.createSharedGoal(org.mockito.ArgumentMatchers.any(GoalCreationDto.class)))
                    .thenReturn(sharedResponse);

            mockMvc.perform(post("/api/v1/buddy/goals")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sharedGoal)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isShared").value(true))
                    .andExpect(jsonPath("$.message").value("Shared goal created successfully"));

            verify(buddyGoalService).createSharedGoal(org.mockito.ArgumentMatchers.any(GoalCreationDto.class));
        }

        @Test
        @DisplayName("POST /api/v1/buddy/goals - Invalid goal data")
        void createGoal_InvalidData() throws Exception {
            GoalCreationDto invalidGoal = GoalCreationDto.builder()
                    .title("") // Empty title
                    .targetDate(LocalDate.now().minusDays(1)) // Past date
                    .initialProgress(-10) // Negative value
                    .build();

            when(buddyGoalService.createIndividualGoal(org.mockito.ArgumentMatchers.any(GoalCreationDto.class)))
                    .thenThrow(new IllegalArgumentException("Goal title cannot be empty"));

            mockMvc.perform(post("/api/v1/buddy/goals")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidGoal)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Goal title cannot be empty"));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals - List user goals")
        void getGoals_Success() throws Exception {
            Page<GoalResponseDto> goalsPage = new PageImpl<>(
                    Arrays.asList(testGoalResponse),
                    PageRequest.of(0, 10),
                    1
            );

            when(buddyGoalService.getGoalsForUser(eq(testUserId), isNull(), any()))
                    .thenReturn(goalsPage);

            mockMvc.perform(get("/api/v1/buddy/goals")
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1));

            verify(buddyGoalService).getGoalsForUser(eq(testUserId), isNull(), any());
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals - Filter by status")
        void getGoals_FilterByStatus() throws Exception {
            // testGoalResponse already has IN_PROGRESS status which is active
            Page<GoalResponseDto> goalsPage = new PageImpl<>(
                    Arrays.asList(testGoalResponse),
                    PageRequest.of(0, 10),
                    1
            );

            when(buddyGoalService.getGoalsForUser(eq(testUserId), eq("ACTIVE"), any()))
                    .thenReturn(goalsPage);

            mockMvc.perform(get("/api/v1/buddy/goals")
                    .header("X-User-ID", testUserId.toString())
                    .param("status", "ACTIVE")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].status").value("IN_PROGRESS"));

            verify(buddyGoalService).getGoalsForUser(eq(testUserId), eq("ACTIVE"), any());
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals/{id} - Get goal details")
        void getGoalDetails_Success() throws Exception {
            when(buddyGoalService.getGoalById(eq(testGoalId), eq(testUserId)))
                    .thenReturn(testGoalResponse);

            mockMvc.perform(get("/api/v1/buddy/goals/{id}", testGoalId)
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Learn Spring Boot"));

            verify(buddyGoalService).getGoalById(eq(testGoalId), eq(testUserId));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals/{id} - Goal not found")
        void getGoalDetails_NotFound() throws Exception {
            when(buddyGoalService.getGoalById(eq(testGoalId), eq(testUserId)))
                    .thenReturn(null);

            mockMvc.perform(get("/api/v1/buddy/goals/{id}", testGoalId)
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("Goal not found"));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/goals/{id} - Update goal")
        void updateGoal_Success() throws Exception {
            GoalCreationDto updateDto = testGoalCreation.toBuilder()
                    .title("Advanced Spring Boot")
                    .description("Deep dive into Spring Boot advanced features")
                    .build();

            GoalResponseDto updatedResponse = testGoalResponse.toBuilder()
                    .title("Advanced Spring Boot")
                    .description("Deep dive into Spring Boot advanced features")
                    .build();

            when(buddyGoalService.updateGoal(eq(testGoalId), eq(updateDto), eq(testUserId)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/buddy/goals/{id}", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Advanced Spring Boot"))
                    .andExpect(jsonPath("$.data.description").value("Deep dive into Spring Boot advanced features"))
                    .andExpect(jsonPath("$.message").value("Goal updated successfully"));

            verify(buddyGoalService).updateGoal(eq(testGoalId), eq(updateDto), eq(testUserId));
        }

        @Test
        @DisplayName("DELETE /api/v1/buddy/goals/{id} - Delete goal")
        void deleteGoal_Success() throws Exception {
            doNothing().when(buddyGoalService).deleteGoal(eq(testGoalId), eq(testUserId));

            mockMvc.perform(delete("/api/v1/buddy/goals/{id}", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Goal deleted successfully"));

            verify(buddyGoalService).deleteGoal(eq(testGoalId), eq(testUserId));
        }

        @Test
        @DisplayName("DELETE /api/v1/buddy/goals/{id} - Unauthorized deletion")
        void deleteGoal_Unauthorized() throws Exception {
            doThrow(new IllegalArgumentException("User not authorized to delete this goal"))
                    .when(buddyGoalService).deleteGoal(eq(testGoalId), eq(testUserId));

            mockMvc.perform(delete("/api/v1/buddy/goals/{id}", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error").value("User not authorized to delete this goal"));
        }
    }

    @Nested
    @DisplayName("Milestone Management")
    class MilestoneTests {

        @Test
        @DisplayName("POST /api/v1/buddy/goals/{goalId}/milestones - Add milestone")
        void addMilestone_Success() throws Exception {
            when(buddyGoalService.addMilestone(eq(testGoalId), eq(testMilestone), eq(testUserId)))
                    .thenReturn(testMilestone);

            mockMvc.perform(post("/api/v1/buddy/goals/{goalId}/milestones", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testMilestone)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testMilestoneId.toString()))
                    .andExpect(jsonPath("$.data.goalId").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Complete Spring Boot tutorial"))
                    .andExpect(jsonPath("$.data.isCompleted").value(false))
                    .andExpect(jsonPath("$.message").value("Milestone created successfully"));

            verify(buddyGoalService).addMilestone(eq(testGoalId), eq(testMilestone), eq(testUserId));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals/{goalId}/milestones - Get milestones")
        void getMilestones_Success() throws Exception {
            List<MilestoneDto> milestones = Arrays.asList(testMilestone);
            when(buddyGoalService.getMilestonesForGoal(eq(testGoalId), eq(true)))
                    .thenReturn(milestones);

            mockMvc.perform(get("/api/v1/buddy/goals/{goalId}/milestones", testGoalId)
                    .header("X-User-ID", testUserId.toString())
                    .param("includeCompleted", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.milestones").isArray())
                    .andExpect(jsonPath("$.data.milestones", hasSize(1)))
                    .andExpect(jsonPath("$.data.milestones[0].id").value(testMilestoneId.toString()))
                    .andExpect(jsonPath("$.data.totalCount").value(1));

            verify(buddyGoalService).getMilestonesForGoal(eq(testGoalId), eq(true));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/goals/{goalId}/milestones/{milestoneId} - Update milestone")
        void updateMilestone_Success() throws Exception {
            MilestoneDto updatedMilestone = testMilestone.toBuilder()
                    .title("Updated milestone title")
                    .description("Updated description")
                    .build();

            when(buddyGoalService.updateMilestone(eq(testMilestoneId), eq(updatedMilestone), eq(testUserId)))
                    .thenReturn(updatedMilestone);

            mockMvc.perform(put("/api/v1/buddy/goals/{goalId}/milestones/{milestoneId}", testGoalId, testMilestoneId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedMilestone)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated milestone title"))
                    .andExpect(jsonPath("$.message").value("Milestone updated successfully"));

            verify(buddyGoalService).updateMilestone(eq(testMilestoneId), eq(updatedMilestone), eq(testUserId));
        }

        @Test
        @DisplayName("POST /api/v1/buddy/goals/{goalId}/milestones/{milestoneId}/complete - Complete milestone")
        void completeMilestone_Success() throws Exception {
            MilestoneCompletionRequest completionRequest = new MilestoneCompletionRequest();
            completionRequest.setCompletionNotes("Successfully completed the tutorial");

            MilestoneDto completedMilestone = testMilestone.toBuilder()
                    .isCompleted(true)
                    .completedAt(LocalDateTime.now())
                    .completionNotes("Successfully completed the tutorial")
                    .build();

            when(buddyGoalService.completeMilestone(eq(testMilestoneId), eq(testUserId), eq("Successfully completed the tutorial")))
                    .thenReturn(completedMilestone);

            mockMvc.perform(post("/api/v1/buddy/goals/{goalId}/milestones/{milestoneId}/complete", testGoalId, testMilestoneId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completionRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.isCompleted").value(true))
                    .andExpect(jsonPath("$.data.completionNotes").value("Successfully completed the tutorial"))
                    .andExpect(jsonPath("$.message").value("Milestone completed successfully"));

            verify(buddyGoalService).completeMilestone(eq(testMilestoneId), eq(testUserId), eq("Successfully completed the tutorial"));
        }

        @Test
        @DisplayName("PUT /api/v1/buddy/goals/{goalId}/milestones/reorder - Reorder milestones")
        void reorderMilestones_Success() throws Exception {
            ReorderMilestonesRequest reorderRequest = new ReorderMilestonesRequest();
            reorderRequest.setMilestoneIds(Arrays.asList(testMilestoneId, UUID.randomUUID()));

            List<MilestoneDto> reorderedMilestones = Arrays.asList(testMilestone);
            when(buddyGoalService.reorderMilestones(eq(testGoalId), eq(reorderRequest.getMilestoneIds()), eq(testUserId)))
                    .thenReturn(reorderedMilestones);

            mockMvc.perform(put("/api/v1/buddy/goals/{goalId}/milestones/reorder", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(reorderRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.milestones").isArray())
                    .andExpect(jsonPath("$.message").value("Milestones reordered successfully"));

            verify(buddyGoalService).reorderMilestones(eq(testGoalId), eq(reorderRequest.getMilestoneIds()), eq(testUserId));
        }
    }

    @Nested
    @DisplayName("Progress Tracking")
    class ProgressTrackingTests {

        @Test
        @DisplayName("POST /api/v1/buddy/goals/{goalId}/progress - Update goal progress")
        void updateProgress_Success() throws Exception {
            GoalResponseDto updatedResponse = testGoalResponse.toBuilder()
                    .progressPercentage(25)
                    .progressPercentage(25)
                    .build();

            when(buddyGoalService.updateProgress(org.mockito.ArgumentMatchers.any(ProgressUpdateDto.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(post("/api/v1/buddy/goals/{goalId}/progress", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testProgressUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.progressPercentage").value(25))
                    .andExpect(jsonPath("$.data.currentValue").value(25))
                    .andExpect(jsonPath("$.message").value("Progress updated successfully"));

            verify(buddyGoalService).updateProgress(org.mockito.ArgumentMatchers.any(ProgressUpdateDto.class));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals/{goalId}/progress - Get progress calculation")
        void getProgressCalculation_Success() throws Exception {
            when(buddyGoalService.calculateOverallProgress(eq(testGoalId)))
                    .thenReturn(25);

            mockMvc.perform(get("/api/v1/buddy/goals/{goalId}/progress", testGoalId)
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.goalId").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.data.overallProgress").value(25))
                    .andExpect(jsonPath("$.data.milestoneProgress").exists());

            verify(buddyGoalService).calculateOverallProgress(eq(testGoalId));
        }

        @Test
        @DisplayName("POST /api/v1/buddy/goals/{goalId}/track-daily - Track daily progress")
        void trackDailyProgress_Success() throws Exception {
            DailyProgressRequest dailyRequest = new DailyProgressRequest();
            dailyRequest.setProgressPercentage(5);
            dailyRequest.setNotes("Completed chapter 3");

            doNothing().when(buddyGoalService)
                    .trackDailyProgress(eq(testGoalId), eq(5), eq(testUserId), eq("Completed chapter 3"));

            mockMvc.perform(post("/api/v1/buddy/goals/{goalId}/track-daily", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dailyRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Daily progress tracked successfully"));

            verify(buddyGoalService).trackDailyProgress(eq(testGoalId), eq(5), eq(testUserId), eq("Completed chapter 3"));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals/{goalId}/analytics - Get goal analytics")
        void getGoalAnalytics_Success() throws Exception {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(buddyGoalService.generateProgressReport(eq(testGoalId), eq(startDate), eq(endDate)))
                    .thenReturn(testAnalytics);

            mockMvc.perform(get("/api/v1/buddy/goals/{goalId}/analytics", testGoalId)
                    .header("X-User-ID", testUserId.toString())
                    .param("startDate", startDate.toString())
                    .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.goalId").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.data.goalTitle").value("Learn Spring Boot"))
                    .andExpect(jsonPath("$.data.analyticsGeneratedAt").exists())
                    .andExpect(jsonPath("$.data.analyticsVersion").value("1.0"));

            verify(buddyGoalService).generateProgressReport(eq(testGoalId), eq(startDate), eq(endDate));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals/{goalId}/stagnation - Detect progress stagnation")
        void detectProgressStagnation_Success() throws Exception {
            when(buddyGoalService.detectProgressStagnation(eq(testGoalId), eq(7)))
                    .thenReturn(true);

            List<String> interventions = Arrays.asList("Set smaller milestones", "Adjust target date");
            when(buddyGoalService.suggestProgressInterventions(eq(testGoalId)))
                    .thenReturn(interventions);

            mockMvc.perform(get("/api/v1/buddy/goals/{goalId}/stagnation", testGoalId)
                    .header("X-User-ID", testUserId.toString())
                    .param("daysThreshold", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.goalId").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.data.isStagnant").value(true))
                    .andExpect(jsonPath("$.data.daysThreshold").value(7))
                    .andExpect(jsonPath("$.data.interventions").isArray())
                    .andExpect(jsonPath("$.data.interventions", hasSize(2)));

            verify(buddyGoalService).detectProgressStagnation(eq(testGoalId), eq(7));
            verify(buddyGoalService).suggestProgressInterventions(eq(testGoalId));
        }
    }

    @Nested
    @DisplayName("Goal Templates and Suggestions")
    class TemplatesAndSuggestionsTests {

        @Test
        
        @DisplayName("GET /api/v1/buddy/goals/templates - Get goal templates")
        void getGoalTemplates_Success() throws Exception {
            GoalTemplateDto template = GoalTemplateDto.builder()
                    .id(UUID.randomUUID())
                    .title("Programming Skills Development")
                    .description("Template for improving programming skills")
                    .category("Programming")
                    .difficulty(3)
                    .estimatedTimeHours(90)
                    .tags(Arrays.asList("programming", "skills", "development"))
                    .statistics(GoalTemplateDto.TemplateStatisticsDto.builder()
                        .successRate(0.78)
                        .build())
                    .build();

            Page<GoalTemplateDto> templatesPage = new PageImpl<>(
                    Arrays.asList(template),
                    PageRequest.of(0, 10),
                    1
            );

            when(buddyGoalService.getGoalTemplates(eq("Programming"), eq(3), any()))
                    .thenReturn(templatesPage);

            mockMvc.perform(get("/api/v1/buddy/goals/templates")
                    .header("X-User-ID", testUserId.toString())
                    .param("category", "Programming")
                    .param("difficulty", "3")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].category").value("Programming"))
                    .andExpect(jsonPath("$.data.content[0].difficulty").value(3));

            verify(buddyGoalService).getGoalTemplates(eq("Programming"), eq(3), any());
        }

        @Test
        @DisplayName("POST /api/v1/buddy/goals/from-template/{templateId} - Create goal from template")
        void createGoalFromTemplate_Success() throws Exception {
            UUID templateId = UUID.randomUUID();
            GoalCreationDto customization = testGoalCreation.toBuilder()
                    .targetDate(LocalDate.now().plusDays(120)) // Customize target date
                    .build();

            when(buddyGoalService.cloneGoalFromTemplate(eq(templateId), eq(customization), eq(testUserId)))
                    .thenReturn(testGoalResponse);

            mockMvc.perform(post("/api/v1/buddy/goals/from-template/{templateId}", templateId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(customization)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testGoalId.toString()))
                    .andExpect(jsonPath("$.message").value("Goal created from template successfully"));

            verify(buddyGoalService).cloneGoalFromTemplate(eq(templateId), eq(customization), eq(testUserId));
        }

        @Test
        @DisplayName("GET /api/v1/buddy/goals/suggestions - Get personalized goal suggestions")
        void getGoalSuggestions_Success() throws Exception {
            GoalTemplateDto suggestion1 = GoalTemplateDto.builder()
                    .id(UUID.randomUUID())
                    .title("Backend Development Mastery")
                    .category("Programming")
                    .build();

            GoalTemplateDto suggestion2 = GoalTemplateDto.builder()
                    .id(UUID.randomUUID())
                    .title("Database Design Skills")
                    .category("Database")
                    .build();

            List<GoalTemplateDto> suggestions = Arrays.asList(suggestion1, suggestion2);
            when(buddyGoalService.suggestGoalsBasedOnProfile(eq(testUserId), eq(5)))
                    .thenReturn(suggestions);

            mockMvc.perform(get("/api/v1/buddy/goals/suggestions")
                    .header("X-User-ID", testUserId.toString())
                    .param("maxSuggestions", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.suggestions").isArray())
                    .andExpect(jsonPath("$.data.suggestions", hasSize(2)))
                    .andExpect(jsonPath("$.data.suggestions[0].category").value("Programming"))
                    .andExpect(jsonPath("$.data.suggestions[1].category").value("Database"))
                    .andExpect(jsonPath("$.data.totalSuggestions").value(2));

            verify(buddyGoalService).suggestGoalsBasedOnProfile(eq(testUserId), eq(5));
        }
    }

    @Nested
    @DisplayName("Search and Filtering")
    class SearchAndFilteringTests {

        @Test
        @DisplayName("POST /api/v1/buddy/goals/search - Search goals with criteria")
        void searchGoals_Success() throws Exception {
            GoalSearchRequest searchRequest = new GoalSearchRequest();
            searchRequest.setTitle("Spring");
            searchRequest.setCategory("Programming");
            searchRequest.setStatus("ACTIVE");
            searchRequest.setMinProgress(10);
            searchRequest.setMaxProgress(50);
            searchRequest.setTags(Arrays.asList("spring", "java"));

            Page<GoalResponseDto> searchResults = new PageImpl<>(
                    Arrays.asList(testGoalResponse),
                    PageRequest.of(0, 10),
                    1
            );

            when(buddyGoalService.searchGoals(any(), eq(testUserId), any()))
                    .thenReturn(searchResults);

            mockMvc.perform(post("/api/v1/buddy/goals/search")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(buddyGoalService).searchGoals(any(), eq(testUserId), any());
        }

        @Test
        
        @DisplayName("GET /api/v1/buddy/goals/partnerships/{partnershipId} - Get shared goals")
        void getSharedGoals_Success() throws Exception {
            UUID partnershipId = UUID.randomUUID();
            GoalResponseDto sharedGoal = testGoalResponse.toBuilder()
                    .goalType(GoalCreationDto.GoalType.SHARED)
                    .isShared(true)
                    .build();

            Page<GoalResponseDto> sharedGoals = new PageImpl<>(
                    Arrays.asList(sharedGoal),
                    PageRequest.of(0, 10),
                    1
            );

            when(buddyGoalService.getSharedGoalsForPartnership(eq(partnershipId), any()))
                    .thenReturn(sharedGoals);

            mockMvc.perform(get("/api/v1/buddy/goals/partnerships/{partnershipId}", partnershipId)
                    .header("X-User-ID", testUserId.toString())
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].isShared").value(true));

            verify(buddyGoalService).getSharedGoalsForPartnership(eq(partnershipId), any());
        }
    }

    @Nested
    @DisplayName("Input Validation and Error Handling")
    class ValidationAndErrorHandlingTests {

        @Test
        
        @DisplayName("All endpoints - Missing user ID header")
        void allEndpoints_MissingUserIdHeader() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/goals"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/v1/buddy/goals").with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        
        @DisplayName("Goal creation - Invalid JSON payload")
        void createGoal_InvalidJson() throws Exception {
            mockMvc.perform(post("/api/v1/buddy/goals")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid JSON")));
        }

        @Test
        
        @DisplayName("Goal operations - Invalid UUID")
        void goalOperations_InvalidUuid() throws Exception {
            mockMvc.perform(get("/api/v1/buddy/goals/{id}", "invalid-uuid")
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid goal ID format")));
        }

        @Test
        @DisplayName("Progress update - Out of range values")
        void progressUpdate_OutOfRange() throws Exception {
            ProgressUpdateDto invalidProgress = testProgressUpdate.toBuilder()
                    .progressPercentage(150) // Over 100%
                    .build();

            when(buddyGoalService.updateProgress(org.mockito.ArgumentMatchers.any(ProgressUpdateDto.class)))
                    .thenThrow(new IllegalArgumentException("Progress percentage must be between 0 and 100"));

            mockMvc.perform(post("/api/v1/buddy/goals/{goalId}/progress", testGoalId)
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidProgress)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Progress percentage must be between 0 and 100"));
        }

        @Test
        @DisplayName("Service unavailable handling")
        void serviceUnavailable_Handling() throws Exception {
            when(buddyGoalService.getGoalsForUser(any(UUID.class), isNull(), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            mockMvc.perform(get("/api/v1/buddy/goals")
                    .header("X-User-ID", testUserId.toString()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Internal server error"));
        }

        @Test
        @DisplayName("Goal limit exceeded")
        void goalLimitExceeded_Handling() throws Exception {
            when(buddyGoalService.createIndividualGoal(org.mockito.ArgumentMatchers.any(GoalCreationDto.class)))
                    .thenThrow(new BuddyServiceException("Maximum goal limit exceeded for user"));

            mockMvc.perform(post("/api/v1/buddy/goals")
                    .with(csrf())
                    .header("X-User-ID", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testGoalCreation)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Maximum goal limit exceeded for user"));
        }
    }

    // Helper request classes for test data
    private static class MilestoneCompletionRequest {
        private String completionNotes;

        public String getCompletionNotes() { return completionNotes; }
        public void setCompletionNotes(String completionNotes) { this.completionNotes = completionNotes; }
    }

    private static class ReorderMilestonesRequest {
        private List<UUID> milestoneIds;

        public List<UUID> getMilestoneIds() { return milestoneIds; }
        public void setMilestoneIds(List<UUID> milestoneIds) { this.milestoneIds = milestoneIds; }
    }

    private static class DailyProgressRequest {
        private Integer progressPercentage;
        private String notes;

        public Integer getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(Integer progressPercentage) { this.progressPercentage = progressPercentage; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    private static class GoalSearchRequest {
        private String title;
        private String category;
        private String status;
        private Integer minProgress;
        private Integer maxProgress;
        private List<String> tags;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getMinProgress() { return minProgress; }
        public void setMinProgress(Integer minProgress) { this.minProgress = minProgress; }
        public Integer getMaxProgress() { return maxProgress; }
        public void setMaxProgress(Integer maxProgress) { this.maxProgress = maxProgress; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
}