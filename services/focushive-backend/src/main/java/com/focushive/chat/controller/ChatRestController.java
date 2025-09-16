package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.dto.SendMessageRequest;
import com.focushive.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Simplified REST controller for chat functionality.
 * Advanced features (threading, reactions, attachments, search, pinned messages)
 * are temporarily disabled during the simplification phase.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat messaging API - Simplified version")
public class ChatRestController {

    private final ChatService chatService;

    // CORE MESSAGING - KEPT

    /**
     * Send a message to a hive.
     */
    @PostMapping("/hives/{hiveId}/messages")
    @Operation(summary = "Send message", description = "Send a message to a hive")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable String hiveId,
            @RequestBody @Valid SendMessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ChatMessageDto message = chatService.sendMessage(hiveId, userDetails.getUsername(), request);
        return ResponseEntity.ok(message);
    }

    /**
     * Get message history for a hive.
     */
    @GetMapping("/hives/{hiveId}/messages")
    @Operation(summary = "Get message history", description = "Get paginated message history for a hive")
    public ResponseEntity<MessageHistoryResponse> getMessageHistory(
            @PathVariable String hiveId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @AuthenticationPrincipal UserDetails userDetails) {

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy);
        MessageHistoryResponse response = chatService.getMessageHistory(
                hiveId, userDetails.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent messages for a hive.
     */
    @GetMapping("/hives/{hiveId}/messages/recent")
    @Operation(summary = "Get recent messages", description = "Get the most recent messages in a hive")
    public ResponseEntity<List<ChatMessageDto>> getRecentMessages(
            @PathVariable String hiveId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ChatMessageDto> messages = chatService.getRecentMessages(
                hiveId, userDetails.getUsername(), limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get messages after a specific timestamp.
     */
    @GetMapping("/hives/{hiveId}/messages/after")
    @Operation(summary = "Get messages after timestamp", description = "Get messages created after a specific timestamp")
    public ResponseEntity<List<ChatMessageDto>> getMessagesAfter(
            @PathVariable String hiveId,
            @RequestParam String after,
            @AuthenticationPrincipal UserDetails userDetails) {

        LocalDateTime timestamp = LocalDateTime.parse(after);
        List<ChatMessageDto> messages = chatService.getMessagesAfter(
                hiveId, userDetails.getUsername(), timestamp);
        return ResponseEntity.ok(messages);
    }

    /**
     * Edit a message.
     */
    @PutMapping("/messages/{messageId}")
    @Operation(summary = "Edit message", description = "Edit an existing message")
    public ResponseEntity<ChatMessageDto> editMessage(
            @PathVariable String messageId,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails) {

        ChatMessageDto message = chatService.editMessage(messageId, userDetails.getUsername(), content);
        return ResponseEntity.ok(message);
    }

    /**
     * Delete a message.
     */
    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete message", description = "Delete a message (soft delete)")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {

        chatService.deleteMessage(messageId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Mark message as read.
     */
    @PostMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark as read", description = "Mark a message as read by the current user")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal UserDetails userDetails) {

        chatService.markMessageAsRead(messageId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Get chat statistics for a hive.
     */
    @GetMapping("/hives/{hiveId}/stats")
    @Operation(summary = "Get chat stats", description = "Get chat statistics for a hive")
    public ResponseEntity<Object> getHiveChatStats(
            @PathVariable String hiveId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Object stats = chatService.getHiveChatStatistics(hiveId, userDetails.getUsername());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get user chat activity.
     */
    @GetMapping("/hives/{hiveId}/activity")
    @Operation(summary = "Get user activity", description = "Get user's chat activity in a hive")
    public ResponseEntity<Object> getUserChatActivity(
            @PathVariable String hiveId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Object activity = chatService.getUserChatActivity(hiveId, userDetails.getUsername());
        return ResponseEntity.ok(activity);
    }

    // ADVANCED FEATURES TEMPORARILY DISABLED
    // TODO: Re-enable these features in future iterations:
    // - Threading endpoints (/threads/*)
    // - Reaction endpoints (/messages/*/reactions/*)
    // - Attachment endpoints (/messages/*/attachments/*)
    // - Search endpoints (/hives/*/search/*)
    // - Pinned message endpoints (/messages/*/pin, /hives/*/pinned)
}