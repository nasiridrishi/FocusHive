package com.focushive.hive.controller;

import com.focushive.hive.dto.HiveMemberResponse;
import com.focushive.hive.dto.InvitationRequest;
import com.focushive.hive.dto.InvitationResponse;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.service.HiveMembershipService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for hive membership operations
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('USER')")
public class HiveMembershipController {

    private static final Logger logger = LoggerFactory.getLogger(HiveMembershipController.class);

    private final HiveMembershipService hiveMembershipService;

    @Autowired
    public HiveMembershipController(HiveMembershipService hiveMembershipService) {
        this.hiveMembershipService = hiveMembershipService;
    }

    /**
     * Join a hive
     */
    @PostMapping("/hives/{hiveId}/join")
    public ResponseEntity<HiveMemberResponse> joinHive(
            @PathVariable String hiveId,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} attempting to join hive {}", userId, hiveId);

        HiveMemberResponse response = hiveMembershipService.joinHive(hiveId, userId);

        logger.info("User {} successfully joined hive {}", userId, hiveId);
        return ResponseEntity.ok(response);
    }

    /**
     * Leave a hive
     */
    @DeleteMapping("/hives/{hiveId}/leave")
    public ResponseEntity<Map<String, String>> leaveHive(
            @PathVariable String hiveId,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} attempting to leave hive {}", userId, hiveId);

        hiveMembershipService.leaveHive(hiveId, userId);

        logger.info("User {} successfully left hive {}", userId, hiveId);
        return ResponseEntity.ok(Map.of("message", "Successfully left the hive"));
    }

    /**
     * Get all members of a hive
     */
    @GetMapping("/hives/{hiveId}/members")
    public ResponseEntity<Page<HiveMemberResponse>> getHiveMembers(
            @PathVariable String hiveId,
            @RequestHeader("User-ID") String userId,
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        logger.debug("User {} requesting members of hive {}", userId, hiveId);

        Page<HiveMemberResponse> members = hiveMembershipService.getHiveMembers(hiveId, userId, pageable);

        return ResponseEntity.ok(members);
    }

    /**
     * Get details of a specific member
     */
    @GetMapping("/hives/{hiveId}/members/{memberId}")
    public ResponseEntity<HiveMemberResponse> getMember(
            @PathVariable String hiveId,
            @PathVariable String memberId,
            @RequestHeader("User-ID") String userId) {
        logger.debug("User {} requesting member {} details from hive {}", userId, memberId, hiveId);

        HiveMemberResponse member = hiveMembershipService.getMember(hiveId, memberId, userId);

        return ResponseEntity.ok(member);
    }

    /**
     * Update a member's role
     */
    @PutMapping("/hives/{hiveId}/members/{memberId}/role")
    public ResponseEntity<HiveMemberResponse> updateMemberRole(
            @PathVariable String hiveId,
            @PathVariable String memberId,
            @RequestBody @Valid UpdateRoleRequest request,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} updating role of member {} in hive {} to {}", userId, memberId, hiveId, request.getRole());

        HiveMemberResponse response = hiveMembershipService.updateMemberRole(hiveId, memberId, request.getRole(), userId);

        logger.info("Successfully updated role of member {} in hive {} to {}", memberId, hiveId, request.getRole());
        return ResponseEntity.ok(response);
    }

    /**
     * Transfer ownership of a hive
     */
    @PostMapping("/hives/{hiveId}/transfer-ownership")
    public ResponseEntity<Map<String, String>> transferOwnership(
            @PathVariable String hiveId,
            @RequestBody @Valid TransferOwnershipRequest request,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} transferring ownership of hive {} to {}", userId, hiveId, request.getNewOwnerId());

        hiveMembershipService.transferOwnership(hiveId, userId, request.getNewOwnerId());

        logger.info("Successfully transferred ownership of hive {} to {}", hiveId, request.getNewOwnerId());
        return ResponseEntity.ok(Map.of("message", "Ownership transferred successfully"));
    }

    /**
     * Send an invitation to join a hive
     */
    @PostMapping("/hives/{hiveId}/invitations")
    public ResponseEntity<InvitationResponse> sendInvitation(
            @PathVariable String hiveId,
            @RequestBody @Valid InvitationRequest request,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} sending invitation to {} for hive {}", userId, request.getEmail(), hiveId);

        InvitationResponse response = hiveMembershipService.sendInvitation(hiveId, request, userId);

        logger.info("Successfully sent invitation {} to {} for hive {}", response.getId(), request.getEmail(), hiveId);
        return ResponseEntity.ok(response);
    }

    /**
     * Accept an invitation
     */
    @PostMapping("/invitations/{invitationCode}/accept")
    public ResponseEntity<HiveMemberResponse> acceptInvitation(
            @PathVariable String invitationCode,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} accepting invitation {}", userId, invitationCode);

        HiveMemberResponse response = hiveMembershipService.acceptInvitation(invitationCode, userId);

        logger.info("User {} successfully accepted invitation {}", userId, invitationCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject an invitation
     */
    @PostMapping("/invitations/{invitationCode}/reject")
    public ResponseEntity<Map<String, String>> rejectInvitation(
            @PathVariable String invitationCode,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} rejecting invitation {}", userId, invitationCode);

        hiveMembershipService.rejectInvitation(invitationCode, userId);

        logger.info("User {} successfully rejected invitation {}", userId, invitationCode);
        return ResponseEntity.ok(Map.of("message", "Invitation rejected successfully"));
    }

    /**
     * Revoke an invitation
     */
    @DeleteMapping("/invitations/{invitationCode}")
    public ResponseEntity<Map<String, String>> revokeInvitation(
            @PathVariable String invitationCode,
            @RequestHeader("User-ID") String userId) {
        logger.info("User {} revoking invitation {}", userId, invitationCode);

        hiveMembershipService.revokeInvitation(invitationCode, userId);

        logger.info("User {} successfully revoked invitation {}", userId, invitationCode);
        return ResponseEntity.ok(Map.of("message", "Invitation revoked successfully"));
    }

    /**
     * Get invitations for the current user
     */
    @GetMapping("/users/{userId}/invitations")
    public ResponseEntity<Page<InvitationResponse>> getUserInvitations(
            @PathVariable String userId,
            @RequestHeader("User-ID") String requesterId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("User {} requesting invitations for user {}", requesterId, userId);

        // Users can only view their own invitations
        if (!userId.equals(requesterId)) {
            return ResponseEntity.status(403).build();
        }

        Page<InvitationResponse> invitations = hiveMembershipService.getUserInvitations(userId, pageable);

        return ResponseEntity.ok(invitations);
    }

    /**
     * Get current user's invitations (convenience endpoint)
     */
    @GetMapping("/my/invitations")
    public ResponseEntity<Page<InvitationResponse>> getMyInvitations(
            @RequestHeader("User-ID") String userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.debug("User {} requesting their own invitations", userId);

        Page<InvitationResponse> invitations = hiveMembershipService.getUserInvitations(userId, pageable);

        return ResponseEntity.ok(invitations);
    }

    // Inner classes for request DTOs
    public static class UpdateRoleRequest {
        @NotBlank(message = "Role is required")
        private HiveMember.MemberRole role;

        public HiveMember.MemberRole getRole() {
            return role;
        }

        public void setRole(HiveMember.MemberRole role) {
            this.role = role;
        }
    }

    public static class TransferOwnershipRequest {
        @NotBlank(message = "New owner ID is required")
        private String newOwnerId;

        public String getNewOwnerId() {
            return newOwnerId;
        }

        public void setNewOwnerId(String newOwnerId) {
            this.newOwnerId = newOwnerId;
        }
    }
}