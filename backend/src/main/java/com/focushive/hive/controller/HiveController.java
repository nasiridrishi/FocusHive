package com.focushive.hive.controller;

import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hives")
@Tag(name = "Hive Management", description = "Endpoints for managing virtual co-working spaces")
@SecurityRequirement(name = "bearerAuth")
public class HiveController {
    
    @Operation(summary = "Create a new hive", description = "Creates a new virtual co-working space")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Hive successfully created",
            content = @Content(schema = @Schema(implementation = HiveResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<HiveResponse> createHive(@Valid @RequestBody CreateHiveRequest request) {
        // TODO: Implement hive creation logic
        return ResponseEntity.status(HttpStatus.CREATED).body(new HiveResponse());
    }
    
    @Operation(summary = "Get hive by ID", description = "Retrieves details of a specific hive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hive found",
            content = @Content(schema = @Schema(implementation = HiveResponse.class))),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<HiveResponse> getHive(
            @Parameter(description = "Hive ID") @PathVariable String id) {
        // TODO: Implement get hive logic
        return ResponseEntity.ok(new HiveResponse());
    }
    
    @Operation(summary = "List all hives", description = "Retrieves a list of all public hives or user's hives")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hives retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<List<HiveResponse>> listHives(
            @Parameter(description = "Filter by public hives only") @RequestParam(defaultValue = "true") boolean publicOnly,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        // TODO: Implement list hives logic
        return ResponseEntity.ok(List.of());
    }
    
    @Operation(summary = "Join a hive", description = "Join an existing hive as a member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully joined hive"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "409", description = "Already a member or hive is full"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinHive(@PathVariable String id) {
        // TODO: Implement join hive logic
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "Leave a hive", description = "Leave a hive you are a member of")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully left hive"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "400", description = "Cannot leave - you are the owner"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveHive(@PathVariable String id) {
        // TODO: Implement leave hive logic
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "Get hive members", description = "Retrieves list of members in a hive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> getHiveMembers(@PathVariable String id) {
        // TODO: Implement get members logic
        return ResponseEntity.ok(List.of());
    }
    
    public record MemberResponse(String userId, String username, String role, String status) {}
}