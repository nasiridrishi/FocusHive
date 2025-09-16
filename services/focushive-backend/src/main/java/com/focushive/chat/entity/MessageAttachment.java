package com.focushive.chat.entity;

import com.focushive.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity representing a file attachment to a chat message.
 */
@Entity
@Table(name = "message_attachments",
       indexes = {
           @Index(name = "idx_message_attachments_message", columnList = "message_id"),
           @Index(name = "idx_message_attachments_type", columnList = "file_type")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class MessageAttachment extends BaseEntity {

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "download_count")
    @Builder.Default
    private Long downloadCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private ChatMessage message;

    /**
     * Check if the file is an image.
     */
    @Transient
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Check if the file is a video.
     */
    @Transient
    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    /**
     * Check if the file is a document.
     */
    @Transient
    public boolean isDocument() {
        return mimeType != null && (
            mimeType.contains("pdf") ||
            mimeType.contains("document") ||
            mimeType.contains("text") ||
            mimeType.contains("spreadsheet") ||
            mimeType.contains("presentation")
        );
    }

    /**
     * Get human-readable file size.
     */
    @Transient
    public String getFormattedFileSize() {
        if (fileSize == null) return "Unknown";

        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
    }
}