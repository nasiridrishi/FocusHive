package com.focushive.analytics.service;

import com.focushive.analytics.dto.*;
import com.focushive.analytics.entity.*;
import com.focushive.analytics.enums.AchievementType;
import com.focushive.analytics.enums.ReportPeriod;
import com.focushive.analytics.repository.*;
import com.focushive.analytics.service.impl.EnhancedAnalyticsServiceImpl;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.common.exception.BadRequestException;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.repository.FocusSessionRepository;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.repository.HiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Enhanced Analytics Service Tests")
class EnhancedAnalyticsServiceTest {

    @Mock
    private ProductivityMetricRepository productivityMetricRepository;

    @Mock
    private HiveAnalyticsRepository hiveAnalyticsRepository;

    @Mock
    private UserStreakRepository userStreakRepository;

    @Mock
    private AchievementProgressRepository achievementProgressRepository;

    @Mock
    private DailyGoalRepository dailyGoalRepository;

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HiveRepository hiveRepository;

    @InjectMocks
    private EnhancedAnalyticsServiceImpl analyticsService;

    private User testUser;
    private Hive testHive;
    private String userId = "test-user-1";
    private String hiveId = "test-hive-1";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(userId);
        testUser.setDisplayName("Test User");

        testHive = new Hive();
        testHive.setId(hiveId);
        testHive.setName("Test Hive");
    }

    @Nested
    @DisplayName("Productivity Metrics Tests")
    class ProductivityMetricsTests {

        @Test
        @DisplayName("Should calculate productivity score correctly")
        void shouldCalculateProductivityScoreCorrectly() {
            // Given
            int focusMinutes = 120;
            int completedSessions = 3;
            int distractions = 2;
            int streakBonus = 5;

            // When
            int result = analyticsService.calculateProductivityScore(focusMinutes, completedSessions, distractions, streakBonus);

            // Then
            int expected = (focusMinutes * 2) + (completedSessions * 10) - (distractions * 5) + streakBonus;
            assertEquals(expected, result);
            assertEquals(255, result); // (120*2) + (3*10) - (2*5) + 5 = 240 + 30 - 10 + 5 = 255
        }

        @Test
        @DisplayName("Should handle minimum productivity score of zero")
        void shouldHandleMinimumProductivityScoreOfZero() {
            // Given
            int focusMinutes = 10;
            int completedSessions = 0;
            int distractions = 100;
            int streakBonus = 0;

            // When
            int result = analyticsService.calculateProductivityScore(focusMinutes, completedSessions, distractions, streakBonus);

            // Then
            assertTrue(result >= 0, "Productivity score should not be negative");
        }

        @Test
        @DisplayName("Should get user productivity summary")
        void shouldGetUserProductivitySummary() {
            // Given
            when(userRepository.existsById(userId)).thenReturn(true);

            ProductivityMetric metric = new ProductivityMetric();
            metric.setUserId(userId);
            metric.setDate(LocalDate.now());
            metric.setFocusMinutes(120);
            metric.setCompletedSessions(3);
            metric.setProductivityScore(85);

            when(productivityMetricRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .thenReturn(List.of(metric));

            // When
            ProductivitySummaryResponse response = analyticsService.getUserProductivitySummary(userId);

            // Then
            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals(120, response.getTotalFocusMinutes());
            assertEquals(3, response.getTotalCompletedSessions());
            assertEquals(85, response.getAverageProductivityScore());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void shouldThrowExceptionForNonExistentUser() {
            // Given
            when(userRepository.existsById(userId)).thenReturn(false);

            // When & Then
            assertThrows(ResourceNotFoundException.class,
                () -> analyticsService.getUserProductivitySummary(userId));
        }

        @Test
        @DisplayName("Should create productivity metric from focus session")
        void shouldCreateProductivityMetricFromFocusSession() {
            // Given
            FocusSession session = new FocusSession();
            session.setUserId(userId);
            session.setDurationMinutes(60);
            session.setStatus(FocusSession.SessionStatus.COMPLETED);
            session.setTabSwitches(2);
            session.setCompletedAt(LocalDateTime.now());

            when(productivityMetricRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(Optional.empty());
            when(productivityMetricRepository.save(any(ProductivityMetric.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateProductivityMetrics(session);

            // Then
            verify(productivityMetricRepository).save(any(ProductivityMetric.class));
        }
    }

    @Nested
    @DisplayName("Streak Calculation Tests")
    class StreakCalculationTests {

        @Test
        @DisplayName("Should calculate current streak correctly")
        void shouldCalculateCurrentStreakCorrectly() {
            // Given
            UserStreak existingStreak = new UserStreak();
            existingStreak.setUserId(userId);
            existingStreak.setCurrentStreak(5);
            existingStreak.setLastActiveDate(LocalDate.now().minusDays(1));

            when(userStreakRepository.findByUserId(userId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateUserStreak(userId, true);

            // Then
            verify(userStreakRepository).save(argThat(streak -> streak.getCurrentStreak() == 6));
        }

        @Test
        @DisplayName("Should reset streak when gap detected")
        void shouldResetStreakWhenGapDetected() {
            // Given
            UserStreak existingStreak = new UserStreak();
            existingStreak.setUserId(userId);
            existingStreak.setCurrentStreak(5);
            existingStreak.setLongestStreak(10);
            existingStreak.setLastActiveDate(LocalDate.now().minusDays(3)); // Gap of 2 days

            when(userStreakRepository.findByUserId(userId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateUserStreak(userId, true);

            // Then
            verify(userStreakRepository).save(argThat(streak -> streak.getCurrentStreak() == 1));
        }

        @Test
        @DisplayName("Should update longest streak when current exceeds it")
        void shouldUpdateLongestStreakWhenCurrentExceedsIt() {
            // Given
            UserStreak existingStreak = new UserStreak();
            existingStreak.setUserId(userId);
            existingStreak.setCurrentStreak(10);
            existingStreak.setLongestStreak(5);
            existingStreak.setLastActiveDate(LocalDate.now().minusDays(1));

            when(userStreakRepository.findByUserId(userId)).thenReturn(Optional.of(existingStreak));
            when(userStreakRepository.save(any(UserStreak.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateUserStreak(userId, true);

            // Then
            verify(userStreakRepository).save(argThat(streak ->
                streak.getCurrentStreak() == 11 && streak.getLongestStreak() == 11));
        }

        @Test
        @DisplayName("Should get streak information")
        void shouldGetStreakInformation() {
            // Given
            UserStreak streak = new UserStreak();
            streak.setUserId(userId);
            streak.setCurrentStreak(7);
            streak.setLongestStreak(15);
            streak.setLastActiveDate(LocalDate.now());

            when(userStreakRepository.findByUserId(userId)).thenReturn(Optional.of(streak));

            // When
            StreakInfoResponse response = analyticsService.getStreakInformation(userId);

            // Then
            assertNotNull(response);
            assertEquals(7, response.getCurrentStreak());
            assertEquals(15, response.getLongestStreak());
            assertEquals(LocalDate.now(), response.getLastActiveDate());
        }
    }

    @Nested
    @DisplayName("Achievement System Tests")
    class AchievementSystemTests {

        @Test
        @DisplayName("Should unlock achievement when criteria met")
        void shouldUnlockAchievementWhenCriteriaMet() {
            // Given
            when(achievementProgressRepository.findByUserIdAndAchievementType(userId, AchievementType.FIRST_FOCUS))
                .thenReturn(Optional.empty());
            when(achievementProgressRepository.save(any(AchievementProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            boolean result = analyticsService.checkAndUnlockAchievement(userId, AchievementType.FIRST_FOCUS, 1);

            // Then
            assertTrue(result);
            verify(achievementProgressRepository).save(any(AchievementProgress.class));
        }

        @Test
        @DisplayName("Should not unlock already unlocked achievement")
        void shouldNotUnlockAlreadyUnlockedAchievement() {
            // Given
            AchievementProgress existingProgress = new AchievementProgress();
            existingProgress.setUserId(userId);
            existingProgress.setAchievementType(AchievementType.FIRST_FOCUS);
            existingProgress.setUnlockedAt(LocalDateTime.now().minusDays(1));

            when(achievementProgressRepository.findByUserIdAndAchievementType(userId, AchievementType.FIRST_FOCUS))
                .thenReturn(Optional.of(existingProgress));

            // When
            boolean result = analyticsService.checkAndUnlockAchievement(userId, AchievementType.FIRST_FOCUS, 1);

            // Then
            assertFalse(result);
            verify(achievementProgressRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should get user achievement progress")
        void shouldGetUserAchievementProgress() {
            // Given
            AchievementProgress progress1 = new AchievementProgress();
            progress1.setUserId(userId);
            progress1.setAchievementType(AchievementType.FIRST_FOCUS);
            progress1.setProgress(100);
            progress1.setUnlockedAt(LocalDateTime.now());

            AchievementProgress progress2 = new AchievementProgress();
            progress2.setUserId(userId);
            progress2.setAchievementType(AchievementType.WEEK_WARRIOR);
            progress2.setProgress(50);

            when(achievementProgressRepository.findByUserId(userId))
                .thenReturn(List.of(progress1, progress2));

            // When
            AchievementProgressResponse response = analyticsService.getUserAchievements(userId);

            // Then
            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals(2, response.getAchievements().size());
            assertEquals(1, response.getUnlockedCount());
            assertEquals(1, response.getInProgressCount());
        }

        @Test
        @DisplayName("Should update achievement progress")
        void shouldUpdateAchievementProgress() {
            // Given
            AchievementProgress existing = new AchievementProgress();
            existing.setUserId(userId);
            existing.setAchievementType(AchievementType.WEEK_WARRIOR);
            existing.setProgress(30);

            when(achievementProgressRepository.findByUserIdAndAchievementType(userId, AchievementType.WEEK_WARRIOR))
                .thenReturn(Optional.of(existing));
            when(achievementProgressRepository.save(any(AchievementProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateAchievementProgress(userId, AchievementType.WEEK_WARRIOR, 50);

            // Then
            verify(achievementProgressRepository).save(argThat(progress -> progress.getProgress() == 50));
        }
    }

    @Nested
    @DisplayName("Goal Setting Tests")
    class GoalSettingTests {

        @Test
        @DisplayName("Should set daily goal")
        void shouldSetDailyGoal() {
            // Given
            DailyGoalRequest request = new DailyGoalRequest();
            request.setTargetMinutes(120);
            request.setDate(LocalDate.now());

            when(dailyGoalRepository.findByUserIdAndDate(userId, request.getDate()))
                .thenReturn(Optional.empty());
            when(dailyGoalRepository.save(any(DailyGoal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            DailyGoal result = analyticsService.setDailyGoal(userId, request);

            // Then
            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals(120, result.getTargetMinutes());
            assertEquals(0, result.getCompletedMinutes());
        }

        @Test
        @DisplayName("Should update existing daily goal")
        void shouldUpdateExistingDailyGoal() {
            // Given
            DailyGoalRequest request = new DailyGoalRequest();
            request.setTargetMinutes(150);
            request.setDate(LocalDate.now());

            DailyGoal existing = new DailyGoal();
            existing.setUserId(userId);
            existing.setTargetMinutes(120);
            existing.setCompletedMinutes(60);
            existing.setDate(LocalDate.now());

            when(dailyGoalRepository.findByUserIdAndDate(userId, request.getDate()))
                .thenReturn(Optional.of(existing));
            when(dailyGoalRepository.save(any(DailyGoal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            DailyGoal result = analyticsService.setDailyGoal(userId, request);

            // Then
            assertEquals(150, result.getTargetMinutes());
            assertEquals(60, result.getCompletedMinutes()); // Should preserve completed minutes
        }

        @Test
        @DisplayName("Should update goal progress")
        void shouldUpdateGoalProgress() {
            // Given
            DailyGoal goal = new DailyGoal();
            goal.setUserId(userId);
            goal.setTargetMinutes(120);
            goal.setCompletedMinutes(60);
            goal.setDate(LocalDate.now());

            when(dailyGoalRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(Optional.of(goal));
            when(dailyGoalRepository.save(any(DailyGoal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateGoalProgress(userId, 30);

            // Then
            verify(dailyGoalRepository).save(argThat(g -> g.getCompletedMinutes() == 90));
        }

        @Test
        @DisplayName("Should validate goal target minutes")
        void shouldValidateGoalTargetMinutes() {
            // Given
            DailyGoalRequest request = new DailyGoalRequest();
            request.setTargetMinutes(-10); // Invalid negative value
            request.setDate(LocalDate.now());

            // When & Then
            assertThrows(BadRequestException.class,
                () -> analyticsService.setDailyGoal(userId, request));
        }
    }

    @Nested
    @DisplayName("Hive Analytics Tests")
    class HiveAnalyticsTests {

        @Test
        @DisplayName("Should get hive analytics summary")
        void shouldGetHiveAnalyticsSummary() {
            // Given
            when(hiveRepository.existsById(hiveId)).thenReturn(true);

            HiveAnalytics analytics = new HiveAnalytics();
            analytics.setHiveId(hiveId);
            analytics.setDate(LocalDate.now());
            analytics.setActiveUsers(5);
            analytics.setTotalFocusTime(300);
            analytics.setAverageProductivityScore(85);

            when(hiveAnalyticsRepository.findByHiveIdAndDateBetween(eq(hiveId), any(), any()))
                .thenReturn(List.of(analytics));

            // When
            HiveAnalyticsResponse response = analyticsService.getHiveAnalytics(hiveId);

            // Then
            assertNotNull(response);
            assertEquals(hiveId, response.getHiveId());
            assertEquals(5, response.getTotalActiveUsers());
            assertEquals(300, response.getTotalFocusTime());
            assertEquals(85, response.getAverageProductivityScore());
        }

        @Test
        @DisplayName("Should update hive analytics")
        void shouldUpdateHiveAnalytics() {
            // Given
            when(hiveAnalyticsRepository.findByHiveIdAndDate(hiveId, LocalDate.now()))
                .thenReturn(Optional.empty());
            when(hiveAnalyticsRepository.save(any(HiveAnalytics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateHiveAnalytics(hiveId, userId, 60, 85);

            // Then
            verify(hiveAnalyticsRepository).save(any(HiveAnalytics.class));
        }

        @Test
        @DisplayName("Should throw exception for non-existent hive")
        void shouldThrowExceptionForNonExistentHive() {
            // Given
            when(hiveRepository.existsById(hiveId)).thenReturn(false);

            // When & Then
            assertThrows(ResourceNotFoundException.class,
                () -> analyticsService.getHiveAnalytics(hiveId));
        }
    }

    @Nested
    @DisplayName("Report Generation Tests")
    class ReportGenerationTests {

        @Test
        @DisplayName("Should generate daily report")
        void shouldGenerateDailyReport() {
            // Given
            LocalDate date = LocalDate.now();
            when(userRepository.existsById(userId)).thenReturn(true);

            ProductivityMetric metric = new ProductivityMetric();
            metric.setUserId(userId);
            metric.setDate(date);
            metric.setFocusMinutes(120);
            metric.setCompletedSessions(3);
            metric.setProductivityScore(85);

            when(productivityMetricRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .thenReturn(List.of(metric));

            // When
            DetailedReportResponse response = analyticsService.getUserDetailedReport(userId, ReportPeriod.DAILY);

            // Then
            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals(ReportPeriod.DAILY, response.getPeriod());
            assertFalse(response.getDailyMetrics().isEmpty());
        }

        @Test
        @DisplayName("Should generate weekly report")
        void shouldGenerateWeeklyReport() {
            // Given
            when(userRepository.existsById(userId)).thenReturn(true);

            List<ProductivityMetric> metrics = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                ProductivityMetric metric = new ProductivityMetric();
                metric.setUserId(userId);
                metric.setDate(LocalDate.now().minusDays(i));
                metric.setFocusMinutes(60 + (i * 10));
                metric.setCompletedSessions(2 + i);
                metric.setProductivityScore(70 + (i * 2));
                metrics.add(metric);
            }

            when(productivityMetricRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .thenReturn(metrics);

            // When
            DetailedReportResponse response = analyticsService.getUserDetailedReport(userId, ReportPeriod.WEEKLY);

            // Then
            assertNotNull(response);
            assertEquals(ReportPeriod.WEEKLY, response.getPeriod());
            assertEquals(7, response.getDailyMetrics().size());
        }

        @Test
        @DisplayName("Should generate monthly report")
        void shouldGenerateMonthlyReport() {
            // Given
            when(userRepository.existsById(userId)).thenReturn(true);

            List<ProductivityMetric> metrics = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                ProductivityMetric metric = new ProductivityMetric();
                metric.setUserId(userId);
                metric.setDate(LocalDate.now().minusDays(i));
                metric.setFocusMinutes(60);
                metric.setCompletedSessions(2);
                metric.setProductivityScore(80);
                metrics.add(metric);
            }

            when(productivityMetricRepository.findByUserIdAndDateBetween(eq(userId), any(), any()))
                .thenReturn(metrics);

            // When
            DetailedReportResponse response = analyticsService.getUserDetailedReport(userId, ReportPeriod.MONTHLY);

            // Then
            assertNotNull(response);
            assertEquals(ReportPeriod.MONTHLY, response.getPeriod());
            assertEquals(30, response.getDailyMetrics().size());
        }
    }

    @Nested
    @DisplayName("Data Export Tests")
    class DataExportTests {

        @Test
        @DisplayName("Should export user analytics data")
        void shouldExportUserAnalyticsData() {
            // Given
            when(userRepository.existsById(userId)).thenReturn(true);

            // Mock data
            List<ProductivityMetric> metrics = List.of(new ProductivityMetric());
            List<UserStreak> streaks = List.of(new UserStreak());
            List<AchievementProgress> achievements = List.of(new AchievementProgress());
            List<DailyGoal> goals = List.of(new DailyGoal());

            when(productivityMetricRepository.findByUserId(userId)).thenReturn(metrics);
            when(userStreakRepository.findByUserId(userId)).thenReturn(Optional.of(streaks.get(0)));
            when(achievementProgressRepository.findByUserId(userId)).thenReturn(achievements);
            when(dailyGoalRepository.findByUserId(userId)).thenReturn(goals);

            // When
            Map<String, Object> exportData = analyticsService.exportUserAnalyticsData(userId);

            // Then
            assertNotNull(exportData);
            assertTrue(exportData.containsKey("productivityMetrics"));
            assertTrue(exportData.containsKey("streaks"));
            assertTrue(exportData.containsKey("achievements"));
            assertTrue(exportData.containsKey("goals"));
            assertEquals(userId, exportData.get("userId"));
        }

        @Test
        @DisplayName("Should throw exception when exporting data for non-existent user")
        void shouldThrowExceptionWhenExportingDataForNonExistentUser() {
            // Given
            when(userRepository.existsById(userId)).thenReturn(false);

            // When & Then
            assertThrows(ResourceNotFoundException.class,
                () -> analyticsService.exportUserAnalyticsData(userId));
        }
    }

    @Nested
    @DisplayName("Integration and Event Processing Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should process focus session completion event")
        void shouldProcessFocusSessionCompletionEvent() {
            // Given
            FocusSession session = new FocusSession();
            session.setUserId(userId);
            session.setHiveId(hiveId);
            session.setDurationMinutes(60);
            session.setStatus(FocusSession.SessionStatus.COMPLETED);
            session.setTabSwitches(1);
            session.setCompletedAt(LocalDateTime.now());

            when(productivityMetricRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(Optional.empty());
            when(userStreakRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(hiveAnalyticsRepository.findByHiveIdAndDate(hiveId, LocalDate.now()))
                .thenReturn(Optional.empty());
            when(dailyGoalRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(Optional.empty());

            // When
            analyticsService.processFocusSessionCompletion(session);

            // Then
            verify(productivityMetricRepository).save(any(ProductivityMetric.class));
            verify(userStreakRepository).save(any(UserStreak.class));
            verify(hiveAnalyticsRepository).save(any(HiveAnalytics.class));
        }

        @Test
        @DisplayName("Should handle session completion with existing data")
        void shouldHandleSessionCompletionWithExistingData() {
            // Given
            FocusSession session = new FocusSession();
            session.setUserId(userId);
            session.setHiveId(hiveId);
            session.setDurationMinutes(60);
            session.setStatus(FocusSession.SessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());

            ProductivityMetric existingMetric = new ProductivityMetric();
            existingMetric.setUserId(userId);
            existingMetric.setFocusMinutes(120);
            existingMetric.setCompletedSessions(2);

            when(productivityMetricRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(Optional.of(existingMetric));
            when(productivityMetricRepository.save(any(ProductivityMetric.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            analyticsService.updateProductivityMetrics(session);

            // Then
            verify(productivityMetricRepository).save(argThat(metric ->
                metric.getFocusMinutes() == 180 && metric.getCompletedSessions() == 3));
        }
    }
}