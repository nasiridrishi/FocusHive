package com.focushive.presence.controller;

import com.focushive.presence.dto.*;
import com.focushive.presence.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit test for PresenceController focusing on edge cases and error scenarios
 * following TDD methodology with Mockito for mocking.
 */
@ExtendWith(MockitoExtension.class)
class PresenceControllerUnitTest {

    @Mock
    private PresenceService presenceService;
    
    @Mock
    private Principal principal;
    
    @InjectMocks
    private PresenceController presenceController;
    
    private static final String USER_ID = "test-user";
    private static final String HIVE_ID = "test-hive";
    
    @BeforeEach
    void setUp() {
        // Setup will be done per test basis for better isolation
    }

    @Test
    @DisplayName("UPDATE PRESENCE: Should handle null principal gracefully")
    void updatePresence_ShouldThrowException_WhenPrincipalIsNull() {
        // Arrange
        PresenceUpdate update = new PresenceUpdate(
            PresenceStatus.ONLINE,
            HIVE_ID,
            "Working on task"
        );
        
        // Act & Assert
        assertThatThrownBy(() -> presenceController.updatePresence(update, null))
            .isInstanceOf(NullPointerException.class);
        
        // Verify service is never called
        verifyNoInteractions(presenceService);
    }

    @Test
    @DisplayName("UPDATE PRESENCE: Should call service with null PresenceUpdate")
    void updatePresence_ShouldCallService_WhenPresenceUpdateIsNull() {
        // Arrange
        when(principal.getName()).thenReturn(USER_ID);
        
        UserPresence expectedPresence = UserPresence.builder()
            .userId(USER_ID)
            .status(PresenceStatus.OFFLINE)
            .build();
            
        when(presenceService.updateUserPresence(USER_ID, null)).thenReturn(expectedPresence);
        
        // Act
        UserPresence result = presenceController.updatePresence(null, principal);
        
        // Assert
        assertThat(result).isEqualTo(expectedPresence);
        verify(presenceService).updateUserPresence(USER_ID, null);
    }

    @Test
    @DisplayName("UPDATE PRESENCE: Should propagate service exceptions")
    void updatePresence_ShouldPropagateException_WhenServiceThrowsRuntimeException() {
        // Arrange
        when(principal.getName()).thenReturn(USER_ID);
        
        PresenceUpdate update = new PresenceUpdate(
            PresenceStatus.ONLINE,
            HIVE_ID,
            "Working on task"
        );
        
        when(presenceService.updateUserPresence(USER_ID, update))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act & Assert
        assertThatThrownBy(() -> presenceController.updatePresence(update, principal))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Database connection failed");
        
        // Verify service was called
        verify(presenceService).updateUserPresence(USER_ID, update);
    }

    @Test
    @DisplayName("HEARTBEAT: Should propagate service exceptions")
    void heartbeat_ShouldPropagateException_WhenServiceThrowsException() {
        // Arrange
        when(principal.getName()).thenReturn(USER_ID);
        
        doThrow(new RuntimeException("Heartbeat recording failed"))
            .when(presenceService).recordHeartbeat(USER_ID);
        
        // Act & Assert
        assertThatThrownBy(() -> presenceController.heartbeat(principal))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Heartbeat recording failed");
        
        // Verify service was called
        verify(presenceService).recordHeartbeat(USER_ID);
    }

    @Test
    @DisplayName("JOIN HIVE: Should handle service exceptions when joining invalid hive")
    void joinHive_ShouldPropagateException_WhenServiceThrowsException() {
        // Arrange
        when(principal.getName()).thenReturn(USER_ID);
        
        String invalidHiveId = "non-existent-hive";
        
        when(presenceService.joinHivePresence(invalidHiveId, USER_ID))
            .thenThrow(new IllegalArgumentException("Hive not found: " + invalidHiveId));
        
        // Act & Assert
        assertThatThrownBy(() -> presenceController.joinHive(invalidHiveId, principal))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Hive not found: non-existent-hive");
        
        // Verify service was called
        verify(presenceService).joinHivePresence(invalidHiveId, USER_ID);
    }
}