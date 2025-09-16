package com.focushive.hive.service;

import com.focushive.hive.dto.HiveMemberResponse;
import com.focushive.hive.dto.InvitationRequest;
import com.focushive.hive.dto.InvitationResponse;
import com.focushive.hive.entity.HiveMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing hive membership operations
 */
public interface HiveMembershipService {

    /**
     * Join a hive as a member
     *
     * @param hiveId the hive to join
     * @param userId the user joining
     * @return the created membership
     */
    HiveMemberResponse joinHive(String hiveId, String userId);

    /**
     * Leave a hive
     *
     * @param hiveId the hive to leave
     * @param userId the user leaving
     */
    void leaveHive(String hiveId, String userId);

    /**
     * Get all members of a hive
     *
     * @param hiveId the hive ID
     * @param requesterId the user requesting (for permission check)
     * @param pageable pagination parameters
     * @return page of hive members
     */
    Page<HiveMemberResponse> getHiveMembers(String hiveId, String requesterId, Pageable pageable);

    /**
     * Get a specific member of a hive
     *
     * @param hiveId the hive ID
     * @param userId the member user ID
     * @param requesterId the user requesting (for permission check)
     * @return the hive member
     */
    HiveMemberResponse getMember(String hiveId, String userId, String requesterId);

    /**
     * Update a member's role
     *
     * @param hiveId the hive ID
     * @param userId the member to update
     * @param newRole the new role
     * @param requesterId the user making the change
     * @return the updated member
     */
    HiveMemberResponse updateMemberRole(String hiveId, String userId, HiveMember.MemberRole newRole, String requesterId);

    /**
     * Transfer ownership of a hive
     *
     * @param hiveId the hive ID
     * @param currentOwnerId the current owner
     * @param newOwnerId the new owner
     */
    void transferOwnership(String hiveId, String currentOwnerId, String newOwnerId);

    /**
     * Send an invitation to join a hive
     *
     * @param hiveId the hive ID
     * @param request the invitation details
     * @param inviterId the user sending the invitation
     * @return the created invitation
     */
    InvitationResponse sendInvitation(String hiveId, InvitationRequest request, String inviterId);

    /**
     * Accept an invitation
     *
     * @param invitationCode the invitation code
     * @param userId the user accepting
     * @return the created membership
     */
    HiveMemberResponse acceptInvitation(String invitationCode, String userId);

    /**
     * Reject an invitation
     *
     * @param invitationCode the invitation code
     * @param userId the user rejecting
     */
    void rejectInvitation(String invitationCode, String userId);

    /**
     * Get invitations for a user
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of invitations
     */
    Page<InvitationResponse> getUserInvitations(String userId, Pageable pageable);

    /**
     * Expire old invitations (batch job)
     *
     * @return number of invitations expired
     */
    int expireInvitations();

    /**
     * Revoke an invitation
     *
     * @param invitationCode the invitation code
     * @param revokerId the user revoking (must be sender or hive owner)
     */
    void revokeInvitation(String invitationCode, String revokerId);
}