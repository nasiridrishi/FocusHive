package com.focushive.hive.dto;

import com.focushive.hive.entity.Hive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating hive information.
 */
public class UpdateHiveRequest {
    
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Min(value = 1, message = "Max members must be at least 1")
    @Max(value = 100, message = "Max members cannot exceed 100")
    private Integer maxMembers;
    
    private Boolean isPublic;
    
    private Boolean isActive;
    
    private Hive.HiveType type;
    
    @Size(max = 500, message = "Background image URL must not exceed 500 characters")
    private String backgroundImage;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Theme color must be a valid hex color (#RRGGBB)")
    private String themeColor;
    
    @Size(max = 2000, message = "Rules must not exceed 2000 characters")
    private String rules;
    
    private String[] tags;
    
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
}