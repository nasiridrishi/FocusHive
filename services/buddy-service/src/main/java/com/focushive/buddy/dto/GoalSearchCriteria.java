package com.focushive.buddy.dto;

import com.focushive.buddy.constant.GoalStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for goal search criteria.
 * Contains various filters for searching goals.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoalSearchCriteria {

    /**
     * Search text (matches title and description)
     */
    private String title;

    /**
     * Search in description
     */
    private String description;

    /**
     * Filter by goal status
     */
    private GoalStatus status;

    /**
     * Filter by multiple statuses
     */
    private List<GoalStatus> statuses;

    /**
     * Filter by goal category
     */
    private String category;

    /**
     * Filter by categories
     */
    private List<String> categories;

    /**
     * Filter by tags
     */
    private List<String> tags;

    /**
     * Filter by priority level
     */
    private Integer priority;

    /**
     * Filter by difficulty level
     */
    private Integer difficulty;

    /**
     * Filter by minimum progress percentage
     */
    private Integer minProgress;

    /**
     * Filter by maximum progress percentage
     */
    private Integer maxProgress;

    /**
     * Filter by created date range - start
     */
    private LocalDate createdAfter;

    /**
     * Filter by created date range - end
     */
    private LocalDate createdBefore;

    /**
     * Filter by target date range - start
     */
    private LocalDate targetAfter;

    /**
     * Filter by target date range - end
     */
    private LocalDate targetBefore;

    /**
     * Filter by completion date range - start
     */
    private LocalDate completedAfter;

    /**
     * Filter by completion date range - end
     */
    private LocalDate completedBefore;

    /**
     * Filter by goal type (INDIVIDUAL or SHARED)
     */
    private GoalCreationDto.GoalType goalType;

    /**
     * Filter by partnership ID (for shared goals)
     */
    private UUID partnershipId;

    /**
     * Filter by created by user
     */
    private UUID createdBy;

    /**
     * Include overdue goals only
     */
    private Boolean overdueOnly;

    /**
     * Include active goals only
     */
    private Boolean activeOnly;

    /**
     * Include completed goals only
     */
    private Boolean completedOnly;

    /**
     * Sort field
     */
    private String sortBy;

    /**
     * Sort direction (ASC or DESC)
     */
    private String sortDirection;

    /**
     * Include goals with milestones only
     */
    private Boolean withMilestonesOnly;

    /**
     * Minimum number of milestones
     */
    private Integer minMilestones;

    /**
     * Maximum number of milestones
     */
    private Integer maxMilestones;

    /**
     * Filter by template ID (if created from template)
     */
    private UUID templateId;

    /**
     * Include goals with reminders enabled only
     */
    private Boolean remindersEnabledOnly;
}