package com.focushive.chat.repository;

import com.focushive.chat.entity.ChatMessage;
import com.focushive.chat.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for chat message operations.
 * Enhanced with threading, search, and advanced query capabilities.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    /**
     * Find messages by hive ID with pagination.
     */
    Page<ChatMessage> findByHiveIdOrderByCreatedAtDesc(String hiveId, Pageable pageable);

    /**
     * Find messages in a hive after a specific timestamp.
     */
    List<ChatMessage> findByHiveIdAndCreatedAtAfterOrderByCreatedAtAsc(
            String hiveId, LocalDateTime after);

    /**
     * Count messages in a hive.
     */
    long countByHiveId(String hiveId);

    /**
     * Count messages in a hive after a specific timestamp.
     */
    long countByHiveIdAndCreatedAtAfter(String hiveId, LocalDateTime after);

    /**
     * Count messages by a specific sender in a hive.
     */
    long countByHiveIdAndSenderId(String hiveId, String senderId);

    /**
     * Find the last message by a specific sender in a hive.
     */
    java.util.Optional<ChatMessage> findTopByHiveIdAndSenderIdOrderByCreatedAtDesc(String hiveId, String senderId);

    /**
     * Find last N messages in a hive.
     */
    @Query(value = "SELECT * FROM chat_messages WHERE hive_id = :hiveId " +
            "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findLastMessagesInHive(@Param("hiveId") String hiveId, @Param("limit") int limit);

    /**
     * Delete all messages in a hive (for cleanup).
     */
    void deleteByHiveId(String hiveId);

    // NEW THREADING SUPPORT

    /**
     * Find replies to a specific message.
     */
    List<ChatMessage> findByParentMessageIdOrderByCreatedAtAsc(String parentMessageId);

    /**
     * Find messages in a specific thread.
     */
    List<ChatMessage> findByThreadIdOrderByCreatedAtAsc(String threadId);

    /**
     * Count replies to a message.
     */
    long countByParentMessageId(String parentMessageId);

    // SEARCH FUNCTIONALITY

    /**
     * Search messages by content in a hive.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> searchMessagesByContent(@Param("hiveId") String hiveId,
                                             @Param("searchTerm") String searchTerm);

    /**
     * Search messages by content with pagination.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<ChatMessage> searchMessagesByContent(@Param("hiveId") String hiveId,
                                             @Param("searchTerm") String searchTerm,
                                             Pageable pageable);

    /**
     * Search messages by sender.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND " +
           "LOWER(m.senderUsername) LIKE LOWER(CONCAT('%', :username, '%')) " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> searchMessagesBySender(@Param("hiveId") String hiveId,
                                           @Param("username") String username);

    // MESSAGE TYPE QUERIES

    /**
     * Find messages by type in a hive.
     */
    List<ChatMessage> findByHiveIdAndMessageTypeOrderByCreatedAtDesc(String hiveId, MessageType messageType);

    /**
     * Find system messages in a hive.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND " +
           "m.messageType IN ('SYSTEM', 'JOIN', 'LEAVE', 'ANNOUNCEMENT') " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findSystemMessages(@Param("hiveId") String hiveId);

    // PINNED MESSAGES

    /**
     * Find pinned messages in a hive.
     */
    List<ChatMessage> findByHiveIdAndPinnedTrueOrderByPinnedAtDesc(String hiveId);

    // ADVANCED QUERIES

    /**
     * Find messages with attachments.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND m.attachmentCount > 0 ORDER BY m.createdAt DESC")
    List<ChatMessage> findMessagesWithAttachments(@Param("hiveId") String hiveId);

    /**
     * Find messages with reactions.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND m.reactionCount > 0 ORDER BY m.reactionCount DESC")
    List<ChatMessage> findMessagesWithReactions(@Param("hiveId") String hiveId);

    /**
     * Find most active messages (high reply count).
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND m.replyCount > :minReplies ORDER BY m.replyCount DESC")
    List<ChatMessage> findMostActiveMessages(@Param("hiveId") String hiveId, @Param("minReplies") Integer minReplies);

    // DATE RANGE QUERIES

    /**
     * Find messages between dates.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND " +
           "m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<ChatMessage> findMessagesBetweenDates(@Param("hiveId") String hiveId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find recent edited messages.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND m.edited = true AND " +
           "m.editedAt > :since ORDER BY m.editedAt DESC")
    List<ChatMessage> findRecentlyEditedMessages(@Param("hiveId") String hiveId, @Param("since") LocalDateTime since);

    // STATISTICS

    /**
     * Get message count by sender in a hive.
     */
    @Query("SELECT m.senderId, m.senderUsername, COUNT(m) FROM ChatMessage m " +
           "WHERE m.hiveId = :hiveId GROUP BY m.senderId, m.senderUsername ORDER BY COUNT(m) DESC")
    List<Object[]> getMessageCountBySender(@Param("hiveId") String hiveId);

    /**
     * Get message count by date.
     */
    @Query("SELECT DATE(m.createdAt), COUNT(m) FROM ChatMessage m " +
           "WHERE m.hiveId = :hiveId AND m.createdAt > :since " +
           "GROUP BY DATE(m.createdAt) ORDER BY DATE(m.createdAt)")
    List<Object[]> getMessageCountByDate(@Param("hiveId") String hiveId, @Param("since") LocalDateTime since);

    /**
     * Get message statistics for a hive.
     */
    @Query("SELECT COUNT(m), COUNT(DISTINCT m.senderId), " +
           "AVG(m.reactionCount), AVG(m.replyCount), " +
           "MIN(m.createdAt), MAX(m.createdAt) " +
           "FROM ChatMessage m WHERE m.hiveId = :hiveId")
    Object[] getMessageStatistics(@Param("hiveId") String hiveId);

    // CLEANUP OPERATIONS

    /**
     * Find old messages for cleanup.
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.createdAt < :cutoffDate ORDER BY m.createdAt")
    List<ChatMessage> findOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete messages by sender (for user cleanup).
     */
    void deleteBySenderId(String senderId);
}