package com.focushive.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for paginated notification lists.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    /**
     * List of notifications for the current page
     */
    private List<NotificationDto> notifications;

    /**
     * Current page number (0-based)
     */
    private int page;

    /**
     * Size of each page
     */
    private int size;

    /**
     * Total number of elements across all pages
     */
    private long totalElements;

    /**
     * Total number of pages
     */
    private int totalPages;

    /**
     * Whether this is the first page
     */
    private boolean first;

    /**
     * Whether this is the last page
     */
    private boolean last;

    /**
     * Number of elements in the current page
     */
    private int numberOfElements;

    /**
     * Whether the page is empty
     */
    private boolean empty;
    
    /**
     * Whether there is a next page
     */
    private boolean hasNext;
    
    /**
     * Whether there is a previous page
     */
    private boolean hasPrevious;
    
    /**
     * Current page number (alias for page)
     */
    public int getCurrentPage() {
        return page;
    }
    
    /**
     * Page size (alias for size)
     */
    public int getPageSize() {
        return size;
    }
}
