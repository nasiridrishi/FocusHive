package com.focushive.forum.dto;

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
public class ForumVoteDTO {
    private Long id;
    private Long userId;
    private String username;
    private Long postId;
    private Long replyId;
    
    @NotNull
    private Integer voteType; // 1 for upvote, -1 for downvote
    
    private LocalDateTime createdAt;
}