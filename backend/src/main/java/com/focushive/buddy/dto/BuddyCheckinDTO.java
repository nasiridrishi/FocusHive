package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyCheckinDTO {
    private Long id;
    private Long relationshipId;
    private Long initiatedById;
    private String initiatedByUsername;
    private LocalDateTime checkinTime;
    
    @Min(1) @Max(5)
    private Integer moodRating;
    
    @Min(1) @Max(5)
    private Integer progressRating;
    
    @Size(max = 500)
    private String message;
    
    @Size(max = 500)
    private String currentFocus;
    
    @Size(max = 500)
    private String challenges;
    
    @Size(max = 500)
    private String wins;
    
    private LocalDateTime createdAt;
}