package com.focushive.hive.service.impl;

import com.focushive.common.exception.BadRequestException;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.dto.HiveMemberResponse;
import com.focushive.hive.dto.InvitationRequest;
import com.focushive.hive.dto.InvitationResponse;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveInvitation;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.repository.HiveInvitationRepository;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.hive.service.HiveMembershipService;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class HiveMembershipServiceImpl implements HiveMembershipService {

    private static final Logger logger = LoggerFactory.getLogger(HiveMembershipServiceImpl.class);

    private final HiveRepository hiveRepository;
    private final HiveMemberRepository hiveMemberRepository;
    private final HiveInvitationRepository hiveInvitationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public HiveMembershipServiceImpl(
            HiveRepository hiveRepository,
            HiveMemberRepository hiveMemberRepository,
            HiveInvitationRepository hiveInvitationRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.hiveRepository = hiveRepository;
        this.hiveMemberRepository = hiveMemberRepository;
        this.hiveInvitationRepository = hiveInvitationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @CacheEvict(value = {"hive-members", "hive-member-count"}, key = "#hiveId")
    public HiveMemberResponse joinHive(String hiveId, String userId) {
        logger.info("User {} attempting to join hive {}", userId, hiveId);

        // Validate hive exists and is active
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user is already a member
        if (hiveMemberRepository.existsByHiveIdAndUserId(hiveId, userId)) {
            throw new BadRequestException("You are already a member of this hive");
        }

        // Check if hive is full
        long currentMemberCount = hiveMemberRepository.countByHiveId(hiveId);
        if (currentMemberCount >= hive.getMaxMembers()) {
            throw new BadRequestException(String.format("This hive is full (%d/%d members)",
                    currentMemberCount, hive.getMaxMembers()));
        }

        // For private hives, check if user has a valid invitation
        if (!hive.getIsPublic()) {
            boolean hasValidInvitation = hiveInvitationRepository
                    .findByHiveIdAndInvitedUserIdAndStatus(hiveId, userId, HiveInvitation.InvitationStatus.PENDING)
                    .map(invitation -> !invitation.isExpired())
                    .orElse(false);

            if (!hasValidInvitation) {
                throw new ForbiddenException("This is a private hive. You need an invitation to join.");
            }
        }

        // Create membership
        HiveMember member = new HiveMember();
        member.setHive(hive);
        member.setUser(user);
        member.setRole(HiveMember.MemberRole.MEMBER);
        member.setStatus(HiveMember.MemberStatus.ACTIVE);
        member.setJoinedAt(LocalDateTime.now());

        // If user joined via invitation, mark it as accepted and set invited by
        HiveInvitation existingInvitation = hiveInvitationRepository
                .findByHiveIdAndInvitedUserIdAndStatus(hiveId, userId, HiveInvitation.InvitationStatus.PENDING)
                .orElse(null);

        if (existingInvitation != null) {
            member.setInvitedBy(existingInvitation.getInvitedBy());
            existingInvitation.setStatus(HiveInvitation.InvitationStatus.ACCEPTED);
            existingInvitation.setRespondedAt(LocalDateTime.now());
            hiveInvitationRepository.save(existingInvitation);
        }

        member = hiveMemberRepository.save(member);

        // Update hive member count
        hive.setMemberCount(hive.getMemberCount() + 1);
        hiveRepository.save(hive);

        // Publish event
        eventPublisher.publishEvent(new MemberJoinedEvent(hive, member));

        logger.info("User {} successfully joined hive {}", userId, hiveId);
        return new HiveMemberResponse(member);
    }

    @Override
    @CacheEvict(value = {"hive-members", "hive-member-count"}, key = "#hiveId")
    public void leaveHive(String hiveId, String userId) {
        logger.info("User {} attempting to leave hive {}", userId, hiveId);

        HiveMember member = hiveMemberRepository.findByHiveIdAndUserId(hiveId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this hive"));

        // If owner is leaving, handle special cases
        if (member.getRole() == HiveMember.MemberRole.OWNER) {
            long memberCount = hiveMemberRepository.countByHiveId(hiveId);
            if (memberCount > 1) {
                throw new BadRequestException(
                        "As the owner, you cannot leave the hive while other members are present. " +
                        "Please transfer ownership first or wait for other members to leave.");
            } else {
                // Owner is the last member - archive the hive
                Hive hive = member.getHive();
                hive.setIsActive(false);
                hive.setDeletedAt(LocalDateTime.now());
                hiveRepository.save(hive);
                eventPublisher.publishEvent(new HiveArchivedEvent(hive, member));
            }
        }

        // Remove membership
        hiveMemberRepository.delete(member);

        // Update member count if hive is still active
        if (member.getHive().getIsActive()) {
            Hive hive = member.getHive();
            hive.setMemberCount(Math.max(0, hive.getMemberCount() - 1));
            hiveRepository.save(hive);
        }

        // Publish event
        eventPublisher.publishEvent(new MemberLeftEvent(member.getHive(), member));

        logger.info("User {} successfully left hive {}", userId, hiveId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "hive-members", key = "#hiveId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<HiveMemberResponse> getHiveMembers(String hiveId, String requesterId, Pageable pageable) {
        // Validate hive exists
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));

        // Check if requester is a member (for private hives)
        if (!hive.getIsPublic()) {
            boolean isMember = hiveMemberRepository.existsByHiveIdAndUserId(hiveId, requesterId);
            if (!isMember) {
                throw new ForbiddenException("You must be a member to view the member list of this private hive");
            }
        }

        Page<HiveMember> members = hiveMemberRepository.findByHiveIdOrderByJoinedAtAsc(hiveId, pageable);
        return members.map(HiveMemberResponse::new);
    }

    @Override
    @Transactional(readOnly = true)
    public HiveMemberResponse getMember(String hiveId, String userId, String requesterId) {
        // Validate hive exists
        hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));

        // Check permissions (member can view themselves, others need to be members)
        if (!userId.equals(requesterId)) {
            boolean isRequesterMember = hiveMemberRepository.existsByHiveIdAndUserId(hiveId, requesterId);
            if (!isRequesterMember) {
                throw new ForbiddenException("You must be a member to view member details");
            }
        }

        HiveMember member = hiveMemberRepository.findByHiveIdAndUserId(hiveId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        return new HiveMemberResponse(member);
    }

    @Override
    @CacheEvict(value = "hive-members", key = "#hiveId + '*'")
    public HiveMemberResponse updateMemberRole(String hiveId, String userId, HiveMember.MemberRole newRole, String requesterId) {
        logger.info("User {} attempting to update role of user {} in hive {} to {}", requesterId, userId, hiveId, newRole);

        // Validate requester permissions
        HiveMember requester = hiveMemberRepository.findByHiveIdAndUserId(hiveId, requesterId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this hive"));

        // Prevent self-role changes
        if (userId.equals(requesterId)) {
            throw new ForbiddenException("You cannot change your own role");
        }

        // Get target member
        HiveMember targetMember = hiveMemberRepository.findByHiveIdAndUserId(hiveId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        // Validate role hierarchy permissions
        validateRoleChangePermission(requester.getRole(), targetMember.getRole(), newRole);

        // Handle ownership transfer specially
        if (newRole == HiveMember.MemberRole.OWNER) {
            transferOwnership(hiveId, requesterId, userId);
            return new HiveMemberResponse(hiveMemberRepository.findByHiveIdAndUserId(hiveId, userId).orElseThrow());
        }

        // Update role
        targetMember.setRole(newRole);
        targetMember = hiveMemberRepository.save(targetMember);

        // Publish event
        eventPublisher.publishEvent(new MemberRoleChangedEvent(targetMember, requester));

        logger.info("Successfully updated role of user {} in hive {} to {}", userId, hiveId, newRole);
        return new HiveMemberResponse(targetMember);
    }

    @Override
    public void transferOwnership(String hiveId, String currentOwnerId, String newOwnerId) {
        logger.info("Transferring ownership of hive {} from {} to {}", hiveId, currentOwnerId, newOwnerId);

        // Validate current owner
        HiveMember currentOwner = hiveMemberRepository.findByHiveIdAndUserId(hiveId, currentOwnerId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this hive"));

        if (currentOwner.getRole() != HiveMember.MemberRole.OWNER) {
            throw new ForbiddenException("Only the hive owner can transfer ownership");
        }

        // Validate new owner
        HiveMember newOwner = hiveMemberRepository.findByHiveIdAndUserId(hiveId, newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("New owner must be a member of the hive"));

        if (newOwner.getRole() == HiveMember.MemberRole.OWNER) {
            throw new BadRequestException("User is already the owner");
        }

        // Update roles
        currentOwner.setRole(HiveMember.MemberRole.MODERATOR);
        newOwner.setRole(HiveMember.MemberRole.OWNER);

        hiveMemberRepository.save(currentOwner);
        hiveMemberRepository.save(newOwner);

        // Update hive owner
        Hive hive = currentOwner.getHive();
        hive.setOwner(newOwner.getUser());
        hiveRepository.save(hive);

        // Publish event
        eventPublisher.publishEvent(new OwnershipTransferredEvent(hive, currentOwner, newOwner));

        logger.info("Successfully transferred ownership of hive {} to {}", hiveId, newOwnerId);
    }

    @Override
    public InvitationResponse sendInvitation(String hiveId, InvitationRequest request, String inviterId) {
        logger.info("User {} sending invitation to {} for hive {}", inviterId, request.getEmail(), hiveId);

        // Validate hive and inviter permissions
        Hive hive = hiveRepository.findByIdAndActive(hiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Hive not found or inactive"));

        HiveMember inviter = hiveMemberRepository.findByHiveIdAndUserId(hiveId, inviterId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this hive"));

        if (inviter.getRole() == HiveMember.MemberRole.MEMBER) {
            throw new ForbiddenException("Only hive owners and moderators can send invitations");
        }

        // Find invited user
        User invitedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + request.getEmail() + " not found"));

        // Check if user is already a member
        if (hiveMemberRepository.existsByHiveIdAndUserId(hiveId, invitedUser.getId())) {
            throw new BadRequestException("User is already a member of this hive");
        }

        // Check for existing pending invitation
        if (hiveInvitationRepository.existsByHiveIdAndInvitedUserIdAndStatus(
                hiveId, invitedUser.getId(), HiveInvitation.InvitationStatus.PENDING)) {
            throw new BadRequestException("User already has a pending invitation to this hive");
        }

        // Create invitation
        HiveInvitation invitation = new HiveInvitation();
        invitation.setHive(hive);
        invitation.setInvitedUser(invitedUser);
        invitation.setInvitedBy(inviter.getUser());
        invitation.setMessage(request.getMessage());

        invitation = hiveInvitationRepository.save(invitation);

        // Publish event
        eventPublisher.publishEvent(new InvitationSentEvent(invitation));

        logger.info("Successfully sent invitation {} to {} for hive {}", invitation.getId(), request.getEmail(), hiveId);
        return new InvitationResponse(invitation);
    }

    @Override
    public HiveMemberResponse acceptInvitation(String invitationCode, String userId) {
        logger.info("User {} accepting invitation {}", userId, invitationCode);

        HiveInvitation invitation = hiveInvitationRepository.findByInvitationCode(invitationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Validate invitation
        if (!invitation.getInvitedUser().getId().equals(userId)) {
            throw new ForbiddenException("This invitation is not for you");
        }

        if (invitation.getStatus() != HiveInvitation.InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is no longer valid");
        }

        if (invitation.isExpired()) {
            invitation.setStatus(HiveInvitation.InvitationStatus.EXPIRED);
            hiveInvitationRepository.save(invitation);
            throw new BadRequestException("Invitation has expired");
        }

        // Accept invitation
        invitation.setStatus(HiveInvitation.InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        hiveInvitationRepository.save(invitation);

        // Join the hive
        HiveMemberResponse memberResponse = joinHive(invitation.getHive().getId(), userId);

        // Publish event
        eventPublisher.publishEvent(new InvitationAcceptedEvent(invitation));

        logger.info("User {} successfully accepted invitation {}", userId, invitationCode);
        return memberResponse;
    }

    @Override
    public void rejectInvitation(String invitationCode, String userId) {
        logger.info("User {} rejecting invitation {}", userId, invitationCode);

        HiveInvitation invitation = hiveInvitationRepository.findByInvitationCode(invitationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Validate invitation
        if (!invitation.getInvitedUser().getId().equals(userId)) {
            throw new ForbiddenException("This invitation is not for you");
        }

        if (invitation.getStatus() != HiveInvitation.InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is no longer valid");
        }

        // Reject invitation
        invitation.setStatus(HiveInvitation.InvitationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        hiveInvitationRepository.save(invitation);

        // Publish event
        eventPublisher.publishEvent(new InvitationRejectedEvent(invitation));

        logger.info("User {} successfully rejected invitation {}", userId, invitationCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvitationResponse> getUserInvitations(String userId, Pageable pageable) {
        Page<HiveInvitation> invitations = hiveInvitationRepository
                .findByInvitedUserIdOrderByCreatedAtDesc(userId, pageable);
        return invitations.map(InvitationResponse::new);
    }

    @Override
    public int expireInvitations() {
        List<HiveInvitation> expiredInvitations = hiveInvitationRepository.findExpiredInvitations();

        for (HiveInvitation invitation : expiredInvitations) {
            invitation.setStatus(HiveInvitation.InvitationStatus.EXPIRED);
        }

        hiveInvitationRepository.saveAll(expiredInvitations);

        logger.info("Expired {} invitations", expiredInvitations.size());
        return expiredInvitations.size();
    }

    @Override
    public void revokeInvitation(String invitationCode, String revokerId) {
        logger.info("User {} revoking invitation {}", revokerId, invitationCode);

        HiveInvitation invitation = hiveInvitationRepository.findByInvitationCode(invitationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Check permissions - must be sender or hive owner
        boolean canRevoke = invitation.getInvitedBy().getId().equals(revokerId) ||
                invitation.getHive().getOwner().getId().equals(revokerId);

        if (!canRevoke) {
            throw new ForbiddenException("You can only revoke invitations you sent or if you are the hive owner");
        }

        if (invitation.getStatus() != HiveInvitation.InvitationStatus.PENDING) {
            throw new BadRequestException("Only pending invitations can be revoked");
        }

        invitation.setStatus(HiveInvitation.InvitationStatus.REVOKED);
        hiveInvitationRepository.save(invitation);

        logger.info("Successfully revoked invitation {}", invitationCode);
    }

    private void validateRoleChangePermission(HiveMember.MemberRole requesterRole,
                                              HiveMember.MemberRole targetCurrentRole,
                                              HiveMember.MemberRole newRole) {
        // Only owners can promote to owner (handled separately in transferOwnership)
        if (newRole == HiveMember.MemberRole.OWNER) {
            if (requesterRole != HiveMember.MemberRole.OWNER) {
                throw new ForbiddenException("Only the hive owner can transfer ownership");
            }
            return;
        }

        // Owners can change any role (except to owner)
        if (requesterRole == HiveMember.MemberRole.OWNER) {
            return;
        }

        // Moderators can only promote members to moderator or demote moderators to member
        if (requesterRole == HiveMember.MemberRole.MODERATOR) {
            boolean validChange = (targetCurrentRole == HiveMember.MemberRole.MEMBER && newRole == HiveMember.MemberRole.MODERATOR) ||
                    (targetCurrentRole == HiveMember.MemberRole.MODERATOR && newRole == HiveMember.MemberRole.MEMBER);
            if (!validChange) {
                throw new ForbiddenException("You don't have permission to assign this role");
            }
            return;
        }

        // Members cannot change roles
        throw new ForbiddenException("You don't have permission to change member roles");
    }

    // Event classes (would typically be in separate files)
    public static class MemberJoinedEvent {
        private final Hive hive;
        private final HiveMember member;

        public MemberJoinedEvent(Hive hive, HiveMember member) {
            this.hive = hive;
            this.member = member;
        }

        public Hive getHive() { return hive; }
        public HiveMember getMember() { return member; }
    }

    public static class MemberLeftEvent {
        private final Hive hive;
        private final HiveMember member;

        public MemberLeftEvent(Hive hive, HiveMember member) {
            this.hive = hive;
            this.member = member;
        }

        public Hive getHive() { return hive; }
        public HiveMember getMember() { return member; }
    }

    public static class MemberRoleChangedEvent {
        private final HiveMember member;
        private final HiveMember changedBy;

        public MemberRoleChangedEvent(HiveMember member, HiveMember changedBy) {
            this.member = member;
            this.changedBy = changedBy;
        }

        public HiveMember getMember() { return member; }
        public HiveMember getChangedBy() { return changedBy; }
    }

    public static class OwnershipTransferredEvent {
        private final Hive hive;
        private final HiveMember previousOwner;
        private final HiveMember newOwner;

        public OwnershipTransferredEvent(Hive hive, HiveMember previousOwner, HiveMember newOwner) {
            this.hive = hive;
            this.previousOwner = previousOwner;
            this.newOwner = newOwner;
        }

        public Hive getHive() { return hive; }
        public HiveMember getPreviousOwner() { return previousOwner; }
        public HiveMember getNewOwner() { return newOwner; }
    }

    public static class HiveArchivedEvent {
        private final Hive hive;
        private final HiveMember lastMember;

        public HiveArchivedEvent(Hive hive, HiveMember lastMember) {
            this.hive = hive;
            this.lastMember = lastMember;
        }

        public Hive getHive() { return hive; }
        public HiveMember getLastMember() { return lastMember; }
    }

    public static class InvitationSentEvent {
        private final HiveInvitation invitation;

        public InvitationSentEvent(HiveInvitation invitation) {
            this.invitation = invitation;
        }

        public HiveInvitation getInvitation() { return invitation; }
    }

    public static class InvitationAcceptedEvent {
        private final HiveInvitation invitation;

        public InvitationAcceptedEvent(HiveInvitation invitation) {
            this.invitation = invitation;
        }

        public HiveInvitation getInvitation() { return invitation; }
    }

    public static class InvitationRejectedEvent {
        private final HiveInvitation invitation;

        public InvitationRejectedEvent(HiveInvitation invitation) {
            this.invitation = invitation;
        }

        public HiveInvitation getInvitation() { return invitation; }
    }
}