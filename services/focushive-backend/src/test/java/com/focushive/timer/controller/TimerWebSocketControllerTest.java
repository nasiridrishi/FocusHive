package com.focushive.timer.controller;

import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.entity.HiveTimer;
import com.focushive.timer.service.TimerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimerWebSocketControllerTest {
    
    @Mock
    private TimerService timerService;
    
    @Mock
    private Principal principal;
    
    @Mock
    private SimpMessageHeaderAccessor headerAccessor;
    
    @InjectMocks
    private TimerWebSocketController controller;
    
    private String userId;
    private String hiveId;
    private String username;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        hiveId = UUID.randomUUID().toString();
        username = "testuser";
        
        when(principal.getName()).thenReturn(userId);
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("username", username);
        when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttrs);
    }
    
    @Test
    void startHiveTimer_Success() {
        // Given
        TimerStateDto timerRequest = TimerStateDto.builder()
                .timerType(HiveTimer.TimerType.POMODORO)
                .durationMinutes(25)
                .build();
        
        TimerStateDto expectedResponse = TimerStateDto.builder()
                .timerId(UUID.randomUUID().toString())
                .hiveId(hiveId)
                .timerType(HiveTimer.TimerType.POMODORO)
                .durationMinutes(25)
                .remainingSeconds(1500)
                .isRunning(true)
                .startedBy(userId)
                .startedByUsername(username)
                .startedAt(LocalDateTime.now())
                .build();
        
        when(timerService.startHiveTimer(hiveId, userId, timerRequest)).thenReturn(expectedResponse);
        
        // When
        TimerStateDto result = controller.startHiveTimer(hiveId, timerRequest, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(timerService).startHiveTimer(hiveId, userId, timerRequest);
    }
    
    @Test
    void pauseHiveTimer_Success() {
        // Given
        TimerStateDto expectedResponse = TimerStateDto.builder()
                .timerId(UUID.randomUUID().toString())
                .hiveId(hiveId)
                .isRunning(false)
                .pausedAt(LocalDateTime.now())
                .build();
        
        when(timerService.pauseHiveTimer(hiveId, userId)).thenReturn(expectedResponse);
        
        // When
        TimerStateDto result = controller.pauseHiveTimer(hiveId, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(timerService).pauseHiveTimer(hiveId, userId);
    }
    
    @Test
    void resumeHiveTimer_Success() {
        // Given
        TimerStateDto expectedResponse = TimerStateDto.builder()
                .timerId(UUID.randomUUID().toString())
                .hiveId(hiveId)
                .isRunning(true)
                .pausedAt(null)
                .build();
        
        when(timerService.resumeHiveTimer(hiveId, userId)).thenReturn(expectedResponse);
        
        // When
        TimerStateDto result = controller.resumeHiveTimer(hiveId, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(timerService).resumeHiveTimer(hiveId, userId);
    }
    
    @Test
    void stopHiveTimer_Success() {
        // Given
        TimerStateDto expectedResponse = TimerStateDto.builder()
                .timerId(UUID.randomUUID().toString())
                .hiveId(hiveId)
                .isRunning(false)
                .remainingSeconds(0)
                .build();
        
        when(timerService.stopHiveTimer(hiveId, userId)).thenReturn(expectedResponse);
        
        // When
        TimerStateDto result = controller.stopHiveTimer(hiveId, principal);
        
        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(timerService).stopHiveTimer(hiveId, userId);
    }
    
    @Test
    void subscribeToTimer_WithActiveTimer_ReturnsTimerState() {
        // Given
        TimerStateDto activeTimer = TimerStateDto.builder()
                .timerId(UUID.randomUUID().toString())
                .hiveId(hiveId)
                .isRunning(true)
                .remainingSeconds(600)
                .build();
        
        when(timerService.getHiveTimerState(hiveId)).thenReturn(activeTimer);
        
        // When
        TimerStateDto result = controller.subscribeToTimer(hiveId);
        
        // Then
        assertThat(result).isEqualTo(activeTimer);
        verify(timerService).getHiveTimerState(hiveId);
    }
    
    @Test
    void subscribeToTimer_NoActiveTimer_ReturnsNull() {
        // Given
        when(timerService.getHiveTimerState(hiveId)).thenReturn(null);
        
        // When
        TimerStateDto result = controller.subscribeToTimer(hiveId);
        
        // Then
        assertThat(result).isNull();
        verify(timerService).getHiveTimerState(hiveId);
    }
    
    @Test
    void broadcastSessionStart_Success() {
        // Given
        StartSessionRequest request = StartSessionRequest.builder()
                .hiveId(hiveId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(45)
                .notes("Deep work session")
                .build();
        
        String sessionId = UUID.randomUUID().toString();
        FocusSessionDto sessionDto = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .hiveId(hiveId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(45)
                .startTime(LocalDateTime.now())
                .completed(false)
                .build();
        
        when(timerService.startSession(userId, request)).thenReturn(sessionDto);
        
        // When
        SessionBroadcast result = controller.broadcastSessionStart(hiveId, request, principal, headerAccessor);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getAction()).isEqualTo("started");
        assertThat(result.getSessionType()).isEqualTo("WORK");
        assertThat(result.getDurationMinutes()).isEqualTo(45);
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        
        verify(timerService).startSession(userId, request);
    }
    
    @Test
    void broadcastSessionEnd_Success() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        EndSessionRequest request = EndSessionRequest.builder()
                .sessionId(sessionId)
                .build();
        
        FocusSessionDto sessionDto = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .hiveId(hiveId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(45)
                .actualDurationMinutes(42)
                .completed(true)
                .build();
        
        when(timerService.endSession(userId, sessionId)).thenReturn(sessionDto);
        
        // When
        SessionBroadcast result = controller.broadcastSessionEnd(hiveId, request, principal, headerAccessor);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getAction()).isEqualTo("ended");
        assertThat(result.getSessionType()).isEqualTo("WORK");
        assertThat(result.getActualDurationMinutes()).isEqualTo(42);
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getCompleted()).isTrue();
        
        verify(timerService).endSession(userId, sessionId);
    }
}