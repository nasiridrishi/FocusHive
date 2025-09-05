package com.focushive.chat.dto;

import com.focushive.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for chat message responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String id;
    private String hiveId;
    private String senderId;
    private String senderUsername;
    private String content;
    private ChatMessage.MessageType messageType;
    private boolean edited;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
}