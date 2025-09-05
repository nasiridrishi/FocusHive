package com.focushive.buddy.dto;

import com.focushive.buddy.entity.BuddySession.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddySessionDTO {
    private String id;
    private String relationshipId;
    
    @NotNull
    private LocalDateTime sessionDate;
    
    private LocalDateTime scheduledTime; // Alias for sessionDate for WebSocket usage
    
    @NotNull
    @Min(15)
    private Integer plannedDurationMinutes;
    
    private Integer actualDurationMinutes;
    
    @Size(max = 500)
    private String agenda;
    
    @Size(max = 1000)
    private String notes;
    
    private SessionStatus status;
    private LocalDateTime user1Joined;
    private LocalDateTime user1Left;
    private LocalDateTime user2Joined;
    private LocalDateTime user2Left;
    private Integer user1Rating;
    private String user1Feedback;
    private Integer user2Rating;
    private String user2Feedback;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String cancellationReason;
    private Double averageRating;
    private LocalDateTime createdAt;
    
    // User IDs for WebSocket usage
    private String user1Id;
    private String user2Id;
}