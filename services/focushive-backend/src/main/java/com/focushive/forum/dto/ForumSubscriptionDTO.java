package com.focushive.forum.dto;

import com.focushive.forum.entity.ForumSubscription.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumSubscriptionDTO {
    private String id;
    private String userId;
    private String postId;
    private String postTitle;
    private String categoryId;
    private String categoryName;
    
    @NotNull
    private NotificationType notificationType;
    
    private Boolean emailNotifications;
    private Boolean inAppNotifications;
    private Boolean isMuted;
    private LocalDateTime mutedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}