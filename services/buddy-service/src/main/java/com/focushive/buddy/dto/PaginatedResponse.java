package com.focushive.buddy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Paginated response wrapper for endpoints that return large datasets.
 * Provides pagination metadata along with the actual content.
 *
 * @param <T> the type of content being paginated
 */
@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {

    /**
     * The actual content/data for this page.
     */
    private List<T> content;

    /**
     * Total number of elements across all pages.
     */
    private long totalElements;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Current page number (0-based).
     */
    private int currentPage;

    /**
     * Number of elements per page.
     */
    private int pageSize;

    /**
     * Whether there is a next page.
     */
    private boolean hasNext;

    /**
     * Whether there is a previous page.
     */
    private boolean hasPrevious;

    /**
     * Checks if this page is empty (no content).
     *
     * @return true if content is empty
     */
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }

    /**
     * Checks if this is the first page.
     *
     * @return true if current page is 0
     */
    public boolean isFirst() {
        return currentPage == 0;
    }

    /**
     * Checks if this is the last page.
     *
     * @return true if this is the last page
     */
    public boolean isLast() {
        return currentPage == totalPages - 1 || totalPages == 0;
    }
}