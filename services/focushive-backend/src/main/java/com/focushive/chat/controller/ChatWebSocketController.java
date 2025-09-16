package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.SendMessageRequest;
import com.focushive.chat.service.ChatService;
import com.focushive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Simplified WebSocket controller for real-time chat functionality.
 * Advanced features (reactions, threading, pinned messages, typing indicators)
 * are temporarily disabled during the simplification phase.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserService userService;

    /**
     * Send a message to a hive via WebSocket.
     */
    @MessageMapping("/hive/{hiveId}/sendMessage")
    public void sendMessage(
            @DestinationVariable String hiveId,
            @Payload SendMessageRequest request,
            Principal principal) {

        log.debug("WebSocket message received for hive {} from user {}: {}",
                hiveId, principal.getName(), request.getContent());

        try {
            // Send message through service (which will broadcast via WebSocket)
            chatService.sendMessage(hiveId, principal.getName(), request);
        } catch (Exception e) {
            log.error("Error sending WebSocket message to hive {} from user {}: {}",
                    hiveId, principal.getName(), e.getMessage(), e);
        }
    }

    /**
     * Edit a message via WebSocket.
     */
    @MessageMapping("/message/{messageId}/edit")
    public void editMessage(
            @DestinationVariable String messageId,
            @Payload String newContent,
            Principal principal) {

        log.debug("WebSocket edit message request for message {} from user {}: {}",
                messageId, principal.getName(), newContent);

        try {
            // Edit message through service (which will broadcast via WebSocket)
            chatService.editMessage(messageId, principal.getName(), newContent);
        } catch (Exception e) {
            log.error("Error editing WebSocket message {} from user {}: {}",
                    messageId, principal.getName(), e.getMessage(), e);
        }
    }

    /**
     * Delete a message via WebSocket.
     */
    @MessageMapping("/message/{messageId}/delete")
    public void deleteMessage(
            @DestinationVariable String messageId,
            Principal principal) {

        log.debug("WebSocket delete message request for message {} from user {}",
                messageId, principal.getName());

        try {
            // Delete message through service (which will broadcast via WebSocket)
            chatService.deleteMessage(messageId, principal.getName());
        } catch (Exception e) {
            log.error("Error deleting WebSocket message {} from user {}: {}",
                    messageId, principal.getName(), e.getMessage(), e);
        }
    }

    /**
     * Mark message as read via WebSocket.
     */
    @MessageMapping("/message/{messageId}/read")
    public void markAsRead(
            @DestinationVariable String messageId,
            Principal principal) {

        log.debug("WebSocket mark as read request for message {} from user {}",
                messageId, principal.getName());

        try {
            chatService.markMessageAsRead(messageId, principal.getName());
        } catch (Exception e) {
            log.error("Error marking WebSocket message {} as read from user {}: {}",
                    messageId, principal.getName(), e.getMessage(), e);
        }
    }

    // ADVANCED FEATURES TEMPORARILY DISABLED
    // TODO: Re-enable these WebSocket endpoints in future iterations:
    // - Reaction endpoints (/message/{messageId}/reaction)
    // - Threading endpoints (/thread/{threadId}/*)
    // - Pinned message endpoints (/message/{messageId}/pin, /message/{messageId}/unpin)
    // - Typing indicator endpoints (/hive/{hiveId}/typing)
    // - Read receipt endpoints (/message/{messageId}/read-receipt)
}