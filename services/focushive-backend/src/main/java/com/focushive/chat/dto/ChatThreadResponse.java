package com.focushive.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for chat thread information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatThreadResponse {

    private String id;
    private String hiveId;
    private String parentMessageId;
    private String title;
    private Integer replyCount;
    private LocalDateTime lastActivityAt;
    private String lastReplyUserId;
    private String lastReplyUsername;
    private Boolean archived;
    private LocalDateTime createdAt;
    private boolean isActive;

    // Parent message preview
    private String parentMessageContent;
    private String parentMessageSender;
    private LocalDateTime parentMessageCreatedAt;
}