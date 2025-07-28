package com.focushive.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated message history response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryResponse {
    private List<ChatMessageDto> messages;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}