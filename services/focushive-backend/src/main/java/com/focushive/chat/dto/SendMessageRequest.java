package com.focushive.chat.dto;

import com.focushive.chat.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO for sending a chat message.
 * Enhanced with threading and attachment support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Message content is required")
    @Size(max = 4000, message = "Message content cannot exceed 4000 characters")
    private String content;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    // Threading support
    private String parentMessageId;
    private String threadId;

    // Attachments
    private List<MultipartFile> attachments;

    // File upload metadata
    private List<String> allowedFileTypes;
    private Long maxFileSize;

    private String hiveId; // Optional, can be sent via path or WebSocket destination
}