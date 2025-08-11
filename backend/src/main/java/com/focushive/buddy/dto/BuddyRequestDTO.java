package com.focushive.buddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuddyRequestDTO {
    
    @NotNull
    private Long toUserId;
    
    @Size(max = 500)
    private String message;
    
    private LocalDateTime proposedEndDate;
    
    @Size(max = 500)
    private String goals;
    
    @Size(max = 500)
    private String expectations;
}