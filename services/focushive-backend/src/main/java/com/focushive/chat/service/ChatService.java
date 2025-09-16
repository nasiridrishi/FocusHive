package com.focushive.chat.service;

import com.focushive.chat.dto.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Simplified chat service interface with essential methods only.
 * Advanced features (threading, reactions, attachments) are commented out for now.
 */
public interface ChatService {

    // CORE MESSAGING METHODS ONLY

    /**
     * Send a message to a hive.
     */
    ChatMessageDto sendMessage(String hiveId, String senderId, SendMessageRequest request);

    /**
     * Get message history for a hive.
     */
    MessageHistoryResponse getMessageHistory(String hiveId, String userId, Pageable pageable);

    /**
     * Get recent messages for a hive.
     */
    List<ChatMessageDto> getRecentMessages(String hiveId, String userId, int limit);

    /**
     * Get messages after a specific timestamp.
     */
    List<ChatMessageDto> getMessagesAfter(String hiveId, String userId, LocalDateTime after);

    /**
     * Edit a message.
     */
    ChatMessageDto editMessage(String messageId, String userId, String newContent);

    /**
     * Delete a message (soft delete by replacing content).
     */
    void deleteMessage(String messageId, String userId);

    // SYSTEM MESSAGES

    /**
     * Send a system message to a hive.
     */
    ChatMessageDto sendSystemMessage(String hiveId, String content);

    /**
     * Broadcast user joined message.
     */
    void broadcastUserJoined(String hiveId, String userId, String username);

    /**
     * Broadcast user left message.
     */
    void broadcastUserLeft(String hiveId, String userId, String username);

    // BASIC REAL-TIME FEATURES

    /**
     * Mark message as read by user.
     */
    void markMessageAsRead(String messageId, String userId);

    // STATISTICS

    /**
     * Get chat statistics for a hive.
     */
    Object getHiveChatStatistics(String hiveId, String userId);

    /**
     * Get user activity in hive chat.
     */
    Object getUserChatActivity(String hiveId, String userId);

    /*
     * ADVANCED FEATURES COMMENTED OUT FOR SIMPLIFICATION
     * TODO: Re-enable these features in future iterations
     *
     * - Threading (getThreadReplies, getThreadInfo, getActiveThreads, archiveThread)
     * - Reactions (addReaction, removeReaction, getMessageReactions, getMessageReactionSummary)
     * - Attachments (getMessageAttachments, getAttachmentDownloadUrl, getHiveAttachments)
     * - Search (searchMessages, searchMessagesByContent, searchMessagesBySender)
     * - Pinned Messages (pinMessage, unpinMessage, getPinnedMessages)
     * - Advanced Real-time (broadcastTypingIndicator, broadcastMessageUpdate)
     */
}