package com.focushive.forum.entity;

import com.focushive.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_posts",
    indexes = {
        @Index(name = "idx_forum_post_category", columnList = "category_id"),
        @Index(name = "idx_forum_post_user", columnList = "user_id"),
        @Index(name = "idx_forum_post_created", columnList = "created_at DESC"),
        @Index(name = "idx_forum_post_votes", columnList = "vote_score DESC"),
        @Index(name = "idx_forum_post_slug", columnList = "category_id, slug")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ForumCategory category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;
    
    @Column(length = 255)
    private String slug;
    
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] tags;
    
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;
    
    @Column(name = "reply_count", nullable = false)
    @Builder.Default
    private Integer replyCount = 0;
    
    @Column(name = "vote_score", nullable = false)
    @Builder.Default
    private Integer voteScore = 0;
    
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;
    
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;
    
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by")
    private User editedBy;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ForumReply> replies = new ArrayList<>();
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
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
    
    public boolean canEdit(String userId) {
        return user.getId().equals(userId) && !isLocked && !isDeleted;
    }
    
    public boolean canReply() {
        return !isLocked && !isDeleted;
    }
    
    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public void incrementReplyCount() {
        this.replyCount++;
    }
    
    public void decrementReplyCount() {
        if (this.replyCount > 0) {
            this.replyCount--;
        }
    }
    
    public void updateVoteScore(int change) {
        this.voteScore += change;
    }
    
    @PrePersist
    @PreUpdate
    private void generateSlug() {
        if (slug == null || slug.isEmpty()) {
            slug = title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                .substring(0, Math.min(title.length(), 100));
        }
    }
}