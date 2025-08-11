package com.focushive.forum.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_replies",
    indexes = {
        @Index(name = "idx_forum_reply_post", columnList = "post_id"),
        @Index(name = "idx_forum_reply_user", columnList = "user_id"),
        @Index(name = "idx_forum_reply_parent", columnList = "parent_reply_id"),
        @Index(name = "idx_forum_reply_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumReply {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private ForumReply parentReply;
    
    @OneToMany(mappedBy = "parentReply", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ForumReply> childReplies = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;
    
    @Column(name = "vote_score", nullable = false)
    private Integer voteScore = 0;
    
    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted = false;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by")
    private User editedBy;
    
    @OneToMany(mappedBy = "reply", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ForumVote> votes = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public boolean isEdited() {
        return editedAt != null;
    }
    
    public boolean canEdit(Long userId) {
        return user.getId().equals(userId) && !post.getIsLocked() && !isDeleted;
    }
    
    public boolean isTopLevel() {
        return parentReply == null;
    }
    
    public int getDepth() {
        if (parentReply == null) {
            return 0;
        }
        return parentReply.getDepth() + 1;
    }
    
    public void updateVoteScore(int change) {
        this.voteScore += change;
    }
    
    public void markAsAccepted() {
        // Only allow accepting top-level replies
        if (isTopLevel() && !isDeleted) {
            this.isAccepted = true;
        }
    }
}