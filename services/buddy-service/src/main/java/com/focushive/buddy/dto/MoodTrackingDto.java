package com.focushive.buddy.dto;

import com.focushive.buddy.constant.MoodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for mood tracking data
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MoodTrackingDto {
    private MoodType currentMood;
    private Integer emotionalScore;
    private LocalDate date;
    private List<MoodType> weeklyMoods;
    private Double averageEmotionalScore;
    private String moodTrend;

    public MoodTrackingDto(MoodType currentMood, Integer emotionalScore, LocalDate date) {
        this.currentMood = currentMood;
        this.emotionalScore = emotionalScore;
        this.date = date;
    }
}