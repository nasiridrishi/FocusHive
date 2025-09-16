package com.focushive.hive.service;

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
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HiveMembershipService implementation following TDD approach.
 * These tests define the expected behavior before implementation.
 * THIS WILL FAIL initially as HiveMembershipService doesn't exist yet.
 */
@ExtendWith(MockitoExtension.class)
class HiveMembershipServiceTest {

    @Mock
    private HiveRepository hiveRepository;

    @Mock
    private HiveMemberRepository hiveMemberRepository;

    @Mock
    private HiveInvitationRepository hiveInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private HiveMembershipService hiveMembershipService;

    private User testUser;
    private User ownerUser;
    private User moderatorUser;
    private Hive testHive;
    private HiveMember testMember;
    private HiveMember ownerMember;
    private HiveMember moderatorMember;

    @BeforeEach
    void setUp() {
        // Set up test users
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        ownerUser = new User();
        ownerUser.setId(UUID.randomUUID().toString());
        ownerUser.setUsername("owner");
        ownerUser.setEmail("owner@example.com");

        moderatorUser = new User();
        moderatorUser.setId(UUID.randomUUID().toString());
        moderatorUser.setUsername("moderator");
        moderatorUser.setEmail("moderator@example.com");

        // Set up test hive
        testHive = new Hive();
        testHive.setId(UUID.randomUUID().toString());
        testHive.setName("Test Hive");
        testHive.setSlug("test-hive");
        testHive.setOwner(ownerUser);
        testHive.setMaxMembers(10);
        testHive.setIsPublic(true);
        testHive.setIsActive(true);
        testHive.setMemberCount(2);

        // Set up test members
        ownerMember = new HiveMember();
        ownerMember.setId(UUID.randomUUID().toString());
        ownerMember.setHive(testHive);
        ownerMember.setUser(ownerUser);
        ownerMember.setRole(HiveMember.MemberRole.OWNER);
        ownerMember.setJoinedAt(LocalDateTime.now().minusDays(10));

        moderatorMember = new HiveMember();
        moderatorMember.setId(UUID.randomUUID().toString());
        moderatorMember.setHive(testHive);
        moderatorMember.setUser(moderatorUser);
        moderatorMember.setRole(HiveMember.MemberRole.MODERATOR);
        moderatorMember.setJoinedAt(LocalDateTime.now().minusDays(5));

        testMember = new HiveMember();
        testMember.setId(UUID.randomUUID().toString());
        testMember.setHive(testHive);
        testMember.setUser(testUser);
        testMember.setRole(HiveMember.MemberRole.MEMBER);
        testMember.setJoinedAt(LocalDateTime.now());
    }

    @Test
    void shouldJoinPublicHive() {
        // Arrange
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), testUser.getId())).thenReturn(false);
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(2L);
        when(hiveMemberRepository.save(any(HiveMember.class))).thenReturn(testMember);

        // Act
        HiveMemberResponse response = hiveMembershipService.joinHive(testHive.getId(), testUser.getId());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getRole()).isEqualTo(HiveMember.MemberRole.MEMBER);
        verify(hiveMemberRepository).save(any(HiveMember.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldRequireInviteForPrivateHive() {
        // Arrange
        testHive.setIsPublic(false);
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), testUser.getId())).thenReturn(false);
        when(hiveInvitationRepository.findByHiveIdAndInvitedUserIdAndStatus(
            testHive.getId(), testUser.getId(), HiveInvitation.InvitationStatus.PENDING))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> hiveMembershipService.joinHive(testHive.getId(), testUser.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("This is a private hive. You need an invitation to join.");
    }

    @Test
    void shouldEnforceMaxMembersLimit() {
        // Arrange
        testHive.setMaxMembers(2);
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), testUser.getId())).thenReturn(false);
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(2L);

        // Act & Assert
        assertThatThrownBy(() -> hiveMembershipService.joinHive(testHive.getId(), testUser.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This hive is full (2/2 members)");
    }

    @Test
    void shouldLeaveHive() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));

        // Act
        hiveMembershipService.leaveHive(testHive.getId(), testUser.getId());

        // Assert
        verify(hiveMemberRepository).delete(testMember);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldTransferOwnership() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), ownerUser.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), moderatorUser.getId()))
                .thenReturn(Optional.of(moderatorMember));
        when(hiveMemberRepository.save(any(HiveMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hiveRepository.save(any(Hive.class))).thenReturn(testHive);

        // Act
        hiveMembershipService.transferOwnership(testHive.getId(), ownerUser.getId(), moderatorUser.getId());

        // Assert
        verify(hiveMemberRepository, times(2)).save(any(HiveMember.class));
        verify(hiveRepository).save(testHive);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldPromoteMemberToModerator() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), ownerUser.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(hiveMemberRepository.save(any(HiveMember.class))).thenReturn(testMember);

        // Act
        HiveMemberResponse response = hiveMembershipService.updateMemberRole(
                testHive.getId(), testUser.getId(), HiveMember.MemberRole.MODERATOR, ownerUser.getId());

        // Assert
        assertThat(response.getRole()).isEqualTo(HiveMember.MemberRole.MODERATOR);
        verify(hiveMemberRepository).save(testMember);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldDemoteModerator() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), ownerUser.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), moderatorUser.getId()))
                .thenReturn(Optional.of(moderatorMember));
        when(hiveMemberRepository.save(any(HiveMember.class))).thenReturn(moderatorMember);

        // Act
        HiveMemberResponse response = hiveMembershipService.updateMemberRole(
                testHive.getId(), moderatorUser.getId(), HiveMember.MemberRole.MEMBER, ownerUser.getId());

        // Assert
        assertThat(response.getRole()).isEqualTo(HiveMember.MemberRole.MEMBER);
        verify(hiveMemberRepository).save(moderatorMember);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldHandleLastMemberLeaving() {
        // Arrange - owner is the last member
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), ownerUser.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(1L);
        when(hiveRepository.save(any(Hive.class))).thenReturn(testHive);

        // Act
        hiveMembershipService.leaveHive(testHive.getId(), ownerUser.getId());

        // Assert - hive should be archived
        verify(hiveMemberRepository).delete(ownerMember);
        verify(hiveRepository).save(argThat(hive -> !hive.getIsActive()));
        verify(eventPublisher, times(2)).publishEvent(any()); // Leave event + Archive event
    }

    @Test
    void shouldSendInvitation() {
        // Arrange
        InvitationRequest invitationRequest = new InvitationRequest();
        invitationRequest.setEmail("invited@example.com");
        invitationRequest.setMessage("Join our study group!");

        User invitedUser = new User();
        invitedUser.setId(UUID.randomUUID().toString());
        invitedUser.setEmail("invited@example.com");

        HiveInvitation savedInvitation = new HiveInvitation();
        savedInvitation.setId(UUID.randomUUID().toString());
        savedInvitation.setHive(testHive);
        savedInvitation.setInvitedUser(invitedUser);
        savedInvitation.setInvitedBy(ownerUser);
        savedInvitation.setInvitationCode(UUID.randomUUID().toString());

        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), ownerUser.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(userRepository.findByEmail("invited@example.com")).thenReturn(Optional.of(invitedUser));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), invitedUser.getId())).thenReturn(false);
        when(hiveInvitationRepository.existsByHiveIdAndInvitedUserIdAndStatus(
                testHive.getId(), invitedUser.getId(), HiveInvitation.InvitationStatus.PENDING)).thenReturn(false);
        when(hiveInvitationRepository.save(any(HiveInvitation.class))).thenReturn(savedInvitation);

        // Act
        InvitationResponse response = hiveMembershipService.sendInvitation(testHive.getId(), invitationRequest, ownerUser.getId());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getInvitedEmail()).isEqualTo("invited@example.com");
        verify(hiveInvitationRepository).save(any(HiveInvitation.class));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldAcceptInvitation() {
        // Arrange
        HiveInvitation invitation = new HiveInvitation();
        invitation.setId(UUID.randomUUID().toString());
        invitation.setHive(testHive);
        invitation.setInvitedUser(testUser);
        invitation.setInvitedBy(ownerUser);
        invitation.setInvitationCode(UUID.randomUUID().toString());
        invitation.setStatus(HiveInvitation.InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(hiveInvitationRepository.findByInvitationCode(invitation.getInvitationCode()))
                .thenReturn(Optional.of(invitation));
        when(hiveMemberRepository.save(any(HiveMember.class))).thenReturn(testMember);
        when(hiveInvitationRepository.save(any(HiveInvitation.class))).thenReturn(invitation);

        // Act
        HiveMemberResponse response = hiveMembershipService.acceptInvitation(invitation.getInvitationCode(), testUser.getId());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        verify(hiveMemberRepository).save(any(HiveMember.class));
        verify(hiveInvitationRepository).save(argThat(inv ->
            inv.getStatus() == HiveInvitation.InvitationStatus.ACCEPTED));
        verify(eventPublisher, times(2)).publishEvent(any()); // Join event + Accept invitation event
    }

    @Test
    void shouldRejectInvitation() {
        // Arrange
        HiveInvitation invitation = new HiveInvitation();
        invitation.setId(UUID.randomUUID().toString());
        invitation.setHive(testHive);
        invitation.setInvitedUser(testUser);
        invitation.setInvitationCode(UUID.randomUUID().toString());
        invitation.setStatus(HiveInvitation.InvitationStatus.PENDING);

        when(hiveInvitationRepository.findByInvitationCode(invitation.getInvitationCode()))
                .thenReturn(Optional.of(invitation));
        when(hiveInvitationRepository.save(any(HiveInvitation.class))).thenReturn(invitation);

        // Act
        hiveMembershipService.rejectInvitation(invitation.getInvitationCode(), testUser.getId());

        // Assert
        verify(hiveInvitationRepository).save(argThat(inv ->
            inv.getStatus() == HiveInvitation.InvitationStatus.REJECTED));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldExpireInvitations() {
        // Arrange
        List<HiveInvitation> expiredInvitations = List.of(
            createExpiredInvitation(),
            createExpiredInvitation()
        );
        when(hiveInvitationRepository.findExpiredInvitations()).thenReturn(expiredInvitations);
        when(hiveInvitationRepository.saveAll(anyList())).thenReturn(expiredInvitations);

        // Act
        int expiredCount = hiveMembershipService.expireInvitations();

        // Assert
        assertThat(expiredCount).isEqualTo(2);
        verify(hiveInvitationRepository).saveAll(argThat(invitations -> {
            for (HiveInvitation inv : invitations) {
                if (inv.getStatus() != HiveInvitation.InvitationStatus.EXPIRED) {
                    return false;
                }
            }
            return true;
        }));
    }

    @Test
    void shouldPreventSelfPromotion() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), moderatorUser.getId()))
                .thenReturn(Optional.of(moderatorMember));

        // Act & Assert
        assertThatThrownBy(() -> hiveMembershipService.updateMemberRole(
                testHive.getId(), moderatorUser.getId(), HiveMember.MemberRole.OWNER, moderatorUser.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You cannot change your own role");
    }

    @Test
    void shouldValidateRoleHierarchy() {
        // Arrange - moderator trying to promote someone to owner
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), moderatorUser.getId()))
                .thenReturn(Optional.of(moderatorMember));

        // Act & Assert
        assertThatThrownBy(() -> hiveMembershipService.updateMemberRole(
                testHive.getId(), testUser.getId(), HiveMember.MemberRole.OWNER, moderatorUser.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You don't have permission to assign this role");
    }

    @Test
    void shouldListHiveMembers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<HiveMember> members = List.of(ownerMember, moderatorMember, testMember);
        Page<HiveMember> memberPage = new PageImpl<>(members);

        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), ownerUser.getId()))
                .thenReturn(Optional.of(ownerMember));
        when(hiveMemberRepository.findByHiveIdOrderByJoinedAtAsc(testHive.getId(), pageable))
                .thenReturn(memberPage);

        // Act
        Page<HiveMemberResponse> response = hiveMembershipService.getHiveMembers(testHive.getId(), ownerUser.getId(), pageable);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(3);
        assertThat(response.getContent().get(0).getRole()).isEqualTo(HiveMember.MemberRole.OWNER);
    }

    private HiveInvitation createExpiredInvitation() {
        HiveInvitation invitation = new HiveInvitation();
        invitation.setId(UUID.randomUUID().toString());
        invitation.setStatus(HiveInvitation.InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
        return invitation;
    }
}