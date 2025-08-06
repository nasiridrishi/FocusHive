package com.focushive.timer.integration;

import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.entity.HiveTimer;
import com.focushive.timer.repository.FocusSessionRepository;
import com.focushive.timer.repository.HiveTimerRepository;
import com.focushive.timer.repository.PomodoroSettingsRepository;
import com.focushive.timer.repository.ProductivityStatsRepository;
import com.focushive.timer.service.TimerService;
import com.focushive.test.TestApplication;
import com.focushive.test.UnifiedTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@Import(UnifiedTestConfig.class)
@ActiveProfiles("test")
@Transactional
class TimerIntegrationTest {
    
    @Autowired
    private TimerService timerService;
    
    @Autowired
    private FocusSessionRepository focusSessionRepository;
    
    @Autowired
    private ProductivityStatsRepository productivityStatsRepository;
    
    @Autowired
    private PomodoroSettingsRepository pomodoroSettingsRepository;
    
    @Autowired
    private HiveTimerRepository hiveTimerRepository;
    
    private String userId;
    private String hiveId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        hiveId = UUID.randomUUID().toString();
        
        // Clean up any existing data
        focusSessionRepository.deleteAll();
        productivityStatsRepository.deleteAll();
        pomodoroSettingsRepository.deleteAll();
        hiveTimerRepository.deleteAll();
    }
    
    @Test
    void testCompleteSessionWorkflow() {
        // Start a session
        StartSessionRequest startRequest = StartSessionRequest.builder()
                .hiveId(hiveId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .notes("Working on timer integration test")
                .build();
        
        FocusSessionDto startedSession = timerService.startSession(userId, startRequest);
        
        assertThat(startedSession).isNotNull();
        assertThat(startedSession.getUserId()).isEqualTo(userId);
        assertThat(startedSession.getHiveId()).isEqualTo(hiveId);
        assertThat(startedSession.getSessionType()).isEqualTo(FocusSession.SessionType.WORK);
        assertThat(startedSession.getDurationMinutes()).isEqualTo(25);
        assertThat(startedSession.getCompleted()).isFalse();
        
        // Verify session is stored in database
        assertThat(focusSessionRepository.findById(startedSession.getId())).isPresent();
        
        // Get current session
        FocusSessionDto currentSession = timerService.getCurrentSession(userId);
        assertThat(currentSession).isNotNull();
        assertThat(currentSession.getId()).isEqualTo(startedSession.getId());
        
        // End the session
        FocusSessionDto endedSession = timerService.endSession(userId, startedSession.getId());
        
        assertThat(endedSession).isNotNull();
        assertThat(endedSession.getCompleted()).isTrue();
        assertThat(endedSession.getEndTime()).isNotNull();
        assertThat(endedSession.getActualDurationMinutes()).isNotNull();
        
        // Verify no current session after ending
        assertThat(timerService.getCurrentSession(userId)).isNull();
        
        // Check daily stats were updated
        ProductivityStatsDto dailyStats = timerService.getDailyStats(userId, LocalDate.now());
        assertThat(dailyStats).isNotNull();
        assertThat(dailyStats.getSessionsCompleted()).isEqualTo(1);
        assertThat(dailyStats.getSessionsStarted()).isEqualTo(1);
        assertThat(dailyStats.getTotalFocusMinutes()).isGreaterThan(0);
    }
    
    @Test
    void testPomodoroSettings() {
        // Get default settings
        PomodoroSettingsDto defaultSettings = timerService.getPomodoroSettings(userId);
        assertThat(defaultSettings).isNotNull();
        assertThat(defaultSettings.getWorkDurationMinutes()).isEqualTo(25);
        assertThat(defaultSettings.getShortBreakMinutes()).isEqualTo(5);
        assertThat(defaultSettings.getLongBreakMinutes()).isEqualTo(15);
        assertThat(defaultSettings.getSessionsUntilLongBreak()).isEqualTo(4);
        
        // Update settings
        PomodoroSettingsDto updateRequest = PomodoroSettingsDto.builder()
                .workDurationMinutes(30)
                .shortBreakMinutes(10)
                .longBreakMinutes(20)
                .sessionsUntilLongBreak(3)
                .autoStartBreaks(true)
                .autoStartWork(false)
                .notificationEnabled(true)
                .soundEnabled(false)
                .build();
        
        PomodoroSettingsDto updatedSettings = timerService.updatePomodoroSettings(userId, updateRequest);
        
        assertThat(updatedSettings).isNotNull();
        assertThat(updatedSettings.getWorkDurationMinutes()).isEqualTo(30);
        assertThat(updatedSettings.getShortBreakMinutes()).isEqualTo(10);
        assertThat(updatedSettings.getAutoStartBreaks()).isTrue();
        assertThat(updatedSettings.getSoundEnabled()).isFalse();
        
        // Verify settings are persisted
        PomodoroSettingsDto retrievedSettings = timerService.getPomodoroSettings(userId);
        assertThat(retrievedSettings.getWorkDurationMinutes()).isEqualTo(30);
    }
    
    @Test
    void testSessionHistory() {
        // Create multiple sessions
        for (int i = 0; i < 5; i++) {
            StartSessionRequest request = StartSessionRequest.builder()
                    .sessionType(i % 2 == 0 ? FocusSession.SessionType.WORK : FocusSession.SessionType.BREAK)
                    .durationMinutes(i % 2 == 0 ? 25 : 5)
                    .build();
            
            FocusSessionDto session = timerService.startSession(userId, request);
            timerService.endSession(userId, session.getId());
        }
        
        // Get session history
        List<FocusSessionDto> history = timerService.getSessionHistory(userId, 0, 10);
        
        assertThat(history).hasSize(5);
        assertThat(history).allMatch(s -> s.getCompleted());
        
        // Check pagination
        List<FocusSessionDto> firstPage = timerService.getSessionHistory(userId, 0, 2);
        assertThat(firstPage).hasSize(2);
        
        List<FocusSessionDto> secondPage = timerService.getSessionHistory(userId, 1, 2);
        assertThat(secondPage).hasSize(2);
        assertThat(secondPage.get(0).getId()).isNotEqualTo(firstPage.get(0).getId());
    }
    
    @Test
    void testProductivityStats() {
        // Create sessions for multiple days
        LocalDate today = LocalDate.now();
        
        // Today's sessions
        createAndCompleteSession(userId, FocusSession.SessionType.WORK, 25);
        createAndCompleteSession(userId, FocusSession.SessionType.WORK, 25);
        createAndCompleteSession(userId, FocusSession.SessionType.BREAK, 5);
        
        // Check daily stats
        ProductivityStatsDto todayStats = timerService.getDailyStats(userId, today);
        assertThat(todayStats.getSessionsCompleted()).isEqualTo(3);
        assertThat(todayStats.getTotalFocusMinutes()).isEqualTo(50);
        assertThat(todayStats.getTotalBreakMinutes()).isEqualTo(5);
        assertThat(todayStats.getFocusRatio()).isCloseTo(90.9, within(0.1));
        
        // Check weekly stats
        List<ProductivityStatsDto> weeklyStats = timerService.getWeeklyStats(userId, today.minusDays(6));
        assertThat(weeklyStats).isNotEmpty();
        
        // Check current streak
        Integer streak = timerService.getCurrentStreak(userId);
        assertThat(streak).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testConcurrentSessionsNotAllowed() {
        // Start first session
        StartSessionRequest request1 = StartSessionRequest.builder()
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .build();
        
        timerService.startSession(userId, request1);
        
        // Try to start second session
        StartSessionRequest request2 = StartSessionRequest.builder()
                .sessionType(FocusSession.SessionType.STUDY)
                .durationMinutes(45)
                .build();
        
        assertThatThrownBy(() -> timerService.startSession(userId, request2))
                .hasMessageContaining("already have an active session");
    }
    
    // Helper method
    private void createAndCompleteSession(String userId, FocusSession.SessionType type, int duration) {
        StartSessionRequest request = StartSessionRequest.builder()
                .sessionType(type)
                .durationMinutes(duration)
                .build();
        
        FocusSessionDto session = timerService.startSession(userId, request);
        timerService.endSession(userId, session.getId());
    }
}