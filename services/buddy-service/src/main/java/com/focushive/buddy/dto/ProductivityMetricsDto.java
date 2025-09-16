package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for productivity metrics
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityMetricsDto {
    private Integer currentRating;
    private Double averageRating;
    private Integer focusHours;
    private Integer totalHours;
    private Double productivityPercentage;
    private String productivityLevel;
    private LocalDate date;

    public ProductivityMetricsDto(Integer currentRating, Double averageRating, Integer focusHours) {
        this.currentRating = currentRating;
        this.averageRating = averageRating;
        this.focusHours = focusHours;
    }
}