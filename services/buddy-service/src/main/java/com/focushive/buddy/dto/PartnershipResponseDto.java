package com.focushive.buddy.dto;

import com.focushive.buddy.constant.PartnershipStatus;
import lombok.*;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for partnership response data.
 * Contains comprehensive partnership information for API responses.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PartnershipResponseDto {

    private UUID id;
    private UUID user1Id;
    private UUID user2Id;
    private PartnershipStatus status;
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    private ZonedDateTime startedAt;
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    private ZonedDateTime endedAt;
    private String endReason;
    private String agreementText;
    private Integer durationDays;
    private BigDecimal compatibilityScore;
    private BigDecimal healthScore;
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    private ZonedDateTime lastInteractionAt;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;
    private Long version;

    // Additional computed fields
    private Long currentDurationDays;
    private Boolean isActive;
    private Boolean isPending;
    private Boolean isEnded;
    private Boolean isPaused;
    private String partnerIdFor; // Partner ID for the requesting user

    // Related data
    private List<BuddyGoalSummaryDto> goals;
    private List<BuddyCheckinSummaryDto> recentCheckins;

    /**
     * Helper class for goal summaries within partnership responses
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddyGoalSummaryDto {
        private UUID id;
        private String title;
        private String status;
        @JsonSerialize(using = ZonedDateTimeSerializer.class)
        private ZonedDateTime targetDate;
        private Integer progress;
    }

    /**
     * Helper class for checkin summaries within partnership responses
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuddyCheckinSummaryDto {
        private UUID id;
        private String userId;
        private String moodRating;
        private String progressNotes;
        @JsonSerialize(using = ZonedDateTimeSerializer.class)
        private ZonedDateTime checkedInAt;
    }
}