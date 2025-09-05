package com.focushive.hive.dto;

import com.focushive.hive.entity.Hive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateHiveRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private Integer maxMembers = 10;
    
    private Boolean isPublic = true;
    
    private Hive.HiveType type = Hive.HiveType.GENERAL;
    
    private String backgroundImage;
    
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
}