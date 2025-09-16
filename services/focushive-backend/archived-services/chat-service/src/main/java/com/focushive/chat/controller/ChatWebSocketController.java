package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.TypingIndicatorDto;
import com.focushive.chat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class ChatWebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);
    
    @Autowired
    private ChatService chatService;
    
    /**
     * Handle incoming chat messages via WebSocket
     */
    @MessageMapping("/hive/{hiveId}/chat/send")
    public void sendMessage(
            @DestinationVariable UUID hiveId,
            @Payload ChatMessageDto message,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.info("Received WebSocket message for hive: {}", hiveId);
        
        try {
            // Extract user information from WebSocket session
            Jwt jwt = (Jwt) headerAccessor.getSessionAttributes().get("user");
            if (jwt == null) {
                logger.warn("No user authentication found in WebSocket session");
                return;
            }
            
            UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
            String username = jwt.getClaimAsString("preferred_username");
            
            // Verify user can access this hive
            if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
                logger.warn("User {} does not have access to hive {}", userId, hiveId);
                return;
            }
            
            // Set hive ID and send message
            message.setHiveId(hiveId);
            chatService.sendMessage(message, userId, username);
            
        } catch (Exception e) {
            logger.error("Error processing WebSocket message for hive {}: {}", hiveId, e.getMessage(), e);
        }
    }
    
    /**
     * Handle typing indicator updates
     */
    @MessageMapping("/hive/{hiveId}/typing")
    public void handleTyping(
            @DestinationVariable UUID hiveId,
            @Payload TypingIndicatorDto typingIndicator,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.debug("Received typing indicator for hive: {}", hiveId);
        
        try {
            // Extract user information from WebSocket session
            Jwt jwt = (Jwt) headerAccessor.getSessionAttributes().get("user");
            if (jwt == null) {
                logger.warn("No user authentication found in WebSocket session");
                return;
            }
            
            UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
            String username = jwt.getClaimAsString("preferred_username");
            
            // Verify user can access this hive
            if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
                logger.warn("User {} does not have access to hive {}", userId, hiveId);
                return;
            }
            
            if (typingIndicator.isTyping()) {
                chatService.setTypingIndicator(hiveId, userId, username);
            } else {
                chatService.removeTypingIndicator(hiveId, userId);
            }
            
        } catch (Exception e) {
            logger.error("Error processing typing indicator for hive {}: {}", hiveId, e.getMessage(), e);
        }
    }
    
    /**
     * Handle subscription to hive chat - send recent messages
     */
    @SubscribeMapping("/hive/{hiveId}/chat")
    public List<ChatMessageDto> onSubscribeToChat(
            @DestinationVariable UUID hiveId,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.info("User subscribing to chat for hive: {}", hiveId);
        
        try {
            // Extract user information from WebSocket session
            Jwt jwt = (Jwt) headerAccessor.getSessionAttributes().get("user");
            if (jwt == null) {
                logger.warn("No user authentication found in WebSocket session");
                return List.of();
            }
            
            UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
            
            // Verify user can access this hive
            if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
                logger.warn("User {} does not have access to hive {}", userId, hiveId);
                return List.of();
            }
            
            // Return recent messages to the subscribing user
            return chatService.getRecentMessages(hiveId, 50);
            
        } catch (Exception e) {
            logger.error("Error handling chat subscription for hive {}: {}", hiveId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Handle subscription to typing indicators
     */
    @SubscribeMapping("/hive/{hiveId}/typing")
    public List<TypingIndicatorDto> onSubscribeToTyping(
            @DestinationVariable UUID hiveId,
            SimpMessageHeaderAccessor headerAccessor) {
        
        logger.debug("User subscribing to typing indicators for hive: {}", hiveId);
        
        try {
            // Extract user information from WebSocket session
            Jwt jwt = (Jwt) headerAccessor.getSessionAttributes().get("user");
            if (jwt == null) {
                logger.warn("No user authentication found in WebSocket session");
                return List.of();
            }
            
            UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
            
            // Verify user can access this hive
            if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
                logger.warn("User {} does not have access to hive {}", userId, hiveId);
                return List.of();
            }
            
            // Return current typing indicators
            return chatService.getActiveTypingIndicators(hiveId);
            
        } catch (Exception e) {
            logger.error("Error handling typing subscription for hive {}: {}", hiveId, e.getMessage(), e);
            return List.of();
        }
    }
}