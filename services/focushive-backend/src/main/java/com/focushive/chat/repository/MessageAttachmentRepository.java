package com.focushive.chat.repository;

import com.focushive.chat.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MessageAttachment entity.
 */
@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, String> {

    /**
     * Find all attachments for a specific message.
     */
    List<MessageAttachment> findByMessageIdOrderByCreatedAtAsc(String messageId);

    /**
     * Find all attachments for multiple messages.
     */
    @Query("SELECT a FROM MessageAttachment a WHERE a.messageId IN :messageIds ORDER BY a.createdAt ASC")
    List<MessageAttachment> findByMessageIdInOrderByCreatedAtAsc(@Param("messageIds") List<String> messageIds);

    /**
     * Find attachment by stored filename.
     */
    Optional<MessageAttachment> findByStoredFilename(String storedFilename);

    /**
     * Find all attachments of a specific type.
     */
    List<MessageAttachment> findByFileTypeOrderByCreatedAtDesc(String fileType);

    /**
     * Count attachments for a specific message.
     */
    long countByMessageId(String messageId);

    /**
     * Find all attachments in a hive.
     */
    @Query("SELECT a FROM MessageAttachment a " +
           "JOIN ChatMessage m ON a.messageId = m.id " +
           "WHERE m.hiveId = :hiveId " +
           "ORDER BY a.createdAt DESC")
    List<MessageAttachment> findByHiveIdOrderByCreatedAtDesc(@Param("hiveId") String hiveId);

    /**
     * Find attachments by file type in a hive.
     */
    @Query("SELECT a FROM MessageAttachment a " +
           "JOIN ChatMessage m ON a.messageId = m.id " +
           "WHERE m.hiveId = :hiveId AND a.fileType = :fileType " +
           "ORDER BY a.createdAt DESC")
    List<MessageAttachment> findByHiveIdAndFileTypeOrderByCreatedAtDesc(
            @Param("hiveId") String hiveId,
            @Param("fileType") String fileType);

    /**
     * Find large attachments (over specified size).
     */
    @Query("SELECT a FROM MessageAttachment a WHERE a.fileSize > :minSize ORDER BY a.fileSize DESC")
    List<MessageAttachment> findLargeAttachments(@Param("minSize") Long minSize);

    /**
     * Calculate total storage used by a hive.
     */
    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM MessageAttachment a " +
           "JOIN ChatMessage m ON a.messageId = m.id " +
           "WHERE m.hiveId = :hiveId")
    Long getTotalStorageByHive(@Param("hiveId") String hiveId);

    /**
     * Find attachments uploaded after a specific date.
     */
    @Query("SELECT a FROM MessageAttachment a WHERE a.createdAt > :after ORDER BY a.createdAt DESC")
    List<MessageAttachment> findAttachmentsAfter(@Param("after") LocalDateTime after);

    /**
     * Delete all attachments for a specific message.
     */
    void deleteByMessageId(String messageId);

    /**
     * Find orphaned attachments (messages that might have been deleted).
     */
    @Query("SELECT a FROM MessageAttachment a " +
           "WHERE NOT EXISTS (SELECT 1 FROM ChatMessage m WHERE m.id = a.messageId)")
    List<MessageAttachment> findOrphanedAttachments();

    /**
     * Get attachment statistics by file type.
     */
    @Query("SELECT a.fileType, COUNT(a), AVG(a.fileSize), SUM(a.fileSize) " +
           "FROM MessageAttachment a GROUP BY a.fileType ORDER BY COUNT(a) DESC")
    List<Object[]> getAttachmentStatistics();

    /**
     * Find most downloaded attachments.
     */
    @Query("SELECT a FROM MessageAttachment a WHERE a.downloadCount > 0 ORDER BY a.downloadCount DESC")
    List<MessageAttachment> findMostDownloaded();
}