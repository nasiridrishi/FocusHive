package com.focushive.buddy.controller;

import com.focushive.buddy.dto.*;
import com.focushive.buddy.exception.BuddyServiceException;
import com.focushive.buddy.service.BuddyGoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Buddy Goal operations.
 * Handles goal CRUD, milestones, progress tracking, analytics, and templates.
 *
 * Base path: /api/v1/buddy/goals
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/buddy/goals")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Buddy Goals",
    description = "Goal management and progress tracking operations"
)
public class BuddyGoalController {

    private final BuddyGoalService buddyGoalService;

    // =================================================================================================
    // GOAL CRUD OPERATIONS
    // =================================================================================================

    @Operation(
        summary = "Create goal",
        description = "Create a new individual or shared goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Goal created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid goal data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Goal limit exceeded"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PostMapping
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<GoalResponseDto>> createGoal(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Goal creation details", required = true)
            @Valid @RequestBody GoalCreationDto goalDto) {

        log.info("Creating goal for user {}: {}", userId, goalDto.getTitle());

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);

            GoalResponseDto goal;
            String message;

            if (goalDto.getGoalType() == GoalCreationDto.GoalType.SHARED) {
                goal = buddyGoalService.createSharedGoal(goalDto);
                message = "Shared goal created successfully";
            } else {
                goal = buddyGoalService.createIndividualGoal(goalDto);
                message = "Goal created successfully";
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(
                com.focushive.buddy.dto.ApiResponse.success(goal, message)
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid goal data: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(e.getMessage())
            );
        } catch (BuddyServiceException e) {
            log.warn("Goal service error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                com.focushive.buddy.dto.ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error(
                "Error creating goal for user {}: {}",
                userId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Get user goals",
        description = "Get paginated list of goals for the authenticated user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Goals retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request parameters"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Page<GoalResponseDto>>
    > getGoals(
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(description = "Filter by goal status") @RequestParam(
            required = false
        ) String status,
        @Parameter(description = "Page number (0-based)") @RequestParam(
            defaultValue = "0"
        ) @Min(value = 0, message = "Page must be non-negative") int page,
        @Parameter(description = "Page size") @RequestParam(
            defaultValue = "10"
        ) @Min(value = 1, message = "Size must be positive") @Max(
            value = 100,
            message = "Size must not exceed 100"
        ) int size
    ) {
        log.info(
            "Getting goals for user {} with status filter: {}",
            userId,
            status
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            Pageable pageable = PageRequest.of(page, size);

            Page<GoalResponseDto> goals = buddyGoalService.getGoalsForUser(
                userUuid,
                status,
                pageable
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    goals,
                    "Goals retrieved successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Invalid user ID format"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error getting goals for user {}: {}",
                userId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Get goal details",
        description = "Get detailed information about a specific goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Goal details retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid goal ID"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<GoalResponseDto>
    > getGoalDetails(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID id,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId
    ) {
        log.info("Getting goal details for {} requested by {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            GoalResponseDto goal = buddyGoalService.getGoalById(id, userUuid);

            if (goal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    com.focushive.buddy.dto.ApiResponse.error("Goal not found")
                );
            }

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    goal,
                    "Goal details retrieved"
                )
            );
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Invalid goal ID format"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error getting goal details for {}: {}",
                id,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(summary = "Update goal", description = "Update an existing goal")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Goal updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "User not authorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PutMapping("/{id}")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<GoalResponseDto>
    > updateGoal(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID id,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Updated goal data",
            required = true
        ) @Valid @RequestBody GoalCreationDto goalDto
    ) {
        log.info("Updating goal {} for user {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            GoalResponseDto updatedGoal = buddyGoalService.updateGoal(
                id,
                goalDto,
                userUuid
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    updatedGoal,
                    "Goal updated successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    com.focushive.buddy.dto.ApiResponse.error(message)
                );
            }
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(message)
            );
        } catch (Exception e) {
            log.error("Error updating goal {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Delete goal",
        description = "Delete a goal (soft delete with archiving)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Goal deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "User not authorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > deleteGoal(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID id,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId
    ) {
        log.info("Deleting goal {} for user {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            buddyGoalService.deleteGoal(id, userUuid);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    Map.of("goalId", id, "status", "deleted"),
                    "Goal deleted successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    com.focushive.buddy.dto.ApiResponse.error(message)
                );
            }
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(message)
            );
        } catch (Exception e) {
            log.error("Error deleting goal {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    // =================================================================================================
    // MILESTONE MANAGEMENT
    // =================================================================================================

    @Operation(
        summary = "Add milestone to goal",
        description = "Add a new milestone to an existing goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Milestone created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid milestone data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "User not authorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PostMapping("/{goalId}/milestones")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<MilestoneDto>
    > addMilestone(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Milestone details",
            required = true
        ) @Valid @RequestBody MilestoneDto milestoneDto
    ) {
        log.info("Adding milestone to goal {} for user {}", goalId, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            MilestoneDto milestone = buddyGoalService.addMilestone(
                goalId,
                milestoneDto,
                userUuid
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                com.focushive.buddy.dto.ApiResponse.success(
                    milestone,
                    "Milestone created successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    com.focushive.buddy.dto.ApiResponse.error(message)
                );
            }
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(message)
            );
        } catch (Exception e) {
            log.error(
                "Error adding milestone to goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Get goal milestones",
        description = "Get all milestones for a specific goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Milestones retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/{goalId}/milestones")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > getMilestones(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(description = "Include completed milestones") @RequestParam(
            defaultValue = "true"
        ) Boolean includeCompleted
    ) {
        log.info(
            "Getting milestones for goal {} requested by {}",
            goalId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            List<MilestoneDto> milestones =
                buddyGoalService.getMilestonesForGoal(goalId, includeCompleted);

            Map<String, Object> data = Map.of(
                "milestones",
                milestones,
                "totalCount",
                milestones.size(),
                "goalId",
                goalId,
                "includeCompleted",
                includeCompleted
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    data,
                    "Milestones retrieved successfully"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error getting milestones for goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Update milestone",
        description = "Update an existing milestone"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Milestone updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid milestone data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "User not authorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Milestone not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PutMapping("/{goalId}/milestones/{milestoneId}")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<MilestoneDto>
    > updateMilestone(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "Milestone ID",
            required = true
        ) @PathVariable UUID milestoneId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Updated milestone data",
            required = true
        ) @Valid @RequestBody MilestoneDto milestoneDto
    ) {
        log.info(
            "Updating milestone {} in goal {} for user {}",
            milestoneId,
            goalId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            MilestoneDto updatedMilestone = buddyGoalService.updateMilestone(
                milestoneId,
                milestoneDto,
                userUuid
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    updatedMilestone,
                    "Milestone updated successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    com.focushive.buddy.dto.ApiResponse.error(message)
                );
            }
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(message)
            );
        } catch (Exception e) {
            log.error(
                "Error updating milestone {}: {}",
                milestoneId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Complete milestone",
        description = "Mark a milestone as completed"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Milestone completed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "User not authorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Milestone not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PostMapping("/{goalId}/milestones/{milestoneId}/complete")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<MilestoneDto>
    > completeMilestone(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "Milestone ID",
            required = true
        ) @PathVariable UUID milestoneId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(description = "Completion details") @RequestBody(
            required = false
        ) MilestoneCompletionRequest request
    ) {
        log.info(
            "Completing milestone {} in goal {} for user {}",
            milestoneId,
            goalId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            String completionNotes = request != null
                ? request.getCompletionNotes()
                : null;

            MilestoneDto completedMilestone =
                buddyGoalService.completeMilestone(
                    milestoneId,
                    userUuid,
                    completionNotes
                );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    completedMilestone,
                    "Milestone completed successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    com.focushive.buddy.dto.ApiResponse.error(message)
                );
            }
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(message)
            );
        } catch (Exception e) {
            log.error(
                "Error completing milestone {}: {}",
                milestoneId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Reorder milestones",
        description = "Reorder milestones within a goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Milestones reordered successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "User not authorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PutMapping("/{goalId}/milestones/reorder")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > reorderMilestones(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Milestone reorder request",
            required = true
        ) @Valid @RequestBody ReorderMilestonesRequest request
    ) {
        log.info(
            "Reordering milestones for goal {} by user {}",
            goalId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            List<MilestoneDto> reorderedMilestones =
                buddyGoalService.reorderMilestones(
                    goalId,
                    request.getMilestoneIds(),
                    userUuid
                );

            Map<String, Object> data = Map.of(
                "milestones",
                reorderedMilestones,
                "goalId",
                goalId
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    data,
                    "Milestones reordered successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (message.contains("not authorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    com.focushive.buddy.dto.ApiResponse.error(message)
                );
            }
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(message)
            );
        } catch (Exception e) {
            log.error(
                "Error reordering milestones for goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    // =================================================================================================
    // PROGRESS TRACKING
    // =================================================================================================

    @Operation(
        summary = "Update goal progress",
        description = "Update progress for a specific goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Progress updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid progress data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "User not authorized"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PostMapping("/{goalId}/progress")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > updateProgress(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Progress update data",
            required = true
        ) @Valid @RequestBody ProgressUpdateDto progressUpdate
    ) {
        log.info("Updating progress for goal {} by user {}", goalId, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            GoalResponseDto updatedGoal = buddyGoalService.updateProgress(
                progressUpdate
            );

            // Create response with expected format for progress update
            Map<String, Object> progressData = Map.of(
                "goalId",
                updatedGoal.getId(),
                "progressPercentage",
                progressUpdate.getProgressPercentage(),
                "currentValue",
                progressUpdate.getProgressPercentage(),
                "updatedGoal",
                updatedGoal
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    progressData,
                    "Progress updated successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error(
                "Error updating progress for goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Get progress calculation",
        description = "Get calculated progress metrics for a goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Progress calculation retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/{goalId}/progress")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > getProgressCalculation(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId
    ) {
        log.info(
            "Getting progress calculation for goal {} requested by {}",
            goalId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            Integer overallProgress = buddyGoalService.calculateOverallProgress(
                goalId
            );
            Integer milestoneProgress =
                buddyGoalService.calculateMilestoneCompletion(goalId);

            Map<String, Object> data = Map.of(
                "goalId",
                goalId,
                "progressPercentage",  // Changed from overallProgress to match test expectations
                overallProgress != null ? overallProgress : 0,
                "milestonesCompleted",  // Changed from milestoneProgress to match test expectations
                milestoneProgress != null ? milestoneProgress : 0,
                "overallProgress",  // Keep for backwards compatibility
                overallProgress != null ? overallProgress : 0,
                "milestoneProgress",  // Keep for backwards compatibility
                milestoneProgress != null ? milestoneProgress : 0
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    data,
                    "Progress calculation retrieved"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error getting progress calculation for goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Track daily progress",
        description = "Record daily progress for a goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Daily progress tracked successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid progress data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PostMapping("/{goalId}/track-daily")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > trackDailyProgress(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Daily progress data",
            required = true
        ) @Valid @RequestBody DailyProgressRequest request
    ) {
        log.info(
            "Tracking daily progress for goal {} by user {}",
            goalId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            buddyGoalService.trackDailyProgress(
                goalId,
                request.getProgressPercentage(),
                userUuid,
                request.getNotes()
            );

            Map<String, Object> data = Map.of(
                "goalId",
                goalId,
                "progressPercentage",
                request.getProgressPercentage(),
                "date",
                java.time.LocalDate.now()
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    data,
                    "Daily progress tracked successfully"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error tracking daily progress for goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Get goal analytics",
        description = "Get comprehensive analytics for a goal"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Analytics retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid date range"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/{goalId}/analytics")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<GoalAnalyticsDto>
    > getGoalAnalytics(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Start date for analytics period"
        ) @RequestParam(required = false) LocalDate startDate,
        @Parameter(description = "End date for analytics period") @RequestParam(
            required = false
        ) LocalDate endDate
    ) {
        log.info(
            "Getting analytics for goal {} requested by {}",
            goalId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            if (
                startDate != null &&
                endDate != null &&
                startDate.isAfter(endDate)
            ) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "Start date must be before end date"
                    )
                );
            }

            // Default to last 30 days if not specified
            if (startDate == null) startDate = LocalDate.now().minusDays(30);
            if (endDate == null) endDate = LocalDate.now();

            GoalAnalyticsDto analytics =
                buddyGoalService.generateProgressReport(
                    goalId,
                    startDate,
                    endDate
                );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    analytics,
                    "Analytics retrieved successfully"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error getting analytics for goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Check progress stagnation",
        description = "Detect if a goal has stagnant progress and get intervention suggestions"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Stagnation check completed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Goal not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/{goalId}/stagnation")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > detectProgressStagnation(
        @Parameter(
            description = "Goal ID",
            required = true
        ) @PathVariable UUID goalId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Days threshold for stagnation detection"
        ) @RequestParam(defaultValue = "7") Integer daysThreshold
    ) {
        log.info(
            "Checking stagnation for goal {} with threshold {} days",
            goalId,
            daysThreshold
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            Boolean isStagnant = buddyGoalService.detectProgressStagnation(
                goalId,
                daysThreshold
            );
            List<String> interventions =
                buddyGoalService.suggestProgressInterventions(goalId);

            Map<String, Object> data = Map.of(
                "goalId",
                goalId,
                "isStagnant",
                isStagnant,
                "daysThreshold",
                daysThreshold,
                "interventions",
                interventions
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    data,
                    "Stagnation check completed"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error checking stagnation for goal {}: {}",
                goalId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    // =================================================================================================
    // GOAL TEMPLATES AND SUGGESTIONS
    // =================================================================================================

    @Operation(
        summary = "Get goal templates",
        description = "Get available goal templates with optional filtering"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Templates retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/templates")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Page<GoalTemplateDto>>
    > getGoalTemplates(
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(description = "Filter by category") @RequestParam(
            required = false
        ) String category,
        @Parameter(description = "Filter by difficulty level") @RequestParam(
            required = false
        ) Integer difficulty,
        @Parameter(description = "Page number (0-based)") @RequestParam(
            defaultValue = "0"
        ) int page,
        @Parameter(description = "Page size") @RequestParam(
            defaultValue = "10"
        ) int size
    ) {
        log.info(
            "Getting goal templates for user {} with category: {}, difficulty: {}",
            userId,
            category,
            difficulty
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<GoalTemplateDto> templates = buddyGoalService.getGoalTemplates(
                category,
                difficulty,
                pageable
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    templates,
                    "Templates retrieved successfully"
                )
            );
        } catch (Exception e) {
            log.error("Error getting goal templates: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Create goal from template",
        description = "Create a new goal based on a template with customizations"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Goal created from template successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid template or customization data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Template not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PostMapping("/from-template/{templateId}")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<GoalResponseDto>
    > createGoalFromTemplate(
        @Parameter(
            description = "Template ID",
            required = true
        ) @PathVariable UUID templateId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Goal customizations",
            required = true
        ) @Valid @RequestBody GoalCreationDto customization
    ) {
        log.info(
            "Creating goal from template {} for user {}",
            templateId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            GoalResponseDto goal = buddyGoalService.cloneGoalFromTemplate(
                templateId,
                customization,
                userUuid
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                com.focushive.buddy.dto.ApiResponse.success(
                    goal,
                    "Goal created from template successfully"
                )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                com.focushive.buddy.dto.ApiResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error(
                "Error creating goal from template {}: {}",
                templateId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Get personalized goal suggestions",
        description = "Get goal suggestions based on user profile and preferences"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Suggestions retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/suggestions")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Map<String, Object>>
    > getGoalSuggestions(
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(description = "Maximum number of suggestions") @RequestParam(
            defaultValue = "5"
        ) Integer maxSuggestions
    ) {
        log.info(
            "Getting goal suggestions for user {} with max {}",
            userId,
            maxSuggestions
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);
            List<GoalTemplateDto> suggestions =
                buddyGoalService.suggestGoalsBasedOnProfile(
                    userUuid,
                    maxSuggestions
                );

            Map<String, Object> data = Map.of(
                "suggestions",
                suggestions,
                "totalSuggestions",
                suggestions.size(),
                "maxSuggestions",
                maxSuggestions
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    data,
                    "Suggestions retrieved successfully"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error getting goal suggestions for user {}: {}",
                userId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    // =================================================================================================
    // SEARCH AND FILTERING
    // =================================================================================================

    @Operation(
        summary = "Search goals",
        description = "Search goals with various criteria"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid search criteria"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @PostMapping("/search")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Page<GoalResponseDto>>
    > searchGoals(
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(
            description = "Search criteria",
            required = true
        ) @Valid @RequestBody GoalSearchRequest request
    ) {
        log.info(
            "Searching goals for user {} with criteria: {}",
            userId,
            request
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            UUID userUuid = UUID.fromString(userId);

            // Convert request to search criteria
            BuddyGoalService.GoalSearchCriteria criteria =
                new BuddyGoalService.GoalSearchCriteria();
            // Set criteria fields from request...

            Pageable pageable = PageRequest.of(0, 20); // Default pagination
            Page<GoalResponseDto> results = buddyGoalService.searchGoals(
                criteria,
                userUuid,
                pageable
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    results,
                    "Search results retrieved"
                )
            );
        } catch (Exception e) {
            log.error("Error searching goals: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    @Operation(
        summary = "Get shared goals for partnership",
        description = "Get shared goals for a specific partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(
        {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Shared goals retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Partnership not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Internal server error"
            ),
        }
    )
    @GetMapping("/partnerships/{partnershipId}")
    public ResponseEntity<
        com.focushive.buddy.dto.ApiResponse<Page<GoalResponseDto>>
    > getSharedGoals(
        @Parameter(
            description = "Partnership ID",
            required = true
        ) @PathVariable UUID partnershipId,
        @Parameter(
            description = "User ID from authentication context",
            required = true
        ) @RequestHeader("X-User-ID") String userId,
        @Parameter(description = "Page number (0-based)") @RequestParam(
            defaultValue = "0"
        ) int page,
        @Parameter(description = "Page size") @RequestParam(
            defaultValue = "10"
        ) int size
    ) {
        log.info(
            "Getting shared goals for partnership {} requested by {}",
            partnershipId,
            userId
        );

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    com.focushive.buddy.dto.ApiResponse.error(
                        "User ID header is required"
                    )
                );
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<GoalResponseDto> sharedGoals =
                buddyGoalService.getSharedGoalsForPartnership(
                    partnershipId,
                    pageable
                );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    sharedGoals,
                    "Shared goals retrieved successfully"
                )
            );
        } catch (Exception e) {
            log.error(
                "Error getting shared goals for partnership {}: {}",
                partnershipId,
                e.getMessage()
            );
            return ResponseEntity.internalServerError().body(
                com.focushive.buddy.dto.ApiResponse.error(
                    "Internal server error"
                )
            );
        }
    }

    // =================================================================================================
    // HELPER CLASSES
    // =================================================================================================

    public static class MilestoneCompletionRequest {

        private String completionNotes;

        public String getCompletionNotes() {
            return completionNotes;
        }

        public void setCompletionNotes(String completionNotes) {
            this.completionNotes = completionNotes;
        }
    }

    public static class ReorderMilestonesRequest {

        private List<UUID> milestoneIds;

        public List<UUID> getMilestoneIds() {
            return milestoneIds;
        }

        public void setMilestoneIds(List<UUID> milestoneIds) {
            this.milestoneIds = milestoneIds;
        }
    }

    public static class DailyProgressRequest {

        private Integer progressPercentage;
        private String notes;

        public Integer getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(Integer progressPercentage) {
            this.progressPercentage = progressPercentage;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class GoalSearchRequest {

        private String title;
        private String category;
        private String status;
        private Integer minProgress;
        private Integer maxProgress;
        private List<String> tags;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getMinProgress() {
            return minProgress;
        }

        public void setMinProgress(Integer minProgress) {
            this.minProgress = minProgress;
        }

        public Integer getMaxProgress() {
            return maxProgress;
        }

        public void setMaxProgress(Integer maxProgress) {
            this.maxProgress = maxProgress;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }
}
