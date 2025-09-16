package com.focushive.buddy.controller;

import com.focushive.buddy.constant.PartnershipStatus;
import com.focushive.buddy.dto.*;
import com.focushive.buddy.service.BuddyPartnershipService;
import com.focushive.buddy.exception.PartnershipConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Buddy Partnership operations.
 * Handles partnership lifecycle, requests, approvals, health monitoring, and management.
 *
 * Base path: /api/v1/buddy/partnerships
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/buddy/partnerships")
@RequiredArgsConstructor
@Validated
@Tag(name = "Buddy Partnerships", description = "Partnership lifecycle and management operations")
public class BuddyPartnershipController {

    private final BuddyPartnershipService buddyPartnershipService;

    // =================================================================================================
    // PARTNERSHIP REQUEST MANAGEMENT
    // =================================================================================================

    @Operation(
        summary = "Create partnership request",
        description = "Send a partnership request to another user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Partnership request created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Partnership conflict (already exists)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/request")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<PartnershipResponseDto>> createPartnershipRequest(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Partnership request details", required = true)
            @Valid @RequestBody PartnershipRequestDto request) {

        log.info("Creating partnership request from {} to {}", userId, request.getRecipientId());

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            // Validate that requester ID matches authenticated user
            UUID userUuid = UUID.fromString(userId);
            if (!userUuid.equals(request.getRequesterId())) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("Requester ID must match authenticated user"));
            }

            PartnershipResponseDto partnership = buddyPartnershipService.createPartnershipRequest(request);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.focushive.buddy.dto.ApiResponse.success(partnership, "Partnership request created successfully"));

        } catch (IllegalArgumentException e) {
            log.error("Invalid partnership request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (PartnershipConflictException e) {
            log.warn("Partnership conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating partnership request: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // APPROVAL/REJECTION WORKFLOW
    // =================================================================================================

    @Operation(
        summary = "Approve partnership request",
        description = "Approve a pending partnership request"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership approved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Partnership not in pending status"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/approve")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<PartnershipResponseDto>> approvePartnership(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("User {} approving partnership {}", userId, id);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            PartnershipResponseDto partnership = buddyPartnershipService.approvePartnershipRequest(id, UUID.fromString(userId));

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(partnership, "Partnership approved successfully")
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            } else if (message.contains("not involved")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (IllegalStateException e) {
            log.warn("Partnership state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Reject partnership request",
        description = "Reject a pending partnership request"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership rejected successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Partnership not in pending status"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/reject")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> rejectPartnership(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Rejection details")
            @RequestBody(required = false) RejectPartnershipRequest request) {

        log.info("User {} rejecting partnership {}", userId, id);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            String reason = request != null ? request.getReason() : null;
            buddyPartnershipService.rejectPartnershipRequest(id, UUID.fromString(userId), reason);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    Map.of("partnershipId", id, "status", "rejected"),
                    "Partnership rejected successfully"
                )
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            } else if (message.contains("not involved")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (IllegalStateException e) {
            log.warn("Partnership state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // PARTNERSHIP LIFECYCLE MANAGEMENT
    // =================================================================================================

    @Operation(
        summary = "Get partnerships",
        description = "Get list of partnerships for the authenticated user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnerships retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getPartnerships(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Filter by partnership status")
            @RequestParam(required = false) String status,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        log.info("Getting partnerships for user {} with status filter: {}, page: {}, size: {}",
                userId, status, page, size);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            // Validate pagination parameters
            if (page < 0) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("Page number must be >= 0"));
            }
            if (size <= 0) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("Page size must be > 0"));
            }

            List<PartnershipResponseDto> partnerships;

            if (status != null) {
                try {
                    PartnershipStatus partnershipStatus = PartnershipStatus.valueOf(status.toUpperCase());
                    partnerships = buddyPartnershipService.findPartnershipsByStatus(partnershipStatus);
                    // Filter to only include partnerships for this user
                    UUID userUuid = UUID.fromString(userId);
                    partnerships = partnerships.stream()
                        .filter(p -> userUuid.equals(p.getUser1Id()) || userUuid.equals(p.getUser2Id()))
                        .toList();
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                        .body(com.focushive.buddy.dto.ApiResponse.error("Invalid status: " + status));
                }
            } else {
                partnerships = buddyPartnershipService.findActivePartnershipsByUser(UUID.fromString(userId));
            }

            // Apply pagination
            int totalElements = partnerships.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<PartnershipResponseDto> pagedContent = partnerships;
            if (startIndex < totalElements) {
                pagedContent = partnerships.subList(startIndex, endIndex);
            } else {
                pagedContent = new ArrayList<>();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("content", pagedContent);  // Paginated content
            data.put("totalElements", totalElements);  // Total count
            data.put("totalPages", (int) Math.ceil((double) totalElements / size));
            data.put("page", page);
            data.put("size", size);
            data.put("first", page == 0);
            data.put("last", endIndex >= totalElements);
            data.put("numberOfElements", pagedContent.size());

            // Keep backward compatibility
            data.put("partnerships", pagedContent);
            data.put("totalCount", totalElements);
            if (status != null) {
                data.put("status", status);
            }

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Partnerships retrieved successfully")
            );

        } catch (Exception e) {
            log.error("Error getting partnerships for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get partnership details",
        description = "Get detailed information about a specific partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership details retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<PartnershipResponseDto>> getPartnershipDetails(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting partnership details for {} requested by {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            PartnershipResponseDto partnership = buddyPartnershipService.findPartnershipById(id);

            if (partnership == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error("Partnership not found"));
            }

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(partnership, "Partnership details retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting partnership details for {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "End partnership",
        description = "End an active partnership permanently"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership ended successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/end")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> endPartnership(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "End partnership request")
            @RequestBody(required = false) EndPartnershipRequest request) {

        log.info("User {} ending partnership {}", userId, id);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            String reason = request != null ? request.getReason() : null;
            buddyPartnershipService.endPartnership(id, UUID.fromString(userId), reason);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    Map.of("partnershipId", id, "status", "ended"),
                    "Partnership ended successfully"
                )
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            } else if (message.contains("not involved")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (Exception e) {
            log.error("Error ending partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Pause partnership",
        description = "Temporarily pause an active partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership paused successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/pause")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> pausePartnership(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Pause partnership request")
            @RequestBody(required = false) PausePartnershipRequest request) {

        log.info("User {} pausing partnership {}", userId, id);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            String reason = request != null ? request.getReason() : null;
            buddyPartnershipService.pausePartnership(id, UUID.fromString(userId), reason);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(
                    Map.of("partnershipId", id, "status", "paused"),
                    "Partnership paused successfully"
                )
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            } else if (message.contains("not involved")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (Exception e) {
            log.error("Error pausing partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Resume partnership",
        description = "Resume a paused partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership resumed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}/resume")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<PartnershipResponseDto>> resumePartnership(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("User {} resuming partnership {}", userId, id);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            PartnershipResponseDto partnership = buddyPartnershipService.resumePartnership(id, UUID.fromString(userId));

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(partnership, "Partnership resumed successfully")
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            } else if (message.contains("not involved")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (Exception e) {
            log.error("Error resuming partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // HEALTH AND ANALYTICS
    // =================================================================================================

    @Operation(
        summary = "Get partnership health",
        description = "Get comprehensive health assessment for a partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership health retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/health")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<PartnershipHealthDto>> getPartnershipHealth(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting partnership health for {} requested by {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            PartnershipHealthDto health = buddyPartnershipService.calculatePartnershipHealth(id);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(health, "Partnership health retrieved")
            );

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error("Partnership not found"));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting partnership health for {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get partnership statistics",
        description = "Get comprehensive statistics for user's partnerships"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/statistics")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<PartnershipStatisticsDto>> getPartnershipStatistics(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting partnership statistics for user {}", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            PartnershipStatisticsDto statistics = buddyPartnershipService.getPartnershipStatistics(UUID.fromString(userId));

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(statistics, "Statistics retrieved successfully")
            );

        } catch (Exception e) {
            log.error("Error getting partnership statistics for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get partnership summary",
        description = "Get summary of user's partnership status"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/summary")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getPartnershipSummary(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting partnership summary for user {}", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            Map<String, Object> summary = buddyPartnershipService.getUserPartnershipSummary(UUID.fromString(userId));

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(summary, "Summary retrieved successfully")
            );

        } catch (Exception e) {
            log.error("Error getting partnership summary for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Get engagement metrics",
        description = "Get engagement metrics for a specific partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Engagement metrics retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/engagement")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getEngagementMetrics(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting engagement metrics for partnership {} requested by {}", id, userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            Map<String, Object> metrics = buddyPartnershipService.calculateEngagementMetrics(id);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(metrics, "Engagement metrics retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting engagement metrics for partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // ADVANCED OPERATIONS
    // =================================================================================================

    @Operation(
        summary = "Get pending requests",
        description = "Get all pending partnership requests for the user"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending requests retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/pending")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> getPendingRequests(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId) {

        log.info("Getting pending requests for user {}", userId);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            List<PartnershipResponseDto> pendingRequests = buddyPartnershipService.getPendingRequests(UUID.fromString(userId));

            Map<String, Object> data = Map.of(
                "requests", pendingRequests,
                "totalCount", pendingRequests.size()
            );

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(data, "Pending requests retrieved")
            );

        } catch (Exception e) {
            log.error("Error getting pending requests for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Renew partnership",
        description = "Extend the duration of an active partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Partnership renewed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/renew")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<PartnershipResponseDto>> renewPartnership(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Renewal request")
            @RequestBody(required = false) RenewPartnershipRequest request) {

        log.info("User {} renewing partnership {}", userId, id);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            Integer extensionDays = request != null ? request.getExtensionDays() : null;
            PartnershipResponseDto partnership = buddyPartnershipService.renewPartnership(id, UUID.fromString(userId), extensionDays);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(partnership, "Partnership renewed successfully")
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            } else if (message.contains("not involved")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (Exception e) {
            log.error("Error renewing partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Search partnerships",
        description = "Search partnerships with various criteria"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid search criteria"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/search")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Page<PartnershipResponseDto>>> searchPartnerships(
            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Search criteria and pagination")
            @RequestBody PartnershipSearchRequest request) {

        log.info("Searching partnerships for user {} with criteria: {}", userId, request);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            Map<String, Object> criteria = new HashMap<>();
            criteria.put("userId", userId);
            if (request.getStatus() != null) {
                criteria.put("status", PartnershipStatus.valueOf(request.getStatus().toUpperCase()));
            }

            Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 10
            );

            Page<PartnershipResponseDto> results = buddyPartnershipService.searchPartnerships(criteria, pageable);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(results, "Search results retrieved")
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error searching partnerships: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    @Operation(
        summary = "Initiate partnership dissolution",
        description = "Start the process to dissolve a partnership"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dissolution initiated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User not authorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Partnership not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/dissolve")
    public ResponseEntity<com.focushive.buddy.dto.ApiResponse<Map<String, Object>>> initiatePartnershipDissolution(
            @Parameter(description = "Partnership ID", required = true)
            @PathVariable UUID id,

            @Parameter(description = "User ID from authentication context", required = true)
            @RequestHeader("X-User-ID") String userId,

            @Parameter(description = "Dissolution request details")
            @Valid @RequestBody DissolutionRequestDto request) {

        log.info("User {} initiating dissolution for partnership {}", userId, id);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(com.focushive.buddy.dto.ApiResponse.error("User ID header is required"));
            }

            Map<String, Object> result = buddyPartnershipService.initiateDissolution(request);

            return ResponseEntity.ok(
                com.focushive.buddy.dto.ApiResponse.success(result, "Partnership dissolution initiated")
            );

        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            } else if (message.contains("not involved")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(com.focushive.buddy.dto.ApiResponse.error(message));
            }
            return ResponseEntity.badRequest()
                .body(com.focushive.buddy.dto.ApiResponse.error(message));
        } catch (Exception e) {
            log.error("Error initiating dissolution for partnership {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(com.focushive.buddy.dto.ApiResponse.error("Internal server error"));
        }
    }

    // =================================================================================================
    // HELPER CLASSES
    // =================================================================================================

    public static class RejectPartnershipRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class EndPartnershipRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class PausePartnershipRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class RenewPartnershipRequest {
        private Integer extensionDays;

        public Integer getExtensionDays() { return extensionDays; }
        public void setExtensionDays(Integer extensionDays) { this.extensionDays = extensionDays; }
    }

    public static class PartnershipSearchRequest {
        private String status;
        private Integer page;
        private Integer size;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }
}