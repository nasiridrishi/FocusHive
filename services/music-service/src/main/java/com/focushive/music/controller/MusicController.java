package com.focushive.music.controller;

import com.focushive.music.dto.RecommendationDTO;
import com.focushive.music.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for music recommendations.
 */
@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Music Recommendations", description = "Music recommendation endpoints")
public class MusicController {

    private final RecommendationService recommendationService;

    @Operation(summary = "Get music recommendations for focus session")
    @GetMapping
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Session type (deep_work, creative, study, relaxation)") @RequestParam String sessionType,
            @Parameter(description = "Number of recommendations") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Additional context") @RequestParam(required = false) Map<String, Object> context) {
        
        log.info("Getting recommendations for user: {}, sessionType: {}", userId, sessionType);
        
        List<RecommendationDTO> recommendations = recommendationService.generateRecommendations(
            userId, sessionType, limit, context != null ? context : Map.of());
        
        return ResponseEntity.ok(recommendations);
    }

    @Operation(summary = "Get music recommendations by task type")
    @GetMapping("/by-task")
    public ResponseEntity<List<RecommendationDTO>> getRecommendationsByTask(
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Task type (coding, writing, design, etc.)") @RequestParam String taskType,
            @Parameter(description = "Number of recommendations") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting recommendations by task for user: {}, taskType: {}", userId, taskType);
        
        List<RecommendationDTO> recommendations = recommendationService.getRecommendationsByTaskType(
            userId, taskType, limit);
        
        return ResponseEntity.ok(recommendations);
    }

    @Operation(summary = "Health check endpoint")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "music-service",
            "timestamp", java.time.Instant.now().toString()
        ));
    }
}