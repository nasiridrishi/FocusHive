package com.focushive.chat.repository;

import com.focushive.chat.entity.ChatThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ChatThread entity.
 */
@Repository
public interface ChatThreadRepository extends JpaRepository<ChatThread, String> {

    /**
     * Find thread by parent message ID.
     */
    Optional<ChatThread> findByParentMessageId(String parentMessageId);

    /**
     * Find all threads in a hive.
     */
    List<ChatThread> findByHiveIdOrderByLastActivityAtDesc(String hiveId);

    /**
     * Find active threads in a hive (paginated).
     */
    @Query("SELECT t FROM ChatThread t WHERE t.hiveId = :hiveId AND t.archived = false ORDER BY t.lastActivityAt DESC")
    Page<ChatThread> findActiveThreadsByHive(@Param("hiveId") String hiveId, Pageable pageable);

    /**
     * Find archived threads in a hive.
     */
    @Query("SELECT t FROM ChatThread t WHERE t.hiveId = :hiveId AND t.archived = true ORDER BY t.lastActivityAt DESC")
    List<ChatThread> findArchivedThreadsByHive(@Param("hiveId") String hiveId);

    /**
     * Find threads with recent activity.
     */
    @Query("SELECT t FROM ChatThread t WHERE t.hiveId = :hiveId AND t.lastActivityAt > :since AND t.archived = false ORDER BY t.lastActivityAt DESC")
    List<ChatThread> findRecentlyActiveThreads(@Param("hiveId") String hiveId, @Param("since") LocalDateTime since);

    /**
     * Find threads by reply count range.
     */
    @Query("SELECT t FROM ChatThread t WHERE t.hiveId = :hiveId AND t.replyCount >= :minReplies AND t.archived = false ORDER BY t.replyCount DESC")
    List<ChatThread> findPopularThreads(@Param("hiveId") String hiveId, @Param("minReplies") Integer minReplies);

    /**
     * Count active threads in a hive.
     */
    @Query("SELECT COUNT(t) FROM ChatThread t WHERE t.hiveId = :hiveId AND t.archived = false")
    long countActiveThreadsByHive(@Param("hiveId") String hiveId);

    /**
     * Find threads that need archiving (inactive for a period).
     */
    @Query("SELECT t FROM ChatThread t WHERE t.lastActivityAt < :cutoffDate AND t.archived = false")
    List<ChatThread> findThreadsToArchive(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get thread statistics for a hive.
     */
    @Query("SELECT COUNT(t), AVG(t.replyCount), MAX(t.replyCount), MIN(t.lastActivityAt), MAX(t.lastActivityAt) " +
           "FROM ChatThread t WHERE t.hiveId = :hiveId AND t.archived = false")
    Object[] getThreadStatistics(@Param("hiveId") String hiveId);

    /**
     * Find threads by title (search).
     */
    @Query("SELECT t FROM ChatThread t WHERE t.hiveId = :hiveId AND LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND t.archived = false")
    List<ChatThread> searchThreadsByTitle(@Param("hiveId") String hiveId, @Param("searchTerm") String searchTerm);

    /**
     * Find most active contributors in threads.
     */
    @Query("SELECT t.lastReplyUserId, t.lastReplyUsername, COUNT(t) " +
           "FROM ChatThread t WHERE t.hiveId = :hiveId AND t.lastReplyUserId IS NOT NULL " +
           "GROUP BY t.lastReplyUserId, t.lastReplyUsername ORDER BY COUNT(t) DESC")
    List<Object[]> findMostActiveThreadContributors(@Param("hiveId") String hiveId);

    /**
     * Delete threads by hive ID.
     */
    void deleteByHiveId(String hiveId);

    /**
     * Archive old threads.
     */
    @Query("UPDATE ChatThread t SET t.archived = true WHERE t.lastActivityAt < :cutoffDate AND t.archived = false")
    int archiveOldThreads(@Param("cutoffDate") LocalDateTime cutoffDate);
}