package com.focushive.hive.controller;

import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.dto.UpdateHiveRequest;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.service.HiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/hives")
@Tag(name = "Hive Management", description = "Endpoints for managing virtual co-working spaces")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class HiveController {
    
    private final HiveService hiveService;
    
    @Operation(summary = "Create a new hive", description = "Creates a new virtual co-working space")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Hive successfully created",
            content = @Content(schema = @Schema(implementation = HiveResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<HiveResponse> createHive(
            @Valid @RequestBody CreateHiveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Creating new hive: {} by user: {}", request.getName(), userDetails.getUsername());
        HiveResponse response = hiveService.createHive(request, getUserId(userDetails));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            @Parameter(description = "Hive ID") @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        HiveResponse response = hiveService.getHive(id, getUserId(userDetails));
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get hive by slug", description = "Retrieves details of a specific hive by slug")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hive found",
            content = @Content(schema = @Schema(implementation = HiveResponse.class))),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<HiveResponse> getHiveBySlug(
            @Parameter(description = "Hive slug") @PathVariable String slug,
            @AuthenticationPrincipal UserDetails userDetails) {
        HiveResponse response = hiveService.getHiveBySlug(slug, getUserId(userDetails));
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Update hive", description = "Updates hive information (owner/moderator only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hive updated successfully"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    public ResponseEntity<HiveResponse> updateHive(
            @PathVariable String id,
            @Valid @RequestBody UpdateHiveRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        HiveResponse response = hiveService.updateHive(id, request, getUserId(userDetails));
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Delete hive", description = "Soft delete a hive (owner only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Hive deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "403", description = "Only owner can delete"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHive(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        hiveService.deleteHive(id, getUserId(userDetails));
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "List hives", description = "Retrieves a list of public hives or user's hives")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hives retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<Page<HiveResponse>> listHives(
            @Parameter(description = "Filter by user's hives only") @RequestParam(defaultValue = "false") boolean myHivesOnly,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<HiveResponse> hives = myHivesOnly 
            ? hiveService.listUserHives(getUserId(userDetails), pageable)
            : hiveService.listPublicHives(pageable);
        return ResponseEntity.ok(hives);
    }
    
    @Operation(summary = "Search hives", description = "Search public hives by name or description")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<HiveResponse>> searchHives(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<HiveResponse> results = hiveService.searchHives(query, pageable);
        return ResponseEntity.ok(results);
    }
    
    @Operation(summary = "Join a hive", description = "Join an existing hive as a member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully joined hive"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "409", description = "Already a member or hive is full"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/join")
    public ResponseEntity<MemberResponse> joinHive(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        HiveMember member = hiveService.joinHive(id, getUserId(userDetails));
        MemberResponse response = new MemberResponse(
            member.getUser().getId(),
            member.getUser().getUsername(),
            member.getRole().name(),
            "ACTIVE"
        );
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Leave a hive", description = "Leave a hive you are a member of")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully left hive"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "400", description = "Cannot leave - you are the owner"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveHive(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        hiveService.leaveHive(id, getUserId(userDetails));
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Get hive members", description = "Retrieves list of members in a hive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Hive not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{id}/members")
    public ResponseEntity<Page<MemberResponse>> getHiveMembers(
            @PathVariable String id,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<HiveMember> members = hiveService.getHiveMembers(id, getUserId(userDetails), pageable);
        Page<MemberResponse> response = members.map(member -> new MemberResponse(
            member.getUser().getId(),
            member.getUser().getUsername(),
            member.getRole().name(),
            member.getLastActiveAt() != null ? "ACTIVE" : "INACTIVE"
        ));
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method to extract user ID from UserDetails.
     * This assumes the UserDetails implementation includes the user ID.
     */
    private String getUserId(UserDetails userDetails) {
        // TODO: Update this based on your actual UserDetails implementation
        // For now, using username as ID
        return userDetails.getUsername();
    }
    
    public record MemberResponse(String userId, String username, String role, String status) {}
}