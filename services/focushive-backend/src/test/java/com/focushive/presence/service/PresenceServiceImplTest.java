package com.focushive.presence.service;

import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.presence.dto.*;
import com.focushive.presence.service.impl.PresenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private HiveMemberRepository hiveMemberRepository;
    
    @Captor
    private ArgumentCaptor<UserPresence> presenceCaptor;
    
    @Captor
    private ArgumentCaptor<HivePresenceInfo> hivePresenceCaptor;
    
    private PresenceServiceImpl presenceService;
    
    @BeforeEach
    void setUp() {
        presenceService = new PresenceServiceImpl(redisTemplate, messagingTemplate, hiveMemberRepository);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Set heartbeat timeout to 30 seconds for testing
        ReflectionTestUtils.setField(presenceService, "heartbeatTimeoutSeconds", 30);
    }
    
    @Test
    void updateUserPresence_shouldStoreInRedisAndBroadcast() {
        // Given
        String userId = "user123";
        String hiveId = "hive456";
        PresenceUpdate update = new PresenceUpdate(
            PresenceStatus.ONLINE,
            hiveId,
            "Working on task"
        );
        
        // When
        UserPresence result = presenceService.updateUserPresence(userId, update);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getStatus()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(result.getActivity()).isEqualTo("Working on task");
        assertThat(result.getCurrentHiveId()).isEqualTo(hiveId);
        
        // Verify Redis storage
        verify(valueOperations).set(
            eq("presence:user:" + userId),
            presenceCaptor.capture(),
            eq(60L),
            eq(TimeUnit.SECONDS)
        );
        
        UserPresence storedPresence = presenceCaptor.getValue();
        assertThat(storedPresence).isEqualTo(result);
        
        // Verify broadcast
        verify(messagingTemplate).convertAndSend(
            eq("/topic/hive/" + hiveId + "/presence"),
            any(PresenceBroadcast.class)
        );
    }
    
    @Test
    void getUserPresence_whenExists_shouldReturnPresence() {
        // Given
        String userId = "user123";
        UserPresence expectedPresence = UserPresence.builder()
            .userId(userId)
            .status(PresenceStatus.BUSY)
            .lastSeen(Instant.now())
            .build();
        
        when(valueOperations.get("presence:user:" + userId)).thenReturn(expectedPresence);
        
        // When
        UserPresence result = presenceService.getUserPresence(userId);
        
        // Then
        assertThat(result).isEqualTo(expectedPresence);
    }
    
    @Test
    void getUserPresence_whenNotExists_shouldReturnNull() {
        // Given
        String userId = "user123";
        when(valueOperations.get("presence:user:" + userId)).thenReturn(null);
        
        // When
        UserPresence result = presenceService.getUserPresence(userId);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void recordHeartbeat_shouldUpdateLastSeenAndExtendTTL() throws InterruptedException {
        // Given
        String userId = "user123";
        Instant originalTime = Instant.now().minusSeconds(30);
        UserPresence existingPresence = UserPresence.builder()
            .userId(userId)
            .status(PresenceStatus.ONLINE)
            .lastSeen(originalTime)
            .build();
        
        when(valueOperations.get("presence:user:" + userId)).thenReturn(existingPresence);
        
        // Add small delay to ensure time difference
        Thread.sleep(10);
        
        // When
        presenceService.recordHeartbeat(userId);
        
        // Then
        verify(valueOperations).set(
            eq("presence:user:" + userId),
            presenceCaptor.capture(),
            eq(60L),
            eq(TimeUnit.SECONDS)
        );
        
        UserPresence updatedPresence = presenceCaptor.getValue();
        assertThat(updatedPresence.getLastSeen()).isAfter(originalTime);
    }
    
    @Test
    void joinHivePresence_whenUserIsMember_shouldAddToHiveAndBroadcast() {
        // Given
        String hiveId = "hive123";
        String userId = "user456";
        
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(true);
        when(valueOperations.get("presence:hive:" + hiveId)).thenReturn(new HashSet<>());
        
        UserPresence userPresence = UserPresence.builder()
            .userId(userId)
            .status(PresenceStatus.ONLINE)
            .lastSeen(Instant.now())
            .build();
        when(valueOperations.get("presence:user:" + userId)).thenReturn(userPresence);
        
        // When
        HivePresenceInfo result = presenceService.joinHivePresence(hiveId, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getHiveId()).isEqualTo(hiveId);
        assertThat(result.getActiveUsers()).isEqualTo(1);
        
        // Verify user added to hive set
        verify(valueOperations).set(
            eq("presence:hive:" + hiveId),
            argThat(set -> ((Set<?>) set).contains(userId)),
            eq(1L),
            eq(TimeUnit.HOURS)
        );
        
        // Verify broadcast
        verify(messagingTemplate).convertAndSend(
            eq("/topic/hive/" + hiveId + "/presence"),
            any(HivePresenceInfo.class)
        );
    }
    
    @Test
    void joinHivePresence_whenUserNotMember_shouldThrowException() {
        // Given
        String hiveId = "hive123";
        String userId = "user456";
        
        when(hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> presenceService.joinHivePresence(hiveId, userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User is not a member of this hive");
            
        // Verify the membership check was called
        verify(hiveMemberRepository).existsByHiveIdAndUserId(hiveId, userId);
    }
    
    @Test
    void leaveHivePresence_shouldRemoveFromHiveAndBroadcast() {
        // Given
        String hiveId = "hive123";
        String userId = "user456";
        Set<String> hiveUsers = new HashSet<>(Arrays.asList(userId, "user789"));
        
        when(valueOperations.get("presence:hive:" + hiveId)).thenReturn(hiveUsers);
        
        // Mock for the remaining user
        UserPresence otherUserPresence = UserPresence.builder()
            .userId("user789")
            .status(PresenceStatus.ONLINE)
            .lastSeen(Instant.now())
            .build();
        when(valueOperations.get("presence:user:user789")).thenReturn(otherUserPresence);
        
        // When
        HivePresenceInfo result = presenceService.leaveHivePresence(hiveId, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActiveUsers()).isEqualTo(1);
        
        // Verify user removed from hive set
        verify(valueOperations).set(
            eq("presence:hive:" + hiveId),
            argThat(set -> !((Set<?>) set).contains(userId)),
            eq(1L),
            eq(TimeUnit.HOURS)
        );
        
        // Verify broadcast
        verify(messagingTemplate).convertAndSend(
            eq("/topic/hive/" + hiveId + "/presence"),
            any(HivePresenceInfo.class)
        );
    }
    
    @Test
    void startFocusSession_shouldCreateSessionAndBroadcast() {
        // Given
        String userId = "user123";
        String hiveId = "hive456";
        int durationMinutes = 25;
        
        // When
        FocusSession result = presenceService.startFocusSession(userId, hiveId, durationMinutes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getHiveId()).isEqualTo(hiveId);
        assertThat(result.getPlannedDurationMinutes()).isEqualTo(durationMinutes);
        assertThat(result.getStatus()).isEqualTo(FocusSession.SessionStatus.ACTIVE);
        assertThat(result.getStartTime()).isNotNull();
        
        // Verify Redis storage
        verify(valueOperations).set(
            eq("presence:session:" + result.getSessionId()),
            eq(result),
            eq((long) durationMinutes * 2),
            eq(TimeUnit.MINUTES)
        );
        
        verify(valueOperations).set(
            eq("presence:user:" + userId + ":session"),
            eq(result.getSessionId()),
            eq((long) durationMinutes * 2),
            eq(TimeUnit.MINUTES)
        );
        
        // Verify broadcast
        verify(messagingTemplate).convertAndSend(
            eq("/topic/hive/" + hiveId + "/sessions"),
            any(SessionBroadcast.class)
        );
    }
    
    @Test
    void endFocusSession_shouldUpdateSessionAndBroadcast() {
        // Given
        String userId = "user123";
        String sessionId = "session789";
        FocusSession existingSession = FocusSession.builder()
            .sessionId(sessionId)
            .userId(userId)
            .hiveId("hive456")
            .plannedDurationMinutes(25)
            .status(FocusSession.SessionStatus.ACTIVE)
            .startTime(Instant.now().minusSeconds(600))
            .build();
        
        when(valueOperations.get("presence:session:" + sessionId)).thenReturn(existingSession);
        
        // When
        FocusSession result = presenceService.endFocusSession(userId, sessionId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(FocusSession.SessionStatus.COMPLETED);
        assertThat(result.getEndTime()).isNotNull();
        assertThat(result.getActualDurationMinutes()).isGreaterThan(0);
        
        // Verify session updated - use any() instead of specific argument matcher to avoid comparison issues
        verify(valueOperations).set(
            eq("presence:session:" + sessionId),
            any(FocusSession.class),
            eq(1L),
            eq(TimeUnit.HOURS)
        );
        
        // Verify user session key deleted
        verify(redisTemplate).delete("presence:user:" + userId + ":session");
        
        // Verify broadcast
        verify(messagingTemplate).convertAndSend(
            eq("/topic/hive/" + existingSession.getHiveId() + "/sessions"),
            any(SessionBroadcast.class)
        );
    }
    
    @Test
    void getHiveActiveUsers_shouldReturnOnlineUsersInHive() {
        // Given
        String hiveId = "hive123";
        Set<String> hiveUserIds = new HashSet<>(Arrays.asList("user1", "user2", "user3"));
        
        UserPresence user1 = UserPresence.builder()
            .userId("user1")
            .status(PresenceStatus.ONLINE)
            .lastSeen(Instant.now())
            .build();
        
        UserPresence user2 = UserPresence.builder()
            .userId("user2")
            .status(PresenceStatus.AWAY)
            .lastSeen(Instant.now())
            .build();
        
        when(valueOperations.get("presence:hive:" + hiveId)).thenReturn(hiveUserIds);
        when(valueOperations.get("presence:user:user1")).thenReturn(user1);
        when(valueOperations.get("presence:user:user2")).thenReturn(user2);
        when(valueOperations.get("presence:user:user3")).thenReturn(null); // User offline
        
        // When
        List<UserPresence> result = presenceService.getHiveActiveUsers(hiveId);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(user1, user2);
    }
    
    @Test
    void getHivesPresenceInfo_shouldReturnInfoForMultipleHives() {
        // Given
        Set<String> hiveIds = new HashSet<>(Arrays.asList("hive1", "hive2"));
        
        Set<String> hive1Users = new HashSet<>(Arrays.asList("user1", "user2"));
        Set<String> hive2Users = new HashSet<>(Arrays.asList("user3"));
        
        when(valueOperations.get("presence:hive:hive1")).thenReturn(hive1Users);
        when(valueOperations.get("presence:hive:hive2")).thenReturn(hive2Users);
        
        // Mock user presence
        when(valueOperations.get("presence:user:user1")).thenReturn(
            UserPresence.builder().userId("user1").status(PresenceStatus.ONLINE).build()
        );
        when(valueOperations.get("presence:user:user2")).thenReturn(
            UserPresence.builder().userId("user2").status(PresenceStatus.BUSY).build()
        );
        when(valueOperations.get("presence:user:user3")).thenReturn(
            UserPresence.builder().userId("user3").status(PresenceStatus.ONLINE).build()
        );
        
        // Mock focus sessions - empty set means no active sessions
        when(redisTemplate.keys("presence:session:*")).thenReturn(new HashSet<>());
        
        // When
        Map<String, HivePresenceInfo> result = presenceService.getHivesPresenceInfo(hiveIds);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("hive1").getActiveUsers()).isEqualTo(2);
        assertThat(result.get("hive2").getActiveUsers()).isEqualTo(1);
        
        // Verify critical interactions
        verify(valueOperations, atLeastOnce()).get("presence:hive:hive1");
        verify(valueOperations, atLeastOnce()).get("presence:hive:hive2");
        verify(redisTemplate, atLeastOnce()).keys("presence:session:*");
    }
    
    @Test
    void cleanupStalePresence_shouldRemoveExpiredPresence() {
        // Given
        Set<String> userKeys = new HashSet<>(Arrays.asList(
            "presence:user:user1",
            "presence:user:user2"
        ));
        
        UserPresence staleUser = UserPresence.builder()
            .userId("user1")
            .status(PresenceStatus.ONLINE)
            .lastSeen(Instant.now().minusSeconds(120)) // 2 minutes old
            .currentHiveId("hive123") // Set a hive ID for proper cleanup
            .build();
        
        UserPresence activeUser = UserPresence.builder()
            .userId("user2")
            .status(PresenceStatus.ONLINE)
            .lastSeen(Instant.now().minusSeconds(10)) // 10 seconds old
            .build();
        
        // Mock hive presence set for cleanup
        Set<String> hiveUsers = new HashSet<>(Arrays.asList("user1", "user3"));
        when(redisTemplate.keys("presence:user:*")).thenReturn(userKeys);
        when(valueOperations.get("presence:user:user1")).thenReturn(staleUser);
        when(valueOperations.get("presence:user:user2")).thenReturn(activeUser);
        when(valueOperations.get("presence:hive:hive123")).thenReturn(hiveUsers);
        
        // When
        presenceService.cleanupStalePresence();
        
        // Then
        verify(redisTemplate).delete("presence:user:user1");
        verify(redisTemplate, never()).delete("presence:user:user2");
        verify(valueOperations).set(
            eq("presence:hive:hive123"),
            argThat(set -> !((Set<?>) set).contains("user1")),
            eq(1L),
            eq(TimeUnit.HOURS)
        );
    }
}