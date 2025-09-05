package com.focushive.websocket.controller;

import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import com.focushive.websocket.service.PresenceTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PresenceWebSocketController {
    
    private final PresenceTrackingService presenceTrackingService;
    
    /**
     * Handle user heartbeat/activity update
     */
    @MessageMapping("/presence/ws-heartbeat")
    @SendToUser("/queue/presence/ack")
    public Map<String, Object> heartbeat(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        
        // Update user activity
        presenceTrackingService.updateUserActivity(userId);
        
        return Map.of(
            "status", "ok",
            "timestamp", LocalDateTime.now(),
            "userId", userId
        );
    }
    
    /**
     * Update user status
     */
    @MessageMapping("/presence/status")
    @SendTo("/topic/presence")
    public WebSocketMessage<PresenceUpdate> updateStatus(
            @Payload Map<String, Object> statusData,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        PresenceUpdate.PresenceStatus status = PresenceUpdate.PresenceStatus.valueOf(
            statusData.get("status").toString()
        );
        Long hiveId = statusData.get("hiveId") != null ? 
            Long.parseLong(statusData.get("hiveId").toString()) : null;
        String activity = statusData.get("activity") != null ? 
            statusData.get("activity").toString() : null;
        
        // Update presence
        presenceTrackingService.updateUserPresence(userId, status, hiveId, activity);
        
        // Get updated presence
        PresenceUpdate presence = presenceTrackingService.getUserPresence(userId);
        
        return WebSocketMessage.<PresenceUpdate>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.USER_ONLINE)
            .event("presence.status.updated")
            .payload(presence)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Start focus session
     */
    @MessageMapping("/presence/focus/start")
    public void startFocusSession(
            @Payload Map<String, Object> sessionData,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        Long hiveId = sessionData.get("hiveId") != null ? 
            Long.parseLong(sessionData.get("hiveId").toString()) : null;
        Integer focusMinutes = sessionData.get("minutes") != null ? 
            Integer.parseInt(sessionData.get("minutes").toString()) : 25;
        
        presenceTrackingService.startFocusSession(userId, hiveId, focusMinutes);
        
        log.info("User {} started focus session for {} minutes", userId, focusMinutes);
    }
    
    /**
     * Start buddy session
     */
    @MessageMapping("/presence/buddy/start")
    public void startBuddySession(
            @Payload Map<String, Object> sessionData,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        Long buddyId = Long.parseLong(sessionData.get("buddyId").toString());
        
        presenceTrackingService.startBuddySession(userId, buddyId);
        
        log.info("User {} started buddy session with user {}", userId, buddyId);
    }
    
    /**
     * Get user presence
     */
    @MessageMapping("/presence/user/{userId}")
    @SendToUser("/queue/presence/user")
    public WebSocketMessage<PresenceUpdate> getUserPresence(
            @DestinationVariable Long userId) {
        
        PresenceUpdate presence = presenceTrackingService.getUserPresence(userId);
        
        return WebSocketMessage.<PresenceUpdate>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.INFO)
            .event("presence.user.info")
            .payload(presence)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Get hive presence list
     */
    @MessageMapping("/presence/hive/{hiveId}")
    @SendTo("/topic/hive/{hiveId}/presence")
    public WebSocketMessage<List<PresenceUpdate>> getHivePresence(
            @DestinationVariable Long hiveId) {
        
        List<PresenceUpdate> presenceList = presenceTrackingService.getHivePresence(hiveId);
        
        return WebSocketMessage.<List<PresenceUpdate>>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.INFO)
            .event("presence.hive.list")
            .payload(presenceList)
            .metadata(Map.of(
                "hiveId", hiveId,
                "onlineCount", presenceList.size(),
                "timestamp", LocalDateTime.now()
            ))
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Get hive online count
     */
    @MessageMapping("/presence/hive/{hiveId}/count")
    @SendToUser("/queue/presence/count")
    public Map<String, Object> getHiveOnlineCount(@DestinationVariable Long hiveId) {
        long count = presenceTrackingService.getHiveOnlineCount(hiveId);
        
        return Map.of(
            "hiveId", hiveId,
            "onlineCount", count,
            "timestamp", LocalDateTime.now()
        );
    }
    
    /**
     * User typing indicator
     */
    @MessageMapping("/presence/typing")
    @SendTo("/topic/presence/typing")
    public Map<String, Object> userTyping(
            @Payload Map<String, Object> typingData,
            Principal principal) {
        
        Long userId = getUserIdFromPrincipal(principal);
        String location = typingData.get("location").toString(); // e.g., "hive:123", "forum:456"
        boolean isTyping = Boolean.parseBoolean(typingData.get("isTyping").toString());
        
        return Map.of(
            "userId", userId,
            "username", principal.getName(),
            "location", location,
            "isTyping", isTyping,
            "timestamp", LocalDateTime.now()
        );
    }
    
    /**
     * Disconnect user - mark as offline
     */
    @MessageMapping("/presence/disconnect")
    public void disconnect(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        
        presenceTrackingService.removeUserPresence(userId);
        
        log.info("User {} manually disconnected", userId);
    }
    
    // Helper method to extract user ID from principal
    private Long getUserIdFromPrincipal(Principal principal) {
        // In a real implementation, extract from authentication token
        return Long.parseLong(principal.getName());
    }
}