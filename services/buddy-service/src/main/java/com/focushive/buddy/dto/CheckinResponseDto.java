package com.focushive.buddy.dto;

import com.focushive.buddy.constant.CheckInType;
import com.focushive.buddy.constant.MoodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for check-in operations
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponseDto {
    private UUID id;
    private UUID partnershipId;
    private UUID userId;
    private CheckInType checkinType;
    private String content;
    private MoodType mood;
    private Integer productivityRating;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    private String summary;

    public CheckinResponseDto(UUID id, UUID partnershipId, UUID userId, CheckInType checkinType,
                            String content, MoodType mood, Integer productivityRating, LocalDateTime createdAt) {
        this.id = id;
        this.partnershipId = partnershipId;
        this.userId = userId;
        this.checkinType = checkinType;
        this.content = content;
        this.mood = mood;
        this.productivityRating = productivityRating;
        this.createdAt = createdAt;
    }
}