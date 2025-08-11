package com.focushive.buddy.dto;

import com.focushive.buddy.entity.BuddyRelationship.RelationshipStatus;
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
    private Long id;
    private Long user1Id;
    private String user1Username;
    private String user1Avatar;
    private Long user2Id;
    private String user2Username;
    private String user2Avatar;
    private RelationshipStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String terminationReason;
    private Integer totalGoals;
    private Integer completedGoals;
    private Integer totalSessions;
    private Integer totalCheckins;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // For the current user's perspective
    private Long partnerId;
    private String partnerUsername;
    private String partnerAvatar;
    private boolean isInitiator;
}