package com.focushive.hive.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hives")
public class Hive extends BaseEntity {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;
    
    @NotBlank(message = "Slug is required")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug can only contain lowercase letters, numbers, and hyphens")
    @Column(unique = true, nullable = false, length = 100)
    private String slug;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @NotNull(message = "Max members is required")
    @Min(value = 1, message = "Max members must be at least 1")
    @Max(value = 100, message = "Max members cannot exceed 100")
    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 10;
    
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Size(max = 500, message = "Background image URL must not exceed 500 characters")
    @Column(name = "background_image")
    private String backgroundImage;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Theme color must be a valid hex color (#RRGGBB)")
    @Column(name = "theme_color", length = 7)
    private String themeColor;
    
    @Size(max = 2000, message = "Rules must not exceed 2000 characters")
    @Column(columnDefinition = "TEXT")
    private String rules;
    
    @Column(columnDefinition = "text[]")
    private String[] tags;
    
    @Column(columnDefinition = "jsonb")
    private String settings = "{}";
    
    @Column(name = "member_count")
    private Integer memberCount = 0;
    
    @Column(name = "total_focus_minutes")
    private Long totalFocusMinutes = 0L;
    
    @OneToMany(mappedBy = "hive", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HiveMember> members = new HashSet<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HiveType type = HiveType.STUDY;
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public User getOwner() {
        return owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public Integer getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public Boolean getIsPublic() {
        return isPublic;
    }
    
    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getBackgroundImage() {
        return backgroundImage;
    }
    
    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }
    
    public Set<HiveMember> getMembers() {
        return members;
    }
    
    public void setMembers(Set<HiveMember> members) {
        this.members = members;
    }
    
    public HiveType getType() {
        return type;
    }
    
    public void setType(HiveType type) {
        this.type = type;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public String getThemeColor() {
        return themeColor;
    }
    
    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }
    
    public String getRules() {
        return rules;
    }
    
    public void setRules(String rules) {
        this.rules = rules;
    }
    
    public String[] getTags() {
        return tags;
    }
    
    public void setTags(String[] tags) {
        this.tags = tags;
    }
    
    public String getSettings() {
        return settings;
    }
    
    public void setSettings(String settings) {
        this.settings = settings;
    }
    
    public Integer getMemberCount() {
        return memberCount;
    }
    
    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
    
    public Long getTotalFocusMinutes() {
        return totalFocusMinutes;
    }
    
    public void setTotalFocusMinutes(Long totalFocusMinutes) {
        this.totalFocusMinutes = totalFocusMinutes;
    }
    
    public enum HiveType {
        STUDY, WORK, CREATIVE, MEDITATION, GENERAL
    }
}