package com.focushive.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for searching chat messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchRequest {

    @Size(min = 1, max = 200, message = "Search query must be between 1 and 200 characters")
    private String query;

    private String senderUsername;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Boolean hasAttachments;
    private Boolean hasReactions;
    private Boolean isPinned;

    @Min(value = 0, message = "Page must be non-negative")
    private Integer page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size cannot exceed 100")
    private Integer size = 20;

    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}