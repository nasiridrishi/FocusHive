package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for accountability score data
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccountabilityScoreDto {
    private BigDecimal score;
    @JsonProperty("overallScore")  // Also expose as overallScore for backward compatibility
    private BigDecimal overallScore;
    private String level;
    private String percentage;
    private Integer checkinsCompleted;
    private Integer goalsAchieved;
    private BigDecimal responseRate;
    private Integer streakDays;
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime calculatedAt;
    private String improvement;
    private Map<String, Object> metrics;  // Additional metrics for tests

    public AccountabilityScoreDto(BigDecimal score, String level, Integer checkinsCompleted) {
        this.score = score;
        this.level = level;
        this.checkinsCompleted = checkinsCompleted;
    }
}