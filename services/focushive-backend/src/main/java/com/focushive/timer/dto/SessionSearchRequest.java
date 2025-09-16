package com.focushive.timer.dto;

import com.focushive.timer.entity.FocusSession;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSearchRequest {
    private String userId;
    private String query;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String[] tags;
    private FocusSession.SessionStatus status;
}