package com.focushive.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumPostDTO {
    private String id;
    
    @NotNull
    private String categoryId;
    private String categoryName;
    
    private String userId;
    private String username;
    private String userAvatar;
    
    @NotBlank
    @Size(max = 200)
    private String title;
    
    @NotBlank
    @Size(max = 10000)
    private String content;
    
    private String contentHtml;
    private String slug;
    private List<String> tags;
    private Integer viewCount;
    private Integer replyCount;
    private Integer voteScore;
    private Boolean isPinned;
    private Boolean isLocked;
    private Boolean isDeleted;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    private String editedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Boolean hasAcceptedAnswer;
    private Integer userVote; // -1, 0, or 1 for current user's vote
    private Boolean canEdit;
    private Boolean canReply;
}