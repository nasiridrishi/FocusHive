package com.focushive.buddy.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.focushive.buddy.constant.MoodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for weekly review data
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReviewDto {
    private UUID partnershipId;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate weekStartDate;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate weekEndDate;
    private Integer checkinsThisWeek;
    private Double averageProductivity;
    private List<MoodType> dailyMoods;
    private String weeklyProgress;
    private String accomplishments;
    private String challengesFaced;
    private String nextWeekGoals;

    public WeeklyReviewDto(LocalDate weekStartDate, LocalDate weekEndDate,
                         Integer checkinsThisWeek, Double averageProductivity) {
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.checkinsThisWeek = checkinsThisWeek;
        this.averageProductivity = averageProductivity;
    }
}