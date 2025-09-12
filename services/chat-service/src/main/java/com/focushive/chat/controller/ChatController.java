package com.focushive.chat.controller;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.TypingIndicatorDto;
import com.focushive.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Chat messaging operations")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private ChatService chatService;
    
    @PostMapping("/hives/{hiveId}/messages")
    @Operation(summary = "Send a message to a hive chat")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToChat(#hiveId)")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @Parameter(description = "Hive ID") @PathVariable UUID hiveId,
            @Valid @RequestBody ChatMessageDto messageDto,
            Authentication authentication) {
        
        logger.info("Sending message to hive: {}", hiveId);
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        String username = jwt.getClaimAsString("preferred_username");
        
        // Verify user can access this hive
        if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        messageDto.setHiveId(hiveId);
        ChatMessageDto sentMessage = chatService.sendMessage(messageDto, userId, username);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(sentMessage);
    }
    
    @GetMapping("/hives/{hiveId}/messages")
    @Operation(summary = "Get paginated messages for a hive")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToChat(#hiveId)")
    public ResponseEntity<Page<ChatMessageDto>> getHiveMessages(
            @Parameter(description = "Hive ID") @PathVariable UUID hiveId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        
        logger.debug("Fetching messages for hive: {} (page: {}, size: {})", hiveId, page, size);
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        // Verify user can access this hive
        if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Limit max size
        Page<ChatMessageDto> messages = chatService.getHiveMessages(hiveId, pageable);
        
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/hives/{hiveId}/messages/recent")
    @Operation(summary = "Get recent messages for a hive")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToChat(#hiveId)")
    public ResponseEntity<List<ChatMessageDto>> getRecentMessages(
            @Parameter(description = "Hive ID") @PathVariable UUID hiveId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication) {
        
        logger.debug("Fetching {} recent messages for hive: {}", limit, hiveId);
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        // Verify user can access this hive
        if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<ChatMessageDto> messages = chatService.getRecentMessages(hiveId, Math.min(limit, 100));
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/hives/{hiveId}/messages/since")
    @Operation(summary = "Get messages since a specific timestamp")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToChat(#hiveId)")
    public ResponseEntity<List<ChatMessageDto>> getMessagesSince(
            @Parameter(description = "Hive ID") @PathVariable UUID hiveId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime since,
            Authentication authentication) {
        
        logger.debug("Fetching messages for hive: {} since: {}", hiveId, since);
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        // Verify user can access this hive
        if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<ChatMessageDto> messages = chatService.getMessagesAfter(hiveId, since);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/hives/{hiveId}/messages/search")
    @Operation(summary = "Search messages in a hive")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToChat(#hiveId)")
    public ResponseEntity<Page<ChatMessageDto>> searchMessages(
            @Parameter(description = "Hive ID") @PathVariable UUID hiveId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        logger.debug("Searching messages in hive: {} for: {}", hiveId, q);
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        // Verify user can access this hive
        if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ChatMessageDto> messages = chatService.searchMessages(hiveId, q, pageable);
        
        return ResponseEntity.ok(messages);
    }
    
    @PutMapping("/messages/{messageId}")
    @Operation(summary = "Edit a message")
    @PreAuthorize("isAuthenticated() and @securityService.isMessageOwner(#messageId)")
    public ResponseEntity<ChatMessageDto> editMessage(
            @Parameter(description = "Message ID") @PathVariable UUID messageId,
            @RequestBody @Valid ChatMessageDto messageDto,
            Authentication authentication) {
        
        logger.info("Editing message: {}", messageId);
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        try {
            ChatMessageDto editedMessage = chatService.editMessage(messageId, messageDto.getContent(), userId);
            return ResponseEntity.ok(editedMessage);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete a message")
    @PreAuthorize("isAuthenticated() and (@securityService.isMessageOwner(#messageId) or @securityService.canModerateHive(null))")
    public ResponseEntity<Void> deleteMessage(
            @Parameter(description = "Message ID") @PathVariable UUID messageId,
            Authentication authentication) {
        
        logger.info("Deleting message: {}", messageId);
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        try {
            chatService.deleteMessage(messageId, userId);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/hives/{hiveId}/typing")
    @Operation(summary = "Get active typing indicators for a hive")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToChat(#hiveId)")
    public ResponseEntity<List<TypingIndicatorDto>> getTypingIndicators(
            @Parameter(description = "Hive ID") @PathVariable UUID hiveId,
            Authentication authentication) {
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        // Verify user can access this hive
        if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<TypingIndicatorDto> indicators = chatService.getActiveTypingIndicators(hiveId);
        return ResponseEntity.ok(indicators);
    }
    
    @GetMapping("/hives/{hiveId}/stats")
    @Operation(summary = "Get chat statistics for a hive")
    @PreAuthorize("isAuthenticated() and @securityService.hasAccessToChat(#hiveId)")
    public ResponseEntity<Object> getChatStatistics(
            @Parameter(description = "Hive ID") @PathVariable UUID hiveId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime date,
            Authentication authentication) {
        
        // Extract user information from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));
        
        // Verify user can access this hive
        if (!chatService.canUserAccessHiveChat(userId, hiveId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        ZonedDateTime targetDate = date != null ? date : ZonedDateTime.now();
        Object stats = chatService.getChatStatistics(hiveId, targetDate);
        
        return ResponseEntity.ok(stats);
    }
}