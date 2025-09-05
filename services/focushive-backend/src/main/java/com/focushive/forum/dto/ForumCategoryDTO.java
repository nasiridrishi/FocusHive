package com.focushive.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumCategoryDTO {
    private String id;
    
    @NotBlank
    @Size(max = 100)
    private String name;
    
    @Size(max = 1000)
    private String description;
    
    private String slug;
    private String parentId;
    private String hiveId;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    private Integer postCount;
    private List<ForumCategoryDTO> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}