package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.SendMessageRequest;
import com.focushive.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket controller for real-time chat functionality.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    
    private final ChatService chatService;
    
    /**
     * Send a message to a hive.
     * Client sends to: /app/hive/{hiveId}/send
     * Server broadcasts to: /topic/hive/{hiveId}/messages
     */
    @MessageMapping("/hive/{hiveId}/send")
    public void sendMessage(
            @DestinationVariable String hiveId,
            @Payload SendMessageRequest request,
            Principal principal) {
        
        // Removed debug log to avoid logging user interactions frequently
        
        // The service will handle broadcasting
        chatService.sendMessage(hiveId, principal.getName(), request);
    }
    
    /**
     * Edit a message.
     * Client sends to: /app/message/{messageId}/edit
     */
    @MessageMapping("/message/{messageId}/edit")
    @SendToUser("/queue/message/edit/response")
    public ChatMessageDto editMessage(
            @DestinationVariable String messageId,
            @Payload String newContent,
            Principal principal) {
        
        // Removed debug log to avoid logging user interactions frequently
        
        // The service will handle broadcasting to hive members
        return chatService.editMessage(messageId, principal.getName(), newContent);
    }
    
    /**
     * Delete a message.
     * Client sends to: /app/message/{messageId}/delete
     */
    @MessageMapping("/message/{messageId}/delete")
    public void deleteMessage(
            @DestinationVariable String messageId,
            Principal principal) {
        
        // Removed debug log to avoid logging user interactions frequently
        
        // The service will handle broadcasting
        chatService.deleteMessage(messageId, principal.getName());
    }
    
    /**
     * Handle typing indicator.
     * Client sends to: /app/hive/{hiveId}/typing
     * Server broadcasts to: /topic/hive/{hiveId}/typing
     */
    @MessageMapping("/hive/{hiveId}/typing")
    @SendTo("/topic/hive/{hiveId}/typing")
    public TypingIndicator handleTyping(
            @DestinationVariable String hiveId,
            @Payload boolean isTyping,
            Principal principal) {
        
        return new TypingIndicator(principal.getName(), isTyping);
    }
    
    /**
     * Simple DTO for typing indicators.
     */
    public record TypingIndicator(String userId, boolean isTyping) {}
}