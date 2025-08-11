package com.focushive.websocket.handler;

import com.focushive.buddy.service.BuddyService;
import com.focushive.websocket.controller.BuddyWebSocketController;
import com.focushive.websocket.dto.NotificationMessage;
import com.focushive.websocket.dto.PresenceUpdate;
import com.focushive.websocket.dto.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventHandler {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final BuddyService buddyService;
    private final BuddyWebSocketController buddyWebSocketController;
    
    // Track connected users and their sessions
    private final Map<String, UserSession> connectedUsers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSubscriptions = new ConcurrentHashMap<>();
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = event.getUser() != null ? event.getUser().getName() : null;
        
        if (username != null) {
            log.info("User {} connected with session {}", username, sessionId);
            
            UserSession session = new UserSession();
            session.setSessionId(sessionId);
            session.setUsername(username);
            session.setConnectedAt(LocalDateTime.now());
            session.setStatus(PresenceUpdate.PresenceStatus.ONLINE);
            
            connectedUsers.put(sessionId, session);
            
            // Broadcast user online status
            broadcastUserPresence(username, PresenceUpdate.PresenceStatus.ONLINE);
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        UserSession session = connectedUsers.remove(sessionId);
        if (session != null) {
            log.info("User {} disconnected", session.getUsername());
            
            // Remove user subscriptions
            userSubscriptions.remove(sessionId);
            
            // Broadcast user offline status
            broadcastUserPresence(session.getUsername(), PresenceUpdate.PresenceStatus.OFFLINE);
        }
    }
    
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        if (destination != null) {
            userSubscriptions.computeIfAbsent(sessionId, k -> new HashSet<>()).add(destination);
            log.debug("Session {} subscribed to {}", sessionId, destination);
        }
    }
    
    // Scheduled task to send buddy session reminders
    @Scheduled(fixedDelay = 60000) // Check every minute
    public void sendBuddySessionReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = now.plusMinutes(15);
        
        // Get upcoming sessions
        buddyService.getUpcomingSessions().forEach(session -> {
            if (session.getScheduledTime() != null && 
                session.getScheduledTime().isAfter(now) &&
                session.getScheduledTime().isBefore(reminderTime)) {
                
                // Send reminder to both buddies
                buddyWebSocketController.sendSessionReminder(session.getId(), session.getUser1Id());
                buddyWebSocketController.sendSessionReminder(session.getId(), session.getUser2Id());
                
                log.info("Sent reminder for buddy session {}", session.getId());
            }
        });
    }
    
    // Scheduled task to check buddy check-in reminders
    @Scheduled(fixedDelay = 3600000) // Check every hour
    public void sendBuddyCheckinReminders() {
        LocalDateTime now = LocalDateTime.now();
        
        buddyService.getActiveRelationships().forEach(relationship -> {
            LocalDateTime lastCheckin = relationship.getLastCheckinTime();
            
            // If no check-in in last 24 hours, send reminder
            if (lastCheckin == null || lastCheckin.isBefore(now.minusHours(24))) {
                NotificationMessage notification = NotificationMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .type(NotificationMessage.NotificationType.BUDDY_CHECKIN_REMINDER)
                    .title("Time to Check In!")
                    .message("You haven't checked in with your buddy in over 24 hours")
                    .priority(NotificationMessage.NotificationPriority.NORMAL)
                    .requiresAction(true)
                    .data(Map.of("relationshipId", relationship.getId()))
                    .createdAt(LocalDateTime.now())
                    .build();
                
                sendNotificationToUser(relationship.getUser1Id(), notification);
                sendNotificationToUser(relationship.getUser2Id(), notification);
            }
        });
    }
    
    // Scheduled task to check buddy goal deadlines
    @Scheduled(fixedDelay = 3600000) // Check every hour
    public void sendBuddyGoalDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        
        buddyService.getActiveGoals().forEach(goal -> {
            if (goal.getDeadline() != null && 
                goal.getDeadline().isAfter(now) &&
                goal.getDeadline().isBefore(tomorrow)) {
                
                NotificationMessage notification = NotificationMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .type(NotificationMessage.NotificationType.BUDDY_GOAL_DEADLINE)
                    .title("Goal Deadline Approaching")
                    .message("Your goal \"" + goal.getTitle() + "\" is due soon!")
                    .priority(NotificationMessage.NotificationPriority.HIGH)
                    .requiresAction(true)
                    .data(Map.of("goalId", goal.getId(), "relationshipId", goal.getRelationshipId()))
                    .createdAt(LocalDateTime.now())
                    .build();
                
                // Send to both buddies in the relationship
                sendNotificationToRelationshipMembers(goal.getRelationshipId(), notification);
            }
        });
    }
    
    // Send system-wide announcement
    public void sendSystemAnnouncement(String title, String message) {
        NotificationMessage notification = NotificationMessage.builder()
            .id(UUID.randomUUID().toString())
            .type(NotificationMessage.NotificationType.SYSTEM_ANNOUNCEMENT)
            .title(title)
            .message(message)
            .priority(NotificationMessage.NotificationPriority.HIGH)
            .createdAt(LocalDateTime.now())
            .build();
        
        WebSocketMessage<NotificationMessage> wsMessage = WebSocketMessage.<NotificationMessage>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.NOTIFICATION)
            .event("system.announcement")
            .payload(notification)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Broadcast to all connected users
        messagingTemplate.convertAndSend("/topic/system/announcements", wsMessage);
        log.info("Sent system announcement: {}", title);
    }
    
    // Send achievement notification
    public void sendAchievementUnlocked(String userId, String achievementName, String description) {
        NotificationMessage notification = NotificationMessage.builder()
            .id(UUID.randomUUID().toString())
            .type(NotificationMessage.NotificationType.ACHIEVEMENT_UNLOCKED)
            .title("Achievement Unlocked!")
            .message("You earned: " + achievementName)
            .priority(NotificationMessage.NotificationPriority.HIGH)
            .data(Map.of("achievement", achievementName, "description", description))
            .createdAt(LocalDateTime.now())
            .build();
        
        sendNotificationToUser(userId, notification);
    }
    
    // Helper method to broadcast user presence updates
    private void broadcastUserPresence(String username, PresenceUpdate.PresenceStatus status) {
        PresenceUpdate presence = PresenceUpdate.builder()
            .username(username)
            .status(status)
            .lastSeen(LocalDateTime.now())
            .build();
        
        WebSocketMessage<PresenceUpdate> message = WebSocketMessage.<PresenceUpdate>builder()
            .id(UUID.randomUUID().toString())
            .type(status == PresenceUpdate.PresenceStatus.ONLINE ? 
                WebSocketMessage.MessageType.USER_ONLINE : 
                WebSocketMessage.MessageType.USER_OFFLINE)
            .event("presence.update")
            .payload(presence)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Broadcast to all users
        messagingTemplate.convertAndSend("/topic/presence", message);
    }
    
    // Helper method to send notification to specific user
    private void sendNotificationToUser(String userId, NotificationMessage notification) {
        WebSocketMessage<NotificationMessage> message = WebSocketMessage.<NotificationMessage>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.NOTIFICATION)
            .event("notification.received")
            .payload(notification)
            .timestamp(LocalDateTime.now())
            .build();
        
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            message
        );
    }
    
    // Helper method to send notification to relationship members
    private void sendNotificationToRelationshipMembers(Long relationshipId, NotificationMessage notification) {
        // Get relationship details and send to both users
        // This would be implemented with actual service calls
        log.info("Sending notification to relationship {} members", relationshipId);
    }
    
    // Inner class to track user sessions
    private static class UserSession {
        private String sessionId;
        private String username;
        private Long userId;
        private LocalDateTime connectedAt;
        private PresenceUpdate.PresenceStatus status;
        private String currentActivity;
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public LocalDateTime getConnectedAt() { return connectedAt; }
        public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }
        
        public PresenceUpdate.PresenceStatus getStatus() { return status; }
        public void setStatus(PresenceUpdate.PresenceStatus status) { this.status = status; }
        
        public String getCurrentActivity() { return currentActivity; }
        public void setCurrentActivity(String currentActivity) { this.currentActivity = currentActivity; }
    }
}