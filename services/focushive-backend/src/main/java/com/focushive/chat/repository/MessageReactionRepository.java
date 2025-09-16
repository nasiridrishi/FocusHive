package com.focushive.chat.repository;

import com.focushive.chat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MessageReaction entity.
 */
@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, String> {

    /**
     * Find all reactions for a specific message.
     */
    List<MessageReaction> findByMessageIdOrderByCreatedAtAsc(String messageId);

    /**
     * Find all reactions for multiple messages.
     */
    @Query("SELECT r FROM MessageReaction r WHERE r.messageId IN :messageIds ORDER BY r.createdAt ASC")
    List<MessageReaction> findByMessageIdInOrderByCreatedAtAsc(@Param("messageIds") List<String> messageIds);

    /**
     * Find a specific reaction by message, user, and emoji.
     */
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(String messageId, String userId, String emoji);

    /**
     * Find all reactions by a user for a specific message.
     */
    List<MessageReaction> findByMessageIdAndUserId(String messageId, String userId);

    /**
     * Count reactions for a specific message.
     */
    long countByMessageId(String messageId);

    /**
     * Count reactions of a specific emoji for a message.
     */
    long countByMessageIdAndEmoji(String messageId, String emoji);

    /**
     * Check if a user has reacted to a message with a specific emoji.
     */
    boolean existsByMessageIdAndUserIdAndEmoji(String messageId, String userId, String emoji);

    /**
     * Delete all reactions for a specific message.
     */
    void deleteByMessageId(String messageId);

    /**
     * Delete a specific user's reaction to a message.
     */
    void deleteByMessageIdAndUserIdAndEmoji(String messageId, String userId, String emoji);

    /**
     * Get reaction summary for a message (emoji and count).
     */
    @Query("SELECT r.emoji, COUNT(r) as count FROM MessageReaction r WHERE r.messageId = :messageId GROUP BY r.emoji ORDER BY count DESC")
    List<Object[]> getReactionSummary(@Param("messageId") String messageId);

    /**
     * Get top reactions across all messages in a hive.
     */
    @Query("SELECT r.emoji, COUNT(r) as count FROM MessageReaction r " +
           "JOIN ChatMessage m ON r.messageId = m.id " +
           "WHERE m.hiveId = :hiveId " +
           "GROUP BY r.emoji ORDER BY count DESC")
    List<Object[]> getTopReactionsByHive(@Param("hiveId") String hiveId);
}