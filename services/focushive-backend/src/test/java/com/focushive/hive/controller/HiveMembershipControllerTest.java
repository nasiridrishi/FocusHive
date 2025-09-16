package com.focushive.hive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focushive.hive.dto.HiveMemberResponse;
import com.focushive.hive.dto.InvitationRequest;
import com.focushive.hive.dto.InvitationResponse;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.service.HiveMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HiveMembershipController following TDD approach.
 * These tests define the expected behavior before implementation.
 * THIS WILL FAIL initially as HiveMembershipController doesn't exist yet.
 */
@WebMvcTest(HiveMembershipController.class)
class HiveMembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HiveMembershipService hiveMembershipService;


    private String testHiveId;
    private String testUserId;
    private String invitationCode;
    private HiveMemberResponse memberResponse;
    private InvitationResponse invitationResponse;

    @BeforeEach
    void setUp() {
        testHiveId = UUID.randomUUID().toString();
        testUserId = UUID.randomUUID().toString();
        invitationCode = UUID.randomUUID().toString();

        memberResponse = new HiveMemberResponse();
        memberResponse.setId(UUID.randomUUID().toString());
        memberResponse.setUserId(testUserId);
        memberResponse.setUsername("testuser");
        memberResponse.setRole(HiveMember.MemberRole.MEMBER);
        memberResponse.setJoinedAt(LocalDateTime.now());

        invitationResponse = new InvitationResponse();
        invitationResponse.setId(UUID.randomUUID().toString());
        invitationResponse.setHiveId(testHiveId);
        invitationResponse.setInvitedEmail("test@example.com");
        invitationResponse.setInvitationCode(invitationCode);
    }

    @Test
    @WithMockUser
    void shouldJoinHiveSuccessfully() throws Exception {
        // Arrange
        when(hiveMembershipService.joinHive(testHiveId, testUserId)).thenReturn(memberResponse);

        // Act & Assert
        mockMvc.perform(post("/api/hives/{hiveId}/join", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberResponse.getId()))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(hiveMembershipService).joinHive(testHiveId, testUserId);
    }

    @Test
    @WithMockUser
    void shouldReturn403WhenJoiningPrivateHive() throws Exception {
        // Arrange
        when(hiveMembershipService.joinHive(testHiveId, testUserId))
                .thenThrow(new com.focushive.common.exception.ForbiddenException("This is a private hive. You need an invitation to join."));

        // Act & Assert
        mockMvc.perform(post("/api/hives/{hiveId}/join", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This is a private hive. You need an invitation to join."));

        verify(hiveMembershipService).joinHive(testHiveId, testUserId);
    }

    @Test
    @WithMockUser
    void shouldReturn409WhenHiveFull() throws Exception {
        // Arrange
        when(hiveMembershipService.joinHive(testHiveId, testUserId))
                .thenThrow(new com.focushive.common.exception.BadRequestException("This hive is full (10/10 members)"));

        // Act & Assert
        mockMvc.perform(post("/api/hives/{hiveId}/join", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This hive is full (10/10 members)"));

        verify(hiveMembershipService).joinHive(testHiveId, testUserId);
    }

    @Test
    @WithMockUser
    void shouldLeaveHiveSuccessfully() throws Exception {
        // Arrange
        doNothing().when(hiveMembershipService).leaveHive(testHiveId, testUserId);

        // Act & Assert
        mockMvc.perform(delete("/api/hives/{hiveId}/leave", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully left the hive"));

        verify(hiveMembershipService).leaveHive(testHiveId, testUserId);
    }

    @Test
    @WithMockUser
    void shouldListHiveMembers() throws Exception {
        // Arrange
        List<HiveMemberResponse> members = List.of(memberResponse);
        Page<HiveMemberResponse> memberPage = new PageImpl<>(members);
        when(hiveMembershipService.getHiveMembers(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(memberPage);

        // Act & Assert
        mockMvc.perform(get("/api/hives/{hiveId}/members", testHiveId)
                        .header("User-ID", testUserId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(memberResponse.getId()))
                .andExpect(jsonPath("$.content[0].userId").value(testUserId))
                .andExpect(jsonPath("$.content[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(hiveMembershipService).getHiveMembers(eq(testHiveId), eq(testUserId), any(Pageable.class));
    }

    @Test
    @WithMockUser
    void shouldPromoteMember() throws Exception {
        // Arrange
        memberResponse.setRole(HiveMember.MemberRole.MODERATOR);
        when(hiveMembershipService.updateMemberRole(testHiveId, testUserId, HiveMember.MemberRole.MODERATOR, testUserId))
                .thenReturn(memberResponse);

        // Act & Assert
        mockMvc.perform(put("/api/hives/{hiveId}/members/{userId}/role", testHiveId, testUserId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MODERATOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MODERATOR"));

        verify(hiveMembershipService).updateMemberRole(testHiveId, testUserId, HiveMember.MemberRole.MODERATOR, testUserId);
    }

    @Test
    @WithMockUser
    void shouldSendInvitation() throws Exception {
        // Arrange
        InvitationRequest request = new InvitationRequest();
        request.setEmail("test@example.com");
        request.setMessage("Join our study group!");

        when(hiveMembershipService.sendInvitation(testHiveId, request, testUserId))
                .thenReturn(invitationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/hives/{hiveId}/invitations", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationResponse.getId()))
                .andExpect(jsonPath("$.hiveId").value(testHiveId))
                .andExpect(jsonPath("$.invitedEmail").value("test@example.com"))
                .andExpect(jsonPath("$.invitationCode").value(invitationCode));

        verify(hiveMembershipService).sendInvitation(testHiveId, request, testUserId);
    }

    @Test
    @WithMockUser
    void shouldAcceptInvitation() throws Exception {
        // Arrange
        when(hiveMembershipService.acceptInvitation(invitationCode, testUserId))
                .thenReturn(memberResponse);

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{code}/accept", invitationCode)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberResponse.getId()))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        verify(hiveMembershipService).acceptInvitation(invitationCode, testUserId);
    }

    @Test
    @WithMockUser
    void shouldRejectInvitation() throws Exception {
        // Arrange
        doNothing().when(hiveMembershipService).rejectInvitation(invitationCode, testUserId);

        // Act & Assert
        mockMvc.perform(post("/api/invitations/{code}/reject", invitationCode)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation rejected successfully"));

        verify(hiveMembershipService).rejectInvitation(invitationCode, testUserId);
    }

    @Test
    @WithMockUser
    void shouldTransferOwnership() throws Exception {
        // Arrange
        String newOwnerId = UUID.randomUUID().toString();
        doNothing().when(hiveMembershipService).transferOwnership(testHiveId, testUserId, newOwnerId);

        // Act & Assert
        mockMvc.perform(post("/api/hives/{hiveId}/transfer-ownership", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOwnerId\":\"" + newOwnerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ownership transferred successfully"));

        verify(hiveMembershipService).transferOwnership(testHiveId, testUserId, newOwnerId);
    }

    @Test
    @WithMockUser
    void shouldRequireValidRoleWhenUpdating() throws Exception {
        // Act & Assert - invalid role
        mockMvc.perform(put("/api/hives/{hiveId}/members/{userId}/role", testHiveId, testUserId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"INVALID_ROLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldValidateInvitationRequest() throws Exception {
        // Act & Assert - missing email
        mockMvc.perform(post("/api/hives/{hiveId}/invitations", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // Act & Assert - invalid email format
        mockMvc.perform(post("/api/hives/{hiveId}/invitations", testHiveId)
                        .with(csrf())
                        .header("User-ID", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldGetMemberDetails() throws Exception {
        // Arrange
        when(hiveMembershipService.getMember(testHiveId, testUserId, testUserId))
                .thenReturn(memberResponse);

        // Act & Assert
        mockMvc.perform(get("/api/hives/{hiveId}/members/{userId}", testHiveId, testUserId)
                        .header("User-ID", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberResponse.getId()))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        verify(hiveMembershipService).getMember(testHiveId, testUserId, testUserId);
    }

    @Test
    @WithMockUser
    void shouldListUserInvitations() throws Exception {
        // Arrange
        List<InvitationResponse> invitations = List.of(invitationResponse);
        Page<InvitationResponse> invitationPage = new PageImpl<>(invitations);
        when(hiveMembershipService.getUserInvitations(testUserId, any(Pageable.class)))
                .thenReturn(invitationPage);

        // Act & Assert
        mockMvc.perform(get("/api/users/{userId}/invitations", testUserId)
                        .header("User-ID", testUserId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(invitationResponse.getId()))
                .andExpect(jsonPath("$.content[0].hiveId").value(testHiveId))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(hiveMembershipService).getUserInvitations(eq(testUserId), any(Pageable.class));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        // Act & Assert - no authentication
        mockMvc.perform(post("/api/hives/{hiveId}/join", testHiveId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}