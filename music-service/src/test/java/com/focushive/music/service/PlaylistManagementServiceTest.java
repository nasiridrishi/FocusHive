package com.focushive.music.service;

import com.focushive.music.client.HiveServiceClient;
import com.focushive.music.client.UserServiceClient;
import com.focushive.music.dto.PlaylistDTO;
import com.focushive.music.exception.MusicServiceException;
import com.focushive.music.model.Playlist;
import com.focushive.music.model.PlaylistCollaborator;
import com.focushive.music.model.PlaylistTrack;
import com.focushive.music.repository.PlaylistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for PlaylistManagementService.
 * 
 * Following TDD principles - tests written before implementation.
 * Tests cover all playlist operations including CRUD, track management,
 * smart playlists, collaboration, and caching.
 * 
 * @author FocusHive Development Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlaylistManagementService Tests")
class PlaylistManagementServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;
    
    @Mock
    private HiveServiceClient hiveServiceClient;
    
    @Mock
    private UserServiceClient userServiceClient;
    
    @Mock
    private SpotifyIntegrationService spotifyIntegrationService;
    
    @Mock
    private CacheManager cacheManager;
    
    @InjectMocks
    private PlaylistManagementService playlistManagementService;

    private String userId;
    private String hiveId;
    private PlaylistDTO.CreateRequest createRequest;
    private Playlist samplePlaylist;

    @BeforeEach
    void setUp() {
        userId = "user123";
        hiveId = "hive456";
        
        createRequest = PlaylistDTO.CreateRequest.builder()
            .name("Test Playlist")
            .description("A test playlist")
            .isPublic(false)
            .isCollaborative(false)
            .build();
            
        samplePlaylist = Playlist.builder()
            .id(1L)
            .name("Test Playlist")
            .description("A test playlist")
            .createdBy(userId)
            .isPublic(false)
            .isCollaborative(false)
            .totalTracks(0)
            .totalDurationMs(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .tracks(new ArrayList<>())
            .collaborators(new ArrayList<>())
            .build();
    }

    @Nested
    @DisplayName("Playlist CRUD Operations")
    class PlaylistCrudOperations {

        @Test
        @DisplayName("Should create personal playlist successfully")
        void shouldCreatePersonalPlaylistSuccessfully() {
            // Given
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.Response result = playlistManagementService.createPlaylist(createRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Playlist");
            assertThat(result.getCreatedBy()).isEqualTo(userId);
            assertThat(result.getIsPublic()).isFalse();
            assertThat(result.getIsCollaborative()).isFalse();
            
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should create hive playlist when hiveId provided")
        void shouldCreateHivePlaylistWhenHiveIdProvided() {
            // Given
            createRequest.setHiveId(hiveId);
            samplePlaylist.setHiveId(hiveId);
            
            when(hiveServiceClient.verifyHiveMembership(hiveId, userId)).thenReturn(true);
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.Response result = playlistManagementService.createPlaylist(createRequest, userId);
            
            // Then
            assertThat(result.getHiveId()).isEqualTo(hiveId);
            verify(hiveServiceClient).verifyHiveMembership(hiveId, userId);
        }

        @Test
        @DisplayName("Should throw exception when user not member of hive")
        void shouldThrowExceptionWhenUserNotMemberOfHive() {
            // Given
            createRequest.setHiveId(hiveId);
            when(hiveServiceClient.verifyHiveMembership(hiveId, userId)).thenReturn(false);
            
            // When & Then
            assertThatThrownBy(() -> playlistManagementService.createPlaylist(createRequest, userId))
                .isInstanceOf(MusicServiceException.UnauthorizedOperationException.class)
                .hasMessageContaining("not a member of hive");
        }

        @Test
        @DisplayName("Should throw exception when playlist name is blank")
        void shouldThrowExceptionWhenPlaylistNameIsBlank() {
            // Given
            createRequest.setName("");
            
            // When & Then
            assertThatThrownBy(() -> playlistManagementService.createPlaylist(createRequest, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("Should get playlist by id successfully")
        void shouldGetPlaylistByIdSuccessfully() {
            // Given
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When
            PlaylistDTO.Response result = playlistManagementService.getPlaylistById(1L, userId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Playlist");
        }

        @Test
        @DisplayName("Should throw exception when playlist not found")
        void shouldThrowExceptionWhenPlaylistNotFound() {
            // Given
            when(playlistRepository.findById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> playlistManagementService.getPlaylistById(999L, userId))
                .isInstanceOf(MusicServiceException.ResourceNotFoundException.class)
                .hasMessageContaining("Playlist not found");
        }

        @Test
        @DisplayName("Should update playlist metadata successfully")
        void shouldUpdatePlaylistMetadataSuccessfully() {
            // Given
            PlaylistDTO.UpdateRequest updateRequest = PlaylistDTO.UpdateRequest.builder()
                .name("Updated Playlist")
                .description("Updated description")
                .isPublic(true)
                .build();
                
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.Response result = playlistManagementService.updatePlaylist(1L, updateRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should throw exception when user not owner of playlist")
        void shouldThrowExceptionWhenUserNotOwnerOfPlaylist() {
            // Given
            PlaylistDTO.UpdateRequest updateRequest = PlaylistDTO.UpdateRequest.builder()
                .name("Updated Playlist")
                .build();
                
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When & Then
            assertThatThrownBy(() -> playlistManagementService.updatePlaylist(1L, updateRequest, "other-user"))
                .isInstanceOf(MusicServiceException.UnauthorizedOperationException.class)
                .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("Should delete playlist successfully")
        void shouldDeletePlaylistSuccessfully() {
            // Given
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When
            playlistManagementService.deletePlaylist(1L, userId);
            
            // Then
            verify(playlistRepository).delete(samplePlaylist);
        }

        @Test
        @DisplayName("Should get user playlists with pagination")
        void shouldGetUserPlaylistsWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Playlist> playlistPage = new PageImpl<>(List.of(samplePlaylist));
            
            when(playlistRepository.findByCreatedByOrderByUpdatedAtDesc(userId, pageable))
                .thenReturn(playlistPage);
            
            // When
            PlaylistDTO.SearchResult result = playlistManagementService.getUserPlaylists(userId, pageable);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPlaylists()).hasSize(1);
            assertThat(result.getTotalResults()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Track Management")
    class TrackManagement {

        @Test
        @DisplayName("Should add track to playlist successfully")
        void shouldAddTrackToPlaylistSuccessfully() {
            // Given
            PlaylistDTO.AddTrackRequest addTrackRequest = PlaylistDTO.AddTrackRequest.builder()
                .spotifyTrackId("track123")
                .position(0)
                .build();
                
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.TrackInfo result = playlistManagementService.addTrackToPlaylist(1L, addTrackRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSpotifyTrackId()).isEqualTo("track123");
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should throw exception when adding duplicate track")
        void shouldThrowExceptionWhenAddingDuplicateTrack() {
            // Given
            PlaylistDTO.AddTrackRequest addTrackRequest = PlaylistDTO.AddTrackRequest.builder()
                .spotifyTrackId("track123")
                .build();
                
            PlaylistTrack existingTrack = new PlaylistTrack();
            existingTrack.setSpotifyTrackId("track123");
            samplePlaylist.getTracks().add(existingTrack);
            
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When & Then
            assertThatThrownBy(() -> playlistManagementService.addTrackToPlaylist(1L, addTrackRequest, userId))
                .isInstanceOf(MusicServiceException.BusinessRuleViolationException.class)
                .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should remove track from playlist successfully")
        void shouldRemoveTrackFromPlaylistSuccessfully() {
            // Given
            PlaylistTrack trackToRemove = new PlaylistTrack();
            trackToRemove.setId(1L);
            trackToRemove.setSpotifyTrackId("track123");
            trackToRemove.setPositionInPlaylist(0);
            
            samplePlaylist.getTracks().add(trackToRemove);
            
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            playlistManagementService.removeTrackFromPlaylist(1L, 1L, userId);
            
            // Then
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should reorder tracks in playlist successfully")
        void shouldReorderTracksInPlaylistSuccessfully() {
            // Given
            PlaylistDTO.TrackReorderRequest reorderRequest = PlaylistDTO.TrackReorderRequest.builder()
                .trackOrders(Map.of(1L, 1, 2L, 0))
                .build();
                
            PlaylistTrack track1 = new PlaylistTrack();
            track1.setId(1L);
            track1.setPositionInPlaylist(0);
            
            PlaylistTrack track2 = new PlaylistTrack();
            track2.setId(2L);
            track2.setPositionInPlaylist(1);
            
            samplePlaylist.getTracks().addAll(List.of(track1, track2));
            
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            playlistManagementService.reorderPlaylistTracks(1L, reorderRequest, userId);
            
            // Then
            verify(playlistRepository).save(any(Playlist.class));
        }
    }

    @Nested
    @DisplayName("Smart Playlist Features")
    class SmartPlaylistFeatures {

        @Test
        @DisplayName("Should create smart playlist with energy criteria")
        void shouldCreateSmartPlaylistWithEnergyCriteria() {
            // Given
            PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest = 
                PlaylistDTO.SmartPlaylistCriteriaRequest.builder()
                    .name("High Energy Tracks")
                    .energyLevel("HIGH")
                    .minDurationMs(180000) // 3 minutes
                    .maxDurationMs(300000) // 5 minutes
                    .genres(List.of("electronic", "rock"))
                    .maxTracks(50)
                    .build();
                    
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.Response result = playlistManagementService.createSmartPlaylist(criteriaRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should create productivity-based smart playlist")
        void shouldCreateProductivityBasedSmartPlaylist() {
            // Given
            PlaylistDTO.SmartPlaylistCriteriaRequest criteriaRequest = 
                PlaylistDTO.SmartPlaylistCriteriaRequest.builder()
                    .name("Deep Work Focus")
                    .taskType("DEEP_WORK")
                    .instrumentalOnly(true)
                    .minProductivityScore(80.0)
                    .timeOfDay("MORNING")
                    .maxTracks(30)
                    .build();
                    
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.Response result = playlistManagementService.createSmartPlaylist(criteriaRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should update smart playlist based on criteria")
        void shouldUpdateSmartPlaylistBasedOnCriteria() {
            // Given
            samplePlaylist.setSmartPlaylist(true);
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            playlistManagementService.refreshSmartPlaylist(1L, userId);
            
            // Then
            verify(playlistRepository).save(any(Playlist.class));
        }
    }

    @Nested
    @DisplayName("Collaborative Features")
    class CollaborativeFeatures {

        @Test
        @DisplayName("Should share playlist with hive members")
        void shouldSharePlaylistWithHiveMembers() {
            // Given
            PlaylistDTO.SharePlaylistRequest shareRequest = PlaylistDTO.SharePlaylistRequest.builder()
                .hiveId(hiveId)
                .permissionLevel("VIEWER")
                .message("Check out this playlist!")
                .build();
                
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(hiveServiceClient.getHiveMembers(hiveId)).thenReturn(List.of("user1", "user2"));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            playlistManagementService.sharePlaylistWithHive(1L, shareRequest, userId);
            
            // Then
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should add collaborator to playlist")
        void shouldAddCollaboratorToPlaylist() {
            // Given
            PlaylistDTO.AddCollaboratorRequest collaboratorRequest = 
                PlaylistDTO.AddCollaboratorRequest.builder()
                    .userId("user456")
                    .permissionLevel("EDITOR")
                    .canAddTracks(true)
                    .canRemoveTracks(false)
                    .build();
                    
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(userServiceClient.verifyUserExists("user456")).thenReturn(true);
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.CollaboratorInfo result = 
                playlistManagementService.addCollaborator(1L, collaboratorRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user456");
            assertThat(result.getPermissionLevel()).isEqualTo("EDITOR");
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should get hive playlists")
        void shouldGetHivePlaylists() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Playlist> playlistPage = new PageImpl<>(List.of(samplePlaylist));
            
            when(hiveServiceClient.verifyHiveMembership(hiveId, userId)).thenReturn(true);
            when(playlistRepository.findByHiveIdOrderByUpdatedAtDesc(hiveId, pageable))
                .thenReturn(playlistPage);
            
            // When
            PlaylistDTO.SearchResult result = playlistManagementService.getHivePlaylists(hiveId, userId, pageable);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPlaylists()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Import/Export Features")
    class ImportExportFeatures {

        @Test
        @DisplayName("Should export playlist successfully")
        void shouldExportPlaylistSuccessfully() {
            // Given
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When
            PlaylistDTO.ExportResponse result = playlistManagementService.exportPlaylist(1L, userId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getPlaylistName()).isEqualTo("Test Playlist");
        }

        @Test
        @DisplayName("Should import playlist from Spotify")
        void shouldImportPlaylistFromSpotify() {
            // Given
            PlaylistDTO.ImportRequest importRequest = PlaylistDTO.ImportRequest.builder()
                .source("SPOTIFY")
                .externalPlaylistId("spotify123")
                .importTracks(true)
                .newPlaylistName("Imported Playlist")
                .build();
                
            when(spotifyIntegrationService.getPlaylistInfo("spotify123")).thenReturn(mock(Object.class));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.Response result = playlistManagementService.importPlaylist(importRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            verify(playlistRepository).save(any(Playlist.class));
        }

        @Test
        @DisplayName("Should duplicate playlist successfully")
        void shouldDuplicatePlaylistSuccessfully() {
            // Given
            PlaylistDTO.DuplicateRequest duplicateRequest = PlaylistDTO.DuplicateRequest.builder()
                .newName("Copy of Test Playlist")
                .includeCollaborators(false)
                .build();
                
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);
            
            // When
            PlaylistDTO.Response result = playlistManagementService.duplicatePlaylist(1L, duplicateRequest, userId);
            
            // Then
            assertThat(result).isNotNull();
            verify(playlistRepository).save(any(Playlist.class));
        }
    }

    @Nested
    @DisplayName("Caching Tests")
    class CachingTests {

        @Test
        @DisplayName("Should cache popular hive playlists")
        void shouldCachePopularHivePlaylists() {
            // Given
            when(playlistRepository.findMostPopularPublicPlaylists(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(samplePlaylist)));
            
            // When
            playlistManagementService.getPopularPlaylists(PageRequest.of(0, 10));
            
            // Then
            verify(playlistRepository).findMostPopularPublicPlaylists(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Permission System")
    class PermissionSystem {

        @Test
        @DisplayName("Should check user can modify playlist")
        void shouldCheckUserCanModifyPlaylist() {
            // Given
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When
            boolean canModify = playlistManagementService.canUserModifyPlaylist(1L, userId);
            
            // Then
            assertThat(canModify).isTrue(); // Owner can modify
        }

        @Test
        @DisplayName("Should check collaborator can modify playlist")
        void shouldCheckCollaboratorCanModifyPlaylist() {
            // Given
            PlaylistCollaborator collaborator = new PlaylistCollaborator();
            collaborator.setUserId("collaborator123");
            collaborator.setCanAddTracks(true);
            
            samplePlaylist.getCollaborators().add(collaborator);
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When
            boolean canModify = playlistManagementService.canUserModifyPlaylist(1L, "collaborator123");
            
            // Then
            assertThat(canModify).isTrue(); // Collaborator with edit permissions can modify
        }

        @Test
        @DisplayName("Should deny access to unauthorized user")
        void shouldDenyAccessToUnauthorizedUser() {
            // Given
            when(playlistRepository.findById(1L)).thenReturn(Optional.of(samplePlaylist));
            
            // When
            boolean canModify = playlistManagementService.canUserModifyPlaylist(1L, "unauthorized-user");
            
            // Then
            assertThat(canModify).isFalse(); // Unauthorized user cannot modify
        }
    }
}