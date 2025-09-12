package com.focushive.websocket.service;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceTrackingServiceTest {
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private SetOperations<String, Object> setOperations;
    
    @InjectMocks
    private PresenceTrackingService presenceTrackingService;
    
    private Long userId;
    private Long hiveId;
    
    @BeforeEach
    void setUp() {
        userId = 12345L;
        hiveId = 67890L;
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }
    
    @Test
    void updateUserPresence_WithValidData_ShouldStoreInRedisAndBroadcast() {
        // Given
        PresenceUpdate.PresenceStatus status = PresenceUpdate.PresenceStatus.ONLINE;
        String activity = "Coding";
        
        // When
        presenceTrackingService.updateUserPresence(userId, status, hiveId, activity);
        
        // Then
        String expectedKey = "presence:user:" + userId;
        verify(valueOperations).set(eq(expectedKey), any(PresenceUpdate.class), eq(Duration.ofMinutes(5)));
        
        // Verify hive presence update
        String expectedHiveKey = "presence:hive:" + hiveId;
        verify(setOperations).add(eq(expectedHiveKey), eq(userId));
        verify(redisTemplate).expire(eq(expectedHiveKey), eq(1L), eq(TimeUnit.HOURS));
        
        // Verify broadcasting to general presence topic
        verify(messagingTemplate).convertAndSend(
            eq("/topic/presence"), 
            any(WebSocketMessage.class)
        );
        
        // Verify broadcasting to hive-specific topic
        verify(messagingTemplate).convertAndSend(
            eq("/topic/hive/" + hiveId + "/presence"), 
            any(WebSocketMessage.class)
        );
    }
}