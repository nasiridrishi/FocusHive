package com.focushive.forum.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_votes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"}),
        @UniqueConstraint(columnNames = {"user_id", "reply_id"})
    },
    indexes = {
        @Index(name = "idx_forum_vote_user", columnList = "user_id"),
        @Index(name = "idx_forum_vote_post", columnList = "post_id"),
        @Index(name = "idx_forum_vote_reply", columnList = "reply_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumVote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private ForumPost post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    private ForumReply reply;
    
    @Column(name = "vote_type", nullable = false)
    private Integer voteType;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    @PreUpdate
    private void validate() {
        // Ensure vote is either upvote (1) or downvote (-1)
        if (voteType != 1 && voteType != -1) {
            throw new IllegalArgumentException("Vote type must be 1 or -1");
        }
        
        // Ensure either post or reply is set, but not both
        if ((post == null && reply == null) || (post != null && reply != null)) {
            throw new IllegalArgumentException("Vote must be for either a post or a reply, not both");
        }
    }
    
    public boolean isUpvote() {
        return voteType == 1;
    }
    
    public boolean isDownvote() {
        return voteType == -1;
    }
    
    public boolean isForPost() {
        return post != null;
    }
    
    public boolean isForReply() {
        return reply != null;
    }
    
    public Long getTargetId() {
        if (post != null) {
            return post.getId();
        }
        if (reply != null) {
            return reply.getId();
        }
        return null;
    }
}