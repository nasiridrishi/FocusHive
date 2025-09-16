package com.focushive.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for typing indicator WebSocket messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorMessage {

    private String hiveId;
    private String userId;
    private String username;
    private boolean typing;
    private String messageType = "TYPING_INDICATOR";
}