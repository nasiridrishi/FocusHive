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
public class ForumReplyDTO {
    private Long id;
    
    @NotNull
    private Long postId;
    
    private Long parentReplyId;
    private Long userId;
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