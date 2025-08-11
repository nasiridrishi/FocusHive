package com.focushive.websocket.controller;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyService;
import com.focushive.websocket.dto.WebSocketMessage;
import com.focushive.websocket.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BuddyWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final BuddyService buddyService;
    
    // Send buddy request notification
    @MessageMapping("/buddy/request")
    @SendToUser("/queue/buddy/requests")
    public WebSocketMessage<BuddyRelationshipDTO> sendBuddyRequest(
            @Payload BuddyRequestDTO request,
            Principal principal) {
        
        log.info("Processing buddy request from user: {}", principal.getName());
        
        String fromUserId = getUserIdFromPrincipal(principal);
        BuddyRelationshipDTO relationship = buddyService.sendBuddyRequest(
            fromUserId, request.getToUserId(), request
        );
        
        // Create notification for recipient
        WebSocketMessage<BuddyRelationshipDTO> message = WebSocketMessage.<BuddyRelationshipDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.BUDDY_REQUEST)
            .event("buddy.request.received")
            .payload(relationship)
            .senderId(fromUserId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send notification to recipient
        messagingTemplate.convertAndSendToUser(
            request.getToUserId().toString(),
            "/queue/buddy/notifications",
            message
        );
        
        return message;
    }
    
    // Accept buddy request
    @MessageMapping("/buddy/accept/{relationshipId}")
    public void acceptBuddyRequest(
            @DestinationVariable Long relationshipId,
            Principal principal) {
        
        String userId = getUserIdFromPrincipal(principal);
        BuddyRelationshipDTO relationship = buddyService.acceptBuddyRequest(relationshipId, userId);
        
        // Notify both users
        WebSocketMessage<BuddyRelationshipDTO> message = WebSocketMessage.<BuddyRelationshipDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.BUDDY_REQUEST_ACCEPTED)
            .event("buddy.request.accepted")
            .payload(relationship)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send to both users
        messagingTemplate.convertAndSendToUser(
            relationship.getUser1Id().toString(),
            "/queue/buddy/notifications",
            message
        );
        messagingTemplate.convertAndSendToUser(
            relationship.getUser2Id().toString(),
            "/queue/buddy/notifications",
            message
        );
    }
    
    // Send check-in notification
    @MessageMapping("/buddy/checkin/{relationshipId}")
    @SendTo("/topic/buddy/checkins/{relationshipId}")
    public WebSocketMessage<BuddyCheckinDTO> sendCheckin(
            @DestinationVariable Long relationshipId,
            @Payload BuddyCheckinDTO checkin,
            Principal principal) {
        
        String userId = getUserIdFromPrincipal(principal);
        BuddyCheckinDTO created = buddyService.createCheckin(relationshipId, userId, checkin);
        
        return WebSocketMessage.<BuddyCheckinDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.BUDDY_CHECKIN)
            .event("buddy.checkin.created")
            .payload(created)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    // Goal update notification
    @MessageMapping("/buddy/goal/update")
    public void updateGoal(@Payload BuddyGoalDTO goal, Principal principal) {
        
        BuddyGoalDTO updated = buddyService.updateGoal(goal.getId(), goal);
        
        WebSocketMessage<BuddyGoalDTO> message = WebSocketMessage.<BuddyGoalDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.BUDDY_GOAL_UPDATE)
            .event("buddy.goal.updated")
            .payload(updated)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send to all relationship members
        messagingTemplate.convertAndSend(
            "/topic/buddy/goals/" + goal.getRelationshipId(),
            message
        );
    }
    
    // Session start notification
    @MessageMapping("/buddy/session/{sessionId}/start")
    public void startSession(
            @DestinationVariable Long sessionId,
            Principal principal) {
        
        String userId = getUserIdFromPrincipal(principal);
        BuddySessionDTO session = buddyService.startSession(sessionId, userId);
        
        WebSocketMessage<BuddySessionDTO> message = WebSocketMessage.<BuddySessionDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.BUDDY_SESSION_START)
            .event("buddy.session.started")
            .payload(session)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send to session topic
        messagingTemplate.convertAndSend(
            "/topic/buddy/sessions/" + session.getRelationshipId(),
            message
        );
    }
    
    // Session end notification
    @MessageMapping("/buddy/session/{sessionId}/end")
    public void endSession(
            @DestinationVariable Long sessionId,
            Principal principal) {
        
        String userId = getUserIdFromPrincipal(principal);
        BuddySessionDTO session = buddyService.endSession(sessionId, userId);
        
        WebSocketMessage<BuddySessionDTO> message = WebSocketMessage.<BuddySessionDTO>builder()
            .id(UUID.randomUUID().toString())
            .type(WebSocketMessage.MessageType.BUDDY_SESSION_END)
            .event("buddy.session.ended")
            .payload(session)
            .senderId(userId.toString())
            .senderUsername(principal.getName())
            .timestamp(LocalDateTime.now())
            .build();
        
        // Send to session topic
        messagingTemplate.convertAndSend(
            "/topic/buddy/sessions/" + session.getRelationshipId(),
            message
        );
    }
    
    // Session reminder
    public void sendSessionReminder(Long sessionId, String userId) {
        // This method would be called by a scheduled task
        BuddySessionDTO session = buddyService.getUpcomingSessions().stream()
            .filter(s -> s.getId().equals(sessionId))
            .findFirst()
            .orElse(null);
        
        if (session != null) {
            NotificationMessage notification = NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .type(NotificationMessage.NotificationType.BUDDY_SESSION_STARTING)
                .title("Buddy Session Starting Soon")
                .message("Your buddy session starts in 15 minutes")
                .priority(NotificationMessage.NotificationPriority.HIGH)
                .requiresAction(true)
                .data(Map.of("sessionId", sessionId, "relationshipId", session.getRelationshipId()))
                .createdAt(LocalDateTime.now())
                .build();
            
            WebSocketMessage<NotificationMessage> message = WebSocketMessage.<NotificationMessage>builder()
                .id(UUID.randomUUID().toString())
                .type(WebSocketMessage.MessageType.BUDDY_SESSION_REMINDER)
                .event("buddy.session.reminder")
                .payload(notification)
                .timestamp(LocalDateTime.now())
                .build();
            
            // Send reminder to user
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                message
            );
        }
    }
    
    // Helper method to extract user ID from principal
    private String getUserIdFromPrincipal(Principal principal) {
        // In a real implementation, this would extract the user ID from the authentication token
        // For now, we'll return the username directly as User entity uses String IDs
        if (principal instanceof Authentication) {
            Authentication auth = (Authentication) principal;
            // Assuming the user ID is stored in the authentication details
            return auth.getName();
        }
        return principal.getName();
    }
}