package com.focushive.buddy.dto;

import com.focushive.buddy.constant.GoalStatus;
import lombok.*;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for goal response data.
 * Contains complete goal information including progress and milestone summary.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponseDto {

    private UUID id;
    private Long version;
    private UUID partnershipId;
    private String title;
    private String description;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate targetDate;
    private Integer progressPercentage;
    private UUID createdBy;
    private GoalStatus status;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime completedAt;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    /**
     * Goal type: INDIVIDUAL or SHARED
     */
    private GoalCreationDto.GoalType goalType;

    /**
     * Priority level (1=low, 5=high)
     */
    private Integer priority;

    /**
     * Category for goal organization
     */
    private String category;

    /**
     * Tags for goal organization and searching
     */
    private List<String> tags;

    /**
     * Goal difficulty level (1=easy, 5=very hard)
     */
    private Integer difficulty;

    /**
     * Whether this goal has reminders enabled
     */
    private Boolean enableReminders;

    /**
     * Whether this is a shared goal with a buddy
     */
    private Boolean isShared;

    /**
     * Summary information about milestones
     */
    private MilestoneSummaryDto milestoneSummary;

    /**
     * Progress analytics
     */
    private ProgressAnalyticsDto progressAnalytics;

    /**
     * Partnership information (for shared goals)
     */
    private PartnershipInfoDto partnershipInfo;

    /**
     * Template information if this goal was created from a template
     */
    private TemplateInfoDto templateInfo;

    /**
     * Recent activity summary
     */
    private List<ActivitySummaryDto> recentActivity;

    /**
     * Whether the current user can edit this goal
     */
    private Boolean canEdit;

    /**
     * Whether the current user can delete this goal
     */
    private Boolean canDelete;

    /**
     * Computed properties
     */
    private Boolean isOverdue;
    private Integer daysUntilTarget;
    private Integer daysSinceCreation;
    private String completionRate; // e.g., "75% in 30 days"

    /**
     * DTO for milestone summary information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MilestoneSummaryDto {
        private Integer totalMilestones;
        private Integer completedMilestones;
        private Integer overdueMilestones;
        private Integer upcomingMilestones;
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        private LocalDate nextMilestoneDate;
        private String nextMilestoneTitle;
    }

    /**
     * DTO for progress analytics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressAnalyticsDto {
        private Double averageDailyProgress;
        private Integer totalDaysActive;
        private Integer stagnantDays;
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        private LocalDate lastProgressUpdate;
        private Double velocityTrend; // positive = accelerating, negative = slowing
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        private LocalDate estimatedCompletionDate;
    }

    /**
     * DTO for partnership information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartnershipInfoDto {
        private UUID partnerUserId;
        private String partnerUsername;
        private String partnerDisplayName;
        private Boolean partnerActive;
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        private LocalDateTime lastPartnerActivity;
        private Integer partnerContributionPercentage;
    }

    /**
     * DTO for template information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateInfoDto {
        private UUID templateId;
        private String templateName;
        private String templateCategory;
        private Double templateSuccessRate;
        private Integer templateUsageCount;
    }

    /**
     * DTO for activity summaries
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivitySummaryDto {
        private String activityType;
        private String description;
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        private LocalDateTime timestamp;
        private UUID userId;
        private String username;
        private Object metadata; // flexible for different activity types
    }
}