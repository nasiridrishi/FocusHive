package com.focushive.timer.controller;

import com.focushive.timer.dto.*;
import com.focushive.timer.entity.FocusSession;
import com.focushive.timer.service.TimerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * Unit test for TimerController using Mockito to test timer operations
 * without requiring full Spring context.
 */
@ExtendWith(MockitoExtension.class)
class TimerControllerUnitTest {

    @Mock
    private TimerService timerService;

    @InjectMocks
    private TimerController timerController;

    private UserDetails mockUserDetails;
    private String userId;
    private String sessionId;

    @BeforeEach
    void setUp() {
        userId = "test-user";
        sessionId = UUID.randomUUID().toString();
        
        mockUserDetails = User.builder()
            .username(userId)
            .password("password")
            .authorities(Collections.emptyList())
            .build();
    }

    @Test
    @DisplayName("START SESSION: Should create session and return 201 with session response")
    void startSession_ShouldReturnCreatedSession_WhenValidRequest() {
        // Arrange
        StartSessionRequest request = StartSessionRequest.builder()
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .notes("Working on timer unit tests")
                .build();

        FocusSessionDto expectedResponse = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .startTime(LocalDateTime.now())
                .notes("Working on timer unit tests")
                .completed(false)
                .interruptions(0)
                .build();

        // Mock the service call
        when(timerService.startSession(eq(userId), any(StartSessionRequest.class)))
            .thenReturn(expectedResponse);

        // Act - Test the controller method directly
        ResponseEntity<FocusSessionDto> response = timerController.startSession(request, mockUserDetails);
        
        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sessionId, response.getBody().getId());
        assertEquals(userId, response.getBody().getUserId());
        assertEquals(FocusSession.SessionType.WORK, response.getBody().getSessionType());
        assertEquals(25, response.getBody().getDurationMinutes());
        assertEquals("Working on timer unit tests", response.getBody().getNotes());
        assertEquals(false, response.getBody().getCompleted());
        assertEquals(0, response.getBody().getInterruptions());
        
        // Verify service was called with correct parameters
        verify(timerService).startSession(eq(userId), any(StartSessionRequest.class));
    }

    @Test
    @DisplayName("GET CURRENT SESSION: Should return session when active session exists")
    void getCurrentSession_ShouldReturnSession_WhenSessionExists() {
        // Arrange
        FocusSessionDto expectedResponse = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .sessionType(FocusSession.SessionType.STUDY)
                .durationMinutes(45)
                .startTime(LocalDateTime.now().minusMinutes(15))
                .notes("Studying for exams")
                .completed(false)
                .interruptions(2)
                .build();

        // Mock the service call
        when(timerService.getCurrentSession(eq(userId)))
            .thenReturn(expectedResponse);

        // Act - Test the controller method directly
        ResponseEntity<FocusSessionDto> response = timerController.getCurrentSession(mockUserDetails);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sessionId, response.getBody().getId());
        assertEquals(userId, response.getBody().getUserId());
        assertEquals(FocusSession.SessionType.STUDY, response.getBody().getSessionType());
        assertEquals(45, response.getBody().getDurationMinutes());
        assertEquals("Studying for exams", response.getBody().getNotes());
        assertEquals(false, response.getBody().getCompleted());
        assertEquals(2, response.getBody().getInterruptions());
        
        // Verify service was called with correct parameters
        verify(timerService).getCurrentSession(eq(userId));
    }

    @Test
    @DisplayName("GET CURRENT SESSION: Should return no content when no active session exists")
    void getCurrentSession_ShouldReturnNoContent_WhenNoActiveSession() {
        // Arrange
        // Mock the service call to return null (no active session)
        when(timerService.getCurrentSession(eq(userId)))
            .thenReturn(null);

        // Act - Test the controller method directly
        ResponseEntity<FocusSessionDto> response = timerController.getCurrentSession(mockUserDetails);
        
        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        
        // Verify service was called with correct parameters
        verify(timerService).getCurrentSession(eq(userId));
    }

    @Test
    @DisplayName("PAUSE SESSION: Should return paused session when valid session ID provided")
    void pauseSession_ShouldReturnPausedSession_WhenValidSessionId() {
        // Arrange
        FocusSessionDto expectedResponse = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .notes("Working on project - paused for break")
                .completed(false)
                .interruptions(1) // Pausing counts as an interruption
                .build();

        // Mock the service call
        when(timerService.pauseSession(eq(userId), eq(sessionId)))
            .thenReturn(expectedResponse);

        // Act - Test the controller method directly
        ResponseEntity<FocusSessionDto> response = timerController.pauseSession(sessionId, mockUserDetails);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sessionId, response.getBody().getId());
        assertEquals(userId, response.getBody().getUserId());
        assertEquals(FocusSession.SessionType.WORK, response.getBody().getSessionType());
        assertEquals(25, response.getBody().getDurationMinutes());
        assertEquals("Working on project - paused for break", response.getBody().getNotes());
        assertEquals(false, response.getBody().getCompleted());
        assertEquals(1, response.getBody().getInterruptions());
        
        // Verify service was called with correct parameters
        verify(timerService).pauseSession(eq(userId), eq(sessionId));
    }

    @Test
    @DisplayName("END SESSION: Should return completed session when valid session ID provided")
    void endSession_ShouldReturnCompletedSession_WhenValidSessionId() {
        // Arrange
        FocusSessionDto expectedResponse = FocusSessionDto.builder()
                .id(sessionId)
                .userId(userId)
                .sessionType(FocusSession.SessionType.WORK)
                .durationMinutes(25)
                .actualDurationMinutes(23) // Ended slightly early
                .startTime(LocalDateTime.now().minusMinutes(23))
                .endTime(LocalDateTime.now())
                .notes("Work session completed successfully")
                .completed(true)
                .interruptions(0)
                .build();

        // Mock the service call
        when(timerService.endSession(eq(userId), eq(sessionId)))
            .thenReturn(expectedResponse);

        // Act - Test the controller method directly
        ResponseEntity<FocusSessionDto> response = timerController.endSession(sessionId, mockUserDetails);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sessionId, response.getBody().getId());
        assertEquals(userId, response.getBody().getUserId());
        assertEquals(FocusSession.SessionType.WORK, response.getBody().getSessionType());
        assertEquals(25, response.getBody().getDurationMinutes());
        assertEquals(23, response.getBody().getActualDurationMinutes());
        assertEquals("Work session completed successfully", response.getBody().getNotes());
        assertEquals(true, response.getBody().getCompleted());
        assertEquals(0, response.getBody().getInterruptions());
        assertNotNull(response.getBody().getStartTime());
        assertNotNull(response.getBody().getEndTime());
        
        // Verify service was called with correct parameters
        verify(timerService).endSession(eq(userId), eq(sessionId));
    }
}