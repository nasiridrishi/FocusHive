package com.focushive.hive.dto;

import com.focushive.hive.entity.Hive;

import java.time.LocalDateTime;

public class HiveResponse {
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private String ownerUsername;
    private Integer maxMembers;
    private Integer currentMembers;
    private Boolean isPublic;
    private Boolean isActive;
    private Hive.HiveType type;
    private String backgroundImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor
    public HiveResponse() {}
    
    public HiveResponse(Hive hive, int currentMembers) {
        this.id = hive.getId();
        this.name = hive.getName();
        this.description = hive.getDescription();
        this.ownerId = hive.getOwner().getId();
        this.ownerUsername = hive.getOwner().getUsername();
        this.maxMembers = hive.getMaxMembers();
        this.currentMembers = currentMembers;
        this.isPublic = hive.getIsPublic();
        this.isActive = hive.getIsActive();
        this.type = hive.getType();
        this.backgroundImage = hive.getBackgroundImage();
        this.createdAt = hive.getCreatedAt();
        this.updatedAt = hive.getUpdatedAt();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getOwnerUsername() {
        return ownerUsername;
    }
    
    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }
    
    public Integer getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(Integer maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    public Integer getCurrentMembers() {
        return currentMembers;
    }
    
    public void setCurrentMembers(Integer currentMembers) {
        this.currentMembers = currentMembers;
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
    
    public Hive.HiveType getType() {
        return type;
    }
    
    public void setType(Hive.HiveType type) {
        this.type = type;
    }
    
    public String getBackgroundImage() {
        return backgroundImage;
    }
    
    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}