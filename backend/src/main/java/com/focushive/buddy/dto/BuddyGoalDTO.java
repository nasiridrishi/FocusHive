package com.focushive.buddy.dto;

import com.focushive.buddy.entity.BuddyGoal.GoalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private LocalDateTime deadline; // Alias for dueDate for WebSocket usage
    private LocalDateTime completedAt;
    private String completedBy;
    private String completedByUsername;
    private Map<String, Object> metrics;
    private Integer progressPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}