package com.focushive.chat.repository;

import com.focushive.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    /**
     * Find messages by hive ID with pagination, ordered by creation time
     */
    Page<ChatMessage> findByHiveIdOrderByCreatedAtDesc(UUID hiveId, Pageable pageable);
    
    /**
     * Find recent messages for a hive (last N messages)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId ORDER BY m.createdAt DESC LIMIT :limit")
    List<ChatMessage> findRecentMessagesInHive(@Param("hiveId") UUID hiveId, @Param("limit") int limit);
    
    /**
     * Find messages in a hive after a specific timestamp
     */
    List<ChatMessage> findByHiveIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID hiveId, ZonedDateTime after);
    
    /**
     * Find messages by sender in a specific hive
     */
    Page<ChatMessage> findByHiveIdAndSenderIdOrderByCreatedAtDesc(UUID hiveId, UUID senderId, Pageable pageable);
    
    /**
     * Count total messages in a hive
     */
    long countByHiveId(UUID hiveId);
    
    /**
     * Count messages in a hive for a specific date range
     */
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.hiveId = :hiveId AND m.createdAt BETWEEN :start AND :end")
    long countMessagesInHiveBetween(@Param("hiveId") UUID hiveId, 
                                   @Param("start") ZonedDateTime start, 
                                   @Param("end") ZonedDateTime end);
    
    /**
     * Find messages by content search (case-insensitive)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.hiveId = :hiveId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchMessagesInHive(@Param("hiveId") UUID hiveId, 
                                          @Param("searchTerm") String searchTerm, 
                                          Pageable pageable);
    
    /**
     * Get message statistics for a hive on a specific date
     */
    @Query("""
        SELECT new map(
            COUNT(m) as messageCount,
            COUNT(DISTINCT m.senderId) as activeUsers,
            EXTRACT(HOUR FROM m.createdAt) as hour
        ) 
        FROM ChatMessage m 
        WHERE m.hiveId = :hiveId 
        AND DATE(m.createdAt) = DATE(:date)
        GROUP BY EXTRACT(HOUR FROM m.createdAt)
        ORDER BY hour
    """)
    List<Object> getHourlyMessageStatistics(@Param("hiveId") UUID hiveId, @Param("date") ZonedDateTime date);
    
    /**
     * Delete old messages (for cleanup operations)
     */
    void deleteByCreatedAtBefore(ZonedDateTime cutoffDate);
    
    /**
     * Find all hive IDs that have messages (for statistics generation)
     */
    @Query("SELECT DISTINCT m.hiveId FROM ChatMessage m")
    List<UUID> findAllHiveIdsWithMessages();
}