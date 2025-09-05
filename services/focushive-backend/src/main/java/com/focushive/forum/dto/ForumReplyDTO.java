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
public class ForumReplyDTO {
    private String id;
    
    @NotNull
    private String postId;
    
    private String parentReplyId;
    private String userId;
    private String username;
    private String userAvatar;
    
    @NotBlank
    @Size(max = 5000)
    private String content;
    
    private String contentHtml;
    private Integer voteScore;
    private Boolean isAccepted;
    private Boolean isDeleted;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    private String editedByUsername;
    private List<ForumReplyDTO> childReplies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Integer depth;
    private Integer userVote; // -1, 0, or 1 for current user's vote
    private Boolean canEdit;
    private Boolean canAccept; // Can mark as accepted answer
}