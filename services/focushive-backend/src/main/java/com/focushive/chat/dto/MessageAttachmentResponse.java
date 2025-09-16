package com.focushive.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for message attachment information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAttachmentResponse {

    private String id;
    private String messageId;
    private String originalFilename;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String formattedFileSize;
    private String downloadUrl;
    private String thumbnailUrl;
    private Long downloadCount;
    private LocalDateTime createdAt;
    private boolean isImage;
    private boolean isVideo;
    private boolean isDocument;
}