package com.focushive.forum.dto;

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
public class ForumVoteDTO {
    private String id;
    private String userId;
    private String username;
    private String postId;
    private String replyId;
    
    @NotNull
    private Integer voteType; // 1 for upvote, -1 for downvote
    
    private LocalDateTime createdAt;
}