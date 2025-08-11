package com.focushive.forum.entity;

import com.focushive.hive.entity.Hive;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forum_categories",
    indexes = {
        @Index(name = "idx_forum_category_slug", columnList = "slug"),
        @Index(name = "idx_forum_category_hive", columnList = "hive_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(unique = true, nullable = false, length = 100)
    private String slug;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ForumCategory parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ForumCategory> children = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hive_id")
    private Hive hive;
    
    @Column(length = 50)
    private String icon;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "post_count")
    private Integer postCount = 0;
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ForumPost> posts = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public boolean isGlobal() {
        return hive == null;
    }
    
    public boolean isHiveSpecific() {
        return hive != null;
    }
    
    public boolean hasParent() {
        return parent != null;
    }
    
    public String getFullPath() {
        if (parent == null) {
            return "/" + slug;
        }
        return parent.getFullPath() + "/" + slug;
    }
    
    @PrePersist
    @PreUpdate
    private void generateSlug() {
        if (slug == null || slug.isEmpty()) {
            slug = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        }
    }
}