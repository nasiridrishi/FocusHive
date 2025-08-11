package com.focushive.buddy.dto;

import com.focushive.buddy.entity.BuddyGoal.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyGoalDTO {
    private Long id;
    private Long relationshipId;
    
    @NotBlank
    @Size(max = 200)
    private String title;
    
    @Size(max = 1000)
    private String description;
    
    @NotNull
    private GoalStatus status;
    
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private Long completedBy;
    private String completedByUsername;
    private Map<String, Object> metrics;
    private Integer progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}