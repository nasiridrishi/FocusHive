package com.focushive.hive.repository;

import com.focushive.hive.entity.ChatMessage;
import com.focushive.hive.entity.ChatMessage.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    
    // Find messages by hive
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId AND cm.deletedAt IS NULL " +
           "ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByHiveId(@Param("hiveId") String hiveId, Pageable pageable);
    
    // Find messages after certain time
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.createdAt > :after AND cm.deletedAt IS NULL " +
           "ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByHiveIdAfter(
        @Param("hiveId") String hiveId, 
        @Param("after") LocalDateTime after);
    
    // Find messages before certain time
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.createdAt < :before AND cm.deletedAt IS NULL " +
           "ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByHiveIdBefore(
        @Param("hiveId") String hiveId, 
        @Param("before") LocalDateTime before,
        Pageable pageable);
    
    // Find by sender
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sender.id = :senderId " +
           "AND cm.deletedAt IS NULL ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findBySenderId(@Param("senderId") String senderId, Pageable pageable);
    
    // Find thread messages
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.parent.id = :parentId " +
           "AND cm.deletedAt IS NULL ORDER BY cm.createdAt ASC")
    List<ChatMessage> findThreadMessages(@Param("parentId") String parentId);
    
    // Find pinned messages
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.isPinned = true AND cm.deletedAt IS NULL " +
           "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findPinnedMessages(@Param("hiveId") String hiveId);
    
    // Find by type
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.type = :type AND cm.deletedAt IS NULL " +
           "ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByHiveIdAndType(
        @Param("hiveId") String hiveId,
        @Param("type") MessageType type,
        Pageable pageable);
    
    // Search messages
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND LOWER(cm.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND cm.deletedAt IS NULL ORDER BY cm.createdAt DESC")
    Page<ChatMessage> searchMessages(
        @Param("hiveId") String hiveId,
        @Param("query") String query,
        Pageable pageable);
    
    // Count messages
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.deletedAt IS NULL")
    long countByHiveId(@Param("hiveId") String hiveId);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.createdAt > :since AND cm.deletedAt IS NULL")
    long countRecentMessages(
        @Param("hiveId") String hiveId,
        @Param("since") LocalDateTime since);
    
    // Update operations
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.content = :content, " +
           "cm.isEdited = true, cm.editedAt = :now " +
           "WHERE cm.id = :messageId AND cm.sender.id = :senderId")
    void editMessage(
        @Param("messageId") String messageId,
        @Param("senderId") String senderId,
        @Param("content") String content,
        @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.isPinned = :pinned " +
           "WHERE cm.id = :messageId")
    void setPinned(
        @Param("messageId") String messageId,
        @Param("pinned") Boolean pinned);
    
    // Soft delete
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.deletedAt = :now " +
           "WHERE cm.id = :messageId AND cm.sender.id = :senderId")
    void softDelete(
        @Param("messageId") String messageId,
        @Param("senderId") String senderId,
        @Param("now") LocalDateTime now);
    
    // Update reactions
    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.reactions = :reactions " +
           "WHERE cm.id = :messageId")
    void updateReactions(
        @Param("messageId") String messageId,
        @Param("reactions") String reactions);
    
    // Get last message
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.deletedAt IS NULL ORDER BY cm.createdAt DESC")
    Optional<ChatMessage> findLastMessage(@Param("hiveId") String hiveId, Pageable pageable);
    
    // Cleanup old deleted messages
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.deletedAt < :before")
    int cleanupDeletedMessages(@Param("before") LocalDateTime before);
    
    // Get message context
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.hive.id = :hiveId " +
           "AND cm.createdAt >= :start AND cm.createdAt <= :end " +
           "AND cm.deletedAt IS NULL ORDER BY cm.createdAt ASC")
    List<ChatMessage> getMessageContext(
        @Param("hiveId") String hiveId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end);
}