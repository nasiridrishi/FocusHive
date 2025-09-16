package com.focushive.timer.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareRequest {
    private String method; // email, slack, etc.
    private List<String> recipients;
    private String message;
    private boolean includeStats;
}