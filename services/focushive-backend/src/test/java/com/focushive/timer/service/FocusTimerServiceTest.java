package com.focushive.timer.service;

import com.focushive.hive.entity.Hive;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.entity.FocusSession.SessionStatus;
import com.focushive.timer.entity.FocusSession.SessionType;
import com.focushive.timer.entity.TimerTemplate;
import com.focushive.timer.repository.FocusSessionRepository;
import com.focushive.timer.repository.TimerTemplateRepository;
import com.focushive.websocket.service.PresenceTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FocusTimerService.
 * Tests timer management, synchronization, and session tracking.
 * THIS WILL FAIL initially as FocusTimerService doesn't exist yet.
 */
@ExtendWith(MockitoExtension.class)
class FocusTimerServiceTest {

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @Mock
    private TimerTemplateRepository timerTemplateRepository;

    @Mock
    private HiveRepository hiveRepository;

    @Mock
    private PresenceTrackingService presenceTrackingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ScheduledExecutorService scheduledExecutor;

    private FocusTimerService focusTimerService;

    @BeforeEach
    void setUp() {
        focusTimerService = new FocusTimerServiceImpl(
            focusSessionRepository,
            timerTemplateRepository,
            hiveRepository,
            presenceTrackingService,
            eventPublisher,
            scheduledExecutor
        );
    }

    @Test
    void shouldStartFocusSession() {
        // Given
        String userId = "user123";
        String hiveId = "hive456";
        StartTimerRequest request = StartTimerRequest.builder()
            .userId(userId)
            .hiveId(hiveId)
            .durationMinutes(25)
            .sessionType(SessionType.FOCUS)
            .title("Deep Work Session")
            .build();

        Hive hive = new Hive();
        hive.setId(hiveId);
        hive.setName("Study Hive");

        when(hiveRepository.findById(hiveId)).thenReturn(Optional.of(hive));
        when(focusSessionRepository.save(any(FocusSession.class)))
            .thenAnswer(invocation -> {
                FocusSession session = invocation.getArgument(0);
                session.setId(UUID.randomUUID().toString());
                return session;
            });

        // When
        FocusSessionResponse response = focusTimerService.startSession(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getHiveId()).isEqualTo(hiveId);
        assertThat(response.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(response.getDurationMinutes()).isEqualTo(25);
        assertThat(response.getTitle()).isEqualTo("Deep Work Session");

        verify(presenceTrackingService).startFocusSession(
            eq(Long.parseLong(userId)),
            eq(Long.parseLong(hiveId)),
            eq(25)
        );
        verify(eventPublisher).publishEvent(any(FocusSessionStartedEvent.class));
    }

    @Test
    void shouldPauseFocusSession() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String userId = "user123";

        FocusSession activeSession = FocusSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .startedAt(LocalDateTime.now().minusMinutes(10))
            .durationMinutes(25)
            .build();

        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(activeSession));
        when(focusSessionRepository.save(any(FocusSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        FocusSessionResponse response = focusTimerService.pauseSession(sessionId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SessionStatus.PAUSED);
        assertThat(response.getPausedAt()).isNotNull();

        ArgumentCaptor<FocusSession> sessionCaptor = ArgumentCaptor.forClass(FocusSession.class);
        verify(focusSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(SessionStatus.PAUSED);
        verify(eventPublisher).publishEvent(any(FocusSessionPausedEvent.class));
    }

    @Test
    void shouldResumeFocusSession() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String userId = "user123";
        LocalDateTime pausedAt = LocalDateTime.now().minusMinutes(5);

        FocusSession pausedSession = FocusSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.PAUSED)
            .startedAt(LocalDateTime.now().minusMinutes(15))
            .pausedAt(pausedAt)
            .durationMinutes(25)
            .totalPausedDuration(Duration.ZERO)
            .build();

        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(pausedSession));
        when(focusSessionRepository.save(any(FocusSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        FocusSessionResponse response = focusTimerService.resumeSession(sessionId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(response.getTotalPausedMinutes()).isEqualTo(5);

        ArgumentCaptor<FocusSession> sessionCaptor = ArgumentCaptor.forClass(FocusSession.class);
        verify(focusSessionRepository).save(sessionCaptor.capture());
        FocusSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(savedSession.getTotalPausedDuration()).isNotNull();
        verify(eventPublisher).publishEvent(any(FocusSessionResumedEvent.class));
    }

    @Test
    void shouldCompleteFocusSession() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String userId = "user123";

        FocusSession activeSession = FocusSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .startedAt(LocalDateTime.now().minusMinutes(25))
            .durationMinutes(25)
            .hiveId("hive456")
            .build();

        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(activeSession));
        when(focusSessionRepository.save(any(FocusSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        FocusSessionResponse response = focusTimerService.completeSession(sessionId, userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
        assertThat(response.getProductivityScore()).isGreaterThan(0);

        verify(eventPublisher).publishEvent(any(FocusSessionCompletedEvent.class));
    }

    @Test
    void shouldCancelFocusSession() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String userId = "user123";

        FocusSession activeSession = FocusSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .startedAt(LocalDateTime.now().minusMinutes(10))
            .durationMinutes(25)
            .build();

        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(activeSession));
        when(focusSessionRepository.save(any(FocusSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        focusTimerService.cancelSession(sessionId, userId);

        // Then
        ArgumentCaptor<FocusSession> sessionCaptor = ArgumentCaptor.forClass(FocusSession.class);
        verify(focusSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(SessionStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(FocusSessionCancelledEvent.class));
    }

    @Test
    void shouldGetActiveSessionsForHive() {
        // Given
        String hiveId = "hive456";
        List<FocusSession> activeSessions = Arrays.asList(
            FocusSession.builder()
                .id("session1")
                .userId("user1")
                .hiveId(hiveId)
                .status(SessionStatus.ACTIVE)
                .build(),
            FocusSession.builder()
                .id("session2")
                .userId("user2")
                .hiveId(hiveId)
                .status(SessionStatus.ACTIVE)
                .build()
        );

        when(focusSessionRepository.findByHiveIdAndStatus(hiveId, SessionStatus.ACTIVE))
            .thenReturn(activeSessions);

        // When
        List<FocusSessionResponse> response = focusTimerService.getActiveSessionsForHive(hiveId);

        // Then
        assertThat(response).hasSize(2);
        assertThat(response).extracting(FocusSessionResponse::getUserId)
            .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    void shouldSynchronizeTimerAcrossDevices() {
        // Given
        String userId = "user123";
        String sessionId = UUID.randomUUID().toString();

        FocusSession activeSession = FocusSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .startedAt(LocalDateTime.now().minusMinutes(10))
            .durationMinutes(25)
            .build();

        when(focusSessionRepository.findActiveSessionByUserId(userId))
            .thenReturn(Optional.of(activeSession));

        // When
        TimerSyncResponse syncResponse = focusTimerService.synchronizeTimer(userId);

        // Then
        assertThat(syncResponse).isNotNull();
        assertThat(syncResponse.getSessionId()).isEqualTo(sessionId);
        assertThat(syncResponse.getRemainingMinutes()).isEqualTo(15);
        assertThat(syncResponse.getElapsedMinutes()).isEqualTo(10);
        assertThat(syncResponse.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void shouldScheduleTimerReminders() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String userId = "user123";

        FocusSession session = FocusSession.builder()
            .id(sessionId)
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .startedAt(LocalDateTime.now())
            .durationMinutes(25)
            .reminderEnabled(true)
            .reminderMinutesBefore(5)
            .build();

        // When
        focusTimerService.scheduleReminder(session);

        // Then
        verify(scheduledExecutor).schedule(
            any(Runnable.class),
            eq(20L),
            eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void shouldAutoCompleteExpiredSessions() {
        // Given
        List<FocusSession> expiredSessions = Arrays.asList(
            FocusSession.builder()
                .id("expired1")
                .userId("user1")
                .status(SessionStatus.ACTIVE)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .durationMinutes(25)
                .build(),
            FocusSession.builder()
                .id("expired2")
                .userId("user2")
                .status(SessionStatus.ACTIVE)
                .startedAt(LocalDateTime.now().minusMinutes(35))
                .durationMinutes(30)
                .build()
        );

        when(focusSessionRepository.findExpiredActiveSessions(any(LocalDateTime.class)))
            .thenReturn(expiredSessions);
        when(focusSessionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int completedCount = focusTimerService.autoCompleteExpiredSessions();

        // Then
        assertThat(completedCount).isEqualTo(2);
        verify(focusSessionRepository).saveAll(argThat(sessions -> {
            return sessions.stream()
                .allMatch(s -> s.getStatus() == SessionStatus.COMPLETED);
        }));
        verify(eventPublisher, times(2)).publishEvent(any(FocusSessionCompletedEvent.class));
    }

    @Test
    void shouldCreateTimerTemplate() {
        // Given
        String userId = "user123";
        TimerTemplateRequest request = TimerTemplateRequest.builder()
            .name("Pomodoro")
            .focusDuration(25)
            .shortBreakDuration(5)
            .longBreakDuration(15)
            .sessionsBeforeLongBreak(4)
            .isDefault(true)
            .build();

        when(timerTemplateRepository.save(any(TimerTemplate.class)))
            .thenAnswer(invocation -> {
                TimerTemplate template = invocation.getArgument(0);
                template.setId(UUID.randomUUID().toString());
                return template;
            });

        // When
        TimerTemplateResponse response = focusTimerService.createTemplate(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Pomodoro");
        assertThat(response.getFocusDuration()).isEqualTo(25);
        assertThat(response.getShortBreakDuration()).isEqualTo(5);
        assertThat(response.getLongBreakDuration()).isEqualTo(15);
    }

    @Test
    void shouldGetUserStatistics() {
        // Given
        String userId = "user123";
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        List<FocusSession> sessions = Arrays.asList(
            FocusSession.builder()
                .userId(userId)
                .status(SessionStatus.COMPLETED)
                .durationMinutes(25)
                .completedAt(LocalDateTime.now().minusDays(1))
                .productivityScore(85)
                .build(),
            FocusSession.builder()
                .userId(userId)
                .status(SessionStatus.COMPLETED)
                .durationMinutes(30)
                .completedAt(LocalDateTime.now().minusDays(2))
                .productivityScore(90)
                .build()
        );

        when(focusSessionRepository.findByUserIdAndCompletedAtBetween(
            userId, startDate, endDate
        )).thenReturn(sessions);

        // When
        UserTimerStatistics stats = focusTimerService.getUserStatistics(userId, startDate, endDate);

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalSessions()).isEqualTo(2);
        assertThat(stats.getTotalFocusMinutes()).isEqualTo(55);
        assertThat(stats.getAverageProductivityScore()).isEqualTo(87.5);
        assertThat(stats.getCompletionRate()).isEqualTo(100);
    }

    @Test
    void shouldHandleSimultaneousSessionsForSameUser() {
        // Given
        String userId = "user123";
        StartTimerRequest request = StartTimerRequest.builder()
            .userId(userId)
            .hiveId("hive456")
            .durationMinutes(25)
            .sessionType(SessionType.FOCUS)
            .build();

        FocusSession existingActiveSession = FocusSession.builder()
            .id("existing-session")
            .userId(userId)
            .status(SessionStatus.ACTIVE)
            .build();

        when(focusSessionRepository.findActiveSessionByUserId(userId))
            .thenReturn(Optional.of(existingActiveSession));

        // When/Then
        assertThatThrownBy(() -> focusTimerService.startSession(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("User already has an active session");
    }

    @Test
    void shouldTrackSessionProductivity() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        ProductivityData data = ProductivityData.builder()
            .tabSwitches(5)
            .distractionMinutes(3)
            .focusBreaks(1)
            .notesCount(10)
            .build();

        FocusSession session = FocusSession.builder()
            .id(sessionId)
            .userId("user123")
            .status(SessionStatus.ACTIVE)
            .startedAt(LocalDateTime.now().minusMinutes(20))
            .durationMinutes(25)
            .build();

        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(focusSessionRepository.save(any(FocusSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        focusTimerService.updateProductivityData(sessionId, data);

        // Then
        ArgumentCaptor<FocusSession> sessionCaptor = ArgumentCaptor.forClass(FocusSession.class);
        verify(focusSessionRepository).save(sessionCaptor.capture());

        FocusSession updatedSession = sessionCaptor.getValue();
        assertThat(updatedSession.getTabSwitches()).isEqualTo(5);
        assertThat(updatedSession.getDistractionMinutes()).isEqualTo(3);
        assertThat(updatedSession.getProductivityScore()).isLessThan(100);
    }

    @Test
    void shouldIntegrateWithPresenceSystem() {
        // Given
        String userId = "user123";
        String hiveId = "hive456";
        StartTimerRequest request = StartTimerRequest.builder()
            .userId(userId)
            .hiveId(hiveId)
            .durationMinutes(25)
            .sessionType(SessionType.FOCUS)
            .build();

        when(hiveRepository.findById(hiveId)).thenReturn(Optional.of(new Hive()));
        when(focusSessionRepository.save(any(FocusSession.class)))
            .thenAnswer(invocation -> {
                FocusSession session = invocation.getArgument(0);
                session.setId(UUID.randomUUID().toString());
                return session;
            });

        // When
        focusTimerService.startSession(request);

        // Then
        verify(presenceTrackingService).startFocusSession(
            eq(Long.parseLong(userId)),
            eq(Long.parseLong(hiveId)),
            eq(25)
        );
    }
}