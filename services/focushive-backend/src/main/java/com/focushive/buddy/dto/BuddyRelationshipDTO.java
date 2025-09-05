package com.focushive.buddy.dto;

import com.focushive.buddy.entity.BuddyRelationship.BuddyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyRelationshipDTO {
    private String id;
    private String user1Id;
    private String user1Username;
    private String user1Avatar;
    private String user2Id;
    private String user2Username;
    private String user2Avatar;
    private BuddyStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String terminationReason;
    private Integer totalGoals;
    private Integer completedGoals;
    private Integer totalSessions;
    private Integer totalCheckins;
    private LocalDateTime lastCheckinTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // For the current user's perspective
    private String partnerId;
    private String partnerUsername;
    private String partnerAvatar;
    private boolean isInitiator;
}