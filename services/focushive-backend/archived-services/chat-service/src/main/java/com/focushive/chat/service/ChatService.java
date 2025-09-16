package com.focushive.chat.service;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.TypingIndicatorDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatService {
    
    /**
     * Send a new message to a hive
     */
    ChatMessageDto sendMessage(ChatMessageDto messageDto, UUID senderId, String senderUsername);
    
    /**
     * Get paginated messages for a hive
     */
    Page<ChatMessageDto> getHiveMessages(UUID hiveId, Pageable pageable);
    
    /**
     * Get recent messages for a hive (last N messages)
     */
    List<ChatMessageDto> getRecentMessages(UUID hiveId, int limit);
    
    /**
     * Get messages after a specific timestamp
     */
    List<ChatMessageDto> getMessagesAfter(UUID hiveId, ZonedDateTime after);
    
    /**
     * Search messages in a hive
     */
    Page<ChatMessageDto> searchMessages(UUID hiveId, String searchTerm, Pageable pageable);
    
    /**
     * Edit an existing message
     */
    ChatMessageDto editMessage(UUID messageId, String newContent, UUID userId);
    
    /**
     * Delete a message (soft delete by marking as deleted)
     */
    void deleteMessage(UUID messageId, UUID userId);
    
    /**
     * Get message count for a hive
     */
    long getMessageCount(UUID hiveId);
    
    /**
     * Set typing indicator for a user in a hive
     */
    void setTypingIndicator(UUID hiveId, UUID userId, String username);
    
    /**
     * Remove typing indicator for a user in a hive
     */
    void removeTypingIndicator(UUID hiveId, UUID userId);
    
    /**
     * Get active typing indicators for a hive
     */
    List<TypingIndicatorDto> getActiveTypingIndicators(UUID hiveId);
    
    /**
     * Clean up expired typing indicators
     */
    void cleanupExpiredTypingIndicators();
    
    /**
     * Get chat statistics for a hive on a specific date
     */
    Object getChatStatistics(UUID hiveId, ZonedDateTime date);
    
    /**
     * Validate user permission to access hive chat
     */
    boolean canUserAccessHiveChat(UUID userId, UUID hiveId);
}