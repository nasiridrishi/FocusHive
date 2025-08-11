package com.focushive.forum.dto;

import com.focushive.forum.entity.ForumSubscription.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumSubscriptionDTO {
    private Long id;
    private Long userId;
    private Long postId;
    private String postTitle;
    private Long categoryId;
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