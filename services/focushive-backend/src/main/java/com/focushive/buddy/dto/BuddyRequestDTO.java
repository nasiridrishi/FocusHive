package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyRequestDTO {
    
    @NotNull
    private String toUserId;
    
    @Size(max = 500)
    private String message;
    
    private LocalDateTime proposedEndDate;
    
    @Size(max = 500)
    private String goals;
    
    @Size(max = 500)
    private String expectations;
}