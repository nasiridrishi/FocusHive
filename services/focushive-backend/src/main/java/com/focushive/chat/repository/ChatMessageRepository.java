package com.focushive.chat.repository;

import com.focushive.chat.entity.ChatMessage;
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
     * Find last N messages in a hive.
     */
    @Query(value = "SELECT * FROM chat_messages WHERE hive_id = :hiveId " +
            "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findLastMessagesInHive(@Param("hiveId") String hiveId, @Param("limit") int limit);
    
    /**
     * Delete all messages in a hive (for cleanup).
     */
    void deleteByHiveId(String hiveId);
}