package com.focushive.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for message reaction information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReactionResponse {

    private String id;
    private String messageId;
    private String userId;
    private String username;
    private String emoji;
    private LocalDateTime createdAt;
}