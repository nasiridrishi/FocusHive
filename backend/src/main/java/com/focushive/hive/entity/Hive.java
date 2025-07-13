package com.focushive.hive.entity;

import com.focushive.common.entity.BaseEntity;
import com.focushive.user.entity.User;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "hives")
public class Hive extends BaseEntity {
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(nullable = false)
    private Integer maxMembers = 10;
    
    @Column(nullable = false)
    private Boolean isPublic = true;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    private String backgroundImage;
    
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
    
    public enum HiveType {
        STUDY, WORK, CREATIVE, MEDITATION, GENERAL
    }
}