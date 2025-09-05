package com.focushive.timer.service.impl;

import com.focushive.common.exception.BadRequestException;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.timer.dto.*;
import com.focushive.timer.entity.*;
import com.focushive.timer.repository.*;
import com.focushive.user.dto.UserDto;
import com.focushive.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimerServiceImplTest {
    
    @Mock
    private FocusSessionRepository focusSessionRepository;
    
    @Mock
    private ProductivityStatsRepository productivityStatsRepository;
    
    @Mock
    private PomodoroSettingsRepository pomodoroSettingsRepository;
    
    @Mock
    private HiveTimerRepository hiveTimerRepository;
    
    @Mock
    private HiveMemberRepository hiveMemberRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @InjectMocks
    private TimerServiceImpl timerService;
    
    private String userId;
    private String hiveId;
    private String sessionId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        hiveId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
    }
    
    @Test
    void startSession_Success() {
        // Given
        StartSessionRequest request = StartSessionRequest.builder()
                .hiveId(hiveId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .notes("Working on UOL-31")
                .build();
        
        when(focusSessionRepository.findByUserIdAndCompletedFalse(userId)).thenReturn(Optional.empty());
        when(focusSessionRepository.save(any(FocusSession.class))).thenAnswer(i -> {
            FocusSession session = i.getArgument(0);
            session.setId(sessionId);
            session.setCreatedAt(LocalDateTime.now());
            return session;
        });
        when(productivityStatsRepository.findByUserIdAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(productivityStatsRepository.save(any(ProductivityStats.class))).thenAnswer(i -> i.getArgument(0));
        
        UserDto user = UserDto.builder()
                .id(userId)
                .username("testuser")
                .build();
        when(userService.getUserById(userId)).thenReturn(user);
        
        // When
        FocusSessionDto result = timerService.startSession(userId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(sessionId);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getHiveId()).isEqualTo(hiveId);
        assertThat(result.getSessionType()).isEqualTo(FocusSession.SessionType.WORK);
        assertThat(result.getDurationMinutes()).isEqualTo(25);
        assertThat(result.getNotes()).isEqualTo("Working on UOL-31");
        assertThat(result.getCompleted()).isFalse();
        
        verify(focusSessionRepository).save(any(FocusSession.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/hive/" + hiveId + "/sessions"), any(Object.class));
    }
    
    @Test
    void startSession_WithExistingActiveSession_ThrowsBadRequest() {
        // Given
        StartSessionRequest request = StartSessionRequest.builder()
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .build();
        
        FocusSession existingSession = new FocusSession();
        when(focusSessionRepository.findByUserIdAndCompletedFalse(userId))
                .thenReturn(Optional.of(existingSession));
        
        // When/Then
        assertThatThrownBy(() -> timerService.startSession(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You already have an active session. Please end it first.");
    }
    
    @Test
    void endSession_Success() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(20);
        FocusSession session = FocusSession.builder()
                .userId(userId)
                .hiveId(hiveId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .startTime(startTime)
                .completed(false)
                .interruptions(0)
                .build();
        session.setId(sessionId);
        
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(focusSessionRepository.save(any(FocusSession.class))).thenAnswer(i -> i.getArgument(0));
        when(productivityStatsRepository.findByUserIdAndDate(eq(userId), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(productivityStatsRepository.save(any(ProductivityStats.class))).thenAnswer(i -> i.getArgument(0));
        
        UserDto user = UserDto.builder()
                .id(userId)
                .username("testuser")
                .build();
        when(userService.getUserById(userId)).thenReturn(user);
        
        // When
        FocusSessionDto result = timerService.endSession(userId, sessionId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompleted()).isTrue();
        assertThat(result.getActualDurationMinutes()).isEqualTo(20);
        assertThat(result.getEndTime()).isNotNull();
        
        verify(focusSessionRepository).save(argThat(s -> 
            s.getCompleted() && s.getActualDurationMinutes() == 20
        ));
    }
    
    @Test
    void endSession_NotOwner_ThrowsForbidden() {
        // Given
        FocusSession session = FocusSession.builder()
                .userId("other-user-id")
                .completed(false)
                .build();
        session.setId(sessionId);
        
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        
        // When/Then
        assertThatThrownBy(() -> timerService.endSession(userId, sessionId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You can only end your own sessions");
    }
    
    @Test
    void getDailyStats_ExistingStats_ReturnsStats() {
        // Given
        LocalDate date = LocalDate.now();
        ProductivityStats stats = ProductivityStats.builder()
                .userId(userId)
                .date(date)
                .totalFocusMinutes(120)
                .totalBreakMinutes(30)
                .sessionsCompleted(4)
                .sessionsStarted(5)
                .longestStreakMinutes(45)
                .dailyGoalMinutes(480)
                .build();
        stats.setId(UUID.randomUUID().toString());
        
        when(productivityStatsRepository.findByUserIdAndDate(userId, date))
                .thenReturn(Optional.of(stats));
        
        // When
        ProductivityStatsDto result = timerService.getDailyStats(userId, date);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalFocusMinutes()).isEqualTo(120);
        assertThat(result.getTotalBreakMinutes()).isEqualTo(30);
        assertThat(result.getSessionsCompleted()).isEqualTo(4);
        assertThat(result.getTotalMinutes()).isEqualTo(150);
        assertThat(result.getFocusRatio()).isEqualTo(80.0);
        assertThat(result.getAverageSessionLength()).isEqualTo(30.0);
    }
    
    @Test
    void getDailyStats_NoStats_ReturnsEmptyStats() {
        // Given
        LocalDate date = LocalDate.now();
        when(productivityStatsRepository.findByUserIdAndDate(userId, date))
                .thenReturn(Optional.empty());
        
        // When
        ProductivityStatsDto result = timerService.getDailyStats(userId, date);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalFocusMinutes()).isEqualTo(0);
        assertThat(result.getTotalBreakMinutes()).isEqualTo(0);
        assertThat(result.getSessionsCompleted()).isEqualTo(0);
        assertThat(result.getDailyGoalMinutes()).isEqualTo(480);
    }
    
    @Test
    void startHiveTimer_Success() {
        // Given
        TimerStateDto timerRequest = TimerStateDto.builder()
                .timerType(HiveTimer.TimerType.POMODORO)
                .durationMinutes(25)
                .build();
        
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
        when(hiveTimerRepository.existsByHiveIdAndIsRunningTrue(hiveId)).thenReturn(false);
        when(hiveTimerRepository.save(any(HiveTimer.class))).thenAnswer(i -> {
            HiveTimer timer = i.getArgument(0);
            timer.setId(UUID.randomUUID().toString());
            return timer;
        });
        
        UserDto user = UserDto.builder()
                .id(userId)
                .username("testuser")
                .build();
        when(userService.getUserById(userId)).thenReturn(user);
        
        // When
        TimerStateDto result = timerService.startHiveTimer(hiveId, userId, timerRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTimerType()).isEqualTo(HiveTimer.TimerType.POMODORO);
        assertThat(result.getDurationMinutes()).isEqualTo(25);
        assertThat(result.getRemainingSeconds()).isEqualTo(1500); // 25 * 60
        assertThat(result.getIsRunning()).isTrue();
        assertThat(result.getStartedBy()).isEqualTo(userId);
        
        verify(messagingTemplate).convertAndSend(eq("/topic/hive/" + hiveId + "/timer"), any(TimerStateDto.class));
    }
    
    @Test
    void startHiveTimer_NotMember_ThrowsForbidden() {
        // Given
        TimerStateDto timerRequest = TimerStateDto.builder()
                .timerType(HiveTimer.TimerType.POMODORO)
                .durationMinutes(25)
                .build();
        
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> timerService.startHiveTimer(hiveId, userId, timerRequest))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You must be a member of the hive to control timers");
    }
    
    @Test
    void updatePomodoroSettings_CreatesNewSettings() {
        // Given
        PomodoroSettingsDto settings = PomodoroSettingsDto.builder()
                .workDurationMinutes(30)
                .shortBreakMinutes(10)
                .longBreakMinutes(20)
                .sessionsUntilLongBreak(3)
                .autoStartBreaks(true)
                .autoStartWork(false)
                .notificationEnabled(true)
                .soundEnabled(false)
                .build();
        
        when(pomodoroSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pomodoroSettingsRepository.save(any(PomodoroSettings.class))).thenAnswer(i -> {
            PomodoroSettings entity = i.getArgument(0);
            entity.setId(UUID.randomUUID().toString());
            return entity;
        });
        
        // When
        PomodoroSettingsDto result = timerService.updatePomodoroSettings(userId, settings);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getWorkDurationMinutes()).isEqualTo(30);
        assertThat(result.getShortBreakMinutes()).isEqualTo(10);
        assertThat(result.getAutoStartBreaks()).isTrue();
        assertThat(result.getSoundEnabled()).isFalse();
        
        verify(pomodoroSettingsRepository).save(argThat(p -> 
            p.getUserId().equals(userId) && 
            p.getWorkDurationMinutes() == 30
        ));
    }
}