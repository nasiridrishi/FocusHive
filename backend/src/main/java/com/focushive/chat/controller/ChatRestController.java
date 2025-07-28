package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for chat message history and queries.
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat message operations")
public class ChatRestController {
    
    private final ChatService chatService;
    
    /**
     * Get paginated message history for a hive.
     */
    @GetMapping("/hives/{hiveId}/messages")
    @Operation(summary = "Get message history", 
               description = "Retrieve paginated message history for a hive")
    public ResponseEntity<MessageHistoryResponse> getMessageHistory(
            @PathVariable String hiveId,
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        MessageHistoryResponse response = chatService.getMessageHistory(
                hiveId, userDetails.getUsername(), pageable);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get recent messages for a hive.
     */
    @GetMapping("/hives/{hiveId}/messages/recent")
    @Operation(summary = "Get recent messages", 
               description = "Retrieve the most recent messages for quick loading")
    public ResponseEntity<List<ChatMessageDto>> getRecentMessages(
            @PathVariable String hiveId,
            @Parameter(description = "Number of messages to retrieve") 
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
    @Operation(summary = "Get messages after timestamp", 
               description = "Retrieve messages sent after a specific time")
    public ResponseEntity<List<ChatMessageDto>> getMessagesAfter(
            @PathVariable String hiveId,
            @Parameter(description = "Timestamp to get messages after") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        List<ChatMessageDto> messages = chatService.getMessagesAfter(
                hiveId, userDetails.getUsername(), after);
        
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Send a system announcement to a hive (admin only).
     */
    @PostMapping("/hives/{hiveId}/announce")
    @Operation(summary = "Send system announcement", 
               description = "Send a system message to a hive (moderator/owner only)")
    public ResponseEntity<ChatMessageDto> sendAnnouncement(
            @PathVariable String hiveId,
            @RequestBody String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // TODO: Add proper authorization check for moderator/owner
        ChatMessageDto message = chatService.sendSystemMessage(hiveId, content);
        
        return ResponseEntity.ok(message);
    }
}