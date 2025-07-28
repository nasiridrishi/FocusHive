package com.focushive.chat.service;

import com.focushive.chat.dto.ChatMessageDto;
import com.focushive.chat.dto.MessageHistoryResponse;
import com.focushive.chat.dto.SendMessageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for chat functionality.
 */
public interface ChatService {
    
    /**
     * Send a message to a hive.
     * 
     * @param hiveId the hive ID
     * @param senderId the sender's user ID
     * @param request the message request
     * @return the created message
     */
    ChatMessageDto sendMessage(String hiveId, String senderId, SendMessageRequest request);
    
    /**
     * Get message history for a hive.
     * 
     * @param hiveId the hive ID
     * @param userId the requesting user ID (for permission check)
     * @param pageable pagination parameters
     * @return paginated message history
     */
    MessageHistoryResponse getMessageHistory(String hiveId, String userId, Pageable pageable);
    
    /**
     * Get recent messages for a hive.
     * 
     * @param hiveId the hive ID
     * @param userId the requesting user ID
     * @param limit number of messages to retrieve
     * @return list of recent messages
     */
    List<ChatMessageDto> getRecentMessages(String hiveId, String userId, int limit);
    
    /**
     * Get messages after a specific timestamp.
     * 
     * @param hiveId the hive ID
     * @param userId the requesting user ID
     * @param after timestamp to get messages after
     * @return list of messages
     */
    List<ChatMessageDto> getMessagesAfter(String hiveId, String userId, LocalDateTime after);
    
    /**
     * Edit a message.
     * 
     * @param messageId the message ID
     * @param userId the user attempting to edit
     * @param newContent the new content
     * @return the updated message
     */
    ChatMessageDto editMessage(String messageId, String userId, String newContent);
    
    /**
     * Delete a message (soft delete by replacing content).
     * 
     * @param messageId the message ID
     * @param userId the user attempting to delete
     */
    void deleteMessage(String messageId, String userId);
    
    /**
     * Send a system message to a hive.
     * 
     * @param hiveId the hive ID
     * @param content the system message content
     * @return the created message
     */
    ChatMessageDto sendSystemMessage(String hiveId, String content);
    
    /**
     * Broadcast user joined message.
     * 
     * @param hiveId the hive ID
     * @param userId the user who joined
     * @param username the username
     */
    void broadcastUserJoined(String hiveId, String userId, String username);
    
    /**
     * Broadcast user left message.
     * 
     * @param hiveId the hive ID
     * @param userId the user who left
     * @param username the username
     */
    void broadcastUserLeft(String hiveId, String userId, String username);
}