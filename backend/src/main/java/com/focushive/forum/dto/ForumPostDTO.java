package com.focushive.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumPostDTO {
    private Long id;
    
    @NotNull
    private Long categoryId;
    private String categoryName;
    
    private Long userId;
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