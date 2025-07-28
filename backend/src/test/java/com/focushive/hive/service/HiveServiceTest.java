package com.focushive.hive.service;

import com.focushive.api.client.IdentityServiceClient;
import com.focushive.common.exception.BadRequestException;
import com.focushive.common.exception.ForbiddenException;
import com.focushive.common.exception.ResourceNotFoundException;
import com.focushive.hive.dto.CreateHiveRequest;
import com.focushive.hive.dto.HiveResponse;
import com.focushive.hive.dto.UpdateHiveRequest;
import com.focushive.hive.entity.Hive;
import com.focushive.hive.entity.HiveMember;
import com.focushive.hive.repository.HiveMemberRepository;
import com.focushive.hive.repository.HiveRepository;
import com.focushive.hive.service.impl.HiveServiceImpl;
import com.focushive.user.entity.User;
import com.focushive.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * Unit tests for HiveService implementation.
 */
@ExtendWith(MockitoExtension.class)
class HiveServiceTest {
    
    @Mock
    private HiveRepository hiveRepository;
    
    @Mock
    private HiveMemberRepository hiveMemberRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private IdentityServiceClient identityServiceClient;
    
    @InjectMocks
    private HiveServiceImpl hiveService;
    
    private User testUser;
    private Hive testHive;
    private HiveMember testMember;
    private CreateHiveRequest createRequest;
    
    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        
        // Set up test hive
        testHive = new Hive();
        testHive.setId(UUID.randomUUID().toString());
        testHive.setName("Test Hive");
        testHive.setSlug("test-hive");
        testHive.setDescription("A test hive");
        testHive.setOwner(testUser);
        testHive.setMaxMembers(10);
        testHive.setIsPublic(true);
        testHive.setIsActive(true);
        testHive.setType(Hive.HiveType.STUDY);
        testHive.setMemberCount(1);
        
        // Set up test member
        testMember = new HiveMember();
        testMember.setId(UUID.randomUUID().toString());
        testMember.setHive(testHive);
        testMember.setUser(testUser);
        testMember.setRole(HiveMember.MemberRole.OWNER);
        testMember.setJoinedAt(LocalDateTime.now());
        
        // Set up create request
        createRequest = new CreateHiveRequest();
        createRequest.setName("New Hive");
        createRequest.setDescription("A new hive");
        createRequest.setMaxMembers(20);
        createRequest.setIsPublic(true);
        createRequest.setType(Hive.HiveType.WORK);
    }
    
    @Test
    void createHive_Success() {
        // Arrange
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(hiveRepository.existsBySlug(anyString())).thenReturn(false);
        when(hiveRepository.save(any(Hive.class))).thenAnswer(invocation -> {
            Hive hive = invocation.getArgument(0);
            hive.setId(UUID.randomUUID().toString());
            return hive;
        });
        when(hiveMemberRepository.save(any(HiveMember.class))).thenReturn(testMember);
        
        // Act
        HiveResponse response = hiveService.createHive(createRequest, testUser.getId());
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("New Hive");
        assertThat(response.getDescription()).isEqualTo("A new hive");
        assertThat(response.getMaxMembers()).isEqualTo(20);
        assertThat(response.getIsPublic()).isTrue();
        assertThat(response.getType()).isEqualTo(Hive.HiveType.WORK);
        assertThat(response.getCurrentMembers()).isEqualTo(1);
        
        verify(hiveRepository).save(any(Hive.class));
        verify(hiveMemberRepository).save(any(HiveMember.class));
    }
    
    @Test
    void createHive_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.createHive(createRequest, "invalid-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }
    
    @Test
    void getHive_PublicHive_Success() {
        // Arrange
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(5L);
        
        // Act
        HiveResponse response = hiveService.getHive(testHive.getId(), "any-user-id");
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testHive.getId());
        assertThat(response.getName()).isEqualTo(testHive.getName());
        assertThat(response.getCurrentMembers()).isEqualTo(5);
    }
    
    @Test
    void getHive_PrivateHive_NonMember_ThrowsForbidden() {
        // Arrange
        testHive.setIsPublic(false);
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), "non-member-id")).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.getHive(testHive.getId(), "non-member-id"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You don't have permission to view this private hive");
    }
    
    @Test
    void getHive_NotFound_ThrowsException() {
        // Arrange
        when(hiveRepository.findByIdAndActive(anyString())).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.getHive("invalid-id", testUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Hive not found or inactive");
    }
    
    @Test
    void updateHive_AsOwner_Success() {
        // Arrange
        UpdateHiveRequest updateRequest = new UpdateHiveRequest();
        updateRequest.setName("Updated Hive");
        updateRequest.setDescription("Updated description");
        updateRequest.setMaxMembers(30);
        
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(1L);
        when(hiveRepository.save(any(Hive.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        HiveResponse response = hiveService.updateHive(testHive.getId(), updateRequest, testUser.getId());
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Hive");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.getMaxMembers()).isEqualTo(30);
        
        verify(hiveRepository).save(any(Hive.class));
    }
    
    @Test
    void updateHive_AsMember_ThrowsForbidden() {
        // Arrange
        UpdateHiveRequest updateRequest = new UpdateHiveRequest();
        updateRequest.setName("Updated Hive");
        
        testMember.setRole(HiveMember.MemberRole.MEMBER);
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.updateHive(testHive.getId(), updateRequest, testUser.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only hive owner or moderators can update hive settings");
    }
    
    @Test
    void updateHive_MaxMembersLessThanCurrent_ThrowsBadRequest() {
        // Arrange
        UpdateHiveRequest updateRequest = new UpdateHiveRequest();
        updateRequest.setMaxMembers(2);
        
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(5L);
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.updateHive(testHive.getId(), updateRequest, testUser.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot set max members to 2, hive currently has 5 members");
    }
    
    @Test
    void deleteHive_AsOwner_Success() {
        // Arrange
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveRepository.save(any(Hive.class))).thenReturn(testHive);
        
        // Act
        hiveService.deleteHive(testHive.getId(), testUser.getId());
        
        // Assert
        verify(hiveRepository).save(argThat(hive -> 
            hive.getDeletedAt() != null && !hive.getIsActive()
        ));
    }
    
    @Test
    void deleteHive_NotOwner_ThrowsForbidden() {
        // Arrange
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.deleteHive(testHive.getId(), "other-user-id"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only the hive owner can delete the hive");
    }
    
    @Test
    void joinHive_Success() {
        // Arrange
        User newUser = new User();
        newUser.setId(UUID.randomUUID().toString());
        newUser.setUsername("newuser");
        
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), newUser.getId())).thenReturn(false);
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(5L);
        when(userRepository.findById(newUser.getId())).thenReturn(Optional.of(newUser));
        when(hiveMemberRepository.save(any(HiveMember.class))).thenAnswer(invocation -> {
            HiveMember member = invocation.getArgument(0);
            member.setId(UUID.randomUUID().toString());
            return member;
        });
        
        // Act
        HiveMember result = hiveService.joinHive(testHive.getId(), newUser.getId());
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(newUser);
        assertThat(result.getHive()).isEqualTo(testHive);
        assertThat(result.getRole()).isEqualTo(HiveMember.MemberRole.MEMBER);
        
        verify(hiveMemberRepository).save(any(HiveMember.class));
    }
    
    @Test
    void joinHive_AlreadyMember_ThrowsBadRequest() {
        // Arrange
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), testUser.getId())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.joinHive(testHive.getId(), testUser.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You are already a member of this hive");
    }
    
    @Test
    void joinHive_HiveFull_ThrowsBadRequest() {
        // Arrange
        testHive.setMaxMembers(5);
        when(hiveRepository.findByIdAndActive(testHive.getId())).thenReturn(Optional.of(testHive));
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), "new-user-id")).thenReturn(false);
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(5L);
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.joinHive(testHive.getId(), "new-user-id"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("This hive is full");
    }
    
    @Test
    void leaveHive_AsMember_Success() {
        // Arrange
        testMember.setRole(HiveMember.MemberRole.MEMBER);
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        
        // Act
        hiveService.leaveHive(testHive.getId(), testUser.getId());
        
        // Assert
        verify(hiveMemberRepository).delete(testMember);
    }
    
    @Test
    void leaveHive_AsOwnerWithOtherMembers_ThrowsBadRequest() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(5L);
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.leaveHive(testHive.getId(), testUser.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("As the owner, you cannot leave the hive while other members are present");
    }
    
    @Test
    void leaveHive_NotMember_ThrowsNotFound() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), "non-member-id"))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> hiveService.leaveHive(testHive.getId(), "non-member-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("You are not a member of this hive");
    }
    
    @Test
    void listPublicHives_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> hivePage = new PageImpl<>(List.of(testHive));
        when(hiveRepository.findPublicHives(pageable)).thenReturn(hivePage);
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(5L);
        
        // Act
        Page<HiveResponse> result = hiveService.listPublicHives(pageable);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(testHive.getId());
        assertThat(result.getContent().get(0).getCurrentMembers()).isEqualTo(5);
    }
    
    @Test
    void searchHives_Success() {
        // Arrange
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Hive> hivePage = new PageImpl<>(List.of(testHive));
        when(hiveRepository.searchPublicHives(query, pageable)).thenReturn(hivePage);
        when(hiveMemberRepository.countByHiveId(testHive.getId())).thenReturn(3L);
        
        // Act
        Page<HiveResponse> result = hiveService.searchHives(query, pageable);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Hive");
        assertThat(result.getContent().get(0).getCurrentMembers()).isEqualTo(3);
    }
    
    @Test
    void isMember_True() {
        // Arrange
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), testUser.getId())).thenReturn(true);
        
        // Act
        boolean result = hiveService.isMember(testHive.getId(), testUser.getId());
        
        // Assert
        assertThat(result).isTrue();
    }
    
    @Test
    void isMember_False() {
        // Arrange
        when(hiveMemberRepository.existsByHiveIdAndUserId(testHive.getId(), "non-member-id")).thenReturn(false);
        
        // Act
        boolean result = hiveService.isMember(testHive.getId(), "non-member-id");
        
        // Assert
        assertThat(result).isFalse();
    }
    
    @Test
    void getUserRole_Found() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), testUser.getId()))
                .thenReturn(Optional.of(testMember));
        
        // Act
        HiveMember.MemberRole role = hiveService.getUserRole(testHive.getId(), testUser.getId());
        
        // Assert
        assertThat(role).isEqualTo(HiveMember.MemberRole.OWNER);
    }
    
    @Test
    void getUserRole_NotFound() {
        // Arrange
        when(hiveMemberRepository.findByHiveIdAndUserId(testHive.getId(), "non-member-id"))
                .thenReturn(Optional.empty());
        
        // Act
        HiveMember.MemberRole role = hiveService.getUserRole(testHive.getId(), "non-member-id");
        
        // Assert
        assertThat(role).isNull();
    }
}